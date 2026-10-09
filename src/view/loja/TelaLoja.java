package view.loja;

import dao.ClienteLojaDAO;
import dao.ProdutoDAO;
import model.Cliente;
import model.ItemPedido;
import model.Produto;
import util.ConexaoBanco;
import util.TemaLoja;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TelaLoja extends JFrame {

    private Cliente clienteLogado;
    private List<Produto> produtosExibidos = new ArrayList<>();
    private final model.Carrinho carrinho = new model.Carrinho();
    private Set<Integer> favoritosSet;     // carregado UMA vez, não por produto
    private JButton lblCarrinho;
    private JPanel painelGaleria;
    private final JLabel avisoLoja=new JLabel(" ");
    private final JLabel quantidadeProdutos=new JLabel("Carregando catálogo...");
    private TelaCarrinho janelaCarrinho;
    private TelaFavoritos janelaFavoritos;
    private TelaMeusPedidos janelaPedidos;
    private boolean adicionando;
    private long consultaCatalogo;

    private JComboBox<String> cbCategoria;
    private JTextField txtBusca;
    private JSlider sliderPreco;
    private JLabel lblPrecoMax;

    private final ProdutoDAO     produtoDAO = new ProdutoDAO();
    private final ClienteLojaDAO clienteDAO = new ClienteLojaDAO();

    public TelaLoja(Cliente cliente) {
        util.Acesso.telaCliente(cliente.getId());
        this.clienteLogado = cliente;
        // Carrega todos os favoritos de uma vez (evita N+1 queries na galeria)
        this.favoritosSet = new java.util.HashSet<>();
        recarregarFavoritos();
        initComponents();
        carregarProdutos();
    }

    private void recarregarFavoritos() {
        try { favoritosSet = clienteDAO.listarFavoritosSet(clienteLogado.getId()); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, util.Erros.mensagem(e)); }
    }

    // ── Inicialização ────────────────────────────────────────────────────
    private void initComponents() {
        setTitle("OBotBarato - Loja");
        setSize(1060, 680);
        setMinimumSize(new Dimension(960,620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel principal = new JPanel(new BorderLayout());
        principal.setBackground(TemaLoja.BG_PRINCIPAL);
        principal.add(criarTopbar(),      BorderLayout.NORTH);
        principal.add(criarSidebar(),     BorderLayout.WEST);
        principal.add(criarAreaGaleria(), BorderLayout.CENTER);
        setContentPane(principal);
        addWindowListener(new java.awt.event.WindowAdapter(){public void windowActivated(java.awt.event.WindowEvent e){if(util.Sessao.getIdCliente()==clienteLogado.getId()){recarregarFavoritos();carregarProdutos();}}});
    }

    // ── Topbar ───────────────────────────────────────────────────────────
    private JPanel criarTopbar() {
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(TemaLoja.BG_CARD);
        topbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, TemaLoja.BORDA),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JPanel esq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        esq.setOpaque(false);
        JLabel marca = new JLabel("OBotBarato");
        marca.setFont(new Font("Segoe UI", Font.BOLD, 20));
        marca.setForeground(TemaLoja.TEXTO_PRIMARIO);
        esq.add(marca);

        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        dir.setOpaque(false);

        JLabel lblOla = new JLabel("Olá, " + clienteLogado.getNome().split(" ")[0]);
        lblOla.setFont(TemaLoja.FONTE_LABEL);
        lblOla.setForeground(TemaLoja.TEXTO_SECUNDARIO);

        JButton btnFav     = TemaLoja.botaoFantasia("Favoritos");
        JButton btnPedidos = TemaLoja.botaoFantasia("Meus Pedidos");

        lblCarrinho = TemaLoja.botaoPrimario("Carrinho (0)");
        lblCarrinho.addActionListener(e->abrirCarrinho());

        JButton btnSair = TemaLoja.botaoFantasia("Sair");
        btnSair.setForeground(TemaLoja.VERMELHO_ERRO);
        btnSair.addActionListener(e -> { carrinho.encerrar(); favoritosSet.clear(); util.Sessao.sair(); });

        btnFav.addActionListener(e -> {
            if(janelaFavoritos==null || !janelaFavoritos.isDisplayable())janelaFavoritos=new TelaFavoritos(this,clienteLogado);
            mostrarJanela(janelaFavoritos);
        });
        btnPedidos.addActionListener(e ->
            abrirPedidos());

        dir.add(lblOla); dir.add(btnFav); dir.add(btnPedidos);
        dir.add(lblCarrinho); dir.add(btnSair);
        topbar.add(esq, BorderLayout.WEST);
        topbar.add(dir, BorderLayout.EAST);
        return topbar;
    }

    // ── Sidebar de filtros ───────────────────────────────────────────────
    private JPanel criarSidebar() {
        JPanel sb = new JPanel();
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setBackground(TemaLoja.BG_CARD);
        sb.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, TemaLoja.BORDA),
            BorderFactory.createEmptyBorder(20, 16, 20, 16)));
        sb.setPreferredSize(new Dimension(200, 0));

        JLabel titulo = new JLabel("Filtros");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(TemaLoja.TEXTO_PRIMARIO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(titulo);
        sb.add(Box.createVerticalStrut(14));

        // Busca
        sb.add(TemaLoja.rotulo("BUSCAR")); sb.add(Box.createVerticalStrut(4));
        txtBusca = TemaLoja.campo(14);
        txtBusca.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtBusca.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtBusca.addActionListener(e->carregarProdutos());
        sb.add(txtBusca);

        // Categoria
        sb.add(Box.createVerticalStrut(14));
        sb.add(TemaLoja.rotulo("CATEGORIA")); sb.add(Box.createVerticalStrut(4));
        List<String> cats = carregarCategorias();
        cats.add(0, "Todas");
        cbCategoria = TemaLoja.combo(cats.toArray(new String[0]));
        cbCategoria.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbCategoria.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(cbCategoria);

        // Preco
        sb.add(Box.createVerticalStrut(14));
        sb.add(TemaLoja.rotulo("PRECO MAXIMO")); sb.add(Box.createVerticalStrut(4));
        sliderPreco = new JSlider(0, 500, 500);
        sliderPreco.setBackground(TemaLoja.BG_CARD);
        sliderPreco.setForeground(TemaLoja.ROSA_PRIMARIO);
        sliderPreco.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        sliderPreco.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblPrecoMax = new JLabel("Qualquer preço");
        lblPrecoMax.setFont(TemaLoja.FONTE_LABEL);
        lblPrecoMax.setForeground(TemaLoja.VERDE_PRECO);
        lblPrecoMax.setAlignmentX(Component.LEFT_ALIGNMENT);
        sliderPreco.addChangeListener(e ->
            lblPrecoMax.setText(sliderPreco.getValue()==500?"Qualquer preço":String.format("Até R$ %d,00", sliderPreco.getValue())));
        sb.add(sliderPreco); sb.add(lblPrecoMax);

        sb.add(Box.createVerticalStrut(18));
        JButton btnFiltrar = TemaLoja.botaoPrimario("Filtrar");
        btnFiltrar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnFiltrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnFiltrar.addActionListener(e -> carregarProdutos());
        sb.add(btnFiltrar);

        sb.add(Box.createVerticalStrut(8));
        JButton btnLimpar = TemaLoja.botaoFantasia("Limpar");
        btnLimpar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLimpar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnLimpar.addActionListener(e -> limparFiltros());
        sb.add(btnLimpar);

        sb.add(Box.createVerticalGlue());
        return sb;
    }

    // ── Galeria ──────────────────────────────────────────────────────────
    private JPanel criarAreaGaleria() {
        painelGaleria = new JPanel(new GridLayout(0, 3, 14, 14));
        painelGaleria.setBackground(TemaLoja.BG_PRINCIPAL);
        painelGaleria.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel conteudo=new JPanel(new BorderLayout());conteudo.setBackground(TemaLoja.BG_PRINCIPAL);conteudo.add(painelGaleria,BorderLayout.NORTH);
        JScrollPane scroll = TemaLoja.scroll(conteudo);
        scroll.setBorder(null);
        scroll.addComponentListener(new java.awt.event.ComponentAdapter(){public void componentResized(java.awt.event.ComponentEvent e){if(painelGaleria.getLayout() instanceof GridLayout){int colunas=Math.max(2,Math.min(4,(scroll.getViewport().getWidth()-20)/245));((GridLayout)painelGaleria.getLayout()).setColumns(colunas);painelGaleria.revalidate();}}});
        JPanel area=new JPanel(new BorderLayout(0,8));area.setBackground(TemaLoja.BG_PRINCIPAL);
        JPanel topo=new JPanel(new BorderLayout(0,4));topo.setOpaque(false);topo.setBorder(BorderFactory.createEmptyBorder(16,20,0,20));
        JLabel titulo=new JLabel("Encontre seu próximo produto");titulo.setFont(TemaLoja.FONTE_TITULO);topo.add(titulo,BorderLayout.NORTH);
        quantidadeProdutos.setFont(TemaLoja.FONTE_LABEL);quantidadeProdutos.setForeground(TemaLoja.TEXTO_SECUNDARIO);topo.add(quantidadeProdutos);area.add(topo,BorderLayout.NORTH);area.add(scroll);
        avisoLoja.setFont(TemaLoja.FONTE_LABEL);avisoLoja.setForeground(TemaLoja.AZUL_INFO);avisoLoja.setBorder(BorderFactory.createEmptyBorder(8,20,12,20));area.add(avisoLoja,BorderLayout.SOUTH);return area;
    }

    // ── Carregamento de produtos ─────────────────────────────────────────
    private void carregarProdutos() {
        String busca    = txtBusca.getText().trim();
        String catSel   = (String) cbCategoria.getSelectedItem();
        double precoMax = sliderPreco.getValue();
        long versao=++consultaCatalogo;quantidadeProdutos.setText("Atualizando catálogo...");
        new SwingWorker<List<ProdutoDAO.Disponivel>,Void>(){
            protected List<ProdutoDAO.Disponivel> doInBackground()throws Exception{return produtoDAO.listarCatalogo(busca);}
            protected void done(){if(versao!=consultaCatalogo || util.Sessao.getIdCliente()!=clienteLogado.getId())return;
        try {
            produtosExibidos.clear();painelGaleria.removeAll();painelGaleria.setLayout(new GridLayout(0,3,14,14));
            for (var disponivel : get()) {
                Produto p=disponivel.produto();
                if (!"Todas".equals(catSel) && !catSel.equals(p.getNomeCategoria()))
                    continue;
                if (p.getPreco() > precoMax && precoMax < 500)
                    continue;

                int estoque = disponivel.quantidade();
                produtosExibidos.add(p);
                painelGaleria.add(criarCartaoProduto(p, estoque));
            }

            if (produtosExibidos.isEmpty()) {
                JLabel vazio = new JLabel("Nenhum produto encontrado.", SwingConstants.CENTER);
                vazio.setFont(TemaLoja.FONTE_LABEL);
                vazio.setForeground(TemaLoja.TEXTO_SECUNDARIO);
                painelGaleria.setLayout(new BorderLayout());
                painelGaleria.add(vazio, BorderLayout.CENTER);
            } else {
                int largura=painelGaleria.getParent().getWidth();
                painelGaleria.setLayout(new GridLayout(0,Math.max(2,Math.min(4,(largura-32)/245)),14,14));
            }
            quantidadeProdutos.setText(produtosExibidos.size()+" produtos encontrados • Revise quantidades e entrega no carrinho");
        } catch (Exception ex) {
            quantidadeProdutos.setText("Não foi possível atualizar o catálogo.");avisoLoja.setText(util.Erros.mensagem(ex));
        }
        painelGaleria.revalidate();
        painelGaleria.repaint();
        }}.execute();
    }

    // ── Cartao de produto ────────────────────────────────────────────────
    private JPanel criarCartaoProduto(Produto p, int estoque) {
        boolean disponivel = estoque > 0;
        // Consulta o Set em memória — zero query adicional por produto
        boolean isFav = favoritosSet.contains(p.getId());

        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TemaLoja.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(240,300));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(TemaLoja.BORDA, 1),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel img = util.Imagens.painel(p.getImagem(),p.getNomeCategoria(),100);
        card.add(img, BorderLayout.NORTH);

        // Informações
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel nome  = new JLabel("<html><b>" + p.getNome() + "</b></html>");
        nome.setFont(TemaLoja.FONTE_PRODUTO);
        nome.setForeground(TemaLoja.TEXTO_PRIMARIO);
        JLabel preco = new JLabel(String.format("R$ %.2f", p.getPreco()));
        preco.setFont(TemaLoja.FONTE_PRECO);
        preco.setForeground(TemaLoja.VERDE_PRECO);
        JLabel est = new JLabel(disponivel ? "Em estoque (" + estoque + ")" : "Esgotado");
        est.setFont(TemaLoja.FONTE_LABEL);
        est.setForeground(disponivel ? TemaLoja.TEXTO_SECUNDARIO : TemaLoja.VERMELHO_ERRO);
        info.add(nome); info.add(Box.createVerticalStrut(4));
        info.add(preco); info.add(Box.createVerticalStrut(2));
        info.add(est);
        card.add(info, BorderLayout.CENTER);

        // Botões
        JPanel btns = new JPanel(new GridLayout(2,1,0,6));
        btns.setOpaque(false);

        JButton btnFavBtn = TemaLoja.botaoFantasia(isFav ? "Salvo nos favoritos" : "Salvar favorito");
        btnFavBtn.setBackground(isFav ? new Color(255, 243, 205) : TemaLoja.BG_INPUT);
        btnFavBtn.setForeground(isFav ? TemaLoja.AMARELO_FAV : TemaLoja.TEXTO_SECUNDARIO);
        btnFavBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnFavBtn.setFocusPainted(false);
        btnFavBtn.setBorderPainted(false);
        btnFavBtn.setPreferredSize(new Dimension(60, 34));
        btnFavBtn.setMargin(new Insets(2,2,2,2));
        btnFavBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFavBtn.addActionListener(e -> {
            try {
            if (favoritosSet.contains(p.getId())) {
                clienteDAO.removerFavorito(clienteLogado.getId(), p.getId());
                favoritosSet.remove(p.getId());
                btnFavBtn.setText("Salvar favorito");
                btnFavBtn.setForeground(TemaLoja.TEXTO_SECUNDARIO);
                btnFavBtn.setBackground(TemaLoja.BG_INPUT);
            } else {
                clienteDAO.adicionarFavorito(clienteLogado.getId(), p.getId());
                favoritosSet.add(p.getId());
                btnFavBtn.setText("Salvo nos favoritos");
                btnFavBtn.setForeground(TemaLoja.AMARELO_FAV);
                btnFavBtn.setBackground(new Color(255, 243, 205));
            }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, util.Erros.mensagem(ex), "Favoritos", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnAdd = TemaLoja.botaoPrimario(disponivel?"Adicionar ao carrinho":"Sem estoque");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnAdd.setEnabled(disponivel);
        if (!disponivel) {
            btnAdd.setBackground(TemaLoja.BORDA);
            btnAdd.setForeground(TemaLoja.TEXTO_HINT);
        }
        btnAdd.addActionListener(e -> adicionarAoCarrinho(p));

        btns.add(btnFavBtn, BorderLayout.WEST);
        btns.add(btnAdd,    BorderLayout.CENTER);
        card.add(btns, BorderLayout.SOUTH);

        // Hover
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(TemaLoja.BORDA_HOVER, 1),
                    BorderFactory.createEmptyBorder(12,12,12,12)));
            }
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(TemaLoja.BORDA, 1),
                    BorderFactory.createEmptyBorder(12,12,12,12)));
            }
        });
        return card;
    }

    // ── Ações ────────────────────────────────────────────────────────────
    public void adicionarAoCarrinho(Produto p) {
        if(adicionando)return;
        if(carrinho.bloqueado()){avisoLoja.setText("Finalize ou feche a etapa de pagamento antes de alterar o carrinho.");return;}
        adicionando=true;avisoLoja.setText("Conferindo disponibilidade...");
        new SwingWorker<ProdutoDAO.Disponivel,Void>(){
            protected ProdutoDAO.Disponivel doInBackground()throws Exception{return produtoDAO.buscarParaCarrinho(p.getId());}
            protected void done(){adicionando=false;if(util.Sessao.getIdCliente()!=clienteLogado.getId())return;try{var atual=get();carrinho.adicionar(atual.produto(),1,atual.quantidade());atualizarBadgeCarrinho();avisoLoja.setText(atual.produto().getNome()+" adicionado. Abra o carrinho para revisar ou alterar a quantidade.");}catch(Exception ex){avisoLoja.setText(util.Erros.mensagem(ex));}}
        }.execute();
    }
    protected void mostrarJanela(JFrame janela){janela.setVisible(true);janela.toFront();}
    public void abrirCarrinho(){if(janelaCarrinho==null || !janelaCarrinho.isDisplayable())janelaCarrinho=new TelaCarrinho(this,clienteLogado,getCarrinho());mostrarJanela(janelaCarrinho);}
    public void abrirPedidos(){if(janelaPedidos==null || !janelaPedidos.isDisplayable())janelaPedidos=new TelaMeusPedidos(this,clienteLogado);mostrarJanela(janelaPedidos);}
    public void atualizarCatalogo(){carregarProdutos();}
    public model.Carrinho getModeloCarrinho(){return carrinho;}
    public void atualizarBadgeCarrinho(){
        int total=carrinho.itens().stream().mapToInt(ItemPedido::getQuantidade).sum();lblCarrinho.setText("Carrinho ("+total+")");
        for(Window w:Window.getWindows())if(w instanceof TelaCarrinho && w.isDisplayable())((TelaCarrinho)w).carregarTabela();
    }
    /** Cópias: telas não podem alterar o carrinho por fora das regras. */
    public List<ItemPedido> getCarrinho(){return carrinho.itens();}
    private void limparFiltros() {
        txtBusca.setText("");
        cbCategoria.setSelectedIndex(0);
        sliderPreco.setValue(500);
        carregarProdutos();
    }

    private List<String> carregarCategorias() {
        List<String> lista = new ArrayList<>();
        try {for(var categoria:new dao.CategoriaDAO().listar(false))lista.add(categoria.nome());}
        catch(Exception e){avisoLoja.setText(util.Erros.mensagem(e));}
        return lista;
    }
}
