# Busca de produtos

Na loja e na manutenção, a pesquisa usa partes do nome, sem distinguir acentos ou maiúsculas. Espaços extras são ignorados; as palavras podem aparecer em qualquer ordem. Por exemplo, `oleo`, `ÓLEO`, `argan oleo` e `óleo argan` encontram Óleo para Pontas Argan 60 ml. `agua micelar` encontra Água Micelar Essencial 200 ml. Todos os termos precisam ocorrer no nome; não há correção automática de erros de digitação, sinônimos ou busca pela descrição.

O filtro de categoria, preço máximo e situação continua aplicado. A loja lista somente produtos de categorias ativas; a manutenção permite consultar inativos conforme a permissão. `%` e `_` são caracteres literais e não ampliam a pesquisa.

A implementação é compartilhada por `util.BuscaTexto` e pelas três consultas de `ProdutoDAO`. Os termos são parâmetros JDBC, com normalização Unicode NFD no Java e PostgreSQL; não requer extensão nem migration nova. O banco deve usar UTF-8. Referência: [funções de texto do PostgreSQL](https://www.postgresql.org/docs/18/functions-string.html).

Para conferir, feche todas as janelas antigas, execute pelo F5 no VS Code, entre como cliente e pesquise `oleo` e `argan oleo`. A tarefa **OBotBarato: testar busca** verifica oito cenários em banco separado, incluindo acentos, fragmentos, Unicode, permissões, SQL preparado e o filtro Swing. Evidência: `validacao-busca.txt`.
