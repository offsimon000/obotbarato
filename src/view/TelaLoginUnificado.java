package view;

import dao.ClienteLojaDAO;
import dao.UsuarioDAO;
import model.Cliente;
import model.Usuario;
import util.Sessao;
import util.Tema;
import view.loja.TelaLoja;
import javax.swing.*;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Tela de Login Unificada — visual moderno tipo site.
 *
 * Funcionalidades:
 *   - Login (admin vai para gerenciamento, cliente vai para loja)
 *   - Esqueci minha senha (redefinição por CPF/email)
 *   - Cadastro de novo cliente
 */
public class TelaLoginUnificado extends JFrame {

    // ── Painéis de navegação ──
    private JPanel painelConteudo;
    private CardLayout cardLayout;

    private static final String TELA_LOGIN    = "LOGIN";
    private static final String TELA_CADASTRO = "CADASTRO";
    private static final String TELA_SENHA    = "SENHA";

    // ── DAOs ──
    private final UsuarioDAO     usuarioDAO = new UsuarioDAO();
    private final ClienteLojaDAO clienteDAO = new ClienteLojaDAO();

    // ── Controle de tentativas ──
    private int tentativasFalhas = 0;

    public TelaLoginUnificado() {
        initComponents();
    }

    // ═══════════════════════════════════════════════════════════════
    // INICIALIZAÇÃO
    // ═══════════════════════════════════════════════════════════════

    private void initComponents() {
        setTitle("OBotBarato");
        setSize(420, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);

        JPanel raiz = criarPainelRaiz();
        setContentPane(raiz);
        setShape(new RoundRectangle2D.Double(0, 0, 420, 560, 20, 20));
    }

    // ═══════════════════════════════════════════════════════════════
    // PAINEL RAIZ COM GRADIENTE E DRAG
    // ═══════════════════════════════════════════════════════════════

    private JPanel criarPainelRaiz() {
        JPanel raiz = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            }
        };
        raiz.setBorder(BorderFactory.createLineBorder(Tema.BORDA, 1));

        MouseAdapter drag = new MouseAdapter() {
            private Point inicio;
            public void mousePressed(MouseEvent e)  { inicio = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + e.getX() - inicio.x, p.y + e.getY() - inicio.y);
            }
        };
        raiz.addMouseListener(drag);
        raiz.addMouseMotionListener(drag);

        // Botão fechar no canto superior direito
        JButton btnX = new JButton("×");
        btnX.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        btnX.setForeground(Tema.TEXTO_HINT);
        btnX.setContentAreaFilled(false);
        btnX.setBorderPainted(false);
        btnX.setFocusPainted(false);
        btnX.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnX.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 14));
        btnX.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
            public void mouseEntered(MouseEvent e) { btnX.setForeground(Tema.VERMELHO_ERRO); }
            public void mouseExited(MouseEvent e)  { btnX.setForeground(Tema.TEXTO_HINT); }
        });

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topo.setOpaque(false);
        topo.add(btnX);
        raiz.add(topo, BorderLayout.NORTH);

        // Área de conteúdo com CardLayout (troca entre Login / Cadastro / Senha)
        cardLayout    = new CardLayout();
        painelConteudo = new JPanel(cardLayout);
        painelConteudo.setOpaque(false);
        painelConteudo.add(criarTelaLogin(),    TELA_LOGIN);
        painelConteudo.add(criarTelaCadastro(), TELA_CADASTRO);
        painelConteudo.add(criarTelaSenha(),    TELA_SENHA);

        raiz.add(painelConteudo, BorderLayout.CENTER);
        return raiz;
    }

    // ═══════════════════════════════════════════════════════════════
    // TELA 1: LOGIN
    // ═══════════════════════════════════════════════════════════════

    private JPanel criarTelaLogin() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 40, 30, 40));

        GridBagConstraints g = gbc();

        // Logo
        g.gridy = 0; g.insets = new Insets(20, 0, 6, 0);
        p.add(criarLogo(), g);

        // Subtítulo
        g.gridy = 1; g.insets = new Insets(0, 0, 28, 0);
        JLabel sub = new JLabel("Bem-vindo de volta", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(Tema.TEXTO_SECUNDARIO);
        p.add(sub, g);

        // Campo email
        g.gridy = 2; g.insets = new Insets(0, 0, 4, 0);
        p.add(label("Email ou login"), g);

        JTextField txtEmail = campo();
        g.gridy = 3; g.insets = new Insets(0, 0, 16, 0);
        p.add(txtEmail, g);

        // Campo senha
        g.gridy = 4; g.insets = new Insets(0, 0, 4, 0);
        p.add(label("Senha"), g);

        JPasswordField txtSenha = campoSenha();
        g.gridy = 5; g.insets = new Insets(0, 0, 6, 0);
        p.add(txtSenha, g);

        // Link "Esqueci minha senha"
        JButton btnEsqueci = linkBtn("Esqueci minha senha");
        btnEsqueci.setHorizontalAlignment(SwingConstants.RIGHT);
        btnEsqueci.addActionListener(e -> cardLayout.show(painelConteudo, TELA_SENHA));
        g.gridy = 6; g.insets = new Insets(0, 0, 20, 0);
        p.add(btnEsqueci, g);

        // Status de erro
        JLabel lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatus.setForeground(Tema.VERMELHO_ERRO);

        // Botão Entrar
        JButton btnEntrar = botaoPrimario("Entrar");
        btnEntrar.addActionListener(e ->
            fazerLogin(txtEmail, txtSenha, lblStatus, btnEntrar));
        txtSenha.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) btnEntrar.doClick();
            }
        });
        g.gridy = 7; g.insets = new Insets(0, 0, 10, 0);
        p.add(btnEntrar, g);

        g.gridy = 8; g.insets = new Insets(0, 0, 16, 0);
        p.add(lblStatus, g);

        // Divisor + cadastro
        g.gridy = 9; g.insets = new Insets(0, 0, 14, 0);
        p.add(criarDivisor("Não tem uma conta?"), g);

        JButton btnCadastrar = botaoSecundario("Criar conta gratuita");
        btnCadastrar.addActionListener(e -> cardLayout.show(painelConteudo, TELA_CADASTRO));
        g.gridy = 10; g.insets = new Insets(0, 0, 0, 0);
        p.add(btnCadastrar, g);

        return p;
    }

    // ═══════════════════════════════════════════════════════════════
    // TELA 2: CADASTRO
    // ═══════════════════════════════════════════════════════════════

    private JPanel criarTelaCadastro() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 40, 20, 40));

        GridBagConstraints g = gbc();

        // Cabeçalho com voltar
        g.gridy = 0; g.insets = new Insets(16, 0, 20, 0);
        p.add(criarCabecalho("Criar conta", () ->
            cardLayout.show(painelConteudo, TELA_LOGIN)), g);

        // Campos
        String[] rotulos = {"Nome completo", "CPF", "Email", "Telefone", "Endereco"};
        JTextField[] campos = new JTextField[rotulos.length];
        for (int i = 0; i < rotulos.length; i++) {
            campos[i] = campo();
            g.gridy = 1 + i * 2;     g.insets = new Insets(0,0,4,0);  p.add(label(rotulos[i]), g);
            g.gridy = 1 + i * 2 + 1; g.insets = new Insets(0,0,10,0); p.add(campos[i], g);
        }

        JPasswordField fSenha   = campoSenha();
        JPasswordField fConfirm = campoSenha();

        g.gridy = 11; g.insets = new Insets(0,0,4,0);  p.add(label("Senha"), g);
        g.gridy = 12; g.insets = new Insets(0,0,10,0); p.add(fSenha, g);
        g.gridy = 13; g.insets = new Insets(0,0,4,0);  p.add(label("Confirmar senha"), g);
        g.gridy = 14; g.insets = new Insets(0,0,14,0); p.add(fConfirm, g);

        JLabel lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatus.setForeground(Tema.VERMELHO_ERRO);

        JButton btnCriar = botaoPrimario("Criar minha conta");
        btnCriar.addActionListener(e -> fazerCadastro(
            campos[0], campos[1], campos[2], campos[3], campos[4],
            fSenha, fConfirm, lblStatus));

        g.gridy = 15; g.insets = new Insets(0,0,8,0);  p.add(btnCriar, g);
        g.gridy = 16; g.insets = new Insets(0,0,0,0);  p.add(lblStatus, g);

        // CORRECAO: embrulha JScrollPane num JPanel (tipo de retorno e JPanel)
        JScrollPane scroll = new JScrollPane(p);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════════
    // TELA 3: ESQUECI MINHA SENHA
    // ═══════════════════════════════════════════════════════════════

    private JPanel criarTelaSenha() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 40, 30, 40));

        GridBagConstraints g = gbc();

        g.gridy = 0; g.insets = new Insets(16, 0, 20, 0);
        p.add(criarCabecalho("Redefinir senha", () ->
            cardLayout.show(painelConteudo, TELA_LOGIN)), g);

        // Instrução
        JLabel instrucao = new JLabel(
            "<html><div style='text-align:center'>"
            + "Digite seu email e CPF cadastrados.<br>"
            + "Se encontrarmos sua conta, voce podera criar uma nova senha."
            + "</div></html>", SwingConstants.CENTER);
        instrucao.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        instrucao.setForeground(Tema.TEXTO_SECUNDARIO);
        g.gridy = 1; g.insets = new Insets(0, 0, 22, 0);
        p.add(instrucao, g);

        // Campos
        JTextField txtEmailR = campo();
        JTextField txtCpf    = campo();

        g.gridy = 2; g.insets = new Insets(0,0,4,0);  p.add(label("Email"), g);
        g.gridy = 3; g.insets = new Insets(0,0,14,0); p.add(txtEmailR, g);
        g.gridy = 4; g.insets = new Insets(0,0,4,0);  p.add(label("CPF cadastrado"), g);
        g.gridy = 5; g.insets = new Insets(0,0,20,0); p.add(txtCpf, g);

        JLabel lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JButton btnVerificar = botaoPrimario("Verificar minha conta");
        btnVerificar.addActionListener(e ->
            verificarParaRedefinir(txtEmailR, txtCpf, lblStatus, p, g));

        g.gridy = 6; g.insets = new Insets(0,0,10,0); p.add(btnVerificar, g);
        g.gridy = 7; g.insets = new Insets(0,0,0,0);  p.add(lblStatus, g);

        return p;
    }

    // ═══════════════════════════════════════════════════════════════
    // LÓGICAS DE AÇÃO
    // ═══════════════════════════════════════════════════════════════

    private void fazerLogin(JTextField txtEmail, JPasswordField txtSenha,
                             JLabel lblStatus, JButton btnEntrar) {
        String login = txtEmail.getText().trim();
        String senha = new String(txtSenha.getPassword());

        if (login.isEmpty() || senha.isEmpty()) {
            erro(lblStatus, "Preencha todos os campos."); return;
        }

        btnEntrar.setEnabled(false);
        lblStatus.setForeground(Tema.TEXTO_SECUNDARIO);
        lblStatus.setText("Verificando...");

        SwingWorker<Object, Void> worker = new SwingWorker<>() {
            protected Object doInBackground() throws Exception {
                Usuario u = usuarioDAO.autenticar(login, senha);
                if (u != null) return u;
                Cliente c = clienteDAO.autenticar(login, senha);
                if (c != null) return c;
                // Delay crescente após falhas consecutivas
                if (tentativasFalhas >= 2) {
                    try { Thread.sleep(1000L * Math.min(tentativasFalhas, 4)); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                }
                return null;
            }

            protected void done() {
                btnEntrar.setEnabled(true);
                try {
                    Object res = get();
                    if (res instanceof Usuario) {
                        tentativasFalhas = 0;
                        Sessao.iniciar((Usuario) res); // inicia sessão de auditoria
                        dispose(); new TelaInicial((Usuario) res).setVisible(true);
                    } else if (res instanceof Cliente) {
                        tentativasFalhas = 0;
                        Sessao.encerrar();
                        dispose(); new TelaLoja((Cliente) res).setVisible(true);
                    } else {
                        tentativasFalhas++;
                        txtSenha.setText("");
                        txtSenha.requestFocus();
                        erro(lblStatus, tentativasFalhas >= 3
                            ? "Muitas tentativas. Aguarde alguns segundos."
                            : "Email/login ou senha incorretos.");
                    }
                } catch (Exception e) {
                    erro(lblStatus, util.Erros.mensagem(e));
                }
            }
        };
        worker.execute();
    }

    private void fazerCadastro(JTextField fNome, JTextField fCpf, JTextField fEmail,
                                JTextField fTel, JTextField fEnd,
                                JPasswordField fSenha, JPasswordField fConfirm,
                                JLabel lblStatus) {
        String senha   = new String(fSenha.getPassword());
        String confirm = new String(fConfirm.getPassword());

        if (!senha.equals(confirm)) {
            erro(lblStatus, "As senhas nao conferem."); return;
        }

        try {
        String resultado = clienteDAO.cadastrar(
            fNome.getText(), fCpf.getText(), fEmail.getText(),
            fTel.getText(), fEnd.getText(), senha);

        if (resultado == null) {
            // Sucesso — faz login automático
            lblStatus.setForeground(Tema.VERDE_SUCESSO);
            lblStatus.setText("Conta criada! Entrando...");
            SwingWorker<Cliente, Void> w = new SwingWorker<>() {
                protected Cliente doInBackground() throws Exception {
                    return clienteDAO.autenticar(fEmail.getText().trim(), senha);
                }
                protected void done() {
                    try {
                        Cliente c = get();
                        if (c != null) { Sessao.encerrar(); dispose(); new TelaLoja(c).setVisible(true); }
                        else cardLayout.show(painelConteudo, TELA_LOGIN);
                    } catch (Exception ex) { cardLayout.show(painelConteudo, TELA_LOGIN); }
                }
            };
            w.execute();
        } else {
            erro(lblStatus, resultado);
        }
        } catch (Exception e) { erro(lblStatus, util.Erros.mensagem(e)); }
    }

    private void verificarParaRedefinir(JTextField txtEmail, JTextField txtCpf,
                                         JLabel lblStatus, JPanel painel,
                                         GridBagConstraints g) {
        String email = txtEmail.getText().trim().toLowerCase();
        String cpf   = txtCpf.getText().trim();

        if (email.isEmpty() || cpf.isEmpty()) {
            erro(lblStatus, "Preencha email e CPF."); return;
        }

        // Verifica se existe cliente com esse email e CPF
        boolean existe;
        try { existe = clienteDAO.verificarEmailCpf(email, cpf); }
        catch (Exception e) { erro(lblStatus, util.Erros.mensagem(e)); return; }
        if (!existe) {
            erro(lblStatus, "Nao encontramos uma conta com esses dados.");
            return;
        }

        // Mostra campos de nova senha
        lblStatus.setText(" ");
        JPasswordField fNovaSenha   = campoSenha();
        JPasswordField fNovConfirm  = campoSenha();
        JLabel lblNova = new JLabel(" ", SwingConstants.CENTER);
        lblNova.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        g.gridy = 8; g.insets = new Insets(14,0,4,0);
        painel.add(label("Nova senha"), g);
        g.gridy = 9; g.insets = new Insets(0,0,10,0);
        painel.add(fNovaSenha, g);
        g.gridy = 10; g.insets = new Insets(0,0,4,0);
        painel.add(label("Confirmar nova senha"), g);
        g.gridy = 11; g.insets = new Insets(0,0,14,0);
        painel.add(fNovConfirm, g);

        JButton btnSalvar = botaoPrimario("Salvar nova senha");
        btnSalvar.addActionListener(ev -> {
            String nova    = new String(fNovaSenha.getPassword());
            String confirm = new String(fNovConfirm.getPassword());
            if (nova.length() < 6) { erro(lblNova, "Minimo 6 caracteres."); return; }
            if (!nova.equals(confirm)) { erro(lblNova, "As senhas nao conferem."); return; }

            boolean ok;
            try { ok = clienteDAO.redefinirSenha(email, nova); }
            catch (Exception e) { erro(lblNova, util.Erros.mensagem(e)); return; }
            if (ok) {
                lblNova.setForeground(Tema.VERDE_SUCESSO);
                lblNova.setText("Senha alterada! Faca login.");
                Timer t = new Timer(2000, e2 -> cardLayout.show(painelConteudo, TELA_LOGIN));
                t.setRepeats(false); t.start();
            } else {
                erro(lblNova, "Erro ao salvar. Tente novamente.");
            }
        });

        g.gridy = 12; g.insets = new Insets(0,0,8,0); painel.add(btnSalvar, g);
        g.gridy = 13; g.insets = new Insets(0,0,0,0); painel.add(lblNova, g);

        painel.revalidate(); painel.repaint();
        txtEmail.setEnabled(false);
        txtCpf.setEnabled(false);
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPONENTES VISUAIS REUTILIZÁVEIS
    // ═══════════════════════════════════════════════════════════════

    private JPanel criarLogo() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        p.setOpaque(false);
        JLabel star = new JLabel("*");
        star.setFont(new Font("Segoe UI", Font.BOLD, 32));
        star.setForeground(Tema.ROXO_PRIMARIO);
        JLabel nome = new JLabel("OBotBarato");
        nome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        nome.setForeground(Tema.TEXTO_PRIMARIO);
        p.add(star); p.add(nome);
        return p;
    }

    private JPanel criarCabecalho(String titulo, Runnable aoVoltar) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JButton btnVoltar = new JButton("← Voltar");
        btnVoltar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnVoltar.setForeground(Tema.ROXO_PRIMARIO);
        btnVoltar.setContentAreaFilled(false);
        btnVoltar.setBorderPainted(false);
        btnVoltar.setFocusPainted(false);
        btnVoltar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVoltar.addActionListener(e -> aoVoltar.run());

        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);

        p.add(btnVoltar, BorderLayout.WEST);
        p.add(lblTitulo, BorderLayout.CENTER);
        return p;
    }

    private JPanel criarDivisor(String texto) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        JSeparator esq = new JSeparator();
        JSeparator dir = new JSeparator();
        esq.setForeground(Tema.BORDA);
        dir.setForeground(Tema.BORDA);
        JLabel lbl = new JLabel(texto, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(Tema.TEXTO_HINT);
        p.add(esq,  BorderLayout.WEST);
        p.add(lbl,  BorderLayout.CENTER);
        p.add(dir,  BorderLayout.EAST);
        return p;
    }

    private JLabel label(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(Tema.TEXTO_SECUNDARIO);
        return l;
    }

    private JTextField campo() {
        JTextField tf = new JTextField();
        tf.setBackground(Color.WHITE);
        tf.setForeground(Tema.TEXTO_PRIMARIO);
        tf.setCaretColor(Tema.ROXO_PRIMARIO);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setPreferredSize(new Dimension(320, 42));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.BORDA, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        tf.setDocument(new LimiteCaracteres(150));
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Tema.ROXO_PRIMARIO, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
            public void focusLost(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Tema.BORDA, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
        });
        return tf;
    }

    private JPasswordField campoSenha() {
        JPasswordField pf = new JPasswordField();
        pf.setBackground(Color.WHITE);
        pf.setForeground(Tema.TEXTO_PRIMARIO);
        pf.setCaretColor(Tema.ROXO_PRIMARIO);
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pf.setPreferredSize(new Dimension(320, 42));
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.BORDA, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        pf.setDocument(new LimiteCaracteres(128));
        pf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                pf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Tema.ROXO_PRIMARIO, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
            public void focusLost(FocusEvent e) {
                pf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Tema.BORDA, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
        });
        return pf;
    }

    private JButton botaoPrimario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(Tema.ROXO_PRIMARIO);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(320, 46));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(Tema.ROXO_HOVER); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(Tema.ROXO_PRIMARIO); }
        });
        return btn;
    }

    private JButton botaoSecundario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(Color.WHITE);
        btn.setForeground(Tema.ROXO_PRIMARIO);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Tema.ROXO_PRIMARIO, 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(320, 42));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(237, 242, 255)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private JButton linkBtn(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setForeground(Tema.ROXO_PRIMARIO);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(Tema.ROXO_HOVER); }
            public void mouseExited(MouseEvent e)  { btn.setForeground(Tema.ROXO_PRIMARIO); }
        });
        return btn;
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.CENTER;
        return g;
    }

    private void erro(JLabel lbl, String msg) {
        lbl.setForeground(Tema.VERMELHO_ERRO);
        lbl.setText(msg);
    }

    private static class LimiteCaracteres extends PlainDocument {
        private final int limite;
        LimiteCaracteres(int limite) { this.limite = limite; }
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if (str != null && getLength() + str.length() <= limite)
                super.insertString(offs, str, a);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TelaLoginUnificado().setVisible(true));
    }
}