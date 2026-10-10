# Estado atual — 09/10/2026

Etapas 1 a 6 concluídas: partes 1 e 2, compra/pagamento simulado e gestão/cancelamento de pedidos. Desktop Java Swing/PostgreSQL e estilo visual preservados. A versão continua em desenvolvimento, com apresentação prevista para 28/10/2026.

- Cadastros, permissões, usuários/último admin, recuperação/ativação por código, validações/máscaras, categorias, imagens, estoque mínimo e histórico: PARTE_2_CADASTROS_E_ACESSO.md.
- Carrinho com reserva, entrega informada, revalidação de preço/estoque, confirmação idempotente, descontos PIX/débito, pagamento fictício persistido e boleto confirmado por admin: COMPRA_E_PAGAMENTO_SIMULADO.md.
- Filtros de pedidos, venda no balcão separada, estados de entrega independentes do pagamento, histórico, cancelamento autorizado e devolução única: GESTAO_E_CANCELAMENTO_PEDIDOS.md.
- Catálogo de apresentação: 30 produtos, seis por categoria, imagem individual, descrição, preço, saldo e mínimo; produtos anteriores inativos e pedidos preservados. Loja mostra detalhes e imagem maior. Guia: CATALOGO_APRESENTACAO.md; evidência: validacao-catalogo.txt. Dados e prompts ficam em catalogo.
- Perfil privado com senha atual, troca de senha, cupons administrativos, validade e descontos separados no pedido: PERFIL_E_CUPONS.md; evidência validacao-perfil-cupons.txt.
- Avaliações de compras entregues/pagas, média/comentários no catálogo e avisos internos com leitura individual: AVALIACOES_E_NOTIFICACOES.md; evidência validacao-avaliacoes-avisos.txt.
- Como abrir e testar: COMO_TESTAR_PARTES_1_E_2.md e Testar Partes 1 e 2.cmd (inclui também compra/pagamento e gestão/cancelamento).
- Correções solicitadas no teste manual: formulário largo, botões com espaço, datas brasileiras mascaradas, marca sem asterisco, loja reorganizada, janelas únicas e ações de pedidos/carrinho conforme o estado. Guia: AJUSTES_INTERFACE_E_DATAS.md; evidência: validacao-interface.txt.

64 fontes de produção compiladas. Banco isolado: 20 regressões iniciais + 15 cenários de acesso/cadastros + 14 de compra/pagamento + 17 de gestão/cancelamento + 14 de perfil/cupons + 13 de avaliações/avisos (93 cenários), além das verificações de datas/interface. Evolução V1/V3 → V4/V5/V6 preserva dados, pedidos, itens, pagamentos e saldos, com registro explícito de implantação. Construção de 22 telas. Fluxos Swing de checkout e gestão, incluindo duplo clique, filtro, avanço e cancelamento pessoal/da equipe. Evidências: ultimo-teste-integracao.txt, validacao-parte2.txt, validacao-compra.txt, validacao-pedidos.txt e validacao-interface.txt. Capturas em validacao-ui.

A base local foi atualizada preservando dados, com backups anteriores às evoluções. A verificação de reinício/restauração inclui pedido, total, subtotal/desconto, pagamento, imagem, estoque, retirada entregue, cancelamento, devolução, histórico, perfil/senha, cupom/descontos, avaliação e leitura de avisos; resultado em validacao-base.txt. O serviço/banco PostgreSQL antigo é preservado.

Perfil e cupons concluídos na etapa 7. Avaliações e notificações também concluídas. Próximo bloco: dashboard e revisão dos relatórios. Relatórios finais, validação completa, documentação acadêmica e empacotamento continuam pendentes. Os registros históricos do primeiro bloco são mantidos abaixo.
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
