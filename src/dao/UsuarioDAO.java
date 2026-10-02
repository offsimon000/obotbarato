package dao;
import model.Usuario;
import util.*;
import java.sql.*;
import java.util.*;

public class UsuarioDAO {
    public Usuario autenticar(String login,String senha) throws SQLException {
        if(login==null || senha==null || login.isBlank() || senha.isBlank()) return null;
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT * FROM usuarios WHERE lower(trim(login))=? AND ativo")){
            p.setString(1,login.trim().toLowerCase(Locale.ROOT));
            try(ResultSet r=p.executeQuery()){
                if(!r.next()) return null;
                String hash=r.getString("senha"); if(!SenhaCripto.verificar(senha,hash)) return null;
                Usuario u=mapear(r);
                if(SenhaCripto.ehFormatoLegado(hash)){
                    try(PreparedStatement m=c.prepareStatement("UPDATE usuarios SET senha=? WHERE id=? AND senha=?")){
                        m.setString(1,SenhaCripto.migrar(senha));m.setInt(2,u.getId());m.setString(3,hash);
                        if(m.executeUpdate()!=1) return null;
                    }
                }
                return u;
            }
        }
    }
    public List<Usuario> listar() throws SQLException {
        Acesso.admin(); List<Usuario> lista=new ArrayList<>();
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT * FROM usuarios ORDER BY nome");ResultSet r=p.executeQuery()){
            while(r.next()) lista.add(mapear(r));
        }return lista;
    }
    public boolean salvar(Usuario u) throws SQLException { return gravar(u,true); }
    public boolean atualizar(Usuario u) throws SQLException { return gravar(u,false); }
    private boolean gravar(Usuario u,boolean novo) throws SQLException {
        u.setNome(Validacao.texto(u.getNome(),"Nome",3,100));
        u.setLogin(Validacao.texto(u.getLogin(),"Login",3,60).toLowerCase(Locale.ROOT));
        if(!u.getLogin().matches("[a-z0-9._-]+")) throw new IllegalArgumentException("Login: use letras, números, ponto, hífen ou sublinhado.");
        if(!"admin".equals(u.getPerfil()) && !"vendedor".equals(u.getPerfil())) throw new IllegalArgumentException("Perfil inválido.");
        String hash=(novo || (u.getSenha()!=null && !u.getSenha().isEmpty()))?SenhaCripto.hashear(u.getSenha()):null;
        int id=Transacao.executar(c -> {
            try(Statement s=c.createStatement()){s.execute("SELECT pg_advisory_xact_lock(726283)");}
            Acesso.equipe(c,true);
            if(!novo && (!u.isAtivo() || !u.isAdmin())){
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM usuarios WHERE ativo AND perfil='admin' AND id<>?")){
                    p.setInt(1,u.getId());try(ResultSet r=p.executeQuery()){if(!r.next()) throw new IllegalArgumentException("Mantenha pelo menos um administrador ativo.");}
                }
            }
            String sql=novo?"INSERT INTO usuarios(nome,login,perfil,ativo,senha) VALUES(?,?,?,?,?) RETURNING id":
                "UPDATE usuarios SET nome=?,login=?,perfil=?,ativo=?,senha=COALESCE(?,senha) WHERE id=? RETURNING id";
            try(PreparedStatement p=c.prepareStatement(sql)){
                p.setString(1,u.getNome());p.setString(2,u.getLogin());p.setString(3,u.getPerfil());p.setBoolean(4,u.isAtivo());p.setString(5,hash);
                if(!novo)p.setInt(6,u.getId());
                try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Usuário não encontrado.");return r.getInt(1);}
            }
        });u.setId(id);return true;
    }
    private Usuario mapear(ResultSet r)throws SQLException{
        Usuario u=new Usuario(r.getInt("id"),r.getString("nome"),r.getString("login"),r.getString("perfil"));
        u.setAtivo(r.getBoolean("ativo"));return u;
    }
}
