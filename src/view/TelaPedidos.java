package view;

import dao.ClienteDAO;
import dao.PedidoDAO;
import dao.ProdutoDAO;
import model.*;
import util.Tema;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TelaPedidos extends JFrame {

    private JComboBox<String> cbCliente, cbProduto;
    private JSpinner spnQtd;
    private JTable tabelaItens;
    private DefaultTableModel modeloItens;
    private JLabel lblTotal;
    private Pedido pedidoAtual = new Pedido();
    private List<Cliente> clientes;
    private List<Produto> produtos;
    private Usuario usuarioLogado;
    private JFrame telaAnterior;

    private ClienteDAO clienteDAO = new ClienteDAO();
    private ProdutoDAO produtoDAO = new ProdutoDAO();
    private PedidoDAO  pedidoDAO  = new PedidoDAO();

    public TelaPedidos(Usuario usuario, JFrame telaAnterior) {
        this.usuarioLogado = usuario;
        this.telaAnterior  = telaAnterior;
        initComponents();
        carregarCombos();
    }

    private void initComponents() {
        setTitle("OBotBarato - Novo Pedido");
        setSize(780, 600);
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
        JLabel lblTitulo = new JLabel("Novo Pedido");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        headerEsq.add(lblTitulo);
        header.add(headerEsq, BorderLayout.WEST);
        painel.add(header, BorderLayout.NORTH);

        // ── Selecao cliente ──
        JPanel painelCliente = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        painelCliente.setBackground(Tema.BG_ESCURO);
        JLabel lblCli = new JLabel("CLIENTE:");
        lblCli.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCli.setForeground(Tema.TEXTO_SECUNDARIO);
        cbCliente = new JComboBox<>();
        cbCliente.setBackground(Tema.BG_INPUT);
        cbCliente.setForeground(Tema.TEXTO_PRIMARIO);
        cbCliente.setFont(Tema.FONTE_CAMPO);
        cbCliente.setPreferredSize(new Dimension(300, 34));
        painelCliente.add(lblCli); painelCliente.add(cbCliente);

        // ── Adicionar produto ──
        JPanel painelAdd = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        painelAdd.setBackground(Tema.BG_CARD);
        painelAdd.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, Tema.BORDA),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JLabel lblProd = new JLabel("PRODUTO:");
        lblProd.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblProd.setForeground(Tema.TEXTO_SECUNDARIO);
        cbProduto = new JComboBox<>();
        cbProduto.setBackground(Tema.BG_INPUT);
        cbProduto.setForeground(Tema.TEXTO_PRIMARIO);
        cbProduto.setFont(Tema.FONTE_CAMPO);
        cbProduto.setPreferredSize(new Dimension(260, 34));

        JLabel lblQtd = new JLabel("QTD:");
        lblQtd.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblQtd.setForeground(Tema.TEXTO_SECUNDARIO);
        spnQtd = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spnQtd.setPreferredSize(new Dimension(70, 34));

        JButton btnAdd = Tema.botaoPrimario("+ Adicionar");
        btnAdd.addActionListener(e -> adicionarItem());

        painelAdd.add(lblProd); painelAdd.add(cbProduto);
        painelAdd.add(lblQtd);  painelAdd.add(spnQtd);
        painelAdd.add(btnAdd);

        // ── Tabela itens ──
        String[] cols = {"Produto", "Qtd", "Preco Unit.", "Subtotal"};
        modeloItens = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        tabelaItens = new JTable(modeloItens);
        Tema.aplicarTabelaDark(tabelaItens);
        tabelaItens.getColumnModel().getColumn(0).setPreferredWidth(280);

        JPanel tabelaPanel = new JPanel(new BorderLayout());
        tabelaPanel.setBackground(Tema.BG_ESCURO);
        tabelaPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 0, 14));
        tabelaPanel.add(Tema.scrollDark(tabelaItens), BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Tema.BG_ESCURO);
        centro.add(painelCliente, BorderLayout.NORTH);
        centro.add(painelAdd,     BorderLayout.CENTER);
        centro.add(tabelaPanel,   BorderLayout.SOUTH);
        painel.add(centro, BorderLayout.CENTER);

        // ── Rodape ──
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setBackground(Tema.BG_CARD);
        rodape.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        lblTotal = new JLabel("Total: R$ 0,00");
        lblTotal.setFont(Tema.FONTE_TOTAL);
        lblTotal.setForeground(Tema.VERDE_SUCESSO);
        rodape.add(lblTotal, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        JButton btnRemover   = Tema.botaoSecundario("Remover Item");
        JButton btnFinalizar = Tema.botaoPrimario("Finalizar Pedido");
        btnFinalizar.setBackground(Tema.VERDE_SUCESSO);

        btnRemover.addActionListener(e -> removerItem());
        btnFinalizar.addActionListener(e -> finalizarPedido());

        btns.add(btnRemover); btns.add(btnFinalizar);
        rodape.add(btns, BorderLayout.EAST);
        painel.add(rodape, BorderLayout.SOUTH);

        setContentPane(painel);
    }

    private void carregarCombos() {
        try {
            clientes = clienteDAO.listarTodos();
            cbCliente.addItem("-- Selecione o cliente --");
            for (Cliente c : clientes) cbCliente.addItem(c.getNome() + "  |  " + c.getCpf());

            produtos = produtoDAO.listarTodos();
            cbProduto.addItem("-- Selecione o produto --");
            for (Produto p : produtos) cbProduto.addItem(p.getNome() + "  |  R$ " + String.format("%.2f", p.getPreco()));
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erro: " + util.Erros.mensagem(e)); }
    }

    private void adicionarItem() {
        int idx = cbProduto.getSelectedIndex();
        if (idx <= 0) { JOptionPane.showMessageDialog(this, "Selecione um produto."); return; }
        Produto p = produtos.get(idx - 1);
        int qtd = (int) spnQtd.getValue();
        ItemPedido item = new ItemPedido(p.getId(), p.getNome(), qtd, p.getPreco());
        pedidoAtual.adicionarItem(item);
        modeloItens.addRow(new Object[]{
            p.getNome(), qtd,
            String.format("R$ %.2f", p.getPreco()),
            String.format("R$ %.2f", item.getSubtotal())
        });
        lblTotal.setText(String.format("Total: R$ %.2f", pedidoAtual.getTotal()));
    }

    private void removerItem() {
        int l = tabelaItens.getSelectedRow();
        if (l < 0) { JOptionPane.showMessageDialog(this, "Selecione um item."); return; }
        pedidoAtual.removerItem(pedidoAtual.getItens().get(l));
        modeloItens.removeRow(l);
        lblTotal.setText(String.format("Total: R$ %.2f", pedidoAtual.getTotal()));
    }

    private void finalizarPedido() {
        if (cbCliente.getSelectedIndex() <= 0) { JOptionPane.showMessageDialog(this, "Selecione um cliente."); return; }
        if (pedidoAtual.getItens().isEmpty())  { JOptionPane.showMessageDialog(this, "Adicione ao menos um item."); return; }
        Cliente cli = clientes.get(cbCliente.getSelectedIndex() - 1);
        int confirm = JOptionPane.showConfirmDialog(this,
            String.format("Finalizar pedido de %s?\nTotal: R$ %.2f", cli.getNome(), pedidoAtual.getTotal()),
            "Confirmar Pedido", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                pedidoAtual.setIdCliente(cli.getId());
                pedidoAtual.setStatus("finalizado");
                pedidoDAO.salvar(pedidoAtual);
                JOptionPane.showMessageDialog(this, "Pedido #" + pedidoAtual.getId() + " finalizado!");
                dispose();
                if (telaAnterior != null) telaAnterior.setVisible(true);
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erro: " + util.Erros.mensagem(e)); }
        }
    }
}