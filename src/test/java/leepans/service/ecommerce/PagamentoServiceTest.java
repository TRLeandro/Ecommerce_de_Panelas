package leepans.service.ecommerce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import leepans.dto.pagamento.IniciarPagamentoDTO;
import leepans.exception.BusinessRuleViolationException;
import leepans.exception.ResourceNotFoundException;
import leepans.exception.ValidationException;
import leepans.model.CupomDesconto;
import leepans.model.EnderecoEntrega;
import leepans.model.ItemPedido;
import leepans.model.ListaDesejo;
import leepans.model.MetodoPagamento;
import leepans.model.Pagamento;
import leepans.model.Panela;
import leepans.model.Pedido;
import leepans.model.Perfil;
import leepans.model.StatusPagamento;
import leepans.model.StatusPedido;
import leepans.model.TipoTransacao;
import leepans.model.TransacaoGateway;
import leepans.model.Usuario;
import leepans.repository.CupomDescontoRepository;
import leepans.repository.EventoWebhookRepository;
import leepans.repository.ListaDesejoRepository;
import leepans.repository.PagamentoRepository;
import leepans.repository.PanelaRepository;
import leepans.repository.PedidoRepository;
import leepans.repository.TransacaoGatewayRepository;
import leepans.repository.UsuarioRepository;
import leepans.service.pagamento.gateway.EventoGateway;
import leepans.service.pagamento.gateway.GatewayFake;
import leepans.service.pagamento.gateway.GatewayIndisponivelException;
import leepans.service.pagamento.gateway.ResultadoEstorno;
import leepans.service.pagamento.gateway.StatusGateway;

/** Usa o banco H2 e o GatewayFake reais; o spy só conta chamadas e simula falhas pontuais. */
@QuarkusTest
class PagamentoServiceTest {

    private static final BigDecimal TOTAL = new BigDecimal("200.00");

    @Inject
    PagamentoService service;

    @Inject
    PedidoService pedidoService;

    @InjectSpy
    GatewayFake gateway;

    @Inject
    PagamentoRepository pagamentoRepository;

    @Inject
    PedidoRepository pedidoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    PanelaRepository panelaRepository;

    @Inject
    CupomDescontoRepository cupomRepository;

    @Inject
    ListaDesejoRepository listaDesejoRepository;

    @Inject
    TransacaoGatewayRepository transacaoRepository;

    @Inject
    EventoWebhookRepository eventoRepository;

    @Inject
    MockMailbox mailbox;

    private String login;
    private Long panelaId;

    @BeforeEach
    void criarCliente() {
        mailbox.clear();
        login = "cliente-" + UUID.randomUUID();
        panelaId = emTransacao(() -> {
            Usuario usuario = new Usuario();
            usuario.setLogin(login);
            usuario.setNome("Cliente");
            usuario.setSenhaHash("hash");
            usuario.setPerfil(Perfil.CLIENTE);
            usuarioRepository.persist(usuario);

            Panela panela = new Panela();
            panela.setModelo("Teste");
            panela.setPreco(new BigDecimal("100.00"));
            panelaRepository.persist(panela);
            return panela.getId();
        });
    }

    @Test
    void cartaoAprovado_pagaPagamentoEPedido() {
        Long pedidoId = novoPedido();

        Pagamento pagamento = service.iniciar(pedidoId, login, cartao("tok_ok", 3), chave());

        assertEquals(StatusPagamento.PAGO, pagamento.getStatus());
        assertEquals(0, TOTAL.compareTo(pagamento.getValor()));
        assertNotNull(pagamento.getPagoEm());
        assertEquals("4242", pagamento.getCartao().getUltimos4Digitos());
        assertEquals(3, pagamento.getCartao().getParcelas());
        assertEquals(StatusPedido.PAGO, statusDoPedido(pedidoId));
        assertEquals(List.of(TipoTransacao.CRIACAO), tiposDeTransacao(pagamento.getId()));
    }

    @Test
    void cartaoAprovado_enviaEmailDepoisDoCommit() {
        service.iniciar(novoPedido(), login, cartao("tok_ok", 1), chave());

        assertEquals(1, mailbox.getTotalMessagesSent());
        assertEquals("Pagamento Aprovado - LeePans", mailbox.getMailsSentTo("leandrotavares@unitins.br").get(0).getSubject());
    }

    @Test
    void cartaoAprovado_removeItensDaListaDeDesejos() {
        Long listaId = emTransacao(() -> {
            ListaDesejo lista = new ListaDesejo();
            lista.setUsuario(usuarioRepository.findByLogin(login).firstResult());
            lista.setProdutos(new ArrayList<>(List.of(panelaRepository.findById(panelaId))));
            listaDesejoRepository.persist(lista);
            return lista.getId();
        });

        service.iniciar(novoPedido(), login, cartao("tok_ok", 1), chave());

        assertTrue(emTransacao(() -> listaDesejoRepository.findById(listaId).getProdutos().isEmpty()));
    }

    @Test
    void cartaoAprovado_desativaCupomEsgotado() {
        Long cupomId = novoCupom(0);
        Long pedidoId = novoPedido(cupomId);

        service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave());

        assertFalse(emTransacao(() -> cupomRepository.findById(cupomId).isAtivo()));
    }

    @Test
    void cartaoRecusado_permiteNovaTentativa() {
        Long pedidoId = novoPedido();

        Pagamento recusado = service.iniciar(pedidoId, login, cartao("tok_recusado", 1), chave());

        assertEquals(StatusPagamento.RECUSADO, recusado.getStatus());
        assertEquals("Saldo insuficiente.", recusado.getMotivoRecusa());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, statusDoPedido(pedidoId));
        assertEquals("Pagamento Recusado - LeePans", mailbox.getMailsSentTo("leandrotavares@unitins.br").get(0).getSubject());

        Pagamento aprovado = service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave());

        assertEquals(StatusPagamento.PAGO, aprovado.getStatus());
        assertEquals(StatusPedido.PAGO, statusDoPedido(pedidoId));
    }

    @Test
    void gatewayIndisponivel_mantemPendenteERegistraOErro() {
        Long pedidoId = novoPedido();
        String chave = chave();

        assertThrows(GatewayIndisponivelException.class,
                () -> service.iniciar(pedidoId, login, cartao("tok_erro", 1), chave));

        Pagamento pagamento = emTransacao(() -> pagamentoRepository.findByIdempotencyKey(chave));
        assertEquals(StatusPagamento.PENDENTE, pagamento.getStatus());
        List<TransacaoGateway> transacoes = emTransacao(() -> transacaoRepository.findByPagamentoId(pagamento.getId()));
        assertEquals(1, transacoes.size());
        assertNull(transacoes.get(0).getStatusGateway());
        assertTrue(transacoes.get(0).getMensagem().contains("indisponível"));
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, statusDoPedido(pedidoId));
    }

    @Test
    void gatewayIndisponivel_novaChaveSubstituiOPendente() {
        Long pedidoId = novoPedido();
        String chave = chave();
        assertThrows(GatewayIndisponivelException.class,
                () -> service.iniciar(pedidoId, login, cartao("tok_erro", 1), chave));
        clearInvocations(gateway);

        Pagamento novo = service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave());

        assertEquals(StatusPagamento.PAGO, novo.getStatus());
        assertEquals(StatusPagamento.CANCELADO,
                emTransacao(() -> pagamentoRepository.findByIdempotencyKey(chave).getStatus()));
        verify(gateway, never()).cancelar(anyString());
    }

    @Test
    void tokenDeCartaoNaoEPersistido() {
        Pagamento pagamento = service.iniciar(novoPedido(), login, cartao("tok_ok_segredo", 1), chave());

        for (TransacaoGateway transacao : emTransacao(() -> transacaoRepository.findByPagamentoId(pagamento.getId()))) {
            assertFalse(transacao.getPayloadBruto().contains("tok_ok_segredo"));
        }
    }

    @Test
    void pix_aguardaEWebhookConfirma() {
        Long pedidoId = novoPedido();

        Pagamento pix = service.iniciar(pedidoId, login, pix(), chave());

        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, pix.getStatus());
        assertNotNull(pix.getPix().getQrCodeCopiaECola());
        assertNotNull(pix.getExpiraEm());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, statusDoPedido(pedidoId));

        service.processarEventoGateway(gateway.simularPagamento(pix.getGatewayPaymentId()), GatewayFake.NOME);

        assertEquals(StatusPagamento.PAGO, statusDoPagamento(pix.getId()));
        assertEquals(StatusPedido.PAGO, statusDoPedido(pedidoId));
        assertEquals(List.of(TipoTransacao.CRIACAO, TipoTransacao.WEBHOOK), tiposDeTransacao(pix.getId()));
    }

    @Test
    void boleto_aguardaComLinhaDigitavel() {
        Pagamento boleto = service.iniciar(novoPedido(), login, new IniciarPagamentoDTO(MetodoPagamento.BOLETO, null, 5), chave());

        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, boleto.getStatus());
        assertTrue(boleto.getBoleto().getLinhaDigitavel().matches("\\d{47}"));
    }

    @Test
    void webhookRepetido_processadoUmaUnicaVez() {
        Pagamento pix = service.iniciar(novoPedido(), login, pix(), chave());
        EventoGateway evento = gateway.simularPagamento(pix.getGatewayPaymentId());

        service.processarEventoGateway(evento, GatewayFake.NOME);
        mailbox.clear();
        clearInvocations(gateway);
        service.processarEventoGateway(evento, GatewayFake.NOME);

        verify(gateway, never()).consultar(anyString());
        assertEquals(List.of(TipoTransacao.CRIACAO, TipoTransacao.WEBHOOK), tiposDeTransacao(pix.getId()));
        assertEquals(0, mailbox.getTotalMessagesSent());
    }

    @Test
    void webhookEmParalelo_processadoUmaUnicaVez() throws Exception {
        Pagamento pix = service.iniciar(novoPedido(), login, pix(), chave());
        EventoGateway evento = gateway.simularPagamento(pix.getGatewayPaymentId());

        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Void>> chamadas = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                chamadas.add(() -> {
                    service.processarEventoGateway(evento, GatewayFake.NOME);
                    return null;
                });
            }
            for (Future<Void> resultado : executor.invokeAll(chamadas)) {
                resultado.get();
            }
        } finally {
            executor.shutdown();
        }

        assertEquals(List.of(TipoTransacao.CRIACAO, TipoTransacao.WEBHOOK), tiposDeTransacao(pix.getId()));
        assertEquals(StatusPagamento.PAGO, statusDoPagamento(pix.getId()));
    }

    @Test
    void webhook_usaAConsultaComoFonteDaVerdade() {
        Pagamento pix = service.iniciar(novoPedido(), login, pix(), chave());
        EventoGateway mentiroso = new EventoGateway("evt_" + UUID.randomUUID(), pix.getGatewayPaymentId(),
                StatusGateway.PAGO, "{}");

        service.processarEventoGateway(mentiroso, GatewayFake.NOME);

        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, statusDoPagamento(pix.getId()));
    }

    @Test
    void webhookQueFalhou_eReprocessadoNoReenvio() {
        Pagamento pix = service.iniciar(novoPedido(), login, pix(), chave());
        EventoGateway evento = gateway.simularPagamento(pix.getGatewayPaymentId());
        doThrow(new GatewayIndisponivelException("fora do ar")).when(gateway).consultar(anyString());

        assertThrows(GatewayIndisponivelException.class,
                () -> service.processarEventoGateway(evento, GatewayFake.NOME));
        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, statusDoPagamento(pix.getId()));

        doReturn(gatewayReal(pix.getGatewayPaymentId())).when(gateway).consultar(anyString());
        service.processarEventoGateway(evento, GatewayFake.NOME);

        assertEquals(StatusPagamento.PAGO, statusDoPagamento(pix.getId()));
    }

    @Test
    void webhookDePagamentoDesconhecido_naoFicaMarcadoComoProcessado() {
        EventoGateway evento = new EventoGateway("evt_" + UUID.randomUUID(), "fake_desconhecido", StatusGateway.PAGO, "{}");

        assertThrows(ResourceNotFoundException.class, () -> service.processarEventoGateway(evento, GatewayFake.NOME));

        assertFalse(emTransacao(() -> eventoRepository.findByGatewayEEvento(GatewayFake.NOME, evento.eventoId()).isProcessado()));
    }

    @Test
    void webhookPagoDepoisDeExpirar_naoMudaNadaEEncerraOEvento() {
        Pagamento pix = service.iniciar(novoPedido(), login, pix(), chave());
        vencer(pix.getId());
        service.expirarPagamentosVencidos();
        EventoGateway evento = gateway.simularPagamento(pix.getGatewayPaymentId());

        service.processarEventoGateway(evento, GatewayFake.NOME);

        assertEquals(StatusPagamento.EXPIRADO, statusDoPagamento(pix.getId()));
        assertTrue(emTransacao(() -> eventoRepository.findByGatewayEEvento(GatewayFake.NOME, evento.eventoId()).isProcessado()));
    }

    @Test
    void mesmaChave_umUnicoPagamentoEUmaChamadaAoGateway() {
        Long pedidoId = novoPedido();
        String chave = chave();
        clearInvocations(gateway);

        Pagamento primeiro = service.iniciar(pedidoId, login, pix(), chave);
        Pagamento segundo = service.iniciar(pedidoId, login, pix(), chave);

        assertEquals(primeiro.getId(), segundo.getId());
        verify(gateway, times(1)).criarCobranca(any());
    }

    @Test
    void mesmaChave_devolveOPagamentoMesmoComPedidoJaPago() {
        Long pedidoId = novoPedido();
        String chave = chave();

        Pagamento primeiro = service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave);
        Pagamento segundo = service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave);

        assertEquals(primeiro.getId(), segundo.getId());
    }

    @Test
    void mesmaChaveEmOutroPedido_eRejeitada() {
        String chave = chave();
        service.iniciar(novoPedido(), login, pix(), chave);
        Long outroPedido = novoPedido();

        assertThrows(BusinessRuleViolationException.class, () -> service.iniciar(outroPedido, login, pix(), chave));
    }

    @Test
    void chaveDeOutroUsuario_naoDevolveOPagamentoDele() {
        Long pedidoId = novoPedido();
        String chave = chave();
        service.iniciar(pedidoId, login, pix(), chave);
        String dono = login;

        criarCliente();

        assertThrows(ResourceNotFoundException.class, () -> service.iniciar(pedidoId, login, pix(), chave));
        Long pedidoDoIntruso = novoPedido();
        assertThrows(BusinessRuleViolationException.class, () -> service.iniciar(pedidoDoIntruso, login, pix(), chave));
        assertNotEquals(dono, login);
    }

    @Test
    void novoPagamento_cancelaOAtivoNoGateway() {
        Long pedidoId = novoPedido();
        Pagamento primeiro = service.iniciar(pedidoId, login, pix(), chave());

        Pagamento segundo = service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave());

        assertEquals(StatusPagamento.CANCELADO, statusDoPagamento(primeiro.getId()));
        assertEquals(StatusPagamento.PAGO, segundo.getStatus());
        verify(gateway).cancelar(primeiro.getGatewayPaymentId());
        assertEquals(List.of(TipoTransacao.CRIACAO, TipoTransacao.CANCELAMENTO), tiposDeTransacao(primeiro.getId()));
    }

    @Test
    void requisicaoInvalida_naoCancelaOPagamentoAtivo() {
        Long pedidoId = novoPedido();
        Pagamento ativo = service.iniciar(pedidoId, login, pix(), chave());

        assertThrows(ValidationException.class, () -> service.iniciar(pedidoId, login, cartao(" ", 1), chave()));

        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, statusDoPagamento(ativo.getId()));
    }

    @Test
    void validacoesDeMetodo() {
        Long pedidoId = novoPedido();

        assertThrows(ValidationException.class, () -> service.iniciar(pedidoId, login, cartao(null, 1), chave()));
        assertThrows(ValidationException.class, () -> service.iniciar(pedidoId, login, cartao("tok_ok", 0), chave()));
        assertThrows(ValidationException.class, () -> service.iniciar(pedidoId, login, cartao("tok_ok", 13), chave()));
    }

    @Test
    void debitoEPix_sempreEmUmaParcela() {
        Pagamento debito = service.iniciar(novoPedido(), login,
                new IniciarPagamentoDTO(MetodoPagamento.CARTAO_DEBITO, "tok_ok", 6), chave());

        assertEquals(1, debito.getCartao().getParcelas());
    }

    @Test
    void pedidoDeOutroUsuario_retorna404() {
        Long pedidoId = novoPedido();

        assertThrows(ResourceNotFoundException.class, () -> service.iniciar(pedidoId, "outro", pix(), chave()));
    }

    @Test
    void pedidoJaPago_naoAceitaNovoPagamento() {
        Long pedidoId = novoPedido();
        service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave());

        assertThrows(BusinessRuleViolationException.class, () -> service.iniciar(pedidoId, login, pix(), chave()));
    }

    @Test
    void expiracao_mantemPedidoRecenteAguardando() {
        Long pedidoId = novoPedido();
        Pagamento pix = service.iniciar(pedidoId, login, pix(), chave());
        vencer(pix.getId());

        int processados = service.expirarPagamentosVencidos();

        assertTrue(processados >= 1);
        assertEquals(StatusPagamento.EXPIRADO, statusDoPagamento(pix.getId()));
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, statusDoPedido(pedidoId));
        assertEquals(List.of(TipoTransacao.CRIACAO, TipoTransacao.CONSULTA), tiposDeTransacao(pix.getId()));

        assertEquals(StatusPagamento.PAGO,
                service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave()).getStatus());
    }

    @Test
    void expiracao_confirmaSeOGatewayDizQueFoiPago() {
        Long pedidoId = novoPedido();
        Pagamento pix = service.iniciar(pedidoId, login, pix(), chave());
        vencer(pix.getId());
        gateway.simularPagamento(pix.getGatewayPaymentId());

        service.expirarPagamentosVencidos();

        assertEquals(StatusPagamento.PAGO, statusDoPagamento(pix.getId()));
        assertEquals(StatusPedido.PAGO, statusDoPedido(pedidoId));
    }

    @Test
    void expiracao_cancelaPedidoAntigoEDevolveCupom() {
        Long cupomId = novoCupom(4);
        Long pedidoId = novoPedido(cupomId);
        Pagamento pix = service.iniciar(pedidoId, login, pix(), chave());
        vencer(pix.getId());
        envelhecer(pedidoId, 25);

        service.expirarPagamentosVencidos();

        assertEquals(StatusPagamento.EXPIRADO, statusDoPagamento(pix.getId()));
        assertEquals(StatusPedido.CANCELADO, statusDoPedido(pedidoId));
        assertEquals(5, quantidadeDoCupom(cupomId));
    }

    @Test
    void expiracao_naoMexeEmPagamentoNoPrazo() {
        Long pedidoId = novoPedido();
        Pagamento pix = service.iniciar(pedidoId, login, pix(), chave());
        envelhecer(pedidoId, 25);

        service.expirarPagamentosVencidos();

        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, statusDoPagamento(pix.getId()));
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, statusDoPedido(pedidoId));
    }

    @Test
    void expiracao_gatewayForaDoAr_tentaDeNovoDepois() {
        Pagamento pix = service.iniciar(novoPedido(), login, pix(), chave());
        vencer(pix.getId());
        doThrow(new GatewayIndisponivelException("fora do ar")).when(gateway).consultar(pix.getGatewayPaymentId());

        service.expirarPagamentosVencidos();

        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, statusDoPagamento(pix.getId()));
    }

    @Test
    void pedidoAbandonadoSemPagamento_eCanceladoComCupomDevolvido() {
        Long cupomId = novoCupom(4);
        Long pedidoId = novoPedido(cupomId);
        envelhecer(pedidoId, 25);

        service.expirarPagamentosVencidos();

        assertEquals(StatusPedido.CANCELADO, statusDoPedido(pedidoId));
        assertEquals(5, quantidadeDoCupom(cupomId));
    }

    @Test
    void pedidoComPagamentoJaExpirado_eCanceladoQuandoPassaDoPrazo() {
        Long pedidoId = novoPedido();
        Pagamento pix = service.iniciar(pedidoId, login, pix(), chave());
        vencer(pix.getId());
        service.expirarPagamentosVencidos();
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, statusDoPedido(pedidoId));

        envelhecer(pedidoId, 25);
        service.expirarPagamentosVencidos();

        assertEquals(StatusPedido.CANCELADO, statusDoPedido(pedidoId));
    }

    @Test
    void estorno_devolveCupomECancelaPedido() {
        Long cupomId = novoCupom(4);
        Long pedidoId = novoPedido(cupomId);
        Pagamento pagamento = service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave());

        service.estornar(pagamento.getId());

        assertEquals(StatusPagamento.ESTORNADO, statusDoPagamento(pagamento.getId()));
        assertEquals(StatusPedido.CANCELADO, statusDoPedido(pedidoId));
        assertEquals(5, quantidadeDoCupom(cupomId));
        assertEquals(List.of(TipoTransacao.CRIACAO, TipoTransacao.ESTORNO), tiposDeTransacao(pagamento.getId()));
    }

    @Test
    void estorno_pedidoEnviado_naoChamaOGateway() {
        Long pedidoId = novoPedido();
        Pagamento pagamento = service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave());
        pedidoService.iniciarSeparacao(pedidoId);
        pedidoService.marcarComoEnviado(pedidoId);

        assertThrows(BusinessRuleViolationException.class, () -> service.estornar(pagamento.getId()));

        verify(gateway, never()).estornar(anyString(), any());
        assertEquals(StatusPagamento.PAGO, statusDoPagamento(pagamento.getId()));
    }

    @Test
    void estorno_negadoPeloGateway_mantemPagoERegistraATentativa() {
        Pagamento pagamento = service.iniciar(novoPedido(), login, cartao("tok_ok", 1), chave());
        doReturn(new ResultadoEstorno(false, "Prazo de estorno vencido.", "{}")).when(gateway).estornar(anyString(), any());

        assertThrows(BusinessRuleViolationException.class, () -> service.estornar(pagamento.getId()));

        assertEquals(StatusPagamento.PAGO, statusDoPagamento(pagamento.getId()));
        assertEquals(List.of(TipoTransacao.CRIACAO, TipoTransacao.ESTORNO), tiposDeTransacao(pagamento.getId()));
    }

    @Test
    void transicaoInvalida_lancaBusinessRuleViolation() {
        Pagamento pagamento = service.iniciar(novoPedido(), login, pix(), chave());

        assertThrows(BusinessRuleViolationException.class, () -> service.estornar(pagamento.getId()));
    }

    @Test
    void cancelarPedido_comPagamentoPago_disparaEstorno() {
        Long cupomId = novoCupom(4);
        Long pedidoId = novoPedido(cupomId);
        Pagamento pagamento = service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave());

        pedidoService.cancelar(pedidoId);

        assertEquals(StatusPagamento.ESTORNADO, statusDoPagamento(pagamento.getId()));
        assertEquals(StatusPedido.CANCELADO, statusDoPedido(pedidoId));
        assertEquals(5, quantidadeDoCupom(cupomId));
    }

    @Test
    void cancelarPedido_comPagamentoAtivo_cancelaNoGateway() {
        Long cupomId = novoCupom(4);
        Long pedidoId = novoPedido(cupomId);
        Pagamento pix = service.iniciar(pedidoId, login, pix(), chave());

        pedidoService.cancelar(pedidoId);

        assertEquals(StatusPagamento.CANCELADO, statusDoPagamento(pix.getId()));
        assertEquals(StatusPedido.CANCELADO, statusDoPedido(pedidoId));
        assertEquals(5, quantidadeDoCupom(cupomId));
        verify(gateway).cancelar(pix.getGatewayPaymentId());
    }

    @Test
    void cancelarPedido_enviado_eRejeitadoSemEstornar() {
        Long pedidoId = novoPedido();
        service.iniciar(pedidoId, login, cartao("tok_ok", 1), chave());
        pedidoService.iniciarSeparacao(pedidoId);
        pedidoService.marcarComoEnviado(pedidoId);

        assertThrows(BusinessRuleViolationException.class, () -> pedidoService.cancelar(pedidoId));

        verify(gateway, never()).estornar(anyString(), any());
    }

    @Test
    void simularPagamento_exigeCobrancaEmAberto() {
        Pagamento pago = service.iniciar(novoPedido(), login, cartao("tok_ok", 1), chave());

        assertThrows(BusinessRuleViolationException.class, () -> service.simularPagamento(pago.getId()));
    }

    @Test
    void removerPedidoComPagamento_eRejeitado() {
        Long pedidoId = novoPedido();
        service.iniciar(pedidoId, login, pix(), chave());

        assertThrows(BusinessRuleViolationException.class, () -> pedidoService.delete(pedidoId));
    }

    private static IniciarPagamentoDTO pix() {
        return new IniciarPagamentoDTO(MetodoPagamento.PIX, null, null);
    }

    private static IniciarPagamentoDTO cartao(String token, Integer parcelas) {
        return new IniciarPagamentoDTO(MetodoPagamento.CARTAO_CREDITO, token, parcelas);
    }

    private static String chave() {
        return UUID.randomUUID().toString();
    }

    private leepans.service.pagamento.gateway.ResultadoCobranca gatewayReal(String id) {
        return new leepans.service.pagamento.gateway.ResultadoCobranca(id, StatusGateway.PAGO, null, null, null, null,
                null, null, "{}");
    }

    private Long novoPedido() {
        return novoPedido(null);
    }

    private Long novoPedido(Long cupomId) {
        return emTransacao(() -> {
            ItemPedido item = new ItemPedido();
            item.setPanela(panelaRepository.findById(panelaId));
            item.setQuantidade(2);
            item.setValorUnitario(new BigDecimal("100.00"));

            Pedido pedido = new Pedido();
            pedido.setUsuario(usuarioRepository.findByLogin(login).firstResult());
            pedido.setItens(List.of(item));
            pedido.setValorItens(TOTAL);
            pedido.setValorDesconto(BigDecimal.ZERO);
            pedido.setEnderecoEntrega(new EnderecoEntrega("Rua A", "1", null, "Centro", "Palmas", "TO", "77000-000"));
            if (cupomId != null) {
                pedido.setCupomDesconto(cupomRepository.findById(cupomId));
            }
            pedidoRepository.persist(pedido);
            return pedido.getId();
        });
    }

    private Long novoCupom(int quantidade) {
        return emTransacao(() -> {
            CupomDesconto cupom = new CupomDesconto();
            cupom.setCodigo("CUPOM-" + UUID.randomUUID());
            cupom.setQuantidadeDisponivel(quantidade);
            cupom.setAtivo(true);
            cupomRepository.persist(cupom);
            return cupom.getId();
        });
    }

    private void vencer(Long pagamentoId) {
        emTransacao(() -> pagamentoRepository.update("expiraEm = ?1 WHERE id = ?2",
                LocalDateTime.now().minusMinutes(1), pagamentoId));
    }

    private void envelhecer(Long pedidoId, int horas) {
        emTransacao(() -> pedidoRepository.update("dataCadastro = ?1 WHERE id = ?2",
                LocalDateTime.now().minusHours(horas), pedidoId));
    }

    private StatusPedido statusDoPedido(Long id) {
        return emTransacao(() -> pedidoRepository.findById(id).getStatus());
    }

    private StatusPagamento statusDoPagamento(Long id) {
        return emTransacao(() -> pagamentoRepository.findById(id).getStatus());
    }

    private int quantidadeDoCupom(Long id) {
        return emTransacao(() -> cupomRepository.findById(id).getQuantidadeDisponivel());
    }

    private List<TipoTransacao> tiposDeTransacao(Long pagamentoId) {
        return emTransacao(() -> transacaoRepository.findByPagamentoId(pagamentoId).stream()
                .map(TransacaoGateway::getTipo)
                .toList());
    }

    private static <T> T emTransacao(Callable<T> acao) {
        return QuarkusTransaction.requiringNew().call(acao);
    }
}
