# Revisão e apresentação

Entrega desktop para 28/10/2026. Revise a identificação dos autores, o texto acadêmico, os diagramas e a sequência abaixo. As simulações de pagamento e entrega devem ser apresentadas como simulações. O movimento existente é fictício, sem clientes reais.

## Abrir e revisar

1. Dê dois cliques em **Executar OBotBarato** na Área de Trabalho. Para editar/depurar, a alternativa é abrir OBotBarato.code-workspace no VS Code e usar F5. Feche todas as janelas da versão anterior antes de atualizar.
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

## Compartilhamento no GitHub

O projeto é público em https://github.com/offsimon000/obotbarato. Sua dupla pode acessar sem convite. Para usar o pacote compilado com os documentos, baixe o ZIP em Releases, seção Assets, e extraia a pasta inteira. Para desenvolver, use Code → Download ZIP ou clone. JDK 17+ e PostgreSQL são necessários; scripts/criar-atalho.ps1 cria o atalho nessa máquina. A demonstração é carregada manualmente; o banco local dos autores não é publicado.

## Enviar alterações futuras

O repositório correto é a pasta **OBotBarato**, não a pasta Extenção que contém as extrações antigas. README, fontes, SQL, driver, catálogo, .vscode, testes e documentos compõem a entrega. `.gitignore` protege configurações reais, clusters, backups e builds. O ZIP contém a distribuição compilada e é distribuído como anexo da release, sem ser adicionado ao histórico do código.

No terminal, confira `git status`, `git diff --check` e `git remote -v`. Nesta pasta, origin aponta para o repositório acima. Faça commit das alterações revisadas e envie a branch de trabalho com `git push`; para atualizar main a partir de outra branch, revise e faça merge da alteração. Nunca envie `.local`, `config/banco.properties`, backups ou a pasta extraidos. Atualize também o pacote da release quando a distribuição mudar.
