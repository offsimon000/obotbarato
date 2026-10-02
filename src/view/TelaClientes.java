package view;

import dao.ClienteDAO;
import model.Cliente;
import util.Tema;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TelaClientes extends JFrame {

    private JTable tabela;
    private DefaultTableModel modeloTabela;
    private JTextField txtBusca;
    private ClienteDAO dao = new ClienteDAO();
    private JFrame telaAnterior;

    public TelaClientes(JFrame telaAnterior) {
        util.Acesso.telaEquipe();
        this.telaAnterior = telaAnterior;
        initComponents();
        carregarTabela("");
    }

    private void initComponents() {
        setTitle("OBotBarato - Clientes");
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
        JLabel lblTitulo = new JLabel("Clientes");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        headerEsq.add(lblTitulo);
        header.add(headerEsq, BorderLayout.WEST);
        painel.add(header, BorderLayout.NORTH);

        // ── Busca ──
        JPanel barraBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        barraBusca.setBackground(Tema.BG_ESCURO);
        txtBusca = Tema.campo(24);
        JButton btnBuscar = Tema.botaoSecundario("Buscar");
        btnBuscar.addActionListener(e -> carregarTabela(txtBusca.getText()));
        txtBusca.addActionListener(e -> carregarTabela(txtBusca.getText()));
        barraBusca.add(txtBusca); barraBusca.add(btnBuscar);

        // ── Tabela ──
        String[] colunas = {"ID", "Nome", "CPF", "Email", "Telefone", "Situação", "Acesso loja"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modeloTabela);
        Tema.aplicarTabelaDark(tabela);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(220);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Tema.BG_ESCURO);
        centro.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        centro.add(barraBusca, BorderLayout.NORTH);
        centro.add(Tema.scrollDark(tabela), BorderLayout.CENTER);
        painel.add(centro, BorderLayout.CENTER);

        // ── Rodape ──
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rodape.setBackground(Tema.BG_CARD);
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA));

        JButton btnNovo    = Tema.botaoPrimario("+ Novo Cliente");
        JButton btnEditar  = Tema.botaoSecundario("Editar");
        JButton btnExcluir = Tema.botaoSecundario("Desativar");
        btnExcluir.setForeground(Tema.VERMELHO_ERRO);

        btnNovo.addActionListener(e -> abrirFormulario(null));
        btnEditar.addActionListener(e -> {
            int l = tabela.getSelectedRow();
            if (l < 0) { aviso("Selecione um cliente."); return; }
            abrirFormulario((int) modeloTabela.getValueAt(l, 0));
        });
        btnExcluir.addActionListener(e -> excluir());

                if(util.Sessao.isAdminAtivo()) {
            rodape.add(btnExcluir);
            JButton reativar=Tema.botaoSecundario("Reativar"),codigo=Tema.botaoSecundario("Código de acesso");
            reativar.addActionListener(e->{int l=tabela.getSelectedRow();if(l<0){aviso("Selecione um cliente.");return;}try{dao.reativar((int)modeloTabela.getValueAt(l,0));carregarTabela("");}catch(Exception ex){erro(util.Erros.mensagem(ex));}});
            codigo.addActionListener(e->{int l=tabela.getSelectedRow();if(l<0){aviso("Selecione um cliente.");return;}
                if(!confirmar("Emitir código para ativação ou recuperação? Entregue ao cliente após identificar o titular."))return;
                try{String token=new dao.CodigoAcessoDAO().emitir((int)modeloTabela.getValueAt(l,0));
                    JTextField campo=new JTextField(token,32);campo.setEditable(false);campo.selectAll();
                    JPanel painelCodigo=new JPanel(new java.awt.GridLayout(0,1,4,8));painelCodigo.add(new JLabel("Copie e entregue ao cliente. Validade: 30 minutos; uso único."));painelCodigo.add(campo);
                    JOptionPane.showMessageDialog(this,painelCodigo,"Código de acesso",JOptionPane.INFORMATION_MESSAGE);
                }catch(Exception ex){erro(util.Erros.mensagem(ex));}
            });rodape.add(reativar);rodape.add(codigo);
        }
        rodape.add(btnEditar);rodape.add(btnNovo);
        painel.add(rodape, BorderLayout.SOUTH);
        setContentPane(painel);
    }

    private void carregarTabela(String busca) {
        modeloTabela.setRowCount(0);
        try {
            List<Cliente> lista = dao.listarManutencao(busca);
            for (Cliente c : lista)
                modeloTabela.addRow(new Object[]{c.getId(), c.getNome(), c.getCpf(), c.getEmail(), c.getTelefone(), c.isAtivo()?"Ativo":"Inativo", c.isAcessoLoja()?"Ativado":"Pendente"});
        } catch (Exception ex) { erro(util.Erros.mensagem(ex)); }
    }

    private void abrirFormulario(Integer id) {
        JTextField nome = Tema.campo(22), cpf = Tema.campo(14),
                   email = Tema.campo(22), telefone = Tema.campo(15), endereco = Tema.campo(30);

        util.MascaraCampo.aplicarCpf(cpf); util.MascaraCampo.aplicarTelefone(telefone);
        if (id != null) {
            try {
                for (Cliente c : dao.listarManutencao("")) {
                    if (c.getId() == id) {
                        nome.setText(c.getNome()); cpf.setText(c.getCpf());
                        email.setText(c.getEmail()); telefone.setText(c.getTelefone());
                        endereco.setText(c.getEndereco()); break;
                    }
                }
            } catch (Exception ex) { erro(util.Erros.mensagem(ex)); return; }
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Tema.BG_ESCURO);
        form.setPreferredSize(new Dimension(420, 360));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 10, 2, 10); gbc.gridwidth = 2;

        String[] rotulos = {"NOME", "CPF", "EMAIL", "TELEFONE", "ENDERECO"};
        JTextField[] flds = {nome, cpf, email, telefone, endereco};
        for (int i = 0; i < rotulos.length; i++) {
            JLabel lbl = new JLabel(rotulos[i]);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lbl.setForeground(Tema.TEXTO_SECUNDARIO);
            gbc.gridy = i * 2; form.add(lbl, gbc);
            gbc.gridy = i * 2 + 1; form.add(flds[i], gbc);
        }

        int result = JOptionPane.showConfirmDialog(this, form,
            id == null ? "Novo Cliente" : "Editar Cliente", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                Cliente c = new Cliente();
                if (id != null) c.setId(id);
                c.setNome(nome.getText().trim()); c.setCpf(cpf.getText().trim());
                c.setEmail(email.getText().trim()); c.setTelefone(telefone.getText().trim());
                c.setEndereco(endereco.getText().trim());
                boolean salvo = id == null ? dao.salvar(c) : dao.atualizar(c);
                if (!salvo) throw new java.sql.SQLException("Cliente não encontrado para atualização.");
                carregarTabela("");
            } catch (Exception ex) { erro(util.Erros.mensagem(ex)); }
        }
    }

    private void excluir() {
        int l = tabela.getSelectedRow();
        if (l < 0) { aviso("Selecione um cliente."); return; }
        if (confirmar("Desativar este cliente?"))
            try { if (!dao.excluir((int) modeloTabela.getValueAt(l, 0))) throw new java.sql.SQLException("Cliente não encontrado."); carregarTabela(""); }
            catch (Exception ex) { erro(util.Erros.mensagem(ex)); }
    }

    private void aviso(String m) { JOptionPane.showMessageDialog(this, m, "Aviso", JOptionPane.WARNING_MESSAGE); }
    private void erro(String m)  { JOptionPane.showMessageDialog(this, m, "Erro",  JOptionPane.ERROR_MESSAGE); }
    private boolean confirmar(String m) {
        return JOptionPane.showConfirmDialog(this, m, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}