# Registro da primeira base corrigida — 30/09/2026

Entrega confirmada: desktop Swing, estilo visual atual, apresentação em 28/10/2026 e documentação alinhada ao código. Este registro descreve apenas o primeiro bloco de implementação.

## Alterações

- Pasta oficial independente das extrações, mantendo as classes e os temas existentes.
- Java 17 e driver PostgreSQL local; removidos caminhos pessoais da pasta Downloads.
- Configuração externa em UTF-8, com exemplo e precedência das variáveis de ambiente.
- Uma conexão independente por operação. Operações compostas compartilham explicitamente a conexão da transação.
- Pedido, itens e baixa de estoque confirmados juntos. Falha desfaz a transação e não deixa ID no objeto.
- Produto e estoque inicial gravados na mesma transação. Falhas chegam ao formulário e ele só apresenta sucesso depois da gravação.
- Quantidades inválidas rejeitadas e baixa condicionada à disponibilidade, com atualização atômica no PostgreSQL.
- Produto, item e pedido usam `BigDecimal`; métodos antigos de leitura em `double` foram mantidos para compatibilidade com as telas existentes. A revisão completa de moeda na interface continua na etapa de compra.
- Relatório usa parâmetros SQL de data e rejeita intervalo invertido.
- Histórico de cliente consulta os pedidos por ID do cliente no banco.
- Migration inicial versionada com checksum, campos de auditoria e demonstração instalada separadamente, usando senhas compatíveis com o código.
- Scripts de compilação, execução e testes em cluster PostgreSQL isolado.

## Validação

36 fontes de produção compiladas com `javac --release 17`. JAR gerado com a entrada `view.TelaLoginUnificado` e dependência JDBC relativa.

11 cenários de integração aprovados no PostgreSQL 18.6. A evidência está em `ultimo-teste-integracao.txt` e o teste reproduzível em `../test/IntegracaoBanco.java`. Foram verificados rollback por falta de estoque no segundo item, falha proposital ao criar estoque do produto, concorrência na última unidade, total de venda conhecido, autenticação e instalação repetida, além dos outros casos descritos no README.

Os servidores temporários pertencem ao projeto, escutam apenas em loopback e são parados pelo script. O serviço PostgreSQL original não foi iniciado nem teve sua base alterada.

## Próximo bloco

Concluir cadastros, permissões e catálogo; depois checkout, pagamento simulado e gestão de pedidos. Os demais módulos, a revisão visual das telas, a migração da base legada e a entrega acadêmica final ainda não estão concluídos. O plano completo permanece em `../../analise/PLANO_FINALIZACAO_OBOTBARATO.md`.

Driver obtido de https://jdbc.postgresql.org/download/; SHA-256: `6E0E4CC2D8CAE902084F8A2B18728B073A6FD9D1F87C9D8BFF8F298C18185B93`. Licenças do driver e dependências estão incluídas no JAR em `META-INF`.
