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
        int produto;
        try(Connection c=ConexaoBanco.getConexao();Statement s=c.createStatement()){
            s.execute("INSERT INTO categorias(nome) VALUES('Legada teste')");
            try(ResultSet r=s.executeQuery("INSERT INTO produtos(nome,preco,id_categoria) SELECT 'Legado teste',12.34,id FROM categorias WHERE nome='Legada teste' RETURNING id")){r.next();produto=r.getInt(1);}
            s.execute("INSERT INTO estoque(id_produto,quantidade,estoque_minimo) VALUES("+produto+",17,4)");
        }
        InicializarBanco.aplicarMigrations(Path.of("sql"));InicializarBanco.aplicarMigrations(Path.of("sql"));
        try(Connection c=ConexaoBanco.getConexao();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT e.quantidade,e.estoque_minimo,p.preco,m.saldo,c.ativo FROM produtos p JOIN estoque e ON e.id_produto=p.id JOIN movimentos_estoque m ON m.id_produto=p.id JOIN categorias c ON c.id=p.id_categoria WHERE p.id="+produto)){
            if(!r.next() || r.getInt(1)!=17 || r.getInt(2)!=4 || !r.getBigDecimal(3).toPlainString().equals("12.34") || r.getInt(4)!=17 || !r.getBoolean(5))throw new AssertionError("Dados legados perdidos.");
        }
        try(Connection c=ConexaoBanco.getConexao();Statement s=c.createStatement()){
            s.execute("DELETE FROM produtos WHERE id="+produto);s.execute("DELETE FROM categorias WHERE nome='Legada teste'");
        }
        System.out.println("APROVADO: evolução V1 → V2 preserva produto, preço, saldo, mínimo e registra abertura; reexecução idempotente.");
    }
}
