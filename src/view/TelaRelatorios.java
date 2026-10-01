package view;

import dao.EstoqueDAO;
import dao.PedidoDAO;
import util.Tema;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class TelaRelatorios extends JFrame {

    private PedidoDAO  pedidoDAO  = new PedidoDAO();
    private EstoqueDAO estoqueDAO = new EstoqueDAO();
    private JFrame telaAnterior;

    public TelaRelatorios(JFrame telaAnterior) {
        this.telaAnterior = telaAnterior;
        initComponents();
    }

    private void initComponents() {
        setTitle("OBotBarato - Relatorios");
        setSize(800, 580);
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
        sep.setForeground(Tema.BORDA); sep.setPreferredSize(new Dimension(1, 20));
        headerEsq.add(sep);
        JLabel lblTitulo = new JLabel("Relatorios");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        headerEsq.add(lblTitulo);
        header.add(headerEsq, BorderLayout.WEST);
        painel.add(header, BorderLayout.NORTH);

        // ── Abas ──
        JTabbedPane abas = new JTabbedPane();
        abas.setBackground(Tema.BG_ESCURO);
        abas.setForeground(Tema.TEXTO_PRIMARIO);
        abas.setFont(new Font("Segoe UI", Font.BOLD, 13));
        abas.addTab("Vendas por Periodo",     criarAbaVendas());
        abas.addTab("Produtos Mais Vendidos", criarAbaProdutos());
        abas.addTab("Estoque Baixo",          criarAbaEstoque());

        painel.add(abas, BorderLayout.CENTER);
        setContentPane(painel);
    }

    private JPanel criarAbaVendas() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBackground(Tema.BG_ESCURO);
        painel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtros.setBackground(Tema.BG_ESCURO);

        JLabel lblDe = rotulo("DE:"); JTextField txtInicio = Tema.campo(12);
        txtInicio.setText(LocalDate.now().withDayOfMonth(1).toString());
        JLabel lblAte = rotulo("ATE:"); JTextField txtFim = Tema.campo(12);
        txtFim.setText(LocalDate.now().toString());
        JButton btnGerar = Tema.botaoPrimario("Gerar Relatorio");

        filtros.add(lblDe); filtros.add(txtInicio);
        filtros.add(lblAte); filtros.add(txtFim);
        filtros.add(btnGerar);
        painel.add(filtros, BorderLayout.NORTH);

        String[] colunas = {"Data", "Qtd. Pedidos", "Total do Dia"};
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tabela = new JTable(modelo);
        Tema.aplicarTabelaDark(tabela);
        painel.add(Tema.scrollDark(tabela), BorderLayout.CENTER);

        JLabel lblTotalGeral = new JLabel("Total Geral: R$ 0,00", SwingConstants.RIGHT);
        lblTotalGeral.setFont(Tema.FONTE_TOTAL);
        lblTotalGeral.setForeground(Tema.VERDE_SUCESSO);
        lblTotalGeral.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        painel.add(lblTotalGeral, BorderLayout.SOUTH);

        btnGerar.addActionListener(e -> {
            modelo.setRowCount(0);
            try {
                List<String[]> dados = pedidoDAO.relatorioVendasPorPeriodo(txtInicio.getText(), txtFim.getText());
                double total = 0;
                for (String[] row : dados) { modelo.addRow(row); total += Double.parseDouble(row[2].replace(",", ".")); }
                lblTotalGeral.setText(String.format("Total Geral: R$ %.2f", total));
                if (dados.isEmpty()) JOptionPane.showMessageDialog(this, "Nenhum pedido no periodo.");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
        });
        return painel;
    }

    private JPanel criarAbaProdutos() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBackground(Tema.BG_ESCURO);
        painel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topo.setBackground(Tema.BG_ESCURO);
        JLabel info = new JLabel("Top 10 produtos mais vendidos (pedidos finalizados)");
        info.setFont(Tema.FONTE_LABEL); info.setForeground(Tema.TEXTO_SECUNDARIO);
        JButton btnGerar = Tema.botaoPrimario("Gerar Ranking");
        topo.add(info); topo.add(btnGerar);
        painel.add(topo, BorderLayout.NORTH);

        String[] colunas = {"Produto", "Qtd. Vendida", "Receita Total"};
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tabela = new JTable(modelo);
        Tema.aplicarTabelaDark(tabela);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(300);
        painel.add(Tema.scrollDark(tabela), BorderLayout.CENTER);

        btnGerar.addActionListener(e -> {
            modelo.setRowCount(0);
            try {
                List<String[]> dados = pedidoDAO.relatorioProdutosMaisVendidos();
                for (String[] row : dados) modelo.addRow(row);
                if (dados.isEmpty()) JOptionPane.showMessageDialog(this, "Nenhum dado encontrado.");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
        });
        return painel;
    }

    private JPanel criarAbaEstoque() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBackground(Tema.BG_ESCURO);
        painel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topo.setBackground(Tema.BG_ESCURO);
        JLabel info = new JLabel("Produtos com estoque abaixo do minimo definido:");
        info.setFont(Tema.FONTE_LABEL); info.setForeground(Tema.TEXTO_SECUNDARIO);
        JButton btnGerar = Tema.botaoSecundario("Verificar Estoque");
        btnGerar.setForeground(Tema.AMARELO_AVISO);
        topo.add(info); topo.add(btnGerar);
        painel.add(topo, BorderLayout.NORTH);

        String[] colunas = {"Produto", "Qtd. Atual", "Estoque Minimo"};
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tabela = new JTable(modelo);
        Tema.aplicarTabelaDark(tabela);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(300);
        painel.add(Tema.scrollDark(tabela), BorderLayout.CENTER);

        JLabel aviso = new JLabel("Produtos listados aqui precisam de reposicao urgente.", SwingConstants.CENTER);
        aviso.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        aviso.setForeground(Tema.AMARELO_AVISO);
        aviso.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        painel.add(aviso, BorderLayout.SOUTH);

        btnGerar.addActionListener(e -> {
            modelo.setRowCount(0);
            try {
                List<String[]> dados = estoqueDAO.listarEstoqueBaixo();
                for (String[] row : dados) modelo.addRow(row);
                if (dados.isEmpty()) JOptionPane.showMessageDialog(this, "Todos os produtos estao com estoque OK!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
        });
        return painel;
    }

    private JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(Tema.TEXTO_SECUNDARIO);
        return l;
    }
}