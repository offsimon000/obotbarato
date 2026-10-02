package view.loja;

import dao.PedidoDAO;
import model.Cliente;
import model.ItemPedido;
import model.Pedido;
import util.TemaLoja;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TelaCheckout extends JFrame {

    private TelaCarrinho telaCarrinho;
    private TelaLoja telaLoja;
    private Cliente clienteLogado;
    private List<ItemPedido> carrinho;
    private JPanel painelEtapa;
    private int etapaAtual = 0;
    private String formaPagSelecionada = "Cartao de Credito";

    private PedidoDAO pedidoDAO = new PedidoDAO();

    public TelaCheckout(TelaCarrinho telaCarrinho, Cliente cliente, List<ItemPedido> carrinho, TelaLoja telaLoja) {
        util.Acesso.telaCliente(cliente.getId());
        this.telaCarrinho  = telaCarrinho;
        this.clienteLogado = cliente;
        this.carrinho      = carrinho;
        this.telaLoja      = telaLoja;
        initComponents();
    }

    private void initComponents() {
        setTitle("OBotBarato - Checkout");
        setSize(620, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel principal = new JPanel(new BorderLayout());
        principal.setBackground(TemaLoja.BG_PRINCIPAL);

        // ── Cabecalho ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(TemaLoja.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, TemaLoja.BORDA),
            BorderFactory.createEmptyBorder(12, 16, 12, 20)));

        JPanel headerEsq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerEsq.setOpaque(false);
        JButton btnVolt = TemaLoja.botaoFantasia("< Voltar ao carrinho");
        btnVolt.addActionListener(e -> dispose());
        headerEsq.add(btnVolt);

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(TemaLoja.BORDA);
        sep.setPreferredSize(new Dimension(1, 20));
        headerEsq.add(sep);
        JLabel lblT = new JLabel("Checkout");
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblT.setForeground(TemaLoja.TEXTO_PRIMARIO);
        headerEsq.add(lblT);
        header.add(headerEsq, BorderLayout.WEST);

        // Indicador de etapas
        JPanel etapas = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        etapas.setOpaque(false);
        String[] nomes = {"Resumo", "Pagamento", "Confirmacao"};
        for (int i = 0; i < nomes.length; i++) {
            JLabel lbl = new JLabel((i+1) + ". " + nomes[i]);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setForeground(i == etapaAtual ? TemaLoja.ROSA_PRIMARIO : TemaLoja.TEXTO_HINT);
            etapas.add(lbl);
            if (i < nomes.length-1) {
                JLabel seta = new JLabel(" > ");
                seta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                seta.setForeground(TemaLoja.TEXTO_HINT);
                etapas.add(seta);
            }
        }
        header.add(etapas, BorderLayout.EAST);
        principal.add(header, BorderLayout.NORTH);

        // Painel de etapas (troca conforme avanca)
        painelEtapa = new JPanel(new BorderLayout());
        painelEtapa.setBackground(TemaLoja.BG_PRINCIPAL);
        painelEtapa.add(criarEtapaResumo(), BorderLayout.CENTER);
        principal.add(painelEtapa, BorderLayout.CENTER);

        setContentPane(principal);
    }

    // ── Etapa 1: Resumo do pedido ──
    private JPanel criarEtapaResumo() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(TemaLoja.BG_PRINCIPAL);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel lblTit = new JLabel("Resumo do Pedido");
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTit.setForeground(TemaLoja.TEXTO_PRIMARIO);
        p.add(lblTit, BorderLayout.NORTH);

        // Lista de itens
        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBackground(TemaLoja.BG_CARD);
        lista.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        double total = 0;
        for (ItemPedido item : carrinho) {
            JPanel linha = new JPanel(new BorderLayout());
            linha.setOpaque(false);
            linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            linha.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, TemaLoja.BORDA));

            JLabel lblItem = new JLabel(item.getNomeProduto() + " x" + item.getQuantidade());
            lblItem.setFont(TemaLoja.FONTE_LABEL);
            lblItem.setForeground(TemaLoja.TEXTO_PRIMARIO);

            JLabel lblSub = new JLabel(String.format("R$ %.2f", item.getSubtotal()));
            lblSub.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblSub.setForeground(TemaLoja.VERDE_PRECO);

            linha.add(lblItem, BorderLayout.WEST);
            linha.add(lblSub,  BorderLayout.EAST);
            lista.add(linha);
            total += item.getSubtotal();
        }

        // Linha de entrega simulada
        JPanel linhaFrete = new JPanel(new BorderLayout());
        linhaFrete.setOpaque(false);
        linhaFrete.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel lblFrete = new JLabel("Entrega (simulada)");
        lblFrete.setFont(TemaLoja.FONTE_LABEL);
        lblFrete.setForeground(TemaLoja.TEXTO_SECUNDARIO);
        JLabel lblFV = new JLabel("Gratis");
        lblFV.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblFV.setForeground(TemaLoja.VERDE_PRECO);
        linhaFrete.add(lblFrete, BorderLayout.WEST);
        linhaFrete.add(lblFV, BorderLayout.EAST);
        lista.add(linhaFrete);

        p.add(TemaLoja.scroll(lista), BorderLayout.CENTER);

        // CORRECAO: copia final para uso no lambda (total foi modificado no for)
        final double totalFinal = total;

        // Total + endereco + botao
        JPanel rodape = new JPanel(new BorderLayout(0, 10));
        rodape.setOpaque(false);

        JLabel lblTotal = new JLabel(String.format("Total: R$ %.2f", totalFinal));
        lblTotal.setFont(TemaLoja.FONTE_TOTAL);
        lblTotal.setForeground(TemaLoja.VERDE_PRECO);

        JPanel infoEntrega = new JPanel(new GridLayout(2, 1, 0, 2));
        infoEntrega.setOpaque(false);
        JLabel lblEnd1 = new JLabel("Entregar em: " + clienteLogado.getEndereco());
        JLabel lblEnd2 = new JLabel("Cliente: " + clienteLogado.getNome());
        lblEnd1.setFont(TemaLoja.FONTE_LABEL); lblEnd1.setForeground(TemaLoja.TEXTO_SECUNDARIO);
        lblEnd2.setFont(TemaLoja.FONTE_LABEL); lblEnd2.setForeground(TemaLoja.TEXTO_SECUNDARIO);
        infoEntrega.add(lblEnd2); infoEntrega.add(lblEnd1);

        JButton btnAvancar = TemaLoja.botaoPrimario("Ir para Pagamento  >");
        btnAvancar.setPreferredSize(new Dimension(220, 44));
        btnAvancar.addActionListener(e -> trocarEtapa(criarEtapaPagamento(totalFinal)));

        rodape.add(lblTotal, BorderLayout.NORTH);
        rodape.add(infoEntrega, BorderLayout.CENTER);
        rodape.add(btnAvancar, BorderLayout.SOUTH);
        p.add(rodape, BorderLayout.SOUTH);
        return p;
    }

    // ── Etapa 2: Pagamento ──
    private JPanel criarEtapaPagamento(double total) {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(TemaLoja.BG_PRINCIPAL);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel lblTit = new JLabel("Forma de Pagamento");
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTit.setForeground(TemaLoja.TEXTO_PRIMARIO);
        p.add(lblTit, BorderLayout.NORTH);

        // Opcoes de pagamento (simulado)
        JPanel opcoes = new JPanel();
        opcoes.setLayout(new BoxLayout(opcoes, BoxLayout.Y_AXIS));
        opcoes.setBackground(TemaLoja.BG_PRINCIPAL);

        ButtonGroup grupo = new ButtonGroup();
        String[] formas = {"Cartao de Credito", "Cartao de Debito", "PIX", "Boleto Bancario"};
        String[] descs  = {"Em ate 3x sem juros", "Desconto de 5%", "Desconto de 10% - Instante", "Vence em 3 dias"};
        Color[]  cores  = {TemaLoja.AZUL_INFO, TemaLoja.ROXO_ACENTO, TemaLoja.VERDE_PRECO, TemaLoja.AMARELO_FAV};

        for (int i = 0; i < formas.length; i++) {
            final String forma = formas[i];
            final String desc  = descs[i];
            final Color cor    = cores[i];

            JPanel card = new JPanel(new BorderLayout(10, 0)) {
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(TemaLoja.BG_CARD);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
            };
            card.setOpaque(false);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TemaLoja.BORDA, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JRadioButton rb = new JRadioButton();
            rb.setOpaque(false);
            rb.setForeground(TemaLoja.TEXTO_PRIMARIO);
            if (i == 0) rb.setSelected(true);
            grupo.add(rb);

            JPanel textos = new JPanel(new GridLayout(2, 1));
            textos.setOpaque(false);
            JLabel lblForma = new JLabel(forma);
            lblForma.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblForma.setForeground(TemaLoja.TEXTO_PRIMARIO);
            JLabel lblDesc = new JLabel(desc);
            lblDesc.setFont(TemaLoja.FONTE_LABEL);
            lblDesc.setForeground(cor);
            textos.add(lblForma); textos.add(lblDesc);

            card.add(rb,     BorderLayout.WEST);
            card.add(textos, BorderLayout.CENTER);
            opcoes.add(card);
            opcoes.add(Box.createVerticalStrut(8));

            card.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    rb.setSelected(true);
                    formaPagSelecionada = forma;
                    card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(TemaLoja.BORDA_HOVER, 1),
                        BorderFactory.createEmptyBorder(10, 14, 10, 14)));
                }
            });
            rb.addActionListener(e -> formaPagSelecionada = forma);
        }

        p.add(TemaLoja.scroll(opcoes), BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rodape.setOpaque(false);
        JButton btnVoltar = TemaLoja.botaoFantasia("< Voltar");
        btnVoltar.addActionListener(e -> trocarEtapa(criarEtapaResumo()));
        JButton btnPagar = TemaLoja.botaoPrimario("Confirmar Pagamento  >");
        btnPagar.addActionListener(e -> processarPagamento(total));
        rodape.add(btnVoltar); rodape.add(btnPagar);
        p.add(rodape, BorderLayout.SOUTH);
        return p;
    }

    // Processa pagamento e salva pedido no banco
    private void processarPagamento(double total) {
        // Simula processamento
        JDialog dlg = new JDialog(this, "Processando...", true);
        dlg.setSize(280, 120);
        dlg.setLocationRelativeTo(this);
        dlg.setUndecorated(true);
        JPanel pd = new JPanel(new BorderLayout());
        pd.setBackground(TemaLoja.BG_CARD);
        pd.setBorder(BorderFactory.createLineBorder(TemaLoja.BORDA, 1));
        JLabel lbl = new JLabel("Processando pagamento...", SwingConstants.CENTER);
        lbl.setForeground(TemaLoja.TEXTO_PRIMARIO);
        lbl.setFont(TemaLoja.FONTE_BOTAO);
        pd.add(lbl, BorderLayout.CENTER);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setBackground(TemaLoja.BG_INPUT);
        bar.setForeground(TemaLoja.ROSA_PRIMARIO);
        pd.add(bar, BorderLayout.SOUTH);
        dlg.setContentPane(pd);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            protected Boolean doInBackground() throws Exception {
                Thread.sleep(1800); // simula delay de pagamento
                // Salva pedido no banco (INTEGRADO com o sistema admin)
                Pedido pedido = new Pedido();
                pedido.setIdCliente(clienteLogado.getId());
                pedido.setStatus("finalizado");
                pedido.setItens(carrinho);
                pedido.recalcularTotal();
                pedidoDAO.salvar(pedido);
                return true;
            }
            protected void done() {
                dlg.dispose();
                try {
                    if (get()) trocarEtapa(criarEtapaConfirmacao());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TelaCheckout.this, "Erro ao processar: " + util.Erros.mensagem(ex));
                }
            }
        };
        worker.execute();
        dlg.setVisible(true);
    }

    // ── Etapa 3: Confirmacao ──
    private JPanel criarEtapaConfirmacao() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(TemaLoja.BG_PRINCIPAL);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(TemaLoja.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(TemaLoja.VERDE_PRECO, 1),
            BorderFactory.createEmptyBorder(30, 40, 30, 40)));

        JLabel lblCheck = new JLabel("OK", SwingConstants.CENTER);
        lblCheck.setFont(new Font("Segoe UI", Font.BOLD, 42));
        lblCheck.setForeground(TemaLoja.VERDE_PRECO);
        lblCheck.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTit = new JLabel("Compra realizada com sucesso!", SwingConstants.CENTER);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTit.setForeground(TemaLoja.TEXTO_PRIMARIO);
        lblTit.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblPag = new JLabel("Pagamento via " + formaPagSelecionada, SwingConstants.CENTER);
        lblPag.setFont(TemaLoja.FONTE_LABEL);
        lblPag.setForeground(TemaLoja.TEXTO_SECUNDARIO);
        lblPag.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblInfo = new JLabel("Seu pedido ja aparece no seu historico.", SwingConstants.CENTER);
        lblInfo.setFont(TemaLoja.FONTE_LABEL);
        lblInfo.setForeground(TemaLoja.TEXTO_SECUNDARIO);
        lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnContinuar = TemaLoja.botaoPrimario("Continuar comprando");
        btnContinuar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnContinuar.addActionListener(e -> {
            // Limpa carrinho, fecha janelas e volta para a loja
            carrinho.clear();
            telaLoja.atualizarBadgeCarrinho();
            telaCarrinho.carregarTabela();
            dispose();
            telaCarrinho.dispose();
        });

        JButton btnPedidos = TemaLoja.botaoFantasia("Ver meus pedidos");
        btnPedidos.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPedidos.addActionListener(e -> {
            dispose(); telaCarrinho.dispose();
            new TelaMeusPedidos(telaLoja, clienteLogado).setVisible(true);
        });

        card.add(lblCheck);
        card.add(Box.createVerticalStrut(10));
        card.add(lblTit);
        card.add(Box.createVerticalStrut(8));
        card.add(lblPag);
        card.add(Box.createVerticalStrut(4));
        card.add(lblInfo);
        card.add(Box.createVerticalStrut(20));
        card.add(btnContinuar);
        card.add(Box.createVerticalStrut(10));
        card.add(btnPedidos);

        p.add(card);
        return p;
    }

    private void trocarEtapa(JPanel novaEtapa) {
        painelEtapa.removeAll();
        painelEtapa.add(novaEtapa, BorderLayout.CENTER);
        painelEtapa.revalidate();
        painelEtapa.repaint();
    }
}