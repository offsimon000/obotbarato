# Revisão e apresentação

Entrega desktop para 28/10/2026. Revise a identificação dos autores, o texto acadêmico, os diagramas e a sequência abaixo. As simulações de pagamento e entrega devem ser apresentadas como simulações. O movimento existente é fictício, sem clientes reais.

## Abrir e revisar

1. Abra OBotBarato.code-workspace no VS Code e use F5. Feche todas as janelas da versão anterior antes de atualizar.
2. Leia docs/final/Documentacao_OBotBarato.pdf; o Word correspondente permite editar. Confira instituição, autores, orientação e eventuais exigências de formatação do professor.
3. Confira MATRIZ_RASTREABILIDADE.md e DICIONARIO_BANCO.md para localizar a implementação de cada requisito. Diagramas PNG e fontes PlantUML estão em diagramas.
4. Ensaie com os slides de docs/final e o roteiro abaixo. Há PPTX editável e PDF para apresentar sem PowerPoint. Faça backup antes de mudar dados para a demonstração.
5. Abra uma cópia extraída do ZIP em entrega se quiser conferir a reprodução do projeto. Essa cópia cria seu próprio banco e exige JDK/PostgreSQL instalados. Execute a tarefa carregar demonstração apenas em uma cópia nova.

## Roteiro de demonstração de 10 a 15 minutos

| Tempo | Ação | O que explicar |
|---|---|---|
| 1 min | Introdução e login | Problema de integrar catálogo, vendas e estoque; três perfis; aplicação desktop |
| 2 min | Cliente: loja e busca `oleo`, `agua micelar`, `argan oleo` | Busca flexível; filtros; detalhes/imagens; produtos fictícios |
| 2 min | Favoritar, adicionar um item com saldo, revisar carrinho/entrega | Quantidade e preço revalidados; dados de entrega obrigatórios |
| 2 min | Cupom APRESENTA10 e PIX; confirmar uma vez | Cupom aplicado primeiro; PIX 10% no restante; pagamento simulado. Exemplo: R$100 → R$90 → R$81 |
| 1 min | Meus Pedidos e detalhes | Pedido novo aberto, pagamento confirmado e dados da compra preservados |
| 2 min | Sair; vendedor: Pedidos, avançar pedido até entregue | Uma etapa por vez; pagamento separado de entrega; histórico de responsável |
| 1 min | Cliente: avaliar produto daquele pedido e consultar avisos | Apenas compra própria entregue/paga; leitura individual |
| 2 min | Admin: dashboard/relatórios e CSV; estoque | Faturamento líquido confirmado não cancelado; ranking bruto; estoque crítico atual |
| 1 min | Nova venda no balcão: selecionar cliente/produto/quantidade | Retirada imediata, entregue e dinheiro confirmado simulado; baixa de estoque |

Se faltar tempo, use um pedido já entregue da simulação para mostrar avaliação, e um aberto para cancelamento. Não cancele o pedido que pretende entregar/avaliar. Anote o número da nova compra: os IDs não são necessariamente sequenciais sem lacunas. Se o cupom tiver sido alterado/expirado no ensaio, confira sua validade no painel antes de demonstrar.

Contas: admin/admin123; vendedor/vendedor123; cliente@demo.local/cliente123. Os 20 clientes `cliente01@uso.obotbarato.test` a `cliente20@uso.obotbarato.test` usam `demo12345`. Se você trocar senhas, use as novas; a carga não redefine acessos.

## Respostas que precisam estar claras

- **É uma loja online pública?** A loja é uma interface Swing desktop. Não há servidor web ou site publicado.
- **O pagamento acontece de verdade?** Não. São registros e descontos de uma simulação acadêmica. Boleto fica pendente até admin confirmar.
- **Como impede vender sem saldo?** Baixa condicionada e bloqueios na mesma transação de pedido/itens/pagamento; erro desfaz o conjunto. Há teste de duas compras da última unidade.
- **E dois cliques em confirmar?** Carrinho reservado, comandos bloqueados durante gravação e UUID único no banco; repetir retorna a compra original.
- **Cancelamento muda o faturamento?** Sim: pedido/pagamento cancelados saem da receita. Valores históricos são preservados e estoque retorna uma vez.
- **Por que o ranking difere do faturamento?** Ranking soma preço bruto dos itens; faturamento usa total líquido após descontos. Isso é identificado nas telas e documentação.
- **Como as senhas são armazenadas?** PBKDF2 com salt; hashes antigos são migrados ao autenticar. Código temporário é entregue manualmente pelo admin.
- **Como recuperar dados?** Backup PostgreSQL e restauração em banco novo. Configuração e senhas do banco não são publicadas.

## Preparar o GitHub depois de revisar

O repositório correto é a pasta **OBotBarato**, não a pasta Extenção que contém as extrações antigas. README, fontes, SQL, driver, catálogo, .vscode, testes e documentos compõem a entrega. `.gitignore` protege configurações reais, clusters, backups e builds. O ZIP contém a distribuição compilada, mas não deve ser adicionado ao Git como parte do código.

No terminal integrado, confira `git status` e `git diff --check`. Crie no GitHub um repositório vazio. Se não houver remote, execute `git remote add origin URL_DO_SEU_REPOSITORIO`; se já houver, confira `git remote -v` e use o correto. Depois da revisão, envie sua branch com `git push -u origin codex/finalizacao`. Se quiser chamar a branch principal de main, pode renomeá-la com `git branch -m main` antes do primeiro envio e fazer `git push -u origin main`. Não envie `.local`, `config/banco.properties`, backups ou a pasta extraidos. O envio ao GitHub fica sob sua revisão; nenhum push é executado na preparação desta entrega.
