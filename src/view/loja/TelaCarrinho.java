package view.loja;

import model.Cliente;
import model.ItemPedido;
import util.TemaLoja;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TelaCarrinho extends JFrame {

    private TelaLoja telaLoja;
    private Cliente clienteLogado;
    private model.Carrinho carrinho;
    private TelaCheckout checkout;
    private DefaultTableModel modelo;
    private JLabel lblTotal;
    private JTable tabela; // CORRECAO: tabela como campo (antes era local)

    public TelaCarrinho(TelaLoja telaLoja, Cliente cliente, List<ItemPedido> carrinho) {
        util.Acesso.telaCliente(cliente.getId());
        this.telaLoja      = telaLoja;
        this.clienteLogado = cliente;
        this.carrinho      = telaLoja.getModeloCarrinho();
        initComponents();
        carregarTabela();
    }

    private void initComponents() {
        setTitle("OBotBarato - Carrinho");
        setSize(900, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(TemaLoja.BG_PRINCIPAL);
        painel.add(criarHeader(), BorderLayout.NORTH);
        painel.add(criarAreaTabela(), BorderLayout.CENTER); // CORRECAO: nome corrigido
        painel.add(criarRodape(),  BorderLayout.SOUTH);
        setContentPane(painel);
    }

    private JPanel criarHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(TemaLoja.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, TemaLoja.BORDA),
            BorderFactory.createEmptyBorder(12, 16, 12, 20)));

        JPanel esq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        esq.setOpaque(false);

        // Botao Voltar que SO fecha esta janela (nao usa o TemaLoja.botaoVoltar com JFrame)
        JButton btnVolt = new JButton("  < Voltar");
        btnVolt.setBackground(TemaLoja.BG_PRINCIPAL);
        btnVolt.setForeground(TemaLoja.TEXTO_SECUNDARIO);
        btnVolt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnVolt.setFocusPainted(false);
        btnVolt.setBorderPainted(false);
        btnVolt.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVolt.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btnVolt.setForeground(TemaLoja.ROSA_PRIMARIO); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btnVolt.setForeground(TemaLoja.TEXTO_SECUNDARIO); }
        });
        btnVolt.addActionListener(e -> dispose());

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(TemaLoja.BORDA);
        sep.setPreferredSize(new Dimension(1, 20));
        JLabel lbl = new JLabel("Meu Carrinho");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lbl.setForeground(TemaLoja.TEXTO_PRIMARIO);

        esq.add(btnVolt); esq.add(sep); esq.add(lbl);
        header.add(esq, BorderLayout.WEST);
        return header;
    }

    // CORRECAO: metodo retorna JPanel (scroll embrulhado), tabela eh campo
    private JPanel criarAreaTabela() {
        String[] colunas = {"Produto", "Qtd", "Preco Unit.", "Subtotal"};
        modelo = new DefaultTableModel(colunas, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modelo); // salva como campo
        tabela.setBackground(TemaLoja.BG_CARD);
        tabela.setForeground(TemaLoja.TEXTO_PRIMARIO);
        tabela.setGridColor(TemaLoja.BORDA);
        tabela.setFont(TemaLoja.FONTE_LABEL);
        tabela.setRowHeight(32);
        tabela.setSelectionBackground(TemaLoja.ROSA_PRIMARIO);
        tabela.setSelectionForeground(Color.WHITE);
        tabela.getTableHeader().setBackground(TemaLoja.BG_PRINCIPAL);
        tabela.getTableHeader().setForeground(TemaLoja.TEXTO_SECUNDARIO);
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabela.setShowVerticalLines(false);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(250);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(TemaLoja.BG_PRINCIPAL);
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 0, 14));
        p.add(TemaLoja.scroll(tabela), BorderLayout.CENTER);
        return p;
    }

    private JPanel criarRodape() {
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setBackground(TemaLoja.BG_CARD);
        rodape.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, TemaLoja.BORDA),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        lblTotal = new JLabel("Total: R$ 0,00");
        lblTotal.setFont(TemaLoja.FONTE_TOTAL);
        lblTotal.setForeground(TemaLoja.VERDE_PRECO);
        rodape.add(lblTotal, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);

        JButton btnRemover  = TemaLoja.botaoFantasia("Remover Item");
        btnRemover.setForeground(TemaLoja.VERMELHO_ERRO);
        btnRemover.addActionListener(e -> removerItem());

        JButton btnLimpar   = TemaLoja.botaoFantasia("Limpar Carrinho");
        btnLimpar.addActionListener(e -> limparCarrinho());

        JButton btnCheckout = TemaLoja.botaoPrimario("Finalizar Compra");
        btnCheckout.addActionListener(e -> irParaCheckout());

        JButton quantidade=TemaLoja.botaoFantasia("Quantidade");quantidade.addActionListener(e->alterarQuantidade());
        btns.add(quantidade);btns.add(btnRemover);btns.add(btnLimpar);btns.add(btnCheckout);
        rodape.add(btns, BorderLayout.EAST);
        return rodape;
    }

    // CORRECAO: usa this.tabela (campo) ao inves de variavel local
    public void carregarTabela() {
        modelo.setRowCount(0);
        java.math.BigDecimal total = new java.math.BigDecimal("0.00");
        for (ItemPedido item : carrinho.itens()) {
            modelo.addRow(new Object[]{
                item.getNomeProduto(),
                item.getQuantidade(),
                String.format("R$ %.2f", item.getPrecoUnitario()),
                String.format("R$ %.2f", item.getSubtotal())
            });
            total = total.add(item.getSubtotalDecimal());
        }
        lblTotal.setText(String.format("Total: R$ %.2f", total));
    }

    private void removerItem() {
        int linha = tabela.getSelectedRow(); // CORRECAO: usa campo tabela
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um item para remover.");
            return;
        }
        try{carrinho.remover(carrinho.itens().get(linha).getIdProduto());}catch(Exception ex){JOptionPane.showMessageDialog(this,util.Erros.mensagem(ex));return;}
        carregarTabela();
        telaLoja.atualizarBadgeCarrinho();
    }

    private void limparCarrinho() {
        if (JOptionPane.showConfirmDialog(this, "Limpar todo o carrinho?",
                "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try{carrinho.limpar();}catch(Exception ex){JOptionPane.showMessageDialog(this,util.Erros.mensagem(ex));return;}
            carregarTabela();
            telaLoja.atualizarBadgeCarrinho();
        }
    }

    private void irParaCheckout() {
        if (carrinho.itens().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seu carrinho esta vazio.");
            return;
        }
        if(checkout!=null && checkout.isDisplayable()){checkout.toFront();return;}
        java.util.UUID chave=null;
        try{chave=carrinho.iniciarCheckout();checkout=new TelaCheckout(this,clienteLogado,carrinho.itens(),telaLoja,chave);checkout.setVisible(true);}
        catch(Exception ex){if(chave!=null)carrinho.liberar(chave);JOptionPane.showMessageDialog(this,util.Erros.mensagem(ex));}
    }
    private void alterarQuantidade(){
        int linha=tabela.getSelectedRow();if(linha<0){JOptionPane.showMessageDialog(this,"Selecione um item.");return;}
        ItemPedido item=carrinho.itens().get(linha);
        String quantidade=JOptionPane.showInputDialog(this,"Nova quantidade:",item.getQuantidade());if(quantidade==null)return;
        try{var atual=new dao.ProdutoDAO().buscarParaCarrinho(item.getIdProduto());carrinho.definir(atual.produto(),Integer.parseInt(quantidade.trim()),atual.quantidade());atualizar();}
        catch(Exception ex){JOptionPane.showMessageDialog(this,ex instanceof NumberFormatException?"Quantidade inválida.":util.Erros.mensagem(ex));}
    }
    private void atualizar(){carregarTabela();telaLoja.atualizarBadgeCarrinho();}}