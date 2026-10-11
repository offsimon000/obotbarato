# Matriz de rastreabilidade

Escopo da versão desktop de 11/10/2026. Cada requisito representa um comportamento acessível pela aplicação. Utilitários de migração, carga fictícia, backup e teste apoiam a execução e estão descritos no README; não são integrações comerciais. O documento acadêmico consolida estes requisitos e as regras dos guias. Os diagramas são estruturais: o dicionário e as migrations fornecem todos os campos e restrições.

| ID | Funcionalidade e critério | Tela ou entrada | Implementação principal | Banco | Evidência reproduzível |
|---|---|---|---|---|---|
| RF01 | Login, sessão ativa, perfil e saída | TelaLoginUnificado; aliases TelaLogin/TelaLojaLogin | UsuarioDAO, ClienteLojaDAO, Sessao, Acesso, SenhaCripto | usuarios, clientes | CadastrosEAcesso; InicializacaoTelas |
| RF02 | Autocadastro com obrigatório/opcional e máscaras | TelaLoginUnificado | ClienteLojaDAO, Validacao, MascaraCampo | clientes | CadastrosEAcesso; InterfaceBrasileira; cadastro-campos.png |
| RF03 | Ativação/recuperação por código do admin | TelaClientes; TelaLoginUnificado | CodigoAcessoDAO | clientes, acessos_cliente | CadastrosEAcesso |
| RF04 | Manter usuários preservando último admin | TelaUsuarios | UsuarioDAO, Acesso | usuarios | CadastrosEAcesso; EvolucaoDaBase |
| RF05 | Manter clientes, ativar/desativar conforme perfil | TelaClientes | ClienteDAO, Validacao | clientes | CadastrosEAcesso; IntegracaoBanco |
| RF06 | Manter categorias; impedir desativação em uso | TelaCategorias | CategoriaDAO | categorias, produtos | CadastrosEAcesso |
| RF07 | Produto, imagem opcional e saldo inicial atômicos | TelaProdutos; FormularioProduto | ProdutoDAO, Imagens, EstoqueDAO | produtos, estoque, movimentos_estoque | CadastrosEAcesso; IntegracaoBanco; CatalogoApresentacao |
| RF08 | Estoque, mínimo, entrada/saída, movimentos | TelaEstoque | EstoqueDAO | estoque, movimentos_estoque | CadastrosEAcesso; CompraEPagamento; GestaoECancelamento |
| RF09 | Catálogo ativo, busca flexível, categoria/preço e detalhes | TelaLoja; TelaProdutos; venda no balcão | ProdutoDAO, BuscaTexto | produtos, categorias, estoque, avaliacoes | BuscaProdutos; InterfaceBrasileira; CatalogoApresentacao |
| RF10 | Favoritos privados persistidos | TelaLoja; TelaFavoritos | ClienteLojaDAO | favoritos | IntegracaoBanco; CadastrosEAcesso |
| RF11 | Carrinho com quantidade, subtotal e reserva única | TelaCarrinho; TelaCheckout | Carrinho, ItemPedido, Dinheiro | Memória durante a sessão | CompraEPagamento; FluxoCheckout |
| RF12 | Checkout com entrega, revalidação e confirmação única | TelaCheckout | CompraDAO, Transacao, EstoqueDAO | pedidos, itens_pedido, pagamentos, movimentos_estoque, historico_pedidos, notificacoes | CompraEPagamento; FluxoCheckout; PerfilECupons |
| RF13 | PIX/débito/crédito simulados e boleto pendente | TelaCheckout; TelaPagamentos | CompraDAO, PagamentoSimulado | pagamentos, pedidos, historico_pedidos, notificacoes | CompraEPagamento; GestaoECancelamento |
| RF14 | Venda de balcão, cliente/produto/quantidade e retirada | TelaPedidos | PedidoDAO, EstoqueDAO | pedidos, itens_pedido, pagamentos, movimentos_estoque, historico_pedidos, notificacoes | FluxoVendaBalcao; GestaoECancelamento |
| RF15 | Pedidos, detalhes/histórico e avanço uma etapa por vez | TelaGestaoPedidos; TelaMeusPedidos | GestaoPedidoDAO, CompraDAO, PedidoDAO | pedidos, itens_pedido, pagamentos, historico_pedidos | GestaoECancelamento; FluxoGestaoPedidos |
| RF16 | Cancelar antes do envio com motivo e devolução única | TelaGestaoPedidos; TelaMeusPedidos | GestaoPedidoDAO, EstoqueDAO | pedidos, pagamentos, movimentos_estoque, historico_pedidos, notificacoes | GestaoECancelamento; FluxoGestaoPedidos |
| RF17 | Perfil próprio com senha atual e senha nova opcional | TelaPerfil | ClienteLojaDAO, SenhaCripto | clientes, acessos_cliente | PerfilECupons; FluxoPerfilCupons |
| RF18 | Cupons percentuais válidos e histórico do desconto | TelaCupons; TelaCheckout | CupomDAO, CompraDAO, DescontosCompra | cupons, pedidos | PerfilECupons; FluxoPerfilCupons |
| RF19 | Avaliar produto comprado/entregue/pago; média pública | TelaAvaliacao; TelaMeusPedidos; detalhes na loja | AvaliacaoDAO, ProdutoDAO | avaliacoes, pedidos, itens_pedido, pagamentos | AvaliacoesENotificacoes; FluxoAvaliacoesAvisos |
| RF20 | Avisos internos por evento e leitura individual | TelaNotificacoes | NotificacaoDAO, EstoqueDAO, CompraDAO, GestaoPedidoDAO | notificacoes, leituras_notificacoes, estoque | AvaliacoesENotificacoes; FluxoAvaliacoesAvisos |
| RF21 | Indicadores e gráficos gerenciais | TelaDashboard | RelatorioDAO, componentes gráficos de TelaRelatorios | pedidos, itens_pedido, pagamentos, produtos, estoque | RelatoriosFinanceiros; FluxoRelatoriosNavegacao |
| RF22 | Relatórios por período e exportação CSV | TelaRelatorios | RelatorioDAO, ExportarCsv, Datas | Mesmo conjunto de RF21; categorias e clientes | RelatoriosFinanceiros; FluxoRelatoriosNavegacao |
| RNF01 | Integridade transacional e concorrência | Operações compostas | Transacao; DAOs; bloqueios no PostgreSQL | FKs, CHECKs, índices, chave UUID | IntegracaoBanco; CompraEPagamento; GestaoECancelamento; AvaliacoesENotificacoes |
| RNF02 | Execução portátil no projeto e persistência | F5; scripts | .vscode; compilar/executar/preparar-ambiente | schema_migrations; V001–V006 | testar-integracao; verificar-base; teste do pacote |
| RNF03 | Interface desktop coerente e formatos brasileiros | Todas as telas; Voltar nas secundárias | Tema, Navegacao, MascaraCampo, Datas, Dinheiro | Datas tipadas e valores NUMERIC | InicializacaoTelas; InterfaceBrasileira; FluxoVendaBalcao; FluxoRelatoriosNavegacao |

## Conferência inversa

As sete classes de `src/model` são Usuario, Cliente, Categoria, Produto, Pedido, ItemPedido e Carrinho. O diagrama de classes usa os nomes reais; outras informações são registros/DTOs dos DAOs. TelaInicial e TelaManutencao são navegação para os requisitos acima. Os aliases de login redirecionam à entrada oficial. `catalogo`, util.InstalarCatalogo e SimulacaoUsoDAO/util.SimularUsoDiario produzem dados fictícios; não acrescentam outra modalidade de venda. Migração, configuração, criptografia, tratamento de erro, imagem, CSV e gráficos dão suporte às funcionalidades documentadas.

O banco tem 16 tabelas de negócio, todas citadas acima; `schema_migrations` é a tabela técnica. Não há tela ou tabela de pagamento real, nota fiscal, transportadora, chat, e-mail/SMS, estorno ou devolução pós-envio. Esses itens não são requisitos prometidos pela documentação desta entrega.

Detalhes dos cenários e suas saídas estão nos arquivos `validacao-*.txt` e `ultimo-teste-integracao.txt`. Os 102 cenários numerados dos módulos são complementados por oito de busca, nove de carga fictícia e verificações de evolução, catálogo, 23 instâncias de telas e fluxos Swing; as contagens não transformam cada clique em um cenário de negócio.
