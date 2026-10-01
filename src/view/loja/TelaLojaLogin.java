package view.loja;

import view.TelaLoginUnificado;

/** Compatibilidade para projetos antigos: usa o login oficial. */
@Deprecated
public class TelaLojaLogin extends TelaLoginUnificado {
    public TelaLojaLogin() { super(); }
    public static void main(String[] args) { TelaLoginUnificado.main(args); }
}
