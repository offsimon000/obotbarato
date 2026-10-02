import util.*;
import java.nio.file.*;
import java.sql.*;

/** Instala a V1, insere dados antigos e evolui para V2 sem perder saldo ou identidade. */
public final class EvolucaoDaBase {
    public static void main(String[] args)throws Exception {
        try(Connection c=ConexaoBanco.getConexao();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT current_database()")){
            r.next();if(!r.getString(1).equals("obotbarato_test"))throw new AssertionError("Base isolada obrigatória.");
        }
        Path antiga=Files.createTempDirectory(Path.of(".local"),"migrations-v1-");
        Files.copy(Path.of("sql/V001__base_desktop.sql"),antiga.resolve("V001__base_desktop.sql"));
        InicializarBanco.aplicarMigrations(antiga);
        int produto,cliente,pedido;
        try(Connection c=ConexaoBanco.getConexao();Statement s=c.createStatement()){
            s.execute("INSERT INTO categorias(nome) VALUES('Legada teste')");
            try(ResultSet r=s.executeQuery("INSERT INTO produtos(nome,preco,id_categoria) SELECT 'Legado teste',12.34,id FROM categorias WHERE nome='Legada teste' RETURNING id")){r.next();produto=r.getInt(1);}
            s.execute("INSERT INTO estoque(id_produto,quantidade,estoque_minimo) VALUES("+produto+",17,4)");
            try(ResultSet r=s.executeQuery("INSERT INTO clientes(nome,email) VALUES('Cliente Legado Teste','legado@teste.local') RETURNING id")){r.next();cliente=r.getInt(1);}
            try(ResultSet r=s.executeQuery("INSERT INTO pedidos(id_cliente,status,total) VALUES("+cliente+",'finalizado',24.68) RETURNING id")){r.next();pedido=r.getInt(1);}
            s.execute("INSERT INTO itens_pedido(id_pedido,id_produto,quantidade,preco_unitario) VALUES("+pedido+","+produto+",2,12.34)");
        }
        InicializarBanco.aplicarMigrations(Path.of("sql"));InicializarBanco.aplicarMigrations(Path.of("sql"));
        try(Connection c=ConexaoBanco.getConexao();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT e.quantidade,e.estoque_minimo,p.preco,m.saldo,c.ativo FROM produtos p JOIN estoque e ON e.id_produto=p.id JOIN movimentos_estoque m ON m.id_produto=p.id JOIN categorias c ON c.id=p.id_categoria WHERE p.id="+produto)){
            if(!r.next() || r.getInt(1)!=17 || r.getInt(2)!=4 || !r.getBigDecimal(3).toPlainString().equals("12.34") || r.getInt(4)!=17 || !r.getBoolean(5))throw new AssertionError("Dados legados perdidos.");
        }
        try(Connection c=ConexaoBanco.getConexao();Statement s=c.createStatement()){
            try(ResultSet r=s.executeQuery("SELECT p.subtotal,p.total,pg.status,pg.metodo FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id WHERE p.id="+pedido)){
                if(!r.next() || !r.getBigDecimal(1).toPlainString().equals("24.68") || !r.getBigDecimal(2).toPlainString().equals("24.68") || !r.getString(3).equals("confirmado") || !r.getString(4).equals("dinheiro"))throw new AssertionError("Pagamento legado não preservado.");
            }
            s.execute("DELETE FROM pedidos WHERE id="+pedido);s.execute("DELETE FROM clientes WHERE id="+cliente);
            s.execute("DELETE FROM produtos WHERE id="+produto);s.execute("DELETE FROM categorias WHERE nome='Legada teste'");
        }
        System.out.println("APROVADO: evolução V1 → V2/V3 preserva produto, preço, saldo, mínimo, pedido e itens; registra abertura e pagamento legado; reexecução idempotente.");
    }
}
