package view.loja;

import dao.ClienteLojaDAO;
import dao.ProdutoDAO;
import model.Cliente;
import model.ItemPedido;
import model.Produto;
import util.TemaLoja;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TelaFavoritos extends JFrame {

    private final TelaLoja        telaLoja;
    private final Cliente         clienteLogado;
    private DefaultTableModel     modelo;
    private JTable                tabela;
    private List<Produto>         produtosExibidos = new ArrayList<>();

    private final ClienteLojaDAO clienteDAO = new ClienteLojaDAO();
    private final ProdutoDAO     produtoDAO = new ProdutoDAO();

    public TelaFavoritos(TelaLoja telaLoja, Cliente cliente) {
        this.telaLoja      = telaLoja;
        this.clienteLogado = cliente;
        initComponents();
        carregarFavoritos();
    }

    // ── Inicialização ────────────────────────────────────────────────────
    private void initComponents() {
        setTitle("OBotBarato - Favoritos");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(TemaLoja.BG_PRINCIPAL);
        painel.add(criarHeader(),    BorderLayout.NORTH);
        painel.add(criarTabela(),    BorderLayout.CENTER);
        painel.add(criarRodape(),    BorderLayout.SOUTH);
        setContentPane(painel);
    }

    // ── Header ───────────────────────────────────────────────────────────
    private JPanel criarHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(TemaLoja.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, TemaLoja.BORDA),
            BorderFactory.createEmptyBorder(12, 16, 12, 20)));

        JPanel esq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        esq.setOpaque(false);

        JButton btnVoltar = TemaLoja.botaoFantasia("< Voltar");
        btnVoltar.addActionListener(e -> dispose());
        esq.add(btnVoltar);

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(TemaLoja.BORDA);
        sep.setPreferredSize(new Dimension(1, 20));
        esq.add(sep);

        JLabel lblStar = new JLabel("*");
        lblStar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblStar.setForeground(TemaLoja.AMARELO_FAV);

        JLabel lblTit = new JLabel("Meus Favoritos");
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTit.setForeground(TemaLoja.TEXTO_PRIMARIO);

        esq.add(lblStar);
        esq.add(lblTit);
        header.add(esq, BorderLayout.WEST);
        return header;
    }

    // ── Tabela ───────────────────────────────────────────────────────────
    private JPanel criarTabela() {
        String[] colunas = {"Produto", "Categoria", "Preco", "Disponivel"};
        modelo = new DefaultTableModel(colunas, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tabela = new JTable(modelo);
        tabela.setBackground(TemaLoja.BG_CARD);
        tabela.setForeground(TemaLoja.TEXTO_PRIMARIO);
        tabela.setGridColor(TemaLoja.BORDA);
        tabela.setFont(TemaLoja.FONTE_LABEL);
        tabela.setRowHeight(34);
        tabela.setSelectionBackground(TemaLoja.ROSA_PRIMARIO);
        tabela.setSelectionForeground(Color.WHITE);
        tabela.getTableHeader().setBackground(TemaLoja.BG_PRINCIPAL);
        tabela.getTableHeader().setForeground(TemaLoja.TEXTO_SECUNDARIO);
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabela.setShowVerticalLines(false);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(260);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(120);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(90);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(90);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(TemaLoja.BG_PRINCIPAL);
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 0, 14));
        p.add(TemaLoja.scroll(tabela), BorderLayout.CENTER);
        return p;
    }

    // ── Rodapé ───────────────────────────────────────────────────────────
    private JPanel criarRodape() {
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rodape.setBackground(TemaLoja.BG_CARD);
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, TemaLoja.BORDA));

        JButton btnRemover  = TemaLoja.botaoFantasia("Remover Favorito");
        btnRemover.setForeground(TemaLoja.VERMELHO_ERRO);
        btnRemover.addActionListener(e -> removerFavorito());

        JButton btnCarrinho = TemaLoja.botaoPrimario("+ Adicionar ao Carrinho");
        btnCarrinho.addActionListener(e -> adicionarAoCarrinho());

        rodape.add(btnRemover);
        rodape.add(btnCarrinho);
        return rodape;
    }

    // ── Carregamento ─────────────────────────────────────────────────────
    private void carregarFavoritos() {
        modelo.setRowCount(0);
        produtosExibidos.clear();

        try {
            // Uma query busca todos os IDs de favoritos
            Set<Integer> ids = clienteDAO.listarFavoritosSet(clienteLogado.getId());

            if (ids.isEmpty()) {
                modelo.addRow(new Object[]{"Nenhum favorito ainda.", "", "", ""});
                return;
            }

            // Filtra a lista de produtos pelos IDs favoritos
            List<Produto> todos = produtoDAO.listarTodos();
            for (Produto p : todos) {
                if (!ids.contains(p.getId())) continue;
                produtosExibidos.add(p);
                modelo.addRow(new Object[]{
                    p.getNome(),
                    p.getNomeCategoria(),
                    String.format("R$ %.2f", p.getPreco()),
                    p.isAtivo() ? "Sim" : "Inativo"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar favoritos: " + util.Erros.mensagem(ex));
        }
    }

    // ── Ações ────────────────────────────────────────────────────────────
    private void removerFavorito() {
        int linha = tabela.getSelectedRow();
        if (linha < 0 || linha >= produtosExibidos.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um produto para remover.");
            return;
        }
        Produto p = produtosExibidos.get(linha);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Remover \"" + p.getNome() + "\" dos favoritos?",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                clienteDAO.removerFavorito(clienteLogado.getId(), p.getId());
                carregarFavoritos();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, util.Erros.mensagem(ex)); }
        }
    }

    private void adicionarAoCarrinho() {
        int linha = tabela.getSelectedRow();
        if (linha < 0 || linha >= produtosExibidos.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um produto.");
            return;
        }
        Produto p = produtosExibidos.get(linha);

        // Adiciona ao carrinho da TelaLoja
        List<ItemPedido> carrinho = telaLoja.getCarrinho();
        for (ItemPedido item : carrinho) {
            if (item.getIdProduto() == p.getId()) {
                item.setQuantidade(item.getQuantidade() + 1);
                telaLoja.atualizarBadgeCarrinho();
                JOptionPane.showMessageDialog(this,
                    p.getNome() + " — quantidade atualizada no carrinho!");
                return;
            }
        }
        carrinho.add(new ItemPedido(p.getId(), p.getNome(), 1, p.getPreco()));
        telaLoja.atualizarBadgeCarrinho();
        JOptionPane.showMessageDialog(this, p.getNome() + " adicionado ao carrinho!");
    }
}