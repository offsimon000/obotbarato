package util;
import model.Usuario;
import model.Cliente;
import java.awt.Window;

/** Uma identidade por processo desktop; operações revalidam o acesso no banco. */
public final class Sessao {
    private static volatile Usuario usuarioAtual;
    private static volatile Cliente clienteAtual;
    private Sessao() {}
    public static synchronized void iniciar(Usuario u) { clienteAtual=null; usuarioAtual=u; }
    public static synchronized void iniciarCliente(Cliente c) { usuarioAtual=null; clienteAtual=c; }
    public static synchronized void encerrar() { usuarioAtual=null; clienteAtual=null; }
    public static Usuario getUsuario() { return usuarioAtual; }
    public static int getIdUsuario() { Usuario u=usuarioAtual; return u==null?0:u.getId(); }
    public static int getIdCliente() { Cliente c=clienteAtual; return c==null?0:c.getId(); }
    public static boolean isAdminAtivo() { Usuario u=usuarioAtual; return u!=null && u.isAdmin(); }
    public static void sair() {
        encerrar(); for(Window w:Window.getWindows()) w.dispose();
        new view.TelaLoginUnificado().setVisible(true);
    }
}
