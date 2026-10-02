import dao.*;
import model.*;
import util.*;
import view.*;
import view.loja.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Smoke test das próprias telas do projeto, sem mostrar janelas ao usuário. */
public final class InicializacaoTelas {
    private static int quantidade;
    public static void main(String[] args) throws Exception {
        Thread limite = new Thread(() -> {
            try { Thread.sleep(45000); } catch (InterruptedException ignored) { return; }
            System.err.println("Falha: inicialização de telas excedeu o tempo limite.");
            System.exit(1);
        });
        limite.setDaemon(true); limite.start();
        Usuario admin = new UsuarioDAO().autenticar("admin", "admin123");
        Cliente cliente = new ClienteLojaDAO().autenticar("cliente@demo.local", "cliente123");
        if (admin == null || cliente == null) throw new AssertionError("Demonstração indisponível.");
        SwingUtilities.invokeAndWait(() -> {
            try {
                TelaLoginUnificado login = conferir(new TelaLoginUnificado());
                imagem(login, "login.png");
                Sessao.iniciar(admin);
                TelaInicial painel = conferir(new TelaInicial(admin));
                conferir(new TelaProdutos(painel)); imagem(conferir(new TelaClientes(painel)),"clientes.png");
                imagem(conferir(new TelaEstoque(painel)),"estoque.png"); conferir(new TelaPedidos(admin, painel));
                conferir(new TelaGestaoPedidos(painel));
                conferir(new TelaRelatorios(painel)); conferir(new TelaPagamentos(painel)); imagem(conferir(new FormularioProduto(painel, null)),"formulario-produto.png");
                imagem(conferir(new TelaUsuarios(painel)),"usuarios.png"); imagem(conferir(new TelaCategorias(painel)),"categorias.png");
                imagem(painel,"painel-admin.png");
                Sessao.iniciarCliente(cliente);
                TelaLoja loja = conferir(new TelaLoja(cliente));
                imagem(loja, "loja.png");
                conferir(new TelaFavoritos(loja, cliente));
                TelaCarrinho carrinho = conferir(new TelaCarrinho(loja, cliente, loja.getCarrinho()));
                conferir(new TelaCheckout(carrinho, cliente, loja.getCarrinho(), loja));
                conferir(new TelaMeusPedidos(loja, cliente));
                System.out.println("APROVADO: " + quantidade + " telas inicializadas no EDT sem exibir janelas.");
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { for (Window w : Window.getWindows()) w.dispose(); Sessao.encerrar(); }
        });
        System.exit(0);
    }

    private static <T extends Window> T conferir(T tela) {
        if (tela.getWidth() <= 0 || tela.getHeight() <= 0) throw new AssertionError("Tela sem tamanho.");
        tela.addNotify(); tela.validate(); quantidade++;
        System.out.println("OK tela: " + tela.getClass().getSimpleName());
        return tela;
    }
    private static void imagem(Window tela, String nome) throws Exception {
        Container painel = tela instanceof JFrame ? ((JFrame)tela).getContentPane() : ((JDialog)tela).getContentPane();
        BufferedImage img = new BufferedImage(painel.getWidth(), painel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try { painel.printAll(g); } finally { g.dispose(); }
        Path pasta = Path.of("docs/validacao-ui"); Files.createDirectories(pasta);
        ImageIO.write(img, "png", pasta.resolve(nome).toFile());
    }
}
