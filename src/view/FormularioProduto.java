package view;

import dao.ProdutoDAO;
import model.Produto;
import util.ConexaoBanco;
import util.Tema;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class FormularioProduto extends JDialog {

    private JTextField txtNome, txtPreco;
    private JTextArea txtDescricao;
    private JComboBox<String> cbCategoria;
    private JCheckBox chkAtivo;
    private Map<String, Integer> categorias = new LinkedHashMap<>();
    private Integer idProduto;
    private ProdutoDAO dao = new ProdutoDAO();

    public FormularioProduto(JFrame parent, Integer idProduto) {
        super(parent, idProduto == null ? "Novo Produto" : "Editar Produto", true);
        this.idProduto = idProduto;
        initComponents();
        carregarCategorias();
        if (idProduto != null) carregarDados();
    }

    private void initComponents() {
        setSize(440, 400);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBackground(Tema.BG_ESCURO);
        painel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);

        JLabel lblTitulo = new JLabel(idProduto == null ? "Novo Produto" : "Editar Produto");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.insets = new Insets(0, 0, 16, 0);
        painel.add(lblTitulo, gbc);

        gbc.gridwidth = 1; gbc.insets = new Insets(5, 0, 2, 0);

        addLabel(painel, gbc, "NOME DO PRODUTO", 1);
        txtNome = Tema.campo(20);
        gbc.gridy = 2; gbc.gridwidth = 2; painel.add(txtNome, gbc);

        addLabel(painel, gbc, "PRECO (R$)", 3);
        txtPreco = Tema.campo(10);
        gbc.gridy = 4; painel.add(txtPreco, gbc);

        addLabel(painel, gbc, "CATEGORIA", 5);
        cbCategoria = new JComboBox<>();
        cbCategoria.setBackground(Tema.BG_INPUT);
        cbCategoria.setForeground(Tema.TEXTO_PRIMARIO);
        cbCategoria.setFont(Tema.FONTE_CAMPO);
        cbCategoria.setBorder(BorderFactory.createLineBorder(Tema.BORDA, 1));
        gbc.gridy = 6; painel.add(cbCategoria, gbc);

        addLabel(painel, gbc, "DESCRICAO", 7);
        txtDescricao = new JTextArea(3, 20);
        txtDescricao.setBackground(Tema.BG_INPUT);
        txtDescricao.setForeground(Tema.TEXTO_PRIMARIO);
        txtDescricao.setCaretColor(Tema.ROXO_PRIMARIO);
        txtDescricao.setFont(Tema.FONTE_CAMPO);
        txtDescricao.setLineWrap(true);
        txtDescricao.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.BORDA, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        gbc.gridy = 8; painel.add(Tema.scrollDark(txtDescricao), gbc);

        chkAtivo = new JCheckBox("Produto ativo", true);
        chkAtivo.setBackground(Tema.BG_ESCURO);
        chkAtivo.setForeground(Tema.TEXTO_SECUNDARIO);
        chkAtivo.setFont(Tema.FONTE_LABEL);
        gbc.gridy = 9; gbc.insets = new Insets(8, 0, 8, 0);
        painel.add(chkAtivo, gbc);

        JButton btnSalvar = Tema.botaoPrimario("Salvar Produto");
        btnSalvar.setPreferredSize(new Dimension(0, 42));
        btnSalvar.addActionListener(e -> salvar());
        gbc.gridy = 10; gbc.insets = new Insets(4, 0, 0, 0);
        painel.add(btnSalvar, gbc);

        setContentPane(painel);
    }

    private void addLabel(JPanel p, GridBagConstraints gbc, String texto, int linha) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(Tema.TEXTO_SECUNDARIO);
        gbc.gridy = linha; gbc.gridwidth = 2; gbc.insets = new Insets(8, 0, 2, 0);
        p.add(lbl, gbc);
    }

    private void carregarCategorias() {
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement("SELECT id, nome FROM categorias ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categorias.put(rs.getString("nome"), rs.getInt("id"));
                cbCategoria.addItem(rs.getString("nome"));
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erro ao carregar categorias: " + e.getMessage()); }
    }

    private void carregarDados() {
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM produtos WHERE id = ?")) {
            ps.setInt(1, idProduto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtNome.setText(rs.getString("nome"));
                txtPreco.setText(rs.getBigDecimal("preco").toPlainString());
                txtDescricao.setText(rs.getString("descricao"));
                chkAtivo.setSelected(rs.getBoolean("ativo"));
                int idCat = rs.getInt("id_categoria");
                for (Map.Entry<String, Integer> e : categorias.entrySet())
                    if (e.getValue() == idCat) { cbCategoria.setSelectedItem(e.getKey()); break; }
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage()); }
    }

    private void salvar() {
        if (txtNome.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this, "Informe o nome do produto."); return; }
        try {
            Produto p = new Produto();
            if (idProduto != null) p.setId(idProduto);
            p.setNome(txtNome.getText().trim());
            p.setDescricao(txtDescricao.getText().trim());
            p.setPreco(new java.math.BigDecimal(txtPreco.getText().trim().replace(",", ".")));
            if (cbCategoria.getSelectedItem() == null)
                throw new IllegalArgumentException("Cadastre e selecione uma categoria.");
            p.setIdCategoria(categorias.get(cbCategoria.getSelectedItem().toString()));
            p.setAtivo(chkAtivo.isSelected());
            boolean salvo = idProduto == null ? dao.salvar(p) : dao.atualizar(p);
            if (!salvo) throw new SQLException("Produto não encontrado para atualização.");
            JOptionPane.showMessageDialog(this, "Produto salvo com sucesso!");
            dispose();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Preco invalido.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage());
        }
    }
}