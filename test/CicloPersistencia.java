import dao.*;
import model.*;
import util.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Fixture própria para verificar reinício e backup, sem modificar cadastros existentes. */
public final class CicloPersistencia {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Uso: criar|verificar|limpar arquivo-fixture");
        String banco = texto("SELECT current_database()");
        if (!banco.equals("obotbarato") && !banco.startsWith("obotbarato_restaurado_"))
            throw new IllegalArgumentException("Use apenas a base local gerenciada.");
        Path arquivo = Path.of(args[1]);
        if (args[0].equals("criar")) {
            if (Files.exists(arquivo)) throw new IllegalArgumentException("Fixture já existente; arquivo preservado.");
            String marca = "Validacao " + UUID.randomUUID();
            Usuario admin = new UsuarioDAO().autenticar("admin", "admin123");
            if (admin == null) throw new AssertionError("Admin da demonstração não autentica.");
            Sessao.iniciar(admin);
            int categoria = Transacao.executar(con -> {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO categorias(nome) VALUES(?) RETURNING id")) {
                    ps.setString(1, marca); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
                }
            });
            Produto produto = new Produto(); produto.setNome(marca); produto.setIdCategoria(categoria);
            produto.setPreco(new BigDecimal("11.50")); produto.setAtivo(true);
            new ProdutoDAO().salvar(produto); new EstoqueDAO().entrada(produto.getId(), 7);
            Cliente cliente = new Cliente(); cliente.setNome(marca); cliente.setEmail(UUID.randomUUID() + "@teste.local");
            new ClienteDAO().salvar(cliente);
            Pedido pedido = new Pedido(); pedido.setIdCliente(cliente.getId()); pedido.setStatus("finalizado");
            pedido.adicionarItem(new ItemPedido(produto.getId(), marca, 3, 11.50));
            new PedidoDAO().salvar(pedido);
            Properties dados = new Properties();
            dados.setProperty("marca", marca); dados.setProperty("categoria", Integer.toString(categoria));
            dados.setProperty("produto", Integer.toString(produto.getId())); dados.setProperty("cliente", Integer.toString(cliente.getId()));
            dados.setProperty("pedido", Integer.toString(pedido.getId())); dados.setProperty("admin", Integer.toString(admin.getId()));
            try (var writer = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) { dados.store(writer, "Fixture exclusiva de validacao"); }
            Sessao.encerrar();
            System.out.println("OK: venda fictícia criada para testar reinício e backup.");
            return;
        }
        Properties dados = new Properties();
        try (var reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) { dados.load(reader); }
        String marca = dados.getProperty("marca");
        if (marca == null || !marca.matches("Validacao [a-f0-9-]{36}")) throw new IllegalArgumentException("Fixture inválida.");
        int produto = Integer.parseInt(dados.getProperty("produto")), cliente = Integer.parseInt(dados.getProperty("cliente"));
        int pedido = Integer.parseInt(dados.getProperty("pedido")), categoria = Integer.parseInt(dados.getProperty("categoria"));
        int admin = Integer.parseInt(dados.getProperty("admin"));
        exigir(marca.equals(texto("SELECT nome FROM produtos WHERE id=" + produto)), "Produto da fixture não corresponde.");
        exigir(marca.equals(texto("SELECT nome FROM clientes WHERE id=" + cliente)), "Cliente da fixture não corresponde.");
        exigir(marca.equals(texto("SELECT nome FROM categorias WHERE id=" + categoria)), "Categoria da fixture não corresponde.");
        exigir("34.50".equals(texto("SELECT total::text FROM pedidos WHERE id=" + pedido + " AND id_cliente=" + cliente)), "Total/cliente do pedido incorreto.");
        exigir(new EstoqueDAO().getQuantidade(produto) == 4, "Estoque não preservado.");
        exigir(texto("SELECT registrado_por FROM pedidos WHERE id=" + pedido).equals(Integer.toString(admin)), "Auditoria do pedido incorreta.");
        exigir(texto("SELECT criado_por FROM produtos WHERE id=" + produto).equals(Integer.toString(admin)), "Auditoria do produto incorreta.");
        exigir(texto("SELECT criado_por FROM clientes WHERE id=" + cliente).equals(Integer.toString(admin)), "Auditoria do cliente incorreta.");
        exigir(texto("SELECT COUNT(*) FROM itens_pedido WHERE id_pedido=" + pedido).equals("1"), "Itens do pedido não preservados.");
        if (args[0].equals("verificar")) {
            System.out.println("OK: pedido, item, total R$ 34,50, estoque e auditoria preservados em " + banco);
        } else if (args[0].equals("limpar")) {
            Transacao.executar(con -> {
                try (Statement st = con.createStatement()) {
                    st.executeUpdate("DELETE FROM pedidos WHERE id=" + pedido + " AND id_cliente=" + cliente);
                    st.executeUpdate("DELETE FROM produtos WHERE id=" + produto);
                    st.executeUpdate("DELETE FROM clientes WHERE id=" + cliente);
                    st.executeUpdate("DELETE FROM categorias WHERE id=" + categoria);
                }
                return null;
            });
            System.out.println("OK: somente os dados próprios da fixture foram removidos de " + banco);
        } else throw new IllegalArgumentException("Ação inválida.");
    }
    private static String texto(String sql) throws SQLException {
        try (Connection con = ConexaoBanco.getConexao(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            exigir(rs.next(), "Registro da fixture ausente."); return rs.getString(1);
        }
    }
    private static void exigir(boolean condicao, String msg) { if (!condicao) throw new AssertionError(msg); }
}
