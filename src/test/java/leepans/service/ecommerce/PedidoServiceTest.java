package leepans.service.ecommerce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import leepans.exception.ResourceNotFoundException;
import leepans.exception.ValidationException;
import leepans.model.Cidade;
import leepans.model.CupomDesconto;
import leepans.model.Endereco;
import leepans.model.Estado;
import leepans.model.ItemPedido;
import leepans.model.Panela;
import leepans.model.Pedido;
import leepans.model.Perfil;
import leepans.model.Regiao;
import leepans.model.StatusPedido;
import leepans.model.Usuario;
import leepans.repository.CidadeRepository;
import leepans.repository.CupomDescontoRepository;
import leepans.repository.EnderecoRepository;
import leepans.repository.EstadoRepository;
import leepans.repository.PanelaRepository;
import leepans.repository.PedidoRepository;
import leepans.repository.RegiaoRepository;
import leepans.repository.UsuarioRepository;

@QuarkusTest
class PedidoServiceTest {

    @Inject
    PedidoService service;

    @Inject
    PedidoRepository pedidoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    PanelaRepository panelaRepository;

    @Inject
    CupomDescontoRepository cupomRepository;

    @Inject
    CidadeRepository cidadeRepository;

    @Inject
    EstadoRepository estadoRepository;

    @Inject
    RegiaoRepository regiaoRepository;

    @Inject
    EnderecoRepository enderecoRepository;

    @Inject
    MockMailbox mailbox;

    private String login;
    private Panela panela;
    private Long cidadeId;

    @BeforeEach
    void preparar() {
        mailbox.clear();
        login = "cliente-" + UUID.randomUUID();
        emTransacao(() -> {
            Usuario usuario = new Usuario();
            usuario.setLogin(login);
            usuario.setNome("Cliente");
            usuario.setSenhaHash("hash");
            usuario.setPerfil(Perfil.CLIENTE);
            usuarioRepository.persist(usuario);

            panela = new Panela();
            panela.setModelo("Teste");
            panela.setPreco(new BigDecimal("150.00"));
            panelaRepository.persist(panela);

            Regiao regiao = new Regiao();
            regiao.setNome("Norte");
            regiaoRepository.persist(regiao);

            Estado estado = new Estado();
            estado.setNome("Tocantins");
            estado.setSigla("TO");
            estado.setRegiao(regiao);
            estadoRepository.persist(estado);

            Cidade cidade = new Cidade();
            cidade.setNome("Palmas");
            cidade.setEstado(estado);
            cidadeRepository.persist(cidade);
            cidadeId = cidade.getId();
            return null;
        });
    }

    @Test
    void criar_usaPrecoDoCatalogoESemPagamento() {
        Pedido pedido = service.create(pedidoCom(2), login, endereco(), null);

        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedido.getStatus());
        assertEquals(0, new BigDecimal("150.00").compareTo(pedido.getItens().get(0).getValorUnitario()));
        assertEquals(0, new BigDecimal("300.00").compareTo(pedido.getValorItens()));
        assertEquals(0, new BigDecimal("300.00").compareTo(pedido.valorTotal()));
        assertEquals(0, new BigDecimal("0.00").compareTo(pedido.getValorFrete()));
        assertTrue(pedido.getPagamentos().isEmpty());
    }

    @Test
    void criar_copiaOEnderecoSemCriarLinha() {
        long enderecosAntes = emTransacao(() -> enderecoRepository.count());

        Pedido pedido = service.create(pedidoCom(1), login, endereco(), null);

        assertEquals("Rua A", pedido.getEnderecoEntrega().getLogradouro());
        assertEquals("Centro", pedido.getEnderecoEntrega().getBairro());
        assertEquals("Palmas", pedido.getEnderecoEntrega().getCidade());
        assertEquals("TO", pedido.getEnderecoEntrega().getEstado());
        assertEquals(enderecosAntes, emTransacao(() -> enderecoRepository.count()));
    }

    @Test
    void criar_usaEnderecoDoUsuarioEIgnoraEdicaoPosterior() {
        Long enderecoId = emTransacao(() -> {
            Endereco endereco = endereco();
            endereco.setCidade(cidadeRepository.findById(cidadeId));
            Usuario usuario = usuarioRepository.findByLogin(login).firstResult();
            usuario.setEndereco(endereco);
            enderecoRepository.persist(endereco);
            return endereco.getId();
        });

        Pedido pedido = service.create(pedidoCom(1), login, null, null);
        emTransacao(() -> {
            enderecoRepository.findById(enderecoId).setLogradouro("Rua Nova");
            return null;
        });

        String logradouro = emTransacao(() -> pedidoRepository.findById(pedido.getId()).getEnderecoEntrega().getLogradouro());
        assertEquals("Rua A", logradouro);
    }

    @Test
    void criar_semEnderecoNenhum_falha() {
        assertThrows(ValidationException.class, () -> service.create(pedidoCom(1), login, null, null));
    }

    @Test
    void criar_comCidadeInexistente_falha() {
        Endereco endereco = endereco();
        endereco.getCidade().setId(-1L);

        assertThrows(ResourceNotFoundException.class, () -> service.create(pedidoCom(1), login, endereco, null));
    }

    @Test
    void criar_comPanelaInexistente_falha() {
        Pedido pedido = pedidoCom(1);
        pedido.getItens().get(0).setPanela(null);

        assertThrows(ValidationException.class, () -> service.create(pedido, login, endereco(), null));
    }

    @Test
    void criar_aplicaCupomEDecrementaQuantidade() {
        Long cupomId = novoCupom(2, new BigDecimal("50.00"));
        String codigo = emTransacao(() -> cupomRepository.findById(cupomId).getCodigo());

        Pedido pedido = service.create(pedidoCom(1), login, endereco(), codigo.toLowerCase());

        assertEquals(0, new BigDecimal("50.00").compareTo(pedido.getValorDesconto()));
        assertEquals(0, new BigDecimal("100.00").compareTo(pedido.valorTotal()));
        assertEquals(1, quantidade(cupomId));
    }

    @Test
    void criar_descontoNaoPassaDoValorDosItens() {
        Long cupomId = novoCupom(2, new BigDecimal("999.00"));
        String codigo = emTransacao(() -> cupomRepository.findById(cupomId).getCodigo());

        Pedido pedido = service.create(pedidoCom(1), login, endereco(), codigo);

        assertEquals(0, BigDecimal.ZERO.compareTo(pedido.valorTotal()));
    }

    @Test
    void criar_cupomSemLimite_naoQuebra() {
        Long cupomId = novoCupom(null, new BigDecimal("10.00"));
        String codigo = emTransacao(() -> cupomRepository.findById(cupomId).getCodigo());

        Pedido pedido = service.create(pedidoCom(1), login, endereco(), codigo);

        assertEquals(0, new BigDecimal("10.00").compareTo(pedido.getValorDesconto()));
        assertNull(emTransacao(() -> cupomRepository.findById(cupomId).getQuantidadeDisponivel()));
    }

    @Test
    void criar_cupomEsgotado_falha() {
        Long cupomId = novoCupom(0, new BigDecimal("10.00"));
        String codigo = emTransacao(() -> cupomRepository.findById(cupomId).getCodigo());

        assertThrows(ValidationException.class, () -> service.create(pedidoCom(1), login, endereco(), codigo));
    }

    @Test
    void criar_cupomInexistente_falha() {
        assertThrows(ValidationException.class, () -> service.create(pedidoCom(1), login, endereco(), "NAO-EXISTE"));
    }

    @Test
    void criar_falhaDepoisDoCupom_desfazODecrementoENaoEnviaEmail() {
        Long cupomId = novoCupom(2, new BigDecimal("10.00"));
        String codigo = emTransacao(() -> cupomRepository.findById(cupomId).getCodigo());

        assertThrows(ValidationException.class, () -> service.create(pedidoCom(1), login, null, codigo));

        assertEquals(2, quantidade(cupomId));
        assertEquals(0, mailbox.getTotalMessagesSent());
    }

    @Test
    void criar_enviaEmailDePedidoRecebido() {
        service.create(pedidoCom(1), login, endereco(), null);

        assertEquals("Confirmação de Pedido - LeePans",
                mailbox.getMailsSentTo("leandrotavares@unitins.br").get(0).getSubject());
    }

    @Test
    void cancelar_semPagamento_enviaEmail() {
        Pedido pedido = service.create(pedidoCom(1), login, endereco(), null);
        mailbox.clear();

        service.cancelar(pedido.getId());

        assertEquals(StatusPedido.CANCELADO, emTransacao(() -> pedidoRepository.findById(pedido.getId()).getStatus()));
        assertEquals("Pedido Recusado - LeePans",
                mailbox.getMailsSentTo("leandrotavares@unitins.br").get(0).getSubject());
    }

    private Pedido pedidoCom(int quantidade) {
        ItemPedido item = new ItemPedido();
        item.setPanela(panela);
        item.setQuantidade(quantidade);

        Pedido pedido = new Pedido();
        pedido.setItens(new ArrayList<>(List.of(item)));
        return pedido;
    }

    private Endereco endereco() {
        Cidade cidade = new Cidade();
        cidade.setId(cidadeId);

        Endereco endereco = new Endereco();
        endereco.setLogradouro("Rua A");
        endereco.setNumero("10");
        endereco.setBairro("Centro");
        endereco.setCep("77000-000");
        endereco.setCidade(cidade);
        return endereco;
    }

    private Long novoCupom(Integer quantidade, BigDecimal desconto) {
        return emTransacao(() -> {
            CupomDesconto cupom = new CupomDesconto();
            cupom.setCodigo("CUPOM-" + UUID.randomUUID());
            cupom.setQuantidadeDisponivel(quantidade);
            cupom.setValorDesconto(desconto);
            cupom.setAtivo(true);
            cupomRepository.persist(cupom);
            return cupom.getId();
        });
    }

    private int quantidade(Long cupomId) {
        return emTransacao(() -> cupomRepository.findById(cupomId).getQuantidadeDisponivel());
    }

    private static <T> T emTransacao(Callable<T> acao) {
        return QuarkusTransaction.requiringNew().call(acao);
    }
}
