package util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.*;
import java.util.HexFormat;
import java.util.List;

/** Instala migrations em uma base nova, sem apagar tabelas ou dados existentes. */
public final class InicializarBanco {
    private InicializarBanco() {}

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--demo")))
            throw new IllegalArgumentException("Uso: util.InicializarBanco [--demo]");
        aplicarMigrations(Path.of("sql"));
        if (args.length == 1) instalarDemonstracao();
        System.out.println("Banco atualizado. Dados de demonstração: " + (args.length == 1));
    }

    public static void aplicarMigrations(Path pasta) throws Exception {
        List<Path> arquivos;
        try (var stream = Files.list(pasta)) {
            arquivos = stream.filter(p -> p.getFileName().toString().matches("V[0-9]+__.*\\.sql"))
                    .sorted().toList();
        }
        if (arquivos.isEmpty()) throw new IllegalArgumentException("Nenhuma migration encontrada.");
        Transacao.executar(con -> {
            try (Statement st = con.createStatement()) {
                st.execute("SELECT pg_advisory_xact_lock(726281)");
                st.execute("CREATE TABLE IF NOT EXISTS schema_migrations (versao VARCHAR(120) PRIMARY KEY, "
                        + "checksum VARCHAR(64) NOT NULL, aplicada_em TIMESTAMP NOT NULL DEFAULT NOW())");
            }
            for (Path arquivo : arquivos) {
                String nome = arquivo.getFileName().toString();
                String sql;
                String checksum;
                try {
                    byte[] bytes = Files.readAllBytes(arquivo);
                    sql = new String(bytes, StandardCharsets.UTF_8);
                    checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                } catch (Exception e) { throw new SQLException("Erro ao ler migration " + nome, e); }
                try (PreparedStatement ps = con.prepareStatement("SELECT checksum FROM schema_migrations WHERE versao=?")) {
                    ps.setString(1, nome);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            if (!checksum.equals(rs.getString(1)))
                                throw new SQLException("Migration já aplicada foi alterada: " + nome);
                            continue;
                        }
                    }
                }
                try (Statement st = con.createStatement()) { st.execute(sql); }
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO schema_migrations (versao,checksum) VALUES (?,?)")) {
                    ps.setString(1, nome);
                    ps.setString(2, checksum);
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    public static void instalarDemonstracao() throws SQLException {
        Transacao.executar(con -> {
            try (Statement st = con.createStatement()) { st.execute("SELECT pg_advisory_xact_lock(726281)"); }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO usuarios(nome,login,senha,perfil) VALUES (?,?,?,?) ON CONFLICT(login) DO NOTHING")) {
                String[][] usuarios = {{"Administrador Demo", "admin", "admin123", "admin"},
                        {"Vendedor Demo", "vendedor", "vendedor123", "vendedor"}};
                for (String[] u : usuarios) {
                    ps.setString(1, u[0]); ps.setString(2, u[1]);
                    ps.setString(3, SenhaCripto.hashear(u[2])); ps.setString(4, u[3]); ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO clientes(nome,cpf,email,telefone,endereco,senha) VALUES (?,?,?,?,?,?) "
                    + "ON CONFLICT(email) DO NOTHING")) {
                ps.setString(1, "Cliente Demonstração"); ps.setString(2, "529.982.247-25");
                ps.setString(3, "cliente@demo.local"); ps.setString(4, "(11) 99999-0000");
                ps.setString(5, "Rua de Demonstração, 100"); ps.setString(6, SenhaCripto.hashear("cliente123"));
                ps.executeUpdate();
            }
            String[][] catalogo = {{"Perfumes", "Perfume Demo", "129.90"},
                    {"Maquiagem", "Batom Demo", "39.90"}, {"Cuidados com a Pele", "Hidratante Demo", "59.90"},
                    {"Cabelos", "Shampoo Demo", "29.90"}, {"Corpo e Banho", "Sabonete Demo", "19.90"}};
            for (String[] linha : catalogo) {
                int categoria;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO categorias(nome) VALUES (?) ON CONFLICT(nome) DO UPDATE SET nome=EXCLUDED.nome RETURNING id")) {
                    ps.setString(1, linha[0]);
                    try (ResultSet rs = ps.executeQuery()) { rs.next(); categoria = rs.getInt(1); }
                }
                // Reexecutar a demonstração não repõe estoque nem duplica produtos.
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO produtos(nome,descricao,preco,id_categoria) "
                        + "SELECT ?, 'Produto fictício para apresentação acadêmica', ?, ? "
                        + "WHERE NOT EXISTS(SELECT 1 FROM produtos WHERE nome=? AND id_categoria=?) RETURNING id")) {
                    ps.setString(1, linha[1]); ps.setBigDecimal(2, new java.math.BigDecimal(linha[2]));
                    ps.setInt(3, categoria); ps.setString(4, linha[1]); ps.setInt(5, categoria);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            try (PreparedStatement estoque = con.prepareStatement("INSERT INTO estoque(id_produto,quantidade) VALUES (?,20)")) {
                                estoque.setInt(1, rs.getInt(1)); estoque.executeUpdate();
                            }
                        }
                    }
                }
            }
            return null;
        });
    }
}
