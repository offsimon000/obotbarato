# Escopo e critérios de conclusão

Referência de execução definida a partir dos arquivos do projeto e do plano de finalização. Entrega **desktop Java Swing com PostgreSQL**, mantendo os temas e o estilo das telas atuais. Apresentação: **28/10/2026**. O protótipo web não faz parte da entrega.

## Módulos da entrega

| Módulo | Critério de conclusão | Etapa do plano |
|---|---|---|
| Instalação e execução | Compilar com Java 17, preparar banco separado, autenticar, reiniciar e recuperar backup seguindo as instruções. | 1–3 |
| Acesso | Login único; sessões e permissões de administrador, vendedor e cliente aplicadas nas operações. | 4 |
| Usuários internos | Cadastrar, editar e desativar usuários; preservar ao menos um administrador ativo. | 4 |
| Clientes | Cadastrar, editar e desativar com validação e duplicidade tratada; definir acesso dos clientes cadastrados pelo painel. | 4 |
| Categorias e produtos | Manutenção do catálogo pela interface, categoria válida, imagens locais e tratamento de inativos. | 4 |
| Estoque | Reposição, disponibilidade, mínimo e histórico de movimentações; venda não deixa quantidade negativa. | 3–4 |
| Loja, favoritos e carrinho | Filtrar catálogo, salvar/remover favoritos, ajustar quantidades e revalidar disponibilidade e preço. | 5 |
| Compra e pagamento | Pedido, itens e estoque atômicos; pagamento explicitamente simulado, método/status persistidos, desconto real e confirmação única. | 5 |
| Gestão e histórico de pedidos | Consultar detalhes, pagamento e entrega; transições válidas; histórico por responsável; cancelamento devolve estoque uma vez. | 6 |
| Cupons | Cadastro e validação de percentual, prazo e ativação; desconto aplicado fica registrado no pedido. | 7 |
| Perfil | Cliente atualiza seus dados e troca senha mediante autenticação. | 7 |
| Avaliações | Nota de 1 a 5 e comentário associados a produto comprado e entregue; média exibida no catálogo. | 7 |
| Notificações internas | Avisos de pedido, pagamento e estoque; leitura registrada e duplicações evitadas. | 7 |
| Dashboard e relatórios | Indicadores, gráficos e relatórios correspondem às vendas válidas, cancelamentos e estoque; exportação definida. | 7–8 |
| Qualidade | Fluxos completos, falhas e concorrência testados; telas responsivas e navegação coerente. | 9 |
| Documentação e apresentação | Requisitos, regras, casos de uso, classes, banco, sequências, capturas e instruções correspondem à implementação final. | 10 |
| Pacote final | Aplicativo, código, dependências, SQL, documentos e demonstração executados fora da pasta de desenvolvimento e ensaiados. | 11 |

Pagamento e entrega são simulações acadêmicas. Notificações ficam dentro do aplicativo. A apresentação precisa demonstrar as regras efetivamente implementadas, incluindo seus limites.

## Fechamento do primeiro bloco

O primeiro bloco corresponde às etapas 1, 2 e 3 do plano. Seu aceite exige:

1. Pasta oficial organizada, controle de versão e entrada única preservando o estilo visual.
2. Compilação limpa, sem caminhos da antiga pasta Downloads; JAR com driver JDBC relativo.
3. Configuração externa e base local persistente, separada do PostgreSQL antigo.
4. SQL correspondente ao código, migrations versionadas, demonstração compatível e reexecução sem duplicar dados.
5. Pedido, itens e estoque transacionais; produto e estoque inicial transacionais; rollback também em falhas de regra.
6. Quantidades inválidas rejeitadas, concorrência de estoque protegida, valores monetários decimais e relatório com data tipada.
7. Falhas de leitura/gravação propagadas; interface não confunde banco indisponível com senha incorreta nem trata gravação falha como sucesso.
8. Reinício conserva dados; backup é restaurado em banco novo e os registros restaurados são conferidos.
9. Inicialização das telas atuais sem erro e evidências reproduzíveis das verificações.

O aceite desse bloco não conclui os módulos seguintes. A matriz de permissões, validações completas de cadastro, checkout, gestão de entrega/cancelamento e módulos adicionais continuam nas etapas indicadas acima.
