import dao.*;
import model.*;
import util.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.concurrent.*;

/** Regressões em PostgreSQL real, executadas só pelo cluster isolado de teste. */
public final class IntegracaoBanco {
    private static final EstoqueDAO estoque = new EstoqueDAO();
    private static final PedidoDAO pedidos = new PedidoDAO();
    private static int cliente;
    private static int categoria;
    private static int cenarios;

    public static void main(String[] args) throws Exception {
        exigir("obotbarato_test".equals(texto("SELECT current_database()")), "Use apenas a base de teste isolada.");
        InicializarBanco.aplicarMigrations(Path.of("sql"));
        InicializarBanco.instalarDemonstracao();
        long produtosDemo = numero("SELECT COUNT(*) FROM produtos");
        exigir(numero("SELECT COUNT(*) FROM movimentos_estoque WHERE tipo='inicial'")==produtosDemo,"Abertura da demonstração ausente.");
        InicializarBanco.aplicarMigrations(Path.of("sql"));
        InicializarBanco.instalarDemonstracao();
        exigir(numero("SELECT COUNT(*) FROM schema_migrations") == 6, "Migration duplicada.");
        exigir(numero("SELECT COUNT(*) FROM produtos") == produtosDemo, "Seed duplicou produtos.");
        exigir(numero("SELECT COUNT(*) FROM movimentos_estoque WHERE tipo='inicial'")==produtosDemo,"Seed duplicou abertura.");
        exigir(numero("SELECT COUNT(*) FROM usuarios") == 2, "Seed duplicou usuários.");
        ok("Instalação e demonstração reexecutadas sem duplicação");

        exigir(new UsuarioDAO().autenticar("admin", "admin123") != null, "Admin não autentica.");
        exigir(new UsuarioDAO().autenticar("admin", "errada") == null, "Senha errada aceita.");
        Cliente c = new ClienteLojaDAO().autenticar("cliente@demo.local", "cliente123");
        exigir(c != null, "Cliente não autentica.");
        Sessao.iniciar(new UsuarioDAO().autenticar("admin","admin123"));
        cliente = c.getId();
        categoria = (int) numero("SELECT MIN(id) FROM categorias");
        ok("Autenticação das credenciais de demonstração");

        try (Connection a = ConexaoBanco.getConexao(); Connection b = ConexaoBanco.getConexao()) {
            exigir(a != b, "Conexão compartilhada entre operações.");
            a.close();
            exigir(b.isValid(2), "Fechar uma conexão invalidou outra.");
        }
        ok("Conexões independentes");

        Produto p = produto("Produto Teste", "0.10", 10);
        Pedido um = pedido(p, 3);
        pedidos.salvar(um);
        exigir(um.getId() > 0 && estoque.getQuantidade(p.getId()) == 7, "Venda não persistiu corretamente.");
        exigir("0.30".equals(texto("SELECT total::text FROM pedidos WHERE id=" + um.getId())), "Total decimal incorreto.");
        exigir(numero("SELECT COUNT(*) FROM itens_pedido WHERE id_pedido=" + um.getId()) == 1, "Item ausente.");
        rejeitar(() -> pedidos.salvar(um), IllegalArgumentException.class);
        ok("Compra de um item, total decimal e prevenção de regravação do mesmo objeto");

        Produto q = produto("Outro Produto Teste", "0.20", 2);
        Pedido varios = pedido(p, 1);
        varios.adicionarItem(item(q, 1));
        pedidos.salvar(varios);
        exigir(varios.getTotalDecimal().equals(new BigDecimal("0.30")), "Soma decimal incorreta.");
        exigir(estoque.getQuantidade(p.getId()) == 6 && estoque.getQuantidade(q.getId()) == 1, "Baixa de vários itens incorreta.");
        ok("Compra de vários itens na mesma transação");

        long antesPedidos = numero("SELECT COUNT(*) FROM pedidos");
        long antesItens = numero("SELECT COUNT(*) FROM itens_pedido");
        long antesMovimentos = numero("SELECT COUNT(*) FROM movimentos_estoque");
        Pedido falta = pedido(p, 1);
        falta.adicionarItem(item(q, 2));
        rejeitar(() -> pedidos.salvar(falta), SQLException.class);
        exigir(falta.getId() == 0, "Pedido falho ficou com ID.");
        exigir(numero("SELECT COUNT(*) FROM pedidos") == antesPedidos, "Pedido parcial persistido.");
        exigir(numero("SELECT COUNT(*) FROM itens_pedido") == antesItens, "Itens parciais persistidos.");
        exigir(numero("SELECT COUNT(*) FROM movimentos_estoque") == antesMovimentos,"Movimento de venda parcial persistido.");
        exigir(estoque.getQuantidade(p.getId()) == 6 && estoque.getQuantidade(q.getId()) == 1, "Rollback não devolveu estoque.");
        ok("Falta de estoque no segundo item desfaz pedido, itens e primeira baixa");

        rejeitar(() -> estoque.entrada(p.getId(), -1), IllegalArgumentException.class);
        rejeitar(() -> estoque.saida(p.getId(), 0), IllegalArgumentException.class);
        rejeitar(() -> estoque.atualizar(p.getId(), -1), IllegalArgumentException.class);
        rejeitar(() -> new ItemPedido(p.getId(), p.getNome(), -1, p.getPreco()), IllegalArgumentException.class);
        rejeitar(() -> pedidos.salvar(new Pedido()), IllegalArgumentException.class);
        exigir(estoque.getQuantidade(p.getId()) == 6, "Quantidade inválida mudou estoque.");
        ok("Quantidades inválidas e pedido vazio rejeitados");

        executar("CREATE FUNCTION teste_rejeitar_estoque() RETURNS trigger LANGUAGE plpgsql AS $$ "
                + "BEGIN RAISE EXCEPTION 'falha proposital de estoque inicial'; END $$; "
                + "CREATE TRIGGER teste_estoque BEFORE INSERT ON estoque FOR EACH ROW EXECUTE FUNCTION teste_rejeitar_estoque()");
        Produto falho = modelo("Produto não deve persistir", "1.00");
        long antesProdutos = numero("SELECT COUNT(*) FROM produtos");
        try {
            rejeitar(() -> new ProdutoDAO().salvar(falho), SQLException.class);
            exigir(falho.getId() == 0 && numero("SELECT COUNT(*) FROM produtos") == antesProdutos, "Produto órfão persistido.");
        } finally { executar("DROP TRIGGER teste_estoque ON estoque; DROP FUNCTION teste_rejeitar_estoque()"); }
        ok("Falha ao criar estoque inicial desfaz cadastro do produto");

        Produto ultimo = produto("Última unidade", "9.90", 1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        long antesCorrida = numero("SELECT COUNT(*) FROM pedidos");
        try {
            Callable<Boolean> compra = () -> {
                iniciar.await();
                try { pedidos.salvar(pedido(ultimo, 1)); return true; }
                catch (SQLException e) {
                    if (!e.getMessage().contains("Estoque insuficiente")) throw e;
                    return false;
                }
            };
            Future<Boolean> a = executor.submit(compra), b = executor.submit(compra);
            iniciar.countDown();
            exigir(a.get(15, TimeUnit.SECONDS) ^ b.get(15, TimeUnit.SECONDS), "A última unidade foi vendida duas vezes.");
            exigir(estoque.getQuantidade(ultimo.getId()) == 0, "Estoque concorrente incorreto.");
            exigir(numero("SELECT COUNT(*) FROM pedidos") == antesCorrida + 1, "Pedido concorrente parcial.");
        } finally { executor.shutdownNow(); }
        ok("Duas compras simultâneas: apenas uma compra a última unidade");

        String dia = texto("SELECT CURRENT_DATE::text");
        var relatorio = pedidos.relatorioVendasPorPeriodo(dia, dia);
        exigir(relatorio.size() == 1 && relatorio.get(0)[1].equals("3"), "Relatório por data incorreto.");
        BigDecimal soma = new BigDecimal(relatorio.get(0)[2].replace(',', '.'));
        exigir(soma.equals(new BigDecimal("10.50")), "Receita por período incorreta.");
        rejeitar(() -> pedidos.relatorioVendasPorPeriodo("2026-10-28", "2026-09-30"), IllegalArgumentException.class);
        ok("Relatório PostgreSQL com datas tipadas e totais conhecidos");

        exigir(pedidos.listarPorCliente(cliente).size() == 3, "Histórico do cliente incorreto.");
        exigir(pedidos.listarPorCliente(Integer.MAX_VALUE).isEmpty(), "Histórico vazou pedidos de outro cliente.");
        ok("Consulta de histórico filtrada por cliente no banco");

        Cliente novo = new Cliente(); novo.setNome("Cliente Teste CRUD"); novo.setEmail("crud@teste.local");
        novo.setCpf(""); novo.setTelefone(""); novo.setEndereco("");
        ClienteDAO clientes = new ClienteDAO();
        exigir(clientes.salvar(novo) && novo.getId() > 0, "Cliente sem ID gerado.");
        exigir(texto("SELECT cpf IS NULL FROM clientes WHERE id=" + novo.getId()).equals("t"), "CPF vazio não normalizado.");
        novo.setNome("Cliente Alterado"); exigir(clientes.atualizar(novo), "Cliente não atualizado.");
        novo.setEmail("cliente@demo.local");
        rejeitar(() -> clientes.atualizar(novo), SQLException.class);
        exigir(texto("SELECT email FROM clientes WHERE id=" + novo.getId()).equals("crud@teste.local"), "Atualização duplicada alterou o cadastro.");
        exigir(clientes.excluir(novo.getId()), "Cliente não desativado.");
        exigir(clientes.buscarPorNome("Cliente Alterado").isEmpty(), "Cliente inativo na lista.");
        ok("Cadastro, atualização, duplicidade e desativação de cliente sem sucesso falso");

        Produto inexistente = modelo("Inexistente", "1.00"); inexistente.setId(Integer.MAX_VALUE);
        exigir(!new ProdutoDAO().atualizar(inexistente) && !new ProdutoDAO().excluir(Integer.MAX_VALUE), "Produto inexistente retornou sucesso.");
        novo.setId(Integer.MAX_VALUE); novo.setEmail("naoexiste@teste.local");
        exigir(!clientes.atualizar(novo) && !clientes.excluir(novo.getId()), "Cliente inexistente retornou sucesso.");
        rejeitar(() -> estoque.entrada(Integer.MAX_VALUE, 1), SQLException.class);
        rejeitar(() -> estoque.atualizar(Integer.MAX_VALUE, 1), SQLException.class);
        ok("Alterações em registros inexistentes não retornam sucesso");

        ClienteLojaDAO loja = new ClienteLojaDAO();
        Sessao.iniciarCliente(c);
        exigir(loja.adicionarFavorito(cliente, p.getId()), "Favorito não inserido.");
        exigir(loja.adicionarFavorito(cliente, p.getId()), "Favorito repetido não idempotente.");
        exigir(loja.listarFavoritosSet(cliente).contains(p.getId()), "Favorito ausente.");
        exigir(numero("SELECT COUNT(*) FROM favoritos WHERE id_cliente=" + cliente + " AND id_produto=" + p.getId()) == 1, "Favorito duplicado.");
        exigir(loja.removerFavorito(cliente, p.getId()) && !loja.listarFavoritosSet(cliente).contains(p.getId()), "Favorito não removido.");
        ok("Favoritos persistidos sem duplicação");
        Sessao.iniciar(new UsuarioDAO().autenticar("admin","admin123"));

        long categoriasAntes = numero("SELECT COUNT(*) FROM categorias");
        rejeitar(() -> Transacao.executar(con -> {
            try (Statement st = con.createStatement()) { st.execute("INSERT INTO categorias(nome) VALUES ('Rollback de runtime')"); }
            throw new IllegalArgumentException("Falha proposital após a gravação");
        }), IllegalArgumentException.class);
        exigir(numero("SELECT COUNT(*) FROM categorias") == categoriasAntes, "Falha de runtime não desfez gravação.");
        ok("Rollback também em falha de regra depois de executar SQL");

        Files.createDirectories(Path.of(".local"));
        Path migrations = Files.createTempDirectory(Path.of(".local"), "migrations-teste-");
        Path base = migrations.resolve("V001__base_desktop.sql");
        Files.copy(Path.of("sql/V001__base_desktop.sql"), base);
        Files.writeString(base, Files.readString(base) + "\n-- alteração indevida", StandardCharsets.UTF_8);
        rejeitar(() -> InicializarBanco.aplicarMigrations(migrations), SQLException.class);
        exigir(numero("SELECT COUNT(*) FROM schema_migrations") == 6, "Migration alterada foi aceita.");
        ok("Checksum impede alteração de migration já aplicada");

        Files.write(base, Files.readAllBytes(Path.of("sql/V001__base_desktop.sql")));
        Path segunda = migrations.resolve("V007__teste_evolucao.sql");
        Files.writeString(segunda, "CREATE TABLE teste_evolucao(id INT); INSERT INTO categorias(nome) VALUES(NULL);", StandardCharsets.UTF_8);
        rejeitar(() -> InicializarBanco.aplicarMigrations(migrations), SQLException.class);
        exigir(texto("SELECT to_regclass('public.teste_evolucao') IS NULL").equals("t"), "Migration falha deixou tabela parcial.");
        exigir(numero("SELECT COUNT(*) FROM schema_migrations") == 6, "Migration falha registrada.");
        Files.writeString(segunda, "CREATE TABLE teste_evolucao(id INT);", StandardCharsets.UTF_8);
        InicializarBanco.aplicarMigrations(migrations);
        InicializarBanco.aplicarMigrations(migrations);
        exigir(numero("SELECT COUNT(*) FROM schema_migrations") == 7, "Evolução não aplicada/reexecutada corretamente.");
        ok("Migration falha é desfeita; evolução válida pode ser aplicada e reexecutada");

        String configOriginal = System.getProperty("obotbarato.config");
        Path invalida = Files.createTempFile(Path.of(".local"), "banco-invalido-", ".properties");
        Files.writeString(invalida, "url=jdbc:postgresql://127.0.0.1:1/indisponivel\nusuario=teste\nsenha=\n", StandardCharsets.UTF_8);
        try {
            System.setProperty("obotbarato.config", invalida.toAbsolutePath().toString());
            rejeitar(() -> new UsuarioDAO().autenticar("admin", "admin123"), SQLException.class);
            rejeitar(() -> loja.autenticar("cliente@demo.local", "cliente123"), SQLException.class);
            rejeitar(() -> clientes.listarTodos(), SQLException.class);
            rejeitar(() -> new ProdutoDAO().listarTodos(), SQLException.class);
            rejeitar(() -> loja.listarFavoritosSet(cliente), SQLException.class);
            rejeitar(() -> pedidos.listarTodos(), SQLException.class);
        } finally {
            if (configOriginal == null) System.clearProperty("obotbarato.config");
            else System.setProperty("obotbarato.config", configOriginal);
        }
        exigir(new UsuarioDAO().autenticar("admin", "admin123") != null, "Conexão não recuperou após restaurar configuração.");
        ok("Falha de conexão propaga erro em todos os DAOs e permite recuperação");

        rejeitar(() -> modelo("Inválido", "-0.01"), IllegalArgumentException.class);
        rejeitar(() -> new Produto().setPreco(Double.NaN), IllegalArgumentException.class);
        rejeitar(() -> new Produto().setPreco(Double.POSITIVE_INFINITY), IllegalArgumentException.class);
        exigir(modelo("Arredondamento", "1.005").getPrecoDecimal().equals(new BigDecimal("1.01")), "Arredondamento incorreto.");
        ok("Moeda rejeita valores inválidos e arredonda meio centavo de forma definida");

        exigir(view.TelaLogin.class.getSuperclass() == view.TelaLoginUnificado.class, "Login antigo não encaminha à entrada oficial.");
        exigir(view.loja.TelaLojaLogin.class.getSuperclass() == view.TelaLoginUnificado.class, "Login antigo da loja não encaminha à entrada oficial.");
        ok("Entradas legadas encaminham ao login oficial");
        System.out.println("APROVADO: " + cenarios + " cenários de integração em PostgreSQL real.");
    }

    private static Produto modelo(String nome, String preco) {
        Produto p = new Produto(); p.setNome(nome); p.setPreco(new BigDecimal(preco));
        p.setDescricao("Teste de integração"); p.setIdCategoria(categoria); p.setAtivo(true); return p;
    }
    private static Produto produto(String nome, String preco, int qtd) throws Exception {
        Produto p = modelo(nome, preco); exigir(new ProdutoDAO().salvar(p), "Produto não salvo.");
        estoque.atualizar(p.getId(), qtd); return p;
    }
    private static ItemPedido item(Produto p, int qtd) {
        ItemPedido i = new ItemPedido(p.getId(), p.getNome(), qtd, p.getPreco());
        i.setPrecoUnitario(p.getPrecoDecimal()); return i;
    }
    private static Pedido pedido(Produto p, int qtd) {
        Pedido pedido = new Pedido(); pedido.setIdCliente(cliente); pedido.setStatus("finalizado");
        pedido.adicionarItem(item(p, qtd)); return pedido;
    }
    private static String texto(String sql) throws SQLException {
        try (Connection con = ConexaoBanco.getConexao(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            exigir(rs.next(), "Consulta sem resultado."); return rs.getString(1);
        }
    }
    private static long numero(String sql) throws SQLException { return Long.parseLong(texto(sql)); }
    private static void executar(String sql) throws SQLException {
        try (Connection con = ConexaoBanco.getConexao(); Statement st = con.createStatement()) { st.execute(sql); }
    }
    private interface Acao { void executar() throws Exception; }
    private static void rejeitar(Acao acao, Class<? extends Exception> tipo) throws Exception {
        try { acao.executar(); }
        catch (Exception e) { if (tipo.isInstance(e)) return; throw e; }
        throw new AssertionError("Operação inválida foi aceita: " + tipo.getSimpleName());
    }
    private static void exigir(boolean condicao, String mensagem) { if (!condicao) throw new AssertionError(mensagem); }
    private static void ok(String mensagem) { cenarios++; System.out.println("OK " + cenarios + ": " + mensagem); }
}
