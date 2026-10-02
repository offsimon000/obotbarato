package view;

import dao.EstoqueDAO;
import dao.ProdutoDAO;
import model.Produto;
import util.Tema;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class TelaEstoque extends JFrame {

    private JTable tabela;
    private DefaultTableModel modeloTabela;
    private EstoqueDAO estoqueDAO = new EstoqueDAO();
    private ProdutoDAO produtoDAO = new ProdutoDAO();
    private JFrame telaAnterior;

    public TelaEstoque(JFrame telaAnterior) {
        util.Acesso.telaEquipe();
        this.telaAnterior = telaAnterior;
        initComponents();
        carregarTabela();
    }

    private void initComponents() {
        setTitle("OBotBarato - Estoque");
        setSize(780, 540);
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
        JLabel lblTitulo = new JLabel("Controle de Estoque");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        headerEsq.add(lblTitulo);
        header.add(headerEsq, BorderLayout.WEST);

        JButton btnAlerta = Tema.botaoSecundario("Ver Alertas");
        btnAlerta.setForeground(Tema.AMARELO_AVISO);
        btnAlerta.addActionListener(e -> mostrarEstoqueBaixo());
        JPanel headerDir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerDir.setOpaque(false);
        headerDir.add(btnAlerta);
        header.add(headerDir, BorderLayout.EAST);
        painel.add(header, BorderLayout.NORTH);

        // ── Tabela ──
        String[] colunas = {"ID", "Produto", "Qtd. Atual", "Est. Minimo", "Situacao"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modeloTabela) {
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                String sit = (String) getValueAt(row, 4);
                if (sit != null && sit.contains("BAIXO")) {
                    c.setBackground(new Color(255, 220, 220));
                    c.setForeground(Tema.VERMELHO_ERRO);
                } else {
                    c.setBackground(row % 2 == 0 ? Tema.BG_CARD : Tema.BG_ESCURO);
                    c.setForeground(Tema.TEXTO_PRIMARIO);
                }
                if (isRowSelected(row)) { c.setBackground(Tema.ROXO_PRIMARIO); c.setForeground(Color.WHITE); }
                return c;
            }
        };
        Tema.aplicarTabelaDark(tabela);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(280);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Tema.BG_ESCURO);
        centro.setBorder(BorderFactory.createEmptyBorder(12, 14, 0, 14));
        centro.add(Tema.scrollDark(tabela), BorderLayout.CENTER);
        painel.add(centro, BorderLayout.CENTER);

        // ── Rodape ──
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rodape.setBackground(Tema.BG_CARD);
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA));

        JButton btnAtualizar = Tema.botaoSecundario("Atualizar");
        JButton btnEntrada   = Tema.botaoPrimario("Entrada");
        JButton btnSaida     = Tema.botaoSecundario("Saida");

        btnAtualizar.addActionListener(e -> carregarTabela());
        btnEntrada.addActionListener(e -> movimentar(true));
        btnSaida.addActionListener(e -> movimentar(false));

                JButton historico=Tema.botaoSecundario("Histórico");
        historico.addActionListener(e->{int l=tabela.getSelectedRow();if(l<0){JOptionPane.showMessageDialog(this,"Selecione um produto.");return;}
            try{DefaultTableModel m=new DefaultTableModel(new String[]{"Data","Tipo","Quantidade","Saldo","Motivo","Responsável"},0){public boolean isCellEditable(int r,int c){return false;}};
                for(String[] row:estoqueDAO.historico((int)modeloTabela.getValueAt(l,0)))m.addRow(row);
                JTable t=new JTable(m);Tema.aplicarTabelaDark(t);t.setPreferredScrollableViewportSize(new Dimension(850,260));JOptionPane.showMessageDialog(this,Tema.scrollDark(t),"Movimentações",JOptionPane.INFORMATION_MESSAGE);
            }catch(Exception ex){JOptionPane.showMessageDialog(this,util.Erros.mensagem(ex));}
        });rodape.add(historico);rodape.add(btnAtualizar);
        if(util.Sessao.isAdminAtivo()){
            JButton minimo=Tema.botaoSecundario("Definir mínimo");
            minimo.addActionListener(e->{int l=tabela.getSelectedRow();if(l<0){JOptionPane.showMessageDialog(this,"Selecione um produto.");return;}
                String valor=JOptionPane.showInputDialog(this,"Estoque mínimo:",modeloTabela.getValueAt(l,3));if(valor==null)return;
                try{estoqueDAO.definirMinimo((int)modeloTabela.getValueAt(l,0),Integer.parseInt(valor.trim()));carregarTabela();}catch(Exception ex){JOptionPane.showMessageDialog(this,ex instanceof NumberFormatException?"Mínimo inválido.":util.Erros.mensagem(ex));}
            });rodape.add(minimo);rodape.add(btnSaida);rodape.add(btnEntrada);
        }
        painel.add(rodape, BorderLayout.SOUTH);
        setContentPane(painel);
    }

    private void carregarTabela() {
        modeloTabela.setRowCount(0);
        try {
            for (Produto p : produtoDAO.listarTodos()) {
                int qtd = estoqueDAO.getQuantidade(p.getId());
                int min=estoqueDAO.getMinimo(p.getId());
                modeloTabela.addRow(new Object[]{p.getId(), p.getNome(), qtd, min, qtd <= min ? "ESTOQUE BAIXO" : "OK"});
            }
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + util.Erros.mensagem(ex)); }
    }

    private void movimentar(boolean entrada) {
        int l = tabela.getSelectedRow();
        if (l < 0) { JOptionPane.showMessageDialog(this, "Selecione um produto."); return; }

        JTextField txtQtd = Tema.campo(8);
        JTextField motivo = Tema.campo(24); motivo.setText(entrada?"Reposição manual":"Saída manual");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Tema.BG_ESCURO);
        form.setPreferredSize(new Dimension(340, 190));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(6, 10, 6, 10); gbc.gridwidth = 2;
        JLabel lbl = new JLabel(entrada ? "QUANTIDADE DE ENTRADA" : "QUANTIDADE DE SAIDA");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10)); lbl.setForeground(Tema.TEXTO_SECUNDARIO);
        gbc.gridy = 0; form.add(lbl, gbc);
        gbc.gridy = 1; form.add(txtQtd, gbc);
        JLabel lblProd = new JLabel("Produto: " + modeloTabela.getValueAt(l, 1));
        lblProd.setFont(Tema.FONTE_LABEL); lblProd.setForeground(Tema.TEXTO_SECUNDARIO);
        gbc.gridy = 2; form.add(lblProd, gbc);
        gbc.gridy=3; form.add(new JLabel("Motivo"),gbc);gbc.gridy=4;form.add(motivo,gbc);

        int res = JOptionPane.showConfirmDialog(this, form,
            entrada ? "Entrada de Estoque" : "Saida de Estoque", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                int qtd = Integer.parseInt(txtQtd.getText().trim());
                int idP = (int) modeloTabela.getValueAt(l, 0);
                if (entrada) estoqueDAO.entrada(idP, qtd, motivo.getText()); else estoqueDAO.saida(idP, qtd, motivo.getText());
                carregarTabela();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Quantidade invalida.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro: " + util.Erros.mensagem(e));
            }
        }
    }

    private void mostrarEstoqueBaixo() {
        try {
            List<String[]> lista = estoqueDAO.listarEstoqueBaixo();
            if (lista.isEmpty()) { JOptionPane.showMessageDialog(this, "Nenhum produto com estoque baixo!"); return; }
            String[] colunas = {"Produto", "Qtd. Atual", "Minimo"};
            DefaultTableModel m = new DefaultTableModel(colunas, 0) { public boolean isCellEditable(int r, int c) { return false; } };
            for (String[] row : lista) m.addRow(row);
            JTable t = new JTable(m);
            Tema.aplicarTabelaDark(t);
            t.setPreferredScrollableViewportSize(new Dimension(400, 180));
            JOptionPane.showMessageDialog(this, Tema.scrollDark(t), "Produtos com Estoque Baixo", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erro: " + util.Erros.mensagem(e)); }
    }
}