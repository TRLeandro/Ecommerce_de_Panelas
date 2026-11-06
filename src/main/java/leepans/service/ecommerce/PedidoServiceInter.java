package leepans.service.ecommerce;

import java.util.List;

import leepans.model.Endereco;
import leepans.model.Pedido;
import leepans.model.StatusPedido;

public interface PedidoServiceInter {

    List<Pedido> findAll();

    Pedido findById(Long id);

    Pedido findByIdDoUsuario(Long id, String login);

    List<Pedido> findCompras(String usuarioLogin, StatusPedido status);

    List<Pedido> findCompras(String usuarioLogin);

    List<Pedido> findByUsuarioId(Long usuarioId);

    List<Pedido> findByStatus(StatusPedido status);

    List<Pedido> findByEnderecoCidade(String cidade);

    Pedido create(Pedido pedido, String login, Endereco endereco, String codigoCupom);

    void delete(Long id);
}
