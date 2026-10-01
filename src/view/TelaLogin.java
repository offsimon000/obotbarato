package view;

/** Compatibilidade para projetos antigos: usa o login oficial. */
@Deprecated
public class TelaLogin extends TelaLoginUnificado {
    public TelaLogin() { super(); }
    public static void main(String[] args) { TelaLoginUnificado.main(args); }
}
