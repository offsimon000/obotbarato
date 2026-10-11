package dao;

import model.Produto;
import util.ConexaoBanco;
import util.Sessao;
import util.Transacao;
import util.BuscaTexto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProdutoDAO {
    public record Disponivel(Produto produto,int quantidade,java.math.BigDecimal media,long avaliacoes){}
    private static final String MEDIA=" LEFT JOIN (SELECT id_produto,ROUND(AVG(nota),1) media,COUNT(*) avaliacoes FROM avaliacoes GROUP BY id_produto) av ON av.id_produto=p.id ";
    /** Catálogo e saldo em uma consulta, sem consulta extra para cada cartão. */
    public List<Disponivel> listarCatalogo(String busca)throws SQLException{
        List<Disponivel> lista=new ArrayList<>();
        BuscaTexto.Filtro filtro=BuscaTexto.porPalavras("p.nome",busca);
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT p.*,cat.nome AS nome_categoria,e.quantidade,COALESCE(av.media,0) media,COALESCE(av.avaliacoes,0) avaliacoes FROM produtos p JOIN categorias cat ON cat.id=p.id_categoria JOIN estoque e ON e.id_produto=p.id"+MEDIA+"WHERE p.ativo AND cat.ativo AND "+filtro.sql()+" ORDER BY p.nome")){
            filtro.aplicar(p);try(ResultSet r=p.executeQuery()){while(r.next())lista.add(new Disponivel(mapear(r),r.getInt("quantidade"),r.getBigDecimal("media"),r.getLong("avaliacoes")));}
        }return lista;
    }
    public Disponivel buscarParaCarrinho(int id) throws SQLException {
        util.Acesso.cliente(Sessao.getIdCliente());
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT p.*,c.nome AS nome_categoria,e.quantidade,COALESCE(av.media,0) media,COALESCE(av.avaliacoes,0) avaliacoes FROM produtos p JOIN categorias c ON c.id=p.id_categoria JOIN estoque e ON e.id_produto=p.id"+MEDIA+"WHERE p.id=? AND p.ativo AND c.ativo")){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Produto indisponível.");return new Disponivel(mapear(r),r.getInt("quantidade"),r.getBigDecimal("media"),r.getLong("avaliacoes"));}
        }
    }
    public List<Produto> listarManutencao(String busca) throws SQLException {
        util.Acesso.equipe(); List<Produto> lista=new ArrayList<>();
        BuscaTexto.Filtro filtro=BuscaTexto.porPalavras("p.nome",busca);
        try(Connection c=ConexaoBanco.getConexao(); PreparedStatement ps=c.prepareStatement("SELECT p.*,c.nome AS nome_categoria FROM produtos p LEFT JOIN categorias c ON c.id=p.id_categoria WHERE "+filtro.sql()+" ORDER BY p.nome")){
            filtro.aplicar(ps);try(ResultSet r=ps.executeQuery()){while(r.next())lista.add(mapear(r));}
        }return lista;
    }
    public Produto buscarPorId(int id) throws SQLException {
        util.Acesso.admin();
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement ps=c.prepareStatement("SELECT p.*,c.nome AS nome_categoria FROM produtos p LEFT JOIN categorias c ON c.id=p.id_categoria WHERE p.id=?")){
            ps.setInt(1,id);try(ResultSet r=ps.executeQuery()){if(!r.next())throw new SQLException("Produto inexistente.");return mapear(r);}
        }
    }

    private static final Logger LOG = Logger.getLogger(ProdutoDAO.class.getName());

    public List<Produto> listarTodos() throws SQLException {
        List<Produto> lista = new ArrayList<>();
        // PostgreSQL: sem diferença aqui — JOIN padrão funciona igual
        String sql = "SELECT p.*, c.nome AS nome_categoria "
                   + "FROM produtos p "
                   + "LEFT JOIN categorias c ON p.id_categoria = c.id "
                   + "WHERE p.ativo = true AND c.ativo = true "
                   + "ORDER BY p.nome";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw e; }
        return lista;
    }

    public List<Produto> buscarPorNome(String nome) throws SQLException {
        List<Produto> lista = new ArrayList<>();
        BuscaTexto.Filtro filtro=BuscaTexto.porPalavras("p.nome",nome);
        String sql = "SELECT p.*, c.nome AS nome_categoria "
                   + "FROM produtos p "
                   + "LEFT JOIN categorias c ON p.id_categoria = c.id "
                   + "WHERE p.ativo = true AND c.ativo = true AND "+filtro.sql()+" "
                   + "ORDER BY p.nome";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            filtro.aplicar(ps);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw e; }
        return lista;
    }

    public boolean salvar(Produto p) throws SQLException {
        util.Acesso.admin();
        validar(p);
        int id = Transacao.executar(con -> {
            util.Acesso.equipe(con,true);
            CategoriaDAO.validarAtiva(con,p.getIdCategoria());
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO produtos (nome, descricao, preco, id_categoria, ativo, criado_por, imagem) VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getNome());
                ps.setString(2, p.getDescricao());
                ps.setBigDecimal(3, p.getPrecoDecimal());
                ps.setInt(4, p.getIdCategoria());
                ps.setBoolean(5, p.isAtivo());
                if (Sessao.getIdUsuario() > 0) ps.setInt(6, Sessao.getIdUsuario());
                else ps.setNull(6, Types.INTEGER);
                ps.setBytes(7,p.getImagem());
                if (ps.executeUpdate() != 1) throw new SQLException("Produto não gravado.");
                try (ResultSet chave = ps.getGeneratedKeys()) {
                    if (!chave.next()) throw new SQLException("Produto sem identificador gerado.");
                    int idNovo = chave.getInt(1);
                    inserirEstoqueInicial(con, idNovo, p);
                    return idNovo;
                }
            }
        });
        p.setId(id);
        return true;
    }

    private static void validar(Produto p) {
        p.setNome(util.Validacao.texto(p.getNome(),"Produto",2,120));
        if(p.getPrecoDecimal().compareTo(new java.math.BigDecimal("99999999.99"))>0) throw new IllegalArgumentException("Preço acima do limite.");
        util.Validacao.texto(p.getDescricao(),"Descrição",0,4000);
        try { util.Imagens.validar(p.getImagem()); } catch(java.io.IOException e){ throw new IllegalArgumentException("Imagem inválida.",e); }
        if (p.getNome() == null || p.getNome().isBlank() || p.getNome().length() > 120)
            throw new IllegalArgumentException("Informe um nome de produto com até 120 caracteres.");
        if (p.getIdCategoria() <= 0) throw new IllegalArgumentException("Selecione a categoria.");
    }

    public boolean atualizar(Produto p) throws SQLException {
        util.Acesso.admin();
        validar(p);
        String sql = "UPDATE produtos SET nome=?, descricao=?, preco=?, id_categoria=?, ativo=?, imagem=? "
                   + "WHERE id=?";
        return Transacao.executar(con -> {
            util.Acesso.equipe(con,true);
            if(p.isAtivo()) CategoriaDAO.validarAtiva(con,p.getIdCategoria());
            try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setString(2, p.getDescricao());
            ps.setBigDecimal(3, p.getPrecoDecimal());
            ps.setInt(4, p.getIdCategoria());
            ps.setBoolean(5, p.isAtivo());
            ps.setBytes(6,p.getImagem()); ps.setInt(7, p.getId());
            return ps.executeUpdate() > 0;
            }
        });
    }

    /** Desativa produto (soft delete — nunca apaga do banco) */
    public boolean excluir(int id) throws SQLException {
        util.Acesso.admin();
        String sql = "UPDATE produtos SET ativo = false WHERE id = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Privados ─────────────────────────────────────────────────────────

    private void inserirEstoqueInicial(Connection con, int idProduto, Produto p) throws SQLException {
        String sql = "INSERT INTO estoque (id_produto, quantidade, estoque_minimo) "
                   + "VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProduto);
            ps.setInt(2,p.getEstoqueInicial()); ps.setInt(3,p.getEstoqueMinimo());
            if (ps.executeUpdate() != 1) throw new SQLException("Estoque inicial não gravado.");
        }
        EstoqueDAO.registrar(con,idProduto,"inicial",p.getEstoqueInicial(),p.getEstoqueInicial(),"Cadastro de produto");
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setNome(rs.getString("nome"));
        p.setDescricao(rs.getString("descricao"));
        p.setPreco(rs.getBigDecimal("preco"));
        p.setIdCategoria(rs.getInt("id_categoria"));
        p.setNomeCategoria(rs.getString("nome_categoria"));
        p.setAtivo(rs.getBoolean("ativo"));
        p.setImagem(rs.getBytes("imagem"));
        return p;
    }
}
