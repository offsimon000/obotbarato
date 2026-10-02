# Estado atual — 02/10/2026

Partes 1 e 2 concluídas; etapa 5 de compra/pagamento simulado também concluída. Desktop Java Swing/PostgreSQL e estilo visual preservados. A versão continua em desenvolvimento, com apresentação prevista para 28/10/2026.

- Cadastros, permissões, usuários/último admin, recuperação/ativação por código, validações/máscaras, categorias, imagens, estoque mínimo e histórico: PARTE_2_CADASTROS_E_ACESSO.md.
- Carrinho com reserva, entrega informada, revalidação de preço/estoque, confirmação idempotente, descontos PIX/débito, pagamento fictício persistido e boleto confirmado por admin: COMPRA_E_PAGAMENTO_SIMULADO.md.
- Como abrir e testar: COMO_TESTAR_PARTES_1_E_2.md e Testar Partes 1 e 2.cmd (agora inclui também compra/pagamento).

51 fontes de produção compiladas. Banco isolado: 20 regressões iniciais + 15 cenários de acesso/cadastros + 14 de compra/pagamento; evolução V1/V2/V3 preservando dados, pedido e itens; construção de 16 telas. Fluxo Swing do checkout aprovado, incluindo duplo clique, fechamento e continuidade. Evidências: ultimo-teste-integracao.txt, validacao-parte2.txt e validacao-compra.txt. Capturas dos cadastros e checkout foram inspecionadas em validacao-ui.

A base local foi atualizada preservando dados, com backups anteriores às evoluções. A verificação de reinício/restauração inclui pedido, total, subtotal/desconto, pagamento, imagem, estoque e auditoria; resultado em validacao-base.txt. O serviço/banco PostgreSQL antigo é preservado.

Próximo bloco: etapa 6, gestão/transições/histórico/cancelamento de pedidos. Perfil, cupons, avaliações, notificações, dashboard, relatórios finais, documentação acadêmica e empacotamento continuam pendentes. Os registros históricos do primeiro bloco são mantidos abaixo.
---
# Registro do primeiro bloco concluído — 30/09/2026

Entrega confirmada: desktop Swing, estilo visual atual, apresentação em 28/10/2026 e documentação alinhada ao código. Este registro descreve o fechamento das etapas 1 a 3, sem antecipar o aceite dos módulos seguintes.

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
- Entrada oficial única e aliases dos nomes de login antigos.
- Erros públicos de consulta, autenticação e gravação propagados; mensagens da interface distinguem banco indisponível, duplicidade e dados inválidos.
- Formulários verificam os retornos de gravação/alteração; clientes recebem ID gerado e campos opcionais vazios são normalizados.
- Lançador `Iniciar OBotBarato.cmd`, base local persistente com início/parada, backup e restauração em banco novo.
- Escopo e critérios de entrega registrados em `ESCOPO_E_CRITERIOS.md`; estrutura e regras atuais em `CONTRATO_BASE.md`.

## Validação

38 fontes de produção compiladas com `javac --release 17`. JAR gerado com a entrada `view.TelaLoginUnificado` e dependência JDBC relativa. Compilação e roteiro de execução também verificados pelo Windows PowerShell 5.1, sem exigir instalação de PowerShell 7.

20 cenários de integração aprovados no PostgreSQL 18.6. Evidência: `ultimo-teste-integracao.txt`; teste reproduzível: `../test/IntegracaoBanco.java`. Além dos testes iniciais de pedido, estoque e dinheiro, foram conferidos CRUD/duplicidade de clientes, alterações em registros inexistentes, favoritos, rollback por falha de regra, migrations alteradas/falhas/evoluções, falha de conexão nos DAOs e aliases de login.

Roteiro `../scripts/verificar-base.ps1` aprovado: instalação, autenticação das três contas, reinício da base, restauração em banco novo e preservação de pedido/itens/total R$ 34,50/estoque/auditoria. O roteiro removeu apenas seus próprios registros fictícios após a validação. Evidência: `validacao-base.txt`.

As 13 telas existentes foram construídas no EDT sem mostrar janelas. Login e catálogo foram renderizados e inspecionados em `validacao-ui`; o estilo foi preservado. A verificação não cobre todas as interações dos módulos, que serão validadas nas próximas etapas.

Os servidores temporários de teste escutam apenas em loopback e são parados pelo script. A base persistente de desenvolvimento continua disponível para executar o desktop; pode ser encerrada por `parar-banco.ps1`. O serviço PostgreSQL original manteve seu estado e sua base não foi alterada.

## Próximo bloco

Concluir cadastros, permissões e catálogo; depois checkout, pagamento simulado e gestão de pedidos. Os demais módulos, a revisão visual das telas, a migração da base legada e a entrega acadêmica final ainda não estão concluídos. O plano completo permanece em `../../analise/PLANO_FINALIZACAO_OBOTBARATO.md`.

Driver obtido de https://jdbc.postgresql.org/download/; SHA-256: `6E0E4CC2D8CAE902084F8A2B18728B073A6FD9D1F87C9D8BFF8F298C18185B93`. Licenças do driver e dependências estão incluídas no JAR em `META-INF`.
