package util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Cada chamada entrega uma conexão independente, fechada pelo chamador. */
public final class ConexaoBanco {
    private ConexaoBanco() {}

    public static Connection getConexao() throws SQLException {
        Properties config = new Properties();
        String arquivo = System.getProperty("obotbarato.config", "config/banco.properties");
        Path caminho = Path.of(arquivo);
        if (Files.exists(caminho)) {
            try (Reader reader = Files.newBufferedReader(caminho, StandardCharsets.UTF_8)) {
                config.load(reader);
            } catch (IOException e) {
                throw new SQLException("Não foi possível ler a configuração do banco.", e);
            }
        } else if (System.getProperty("obotbarato.config") != null) {
            throw new SQLException("Arquivo de configuração do banco não encontrado: " + arquivo);
        }
        String url = configurar("DB_URL", config, "url", "jdbc:postgresql://localhost:5432/obotbarato");
        Properties acesso = new Properties();
        acesso.setProperty("user", configurar("DB_USUARIO", config, "usuario", "postgres"));
        acesso.setProperty("password", configurar("DB_SENHA", config, "senha", ""));
        acesso.setProperty("connectTimeout", "5");
        acesso.setProperty("socketTimeout", "30");
        acesso.setProperty("ApplicationName", "OBotBarato");
        try {
            return DriverManager.getConnection(url, acesso);
        } catch (SQLException e) {
            throw new SQLException("Não foi possível conectar ao PostgreSQL. Verifique o serviço, "
                    + "o banco e config/banco.properties (ou DB_URL, DB_USUARIO e DB_SENHA). "
                    + "Confira também o driver na pasta lib.", e.getSQLState(), e.getErrorCode(), e);
        }
    }

    private static String configurar(String ambiente, Properties config, String chave, String padrao) {
        String valor = System.getenv(ambiente);
        return valor != null ? valor : config.getProperty(chave, padrao);
    }
}
