package view.loja;

import dao.PedidoDAO;
import model.Cliente;
import model.Pedido;
import util.TemaLoja;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class TelaMeusPedidos extends JFrame {

    private TelaLoja telaLoja;
    private Cliente clienteLogado;
    private DefaultTableModel modelo;
    private PedidoDAO pedidoDAO = new PedidoDAO();

    public TelaMeusPedidos(TelaLoja telaLoja, Cliente cliente) {
        this.telaLoja      = telaLoja;
        this.clienteLogado = cliente;
        initComponents();
        carregarPedidos();
    }

    private void initComponents() {
        setTitle("OBotBarato - Meus Pedidos");
        setSize(720, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(TemaLoja.BG_PRINCIPAL);

        // Cabecalho
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(TemaLoja.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, TemaLoja.BORDA),
            BorderFactory.createEmptyBorder(12, 16, 12, 20)));

        JPanel headerEsq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerEsq.setOpaque(false);
        JButton btnV = TemaLoja.botaoFantasia("< Voltar");
        btnV.addActionListener(e -> dispose());
        headerEsq.add(btnV);

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(TemaLoja.BORDA);
        sep.setPreferredSize(new Dimension(1, 20));
        headerEsq.add(sep);

        JLabel lblTit = new JLabel("Historico de Pedidos");
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTit.setForeground(TemaLoja.TEXTO_PRIMARIO);
        headerEsq.add(lblTit);
        header.add(headerEsq, BorderLayout.WEST);

        JButton btnAtualizar = TemaLoja.botaoFantasia("Atualizar");
        btnAtualizar.addActionListener(e -> carregarPedidos());
        JPanel headerDir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerDir.setOpaque(false);
        headerDir.add(btnAtualizar);
        header.add(headerDir, BorderLayout.EAST);
        painel.add(header, BorderLayout.NORTH);

        // Tabela
        String[] colunas = {"# Pedido", "Data", "Total", "Status"};
        modelo = new DefaultTableModel(colunas, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(modelo) {
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                String status = (String) getValueAt(row, 3);
                if ("finalizado".equals(status)) c.setForeground(TemaLoja.VERDE_PRECO);
                else if ("cancelado".equals(status)) c.setForeground(TemaLoja.VERMELHO_ERRO);
                else c.setForeground(TemaLoja.AMARELO_FAV);
                c.setBackground(isRowSelected(row) ? TemaLoja.ROSA_PRIMARIO : TemaLoja.BG_CARD);
                return c;
            }
        };
        tabela.setBackground(TemaLoja.BG_CARD);
        tabela.setGridColor(TemaLoja.BORDA);
        tabela.setFont(TemaLoja.FONTE_LABEL);
        tabela.setRowHeight(34);
        tabela.getTableHeader().setBackground(TemaLoja.BG_PRINCIPAL);
        tabela.getTableHeader().setForeground(TemaLoja.TEXTO_SECUNDARIO);
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabela.setShowVerticalLines(false);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(180);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(120);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(TemaLoja.BG_PRINCIPAL);
        centro.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        centro.add(TemaLoja.scroll(tabela), BorderLayout.CENTER);
        painel.add(centro, BorderLayout.CENTER);

        // Info rodape
        JLabel lblInfo = new JLabel("Os pedidos realizados na loja tambem aparecem no painel administrativo.", SwingConstants.CENTER);
        lblInfo.setFont(TemaLoja.FONTE_LABEL);
        lblInfo.setForeground(TemaLoja.TEXTO_HINT);
        lblInfo.setBorder(BorderFactory.createEmptyBorder(6, 0, 10, 0));
        painel.add(lblInfo, BorderLayout.SOUTH);

        setContentPane(painel);
    }

    private void carregarPedidos() {
        modelo.setRowCount(0);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            List<Pedido> todos = pedidoDAO.listarPorCliente(clienteLogado.getId());
            int count = 0;
            for (Pedido p : todos) {
                if (p.getIdCliente() == clienteLogado.getId()) {
                    modelo.addRow(new Object[]{
                        "#" + p.getId(),
                        p.getDataPedido() != null ? sdf.format(p.getDataPedido()) : "-",
                        String.format("R$ %.2f", p.getTotal()),
                        p.getStatus()
                    });
                    count++;
                }
            }
            if (count == 0) {
                modelo.addRow(new Object[]{"", "Nenhum pedido realizado ainda.", "", ""});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
        }
    }
}
