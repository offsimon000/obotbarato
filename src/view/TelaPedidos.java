package view;

import dao.ClienteDAO;
import dao.PedidoDAO;
import dao.ProdutoDAO;
import model.*;
import util.Tema;
import util.PagamentoSimulado;
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
    private boolean salvando;
    private JButton btnFinalizar;
    private JButton btnVoltar;
    private JButton btnAdicionar,btnRemover;
    private final int sessaoUsuario=util.Sessao.getIdUsuario();

    private ClienteDAO clienteDAO = new ClienteDAO();
    private ProdutoDAO produtoDAO = new ProdutoDAO();
    private PedidoDAO  pedidoDAO  = new PedidoDAO();

    public TelaPedidos(Usuario usuario, JFrame telaAnterior) {
        util.Acesso.telaEquipe();
        this.usuarioLogado = usuario;
        this.telaAnterior  = telaAnterior;
        initComponents();
        carregarCombos();
    }

    private void initComponents() {
        setTitle("OBotBarato - Nova venda no balcão");
        setSize(780, 600);
        setMinimumSize(new Dimension(780, 520));
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
        btnVoltar=Tema.botaoVoltar(this,telaAnterior,()->!salvando);headerEsq.add(btnVoltar);
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(Tema.BORDA); sep.setPreferredSize(new Dimension(1, 20));
        headerEsq.add(sep);
        JLabel lblTitulo = new JLabel("Venda no balcão — retirada e pagamento simulado");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        header.add(headerEsq, BorderLayout.WEST);
        header.add(lblTitulo, BorderLayout.CENTER);
        painel.add(header, BorderLayout.NORTH);

        // ── Selecao cliente ──
        JPanel painelCliente = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        painelCliente.setBackground(Tema.BG_ESCURO);
        JLabel lblCli = new JLabel("CLIENTE:");
        lblCli.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCli.setForeground(Tema.TEXTO_SECUNDARIO);
        cbCliente = new JComboBox<>();
        cbCliente.setName("balcao-cliente");
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
        cbProduto.setName("balcao-produto");
        cbProduto.setBackground(Tema.BG_INPUT);
        cbProduto.setForeground(Tema.TEXTO_PRIMARIO);
        cbProduto.setFont(Tema.FONTE_CAMPO);
        cbProduto.setPreferredSize(new Dimension(380, 34));

        JLabel lblQtd = new JLabel("QTD:");
        lblQtd.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblQtd.setForeground(Tema.TEXTO_SECUNDARIO);
        spnQtd = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spnQtd.setName("balcao-quantidade");
        spnQtd.setPreferredSize(new Dimension(70, 34));

        btnAdicionar = Tema.botaoPrimario("+ Adicionar");
        btnAdicionar.addActionListener(e -> adicionarItem());

        painelAdd.add(lblProd); painelAdd.add(cbProduto);
        painelAdd.add(lblQtd);  painelAdd.add(spnQtd);
        painelAdd.add(btnAdicionar);

        // ── Tabela itens ──
        String[] cols = {"Produto", "Qtd", "Preco Unit.", "Subtotal"};
        modeloItens = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        tabelaItens = new JTable(modeloItens);
        tabelaItens.setName("balcao-itens");
        Tema.aplicarTabelaDark(tabelaItens);
        tabelaItens.getColumnModel().getColumn(0).setPreferredWidth(280);

        JPanel tabelaPanel = new JPanel(new BorderLayout());
        tabelaPanel.setBackground(Tema.BG_ESCURO);
        tabelaPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 0, 14));
        tabelaPanel.add(Tema.scrollDark(tabelaItens), BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Tema.BG_ESCURO);
        JPanel selecao = new JPanel(new BorderLayout());
        selecao.setOpaque(false);
        selecao.add(painelCliente, BorderLayout.NORTH);
        selecao.add(painelAdd, BorderLayout.CENTER);
        centro.add(selecao, BorderLayout.NORTH);
        centro.add(tabelaPanel, BorderLayout.CENTER);
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
        btnRemover = Tema.botaoSecundario("Remover Item");
        btnFinalizar = Tema.botaoPrimario("Concluir retirada");
        btnFinalizar.setBackground(Tema.VERDE_SUCESSO);

        btnRemover.addActionListener(e -> removerItem());
        btnFinalizar.addActionListener(e -> finalizarPedido());

        btns.add(btnRemover); btns.add(btnFinalizar);
        rodape.add(btns, BorderLayout.EAST);
        painel.add(rodape, BorderLayout.SOUTH);

        setContentPane(painel);
        cbCliente.addActionListener(e -> atualizarAcoes());
        cbProduto.addActionListener(e -> {cbProduto.setToolTipText((String)cbProduto.getSelectedItem());atualizarAcoes();});
        tabelaItens.getSelectionModel().addListSelectionListener(e -> atualizarAcoes());
        atualizarAcoes();
    }

    private void carregarCombos() {
        try {
            clientes = clienteDAO.listarTodos();
            cbCliente.addItem("-- Selecione o cliente --");
            for (Cliente c : clientes) cbCliente.addItem(c.getNome() + (c.getCpf()==null || c.getCpf().isBlank()?"":"  |  "+c.getCpf()));

            produtos = produtoDAO.listarTodos();
            cbProduto.addItem("-- Selecione o produto --");
            for (Produto p : produtos) cbProduto.addItem(p.getNome() + "  |  " + PagamentoSimulado.moeda(p.getPrecoDecimal()));
        } catch (Exception e) { aviso("Erro: " + util.Erros.mensagem(e)); }
        atualizarAcoes();
    }

    private void atualizarAcoes(){
        btnAdicionar.setEnabled(!salvando && cbProduto.getSelectedIndex()>0);
        btnRemover.setEnabled(!salvando && tabelaItens.getSelectedRow()>=0);
        btnFinalizar.setEnabled(!salvando && cbCliente.getSelectedIndex()>0 && modeloItens.getRowCount()>0);
        btnVoltar.setEnabled(!salvando);cbCliente.setEnabled(!salvando);cbProduto.setEnabled(!salvando);spnQtd.setEnabled(!salvando);
    }

    private void adicionarItem() {
        if(salvando)return;
        int idx = cbProduto.getSelectedIndex();
        if (idx <= 0) { aviso("Selecione um produto."); return; }
        try{spnQtd.commitEdit();}catch(java.text.ParseException e){aviso("Informe uma quantidade válida entre 1 e 999.");return;}
        Produto p = produtos.get(idx - 1);
        int qtd = (int) spnQtd.getValue();
        ItemPedido item = new ItemPedido();item.setIdProduto(p.getId());item.setNomeProduto(p.getNome());item.setQuantidade(qtd);item.setPrecoUnitario(p.getPrecoDecimal());
        pedidoAtual.adicionarItem(item);
        modeloItens.addRow(new Object[]{
            p.getNome(), qtd,
            PagamentoSimulado.moeda(p.getPrecoDecimal()),
            PagamentoSimulado.moeda(item.getSubtotalDecimal())
        });
        lblTotal.setText("Total: "+PagamentoSimulado.moeda(pedidoAtual.getTotalDecimal()));
        atualizarAcoes();
    }

    private void removerItem() {
        if(salvando)return;
        int l = tabelaItens.getSelectedRow();
        if (l < 0) { aviso("Selecione um item."); return; }
        pedidoAtual.removerItem(pedidoAtual.getItens().get(l));
        modeloItens.removeRow(l);
        lblTotal.setText("Total: "+PagamentoSimulado.moeda(pedidoAtual.getTotalDecimal()));
        atualizarAcoes();
    }

    private void finalizarPedido() {
        if(salvando)return;
        if (cbCliente.getSelectedIndex() <= 0) { aviso("Selecione um cliente."); return; }
        if (pedidoAtual.getItens().isEmpty())  { aviso("Adicione ao menos um item."); return; }
        Cliente cli = clientes.get(cbCliente.getSelectedIndex() - 1);
        if (confirmarRetirada(cli)) {
            pedidoAtual.setIdCliente(cli.getId());pedidoAtual.setStatus("entregue");
            salvando=true;atualizarAcoes();
            new SwingWorker<Void,Void>(){
                protected Void doInBackground()throws Exception{pedidoDAO.salvar(pedidoAtual);return null;}
                protected void done(){salvando=false;if(util.Sessao.getIdUsuario()!=sessaoUsuario || !isDisplayable())return;atualizarAcoes();try{get();vendaConcluida(pedidoAtual.getId());dispose();if(telaAnterior!=null)telaAnterior.setVisible(true);}catch(Exception e){aviso("Erro: "+util.Erros.mensagem(e));}}
            }.execute();
        }
    }
    protected boolean confirmarRetirada(Cliente cliente){return JOptionPane.showConfirmDialog(this,"Concluir retirada de "+cliente.getNome()+"?\nTotal: "+PagamentoSimulado.moeda(pedidoAtual.getTotalDecimal())+"\nPagamento SIMULADO em dinheiro. A retirada será registrada como entregue.","Venda no balcão",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    protected void vendaConcluida(int id){JOptionPane.showMessageDialog(this,"Pedido #"+id+" entregue no balcão. Pagamento simulado confirmado.");}
    protected void aviso(String mensagem){JOptionPane.showMessageDialog(this,mensagem);}
}
