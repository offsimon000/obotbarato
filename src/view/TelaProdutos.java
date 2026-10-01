package view;

import dao.ProdutoDAO;
import model.Produto;
import util.Tema;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TelaProdutos extends JFrame {

    private JTable tabela;
    private DefaultTableModel modeloTabela;
    private JTextField txtBusca;
    private ProdutoDAO dao = new ProdutoDAO();
    private JFrame telaAnterior;

    public TelaProdutos(JFrame telaAnterior) {
        this.telaAnterior = telaAnterior;
        initComponents();
        carregarTabela("");
    }

    private void initComponents() {
        setTitle("OBotBarato - Produtos");
        setSize(860, 540);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(Tema.BG_ESCURO);

        // ── Cabecalho ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Tema.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
            BorderFactory.createEmptyBorder(12, 16, 12, 20)));

        JPanel headerEsq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerEsq.setOpaque(false);
        headerEsq.add(Tema.botaoVoltar(this, telaAnterior));
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(Tema.BORDA);
        sep.setPreferredSize(new Dimension(1, 20));
        headerEsq.add(sep);
        JLabel lblTitulo = new JLabel("Produtos");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        headerEsq.add(lblTitulo);
        header.add(headerEsq, BorderLayout.WEST);
        painel.add(header, BorderLayout.NORTH);

        // ── Barra de busca ──
        JPanel barraBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        barraBusca.setBackground(Tema.BG_ESCURO);
        txtBusca = Tema.campo(24);
        txtBusca.putClientProperty("JTextField.placeholderText", "Buscar produto...");
        JButton btnBuscar = Tema.botaoSecundario("Buscar");
        btnBuscar.addActionListener(e -> carregarTabela(txtBusca.getText()));
        txtBusca.addActionListener(e -> carregarTabela(txtBusca.getText()));
        barraBusca.add(txtBusca);
        barraBusca.add(btnBuscar);

        // ── Tabela ──
        String[] colunas = {"ID", "Nome", "Categoria", "Preco", "Status"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modeloTabela);
        Tema.aplicarTabelaDark(tabela);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(280);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Tema.BG_ESCURO);
        centro.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        centro.add(barraBusca, BorderLayout.NORTH);
        centro.add(Tema.scrollDark(tabela), BorderLayout.CENTER);
        painel.add(centro, BorderLayout.CENTER);

        // ── Rodape com botoes ──
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rodape.setBackground(Tema.BG_CARD);
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA));

        JButton btnNovo    = Tema.botaoPrimario("+ Novo Produto");
        JButton btnEditar  = Tema.botaoSecundario("Editar");
        JButton btnExcluir = Tema.botaoSecundario("Excluir");
        btnExcluir.setForeground(Tema.VERMELHO_ERRO);

        btnNovo.addActionListener(e -> { new FormularioProduto(this, null).setVisible(true); carregarTabela(""); });
        btnEditar.addActionListener(e -> {
            int l = tabela.getSelectedRow();
            if (l < 0) { aviso("Selecione um produto."); return; }
            new FormularioProduto(this, (int) modeloTabela.getValueAt(l, 0)).setVisible(true);
            carregarTabela("");
        });
        btnExcluir.addActionListener(e -> excluir());

        rodape.add(btnExcluir); rodape.add(btnEditar); rodape.add(btnNovo);
        painel.add(rodape, BorderLayout.SOUTH);

        setContentPane(painel);
    }

    private void carregarTabela(String busca) {
        modeloTabela.setRowCount(0);
        try {
            List<Produto> lista = busca.isEmpty() ? dao.listarTodos() : dao.buscarPorNome(busca);
            for (Produto p : lista)
                modeloTabela.addRow(new Object[]{
                    p.getId(), p.getNome(), p.getNomeCategoria(),
                    String.format("R$ %.2f", p.getPreco()),
                    p.isAtivo() ? "Ativo" : "Inativo"
                });
        } catch (Exception ex) { erro(util.Erros.mensagem(ex)); }
    }

    private void excluir() {
        int l = tabela.getSelectedRow();
        if (l < 0) { aviso("Selecione um produto."); return; }
        if (confirmar("Desativar este produto?"))
            try { if (!dao.excluir((int) modeloTabela.getValueAt(l, 0))) throw new java.sql.SQLException("Produto não encontrado."); carregarTabela(""); }
            catch (Exception ex) { erro(util.Erros.mensagem(ex)); }
    }

    private void aviso(String m)   { JOptionPane.showMessageDialog(this, m, "Aviso", JOptionPane.WARNING_MESSAGE); }
    private void erro(String m)    { JOptionPane.showMessageDialog(this, m, "Erro",  JOptionPane.ERROR_MESSAGE); }
    private boolean confirmar(String m) {
        return JOptionPane.showConfirmDialog(this, m, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}