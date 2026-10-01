package util;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Mensagens para a interface; diagnóstico técnico fica no registro de erros. */
public final class Erros {
    private Erros() {}
    public static String mensagem(Throwable erro) {
        while (erro instanceof ExecutionException && erro.getCause() != null) erro = erro.getCause();
        Logger.getLogger("OBotBarato").log(Level.WARNING, "Operação não concluída", erro);
        if (erro instanceof SQLException) {
            String estado = ((SQLException) erro).getSQLState();
            if ("IM003".equals(estado)) return "Driver do banco ausente. Recompile o projeto.";
            if (estado != null && (estado.startsWith("08") || estado.startsWith("28") || "3D000".equals(estado)))
                return "Banco indisponível. Inicie a base e confira a configuração.";
            if ("23505".equals(estado)) return "Já existe um cadastro com esses dados.";
            if ("23503".equals(estado)) return "Cadastro relacionado não encontrado ou ainda em uso.";
            if ("23514".equals(estado) || "23502".equals(estado) || "22003".equals(estado) || "22001".equals(estado))
                return "Confira os campos obrigatórios e os valores informados.";
            if (estado == null && erro.getMessage() != null) return erro.getMessage();
            return "Não foi possível concluir a operação no banco. Tente novamente.";
        }
        if (erro instanceof IllegalArgumentException) return erro.getMessage();
        return "Não foi possível concluir a operação. Consulte o registro de erros.";
    }
}
