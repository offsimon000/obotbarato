package util;

import model.Usuario;

/**
 * Controla a sessão do usuário administrativo logado.
 * Padrão Singleton estático — armazena o admin ativo durante a execução.
 *
 * Uso:
 *   Sessao.iniciar(usuario)    → ao fazer login
 *   Sessao.encerrar()          → ao fazer logout
 *   Sessao.getIdUsuario()      → nos DAOs, para auditoria
 */
public class Sessao {

    private static Usuario usuarioAtual;

    private Sessao() {}

    /** Inicia a sessão com o usuário autenticado. */
    public static void iniciar(Usuario u) {
        usuarioAtual = u;
    }

    /** Encerra a sessão (logout). */
    public static void encerrar() {
        usuarioAtual = null;
    }

    /** Retorna o usuário logado, ou null se não há sessão ativa. */
    public static Usuario getUsuario() {
        return usuarioAtual;
    }

    /**
     * Retorna o ID do usuário logado.
     * Retorna 0 se não há admin na sessão (ex: ação feita pelo cliente na loja).
     */
    public static int getIdUsuario() {
        return usuarioAtual != null ? usuarioAtual.getId() : 0;
    }

    /** Verifica se há um admin logado na sessão. */
    public static boolean isAdminAtivo() {
        return usuarioAtual != null;
    }
}
