package dao;
import model.Categoria;
import util.*;
import java.sql.*;
import java.util.*;

public final class CategoriaDAO {
    public List<Categoria> listar(boolean todas) throws SQLException {
        if(todas) Acesso.admin();
        List<Categoria> lista=new ArrayList<>();
        try(Connection c=ConexaoBanco.getConexao(); PreparedStatement p=c.prepareStatement("SELECT * FROM categorias "+(todas?"":"WHERE ativo ")+"ORDER BY nome");ResultSet r=p.executeQuery()){
            while(r.next()) lista.add(new Categoria(r.getInt("id"),r.getString("nome"),r.getBoolean("ativo")));
        }
        return lista;
    }
    public void salvar(int id,String nome,boolean ativo) throws SQLException {
        String n=Validacao.texto(nome,"Categoria",2,80);
        Transacao.executar(c -> {
            try(Statement s=c.createStatement()){ s.execute("SELECT pg_advisory_xact_lock(726282)"); }
            Acesso.equipe(c,true);
            if(id>0 && !ativo) {
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM produtos WHERE id_categoria=? AND ativo LIMIT 1")){
                    p.setInt(1,id); try(ResultSet r=p.executeQuery()){ if(r.next()) throw new IllegalArgumentException("Desative ou transfira os produtos ativos antes de desativar a categoria."); }
                }
            }
            try(PreparedStatement p=c.prepareStatement(id==0?"INSERT INTO categorias(nome,ativo) VALUES(?,?)":"UPDATE categorias SET nome=?,ativo=? WHERE id=?")){
                p.setString(1,n); p.setBoolean(2,ativo); if(id>0) p.setInt(3,id);
                if(p.executeUpdate()!=1) throw new SQLException("Categoria não encontrada.");
            }
            return null;
        });
    }
    static void validarAtiva(Connection c,int id) throws SQLException {
        // Mesmo lock utilizado na desativação, evitando produto ativo em categoria desativada por corrida.
        try(Statement s=c.createStatement()){ s.execute("SELECT pg_advisory_xact_lock(726282)"); }
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM categorias WHERE id=? AND ativo")){
            p.setInt(1,id); try(ResultSet r=p.executeQuery()){ if(!r.next()) throw new IllegalArgumentException("Selecione uma categoria ativa."); }
        }
    }
}
