package util;
import java.sql.*;

/** Revalida perfil e ativação no banco em cada operação protegida. */
public final class Acesso {
    private Acesso() {}
    public static void telaAdmin(){try{admin();}catch(SQLException e){throw new IllegalStateException(Erros.mensagem(e),e);}}
    public static void telaEquipe(){try{equipe();}catch(SQLException e){throw new IllegalStateException(Erros.mensagem(e),e);}}
    public static void telaCliente(int id){try{cliente(id);}catch(SQLException e){throw new IllegalStateException(Erros.mensagem(e),e);}}
    public static void admin() throws SQLException { try(Connection c=ConexaoBanco.getConexao()){ equipe(c,true); } }
    public static void equipe() throws SQLException { try(Connection c=ConexaoBanco.getConexao()){ equipe(c,false); } }
    public static void equipe(Connection c,boolean admin) throws SQLException {
        int id=Sessao.getIdUsuario(); if(id<=0) negar();
        try(PreparedStatement p=c.prepareStatement("SELECT perfil FROM usuarios WHERE id=? AND ativo")) {
            p.setInt(1,id);
            try(ResultSet r=p.executeQuery()){ if(!r.next() || (admin && !"admin".equals(r.getString(1)))) negar(); }
        }
    }
    public static void cliente(int id) throws SQLException { try(Connection c=ConexaoBanco.getConexao()){ cliente(c,id); } }
    public static void cliente(Connection c,int id) throws SQLException {
        if(id<=0 || Sessao.getIdCliente()!=id) negar();
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM clientes WHERE id=? AND ativo AND senha IS NOT NULL")) {
            p.setInt(1,id); try(ResultSet r=p.executeQuery()){ if(!r.next()) negar(); }
        }
    }
    public static void venda(Connection c,int id) throws SQLException {
        if(Sessao.getIdUsuario()>0) equipe(c,false); else cliente(c,id);
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM clientes WHERE id=? AND ativo")) {
            p.setInt(1,id); try(ResultSet r=p.executeQuery()){ if(!r.next()) throw new SQLException("Cliente inativo ou inexistente."); }
        }
    }
    private static void negar() throws SQLException { throw new SQLException("Acesso negado. Entre com uma conta autorizada.","42501"); }
}
