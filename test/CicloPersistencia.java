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
            java.io.ByteArrayOutputStream imagem=new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(12,12,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",imagem);
            produto.setImagem(imagem.toByteArray());
            new ProdutoDAO().salvar(produto); new EstoqueDAO().entrada(produto.getId(), 7);
            Cliente cliente = new Cliente(); cliente.setNome(marca); cliente.setEmail(UUID.randomUUID() + "@teste.local");
            new ClienteDAO().salvar(cliente);
            Pedido pedido = new Pedido(); pedido.setIdCliente(cliente.getId()); pedido.setStatus("finalizado");
            pedido.adicionarItem(new ItemPedido(produto.getId(), marca, 3, 11.50));
            new PedidoDAO().salvar(pedido);
            CodigoAcessoDAO acesso=new CodigoAcessoDAO();String codigo=acesso.emitir(cliente.getId());
            exigir(acesso.redefinir(cliente.getEmail(),codigo,"fixture12345"),"Ativação própria da fixture falhou.");
            String codigoCupom="VALIDA_"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
            java.time.LocalDate hoje=java.time.LocalDate.parse(texto("SELECT CURRENT_DATE::text"));
            new CupomDAO().salvar(0,codigoCupom,new BigDecimal("10"),hoje,hoje.plusDays(1),true);
            Sessao.iniciarCliente(new ClienteLojaDAO().autenticar(cliente.getEmail(),"fixture12345"));
            Cliente perfil=new ClienteLojaDAO().buscarPerfil(cliente.getId());perfil.setEndereco("Rua do perfil validado, 200");
            new ClienteLojaDAO().atualizarPerfil(perfil,"fixture12345","fixture67890");
            int cancelado=new CompraDAO().confirmar(cliente.getId(),List.of(new ItemPedido(produto.getId(),marca,2,11.50)),"pix",marca,"11999990000","Rua da validação, 100",UUID.randomUUID(),codigoCupom,new BigDecimal("10")).id();
            new GestaoPedidoDAO().cancelar(cancelado,"Cancelamento próprio para verificar restauração");
            Properties dados = new Properties();
            dados.setProperty("marca", marca); dados.setProperty("categoria", Integer.toString(categoria));
            dados.setProperty("produto", Integer.toString(produto.getId())); dados.setProperty("cliente", Integer.toString(cliente.getId()));
            dados.setProperty("pedido", Integer.toString(pedido.getId())); dados.setProperty("admin", Integer.toString(admin.getId()));
            dados.setProperty("imagemHash",hashImagem(produto.getId()));
            dados.setProperty("cancelado",Integer.toString(cancelado));
            dados.setProperty("cupom",codigoCupom);
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
        exigir(texto("SELECT subtotal::text FROM pedidos WHERE id="+pedido).equals("34.50"),"Subtotal não preservado.");
        exigir(texto("SELECT desconto::text FROM pedidos WHERE id="+pedido).equals("0.00"),"Desconto de balcão incorreto.");
        exigir(texto("SELECT metodo || '/' || status FROM pagamentos WHERE id_pedido="+pedido).equals("dinheiro/confirmado"),"Pagamento simulado não preservado.");
        exigir(texto("SELECT status FROM pedidos WHERE id="+pedido).equals("entregue"),"Retirada não preservada.");
        exigir(texto("SELECT COUNT(*) FROM historico_pedidos WHERE id_pedido="+pedido).equals("1"),"Histórico da retirada não preservado.");
        if(dados.getProperty("cancelado")!=null){
            int cancelado=Integer.parseInt(dados.getProperty("cancelado"));
            exigir(texto("SELECT p.status || '/' || pg.status FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id WHERE p.id="+cancelado+" AND p.id_cliente="+cliente).equals("cancelado/cancelado"),"Cancelamento não preservado.");
            exigir(texto("SELECT COUNT(*) FROM historico_pedidos WHERE id_pedido="+cancelado).equals("2"),"Histórico do cancelamento perdido.");
            exigir(texto("SELECT COUNT(*) FROM movimentos_estoque WHERE id_pedido="+cancelado+" AND id_produto="+produto+" AND tipo='devolucao' AND quantidade=2 AND saldo=4").equals("1"),"Devolução/auditoria não preservada.");
            if(dados.getProperty("cupom")!=null){
                exigir(texto("SELECT total::text||'/'||desconto_cupom::text||'/'||desconto_pagamento::text FROM pedidos WHERE id="+cancelado).equals("18.63/2.30/2.07"),"Cupom/descontos não preservados.");
                exigir(texto("SELECT endereco FROM clientes WHERE id="+cliente).equals("Rua do perfil validado, 200"),"Perfil não preservado.");
                String email=textEmail(cliente);exigir(new ClienteLojaDAO().autenticar(email,"fixture67890")!=null && new ClienteLojaDAO().autenticar(email,"fixture12345")==null,"Senha atual não preservada.");
            }
        }
        if(dados.getProperty("imagemHash")!=null)exigir(dados.getProperty("imagemHash").equals(hashImagem(produto)),"Imagem não preservada.");
        if (args[0].equals("verificar")) {
            System.out.println("OK: pedido, item, total R$ 34,50, pagamentos, imagem, estoque, retirada, cancelamento/devolução/histórico, perfil/senha e cupom/descontos R$18,63 preservados em " + banco);
        } else if (args[0].equals("limpar")) {
            Transacao.executar(con -> {
                try (Statement st = con.createStatement()) {
                    if(dados.getProperty("cancelado")!=null)st.executeUpdate("DELETE FROM pedidos WHERE id="+Integer.parseInt(dados.getProperty("cancelado"))+" AND id_cliente="+cliente);
                    if(dados.getProperty("cupom")!=null){try(PreparedStatement p=con.prepareStatement("DELETE FROM cupons WHERE codigo=?")){p.setString(1,dados.getProperty("cupom"));p.executeUpdate();}}
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
    private static String textEmail(int id)throws SQLException{return texto("SELECT email FROM clientes WHERE id="+id);}
    private static String texto(String sql) throws SQLException {
        try (Connection con = ConexaoBanco.getConexao(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            exigir(rs.next(), "Registro da fixture ausente."); return rs.getString(1);
        }
    }
    private static String hashImagem(int id)throws Exception{
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT imagem FROM produtos WHERE id=?")){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){exigir(r.next(),"Produto não encontrado.");return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(r.getBytes(1)));}
        }
    }
    private static void exigir(boolean condicao, String msg) { if (!condicao) throw new AssertionError(msg); }
}
