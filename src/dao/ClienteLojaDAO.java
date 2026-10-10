package dao;

import model.Cliente;
import util.ConexaoBanco;
import util.SenhaCripto;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClienteLojaDAO {

    private static final Logger LOG = Logger.getLogger(ClienteLojaDAO.class.getName());

    // ── Autenticação ─────────────────────────────────────────────────────
    public Cliente autenticar(String email, String senhaPlana) throws SQLException {
        if (email == null || senhaPlana == null
                || email.isBlank() || senhaPlana.isBlank()) return null;

        String sql = "SELECT * FROM clientes WHERE lower(trim(email)) = ? AND ativo = true";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("senha");
                if (!SenhaCripto.verificar(senhaPlana, hash)) return null;
                Cliente c = mapear(rs);
                if (SenhaCripto.ehFormatoLegado(hash) && !migrarSenha(c.getId(), senhaPlana,hash)) return null;
                return c;
            }
        } catch (SQLException e) { throw e; }
        return null;
    }

    // ── Cadastro ─────────────────────────────────────────────────────────
    /** Retorna null se sucesso, ou mensagem de erro. */
    public String cadastrar(String nome, String cpf, String email,
                             String telefone, String endereco, String senhaPlana) throws SQLException {
        Cliente dados = new Cliente(0,nome,cpf,email,telefone,endereco);
        try { util.Validacao.cliente(dados); util.Validacao.senha(senhaPlana); }
        catch (IllegalArgumentException e) { return e.getMessage(); }
        nome=dados.getNome(); cpf=dados.getCpf(); email=dados.getEmail();
        telefone=dados.getTelefone(); endereco=dados.getEndereco();
        if (emailExiste(email.trim().toLowerCase())) return "Este email ja esta cadastrado.";

        String sql = "INSERT INTO clientes (nome, cpf, email, telefone, endereco, senha, ativo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, true)";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            ps.setString(2, opcional(cpf));
            ps.setString(3, email.trim().toLowerCase());
            ps.setString(4, opcional(telefone));
            ps.setString(5, opcional(endereco));
            ps.setString(6, SenhaCripto.hashear(senhaPlana));
            if (ps.executeUpdate() != 1) throw new SQLException("Cadastro de cliente não gravado.");
            return null;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) return "Email ou CPF já cadastrado.";
            throw e;
        }
    }

    public boolean emailExiste(String email) throws SQLException {
        String sql = "SELECT 1 FROM clientes WHERE lower(trim(email)) = ? LIMIT 1";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            return ps.executeQuery().next();
        } catch (SQLException e) { throw e; }
    }

    // ── Favoritos ────────────────────────────────────────────────────────
    public Cliente buscarPerfil(int id)throws SQLException{
        try(Connection c=ConexaoBanco.getConexao()){util.Acesso.cliente(c,id);
            try(PreparedStatement p=c.prepareStatement("SELECT * FROM clientes WHERE id=? AND ativo")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Cliente não encontrado.");return mapear(r);}}
        }
    }
    public Cliente atualizarPerfil(Cliente dados,String senhaAtual,String novaSenha)throws SQLException{
        util.Validacao.cliente(dados);
        boolean trocar=novaSenha!=null && !novaSenha.isEmpty();String novoHash=trocar?SenhaCripto.hashear(novaSenha):null;
        return util.Transacao.executar(c->{
            util.Acesso.cliente(c,dados.getId());
            try(PreparedStatement p=c.prepareStatement("SELECT senha FROM clientes WHERE id=? AND ativo FOR UPDATE")){
                p.setInt(1,dados.getId());try(ResultSet r=p.executeQuery()){if(!r.next() || !SenhaCripto.verificar(senhaAtual,r.getString(1)))throw new IllegalArgumentException("Senha atual incorreta ou conta indisponível.");}
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE clientes SET nome=?,cpf=?,email=?,telefone=?,endereco=?,senha=COALESCE(?,senha) WHERE id=? AND ativo")){
                p.setString(1,dados.getNome());p.setString(2,dados.getCpf());p.setString(3,dados.getEmail());p.setString(4,dados.getTelefone());p.setString(5,dados.getEndereco());p.setString(6,novoHash);p.setInt(7,dados.getId());if(p.executeUpdate()!=1)throw new SQLException("Perfil não alterado.");
            }
            try(PreparedStatement p=c.prepareStatement("DELETE FROM acessos_cliente WHERE id_cliente=?")){p.setInt(1,dados.getId());p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("SELECT * FROM clientes WHERE id=?")){p.setInt(1,dados.getId());try(ResultSet r=p.executeQuery()){r.next();return mapear(r);}}
        });
    }

    /** Uma única query — use isso na galeria, não ehFavorito() em loop */
    public Set<Integer> listarFavoritosSet(int idCliente) throws SQLException {
        util.Acesso.cliente(idCliente);
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT id_produto FROM favoritos WHERE id_cliente = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id_produto"));
        } catch (SQLException e) { throw e; }
        return ids;
    }

    public boolean adicionarFavorito(int idCliente, int idProduto) throws SQLException {
        util.Acesso.cliente(idCliente);
        String sql = "INSERT INTO favoritos (id_cliente, id_produto) VALUES (?, ?) "
                   + "ON CONFLICT (id_cliente, id_produto) DO NOTHING";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente); ps.setInt(2, idProduto);
            ps.executeUpdate(); return true;
        } catch (SQLException e) { throw e; }
    }

    public boolean removerFavorito(int idCliente, int idProduto) throws SQLException {
        util.Acesso.cliente(idCliente);
        String sql = "DELETE FROM favoritos WHERE id_cliente = ? AND id_produto = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente); ps.setInt(2, idProduto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw e; }
    }

    // ── Privados ─────────────────────────────────────────────────────────

    private boolean migrarSenha(int id, String senhaPlana,String anterior) throws SQLException {
        String sql = "UPDATE clientes SET senha = ? WHERE id = ? AND ativo = true AND senha=?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, SenhaCripto.migrar(senhaPlana));
            ps.setInt(2, id);
            ps.setString(3,anterior);
            if (ps.executeUpdate()!=1) return false;
            LOG.info("Senha migrada legada->PBKDF2 para cliente #" + id);
            return true;
        } catch (SQLException e) {
            throw e;
        }
    }

    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNome(rs.getString("nome"));
        c.setCpf(rs.getString("cpf"));
        c.setEmail(rs.getString("email"));
        c.setTelefone(rs.getString("telefone"));
        c.setEndereco(rs.getString("endereco"));
        c.setAtivo(rs.getBoolean("ativo"));c.setAcessoLoja(rs.getString("senha")!=null);
        return c;
    }
}
