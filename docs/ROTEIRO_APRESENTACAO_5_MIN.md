# Roteiro de apresentação — 5 minutos

Roteiro para duas pessoas: a dupla explica a proposta e o contexto; Samuel demonstra o sistema. O aplicativo é desktop e os pagamentos/entregas são simulações acadêmicas.

## Antes de começar

- Abra o sistema e deixe a tela da loja pronta, já conectado como cliente. Confira isso no mesmo computador e na mesma base que serão usados na apresentação.
- Separe um produto ativo com estoque positivo. Se for demonstrar cupom, confirme que `APRESENTA10` ainda está válido.
- Deixe anotadas as contas de demonstração, caso precise entrar de novo: cliente `cliente@demo.local` / `cliente123`; vendedor `vendedor` / `vendedor123`; administrador `admin` / `admin123`.
- Combine quem controla o mouse. Não tentem mostrar todos os módulos em cinco minutos; façam um fluxo principal e expliquem os demais.

## Fala e demonstração

### 0:00–1:15 — Dupla: contexto e objetivo

> Boa tarde. Nosso projeto é o OBotBarato, um sistema acadêmico desktop para uma loja de perfumaria e cosméticos. A proposta surgiu para reunir em um só lugar atividades que podem ficar separadas no dia a dia: catálogo e preços, clientes, estoque, vendas e acompanhamento de pedidos. Quando essas informações não são conectadas, fica mais difícil saber o que foi vendido, quanto resta no estoque e em que etapa está cada pedido.
>
> O sistema tem três perfis principais: administrador, vendedor e cliente. O administrador mantém os cadastros e acompanha os indicadores; o vendedor trabalha com pedidos e vendas de balcão; o cliente consulta os produtos e acompanha as próprias compras. O projeto foi desenvolvido em Java Swing com PostgreSQL. Agora o Samuel vai mostrar um fluxo do cliente e explicar como o sistema protege os dados da venda. Pagamento e entrega são simulados: não há cobrança bancária nem integração com transportadora.

### 1:15–2:10 — Samuel: loja e busca

> Estou na loja do cliente. O catálogo tem produtos fictícios com imagem, preço e descrição. A busca aceita palavras sem exigir a acentuação exata; também posso filtrar por categoria e preço. Vou procurar “oleo” e abrir um produto.

**Ação:** digite `oleo`, filtre e abra os detalhes de um produto ativo. Não perca tempo percorrendo todo o catálogo.

### 2:10–2:55 — Samuel: carrinho e compra

> Ao adicionar o produto, ele aparece no carrinho com quantidade, subtotal e total. Antes de confirmar, a tela permite revisar a quantidade e os dados de entrega. O checkout registra uma forma de pagamento simulada. Por exemplo, PIX aplica o desconto definido pelo sistema, mas não envia dinheiro a nenhum banco.

**Ação:** mostre o carrinho e o resumo do checkout. Só confirme a compra se tiver ensaiado a operação e quiser criar um novo registro na base da apresentação.

### 2:55–3:45 — Samuel: pedido, pagamento e estoque

> Por trás dessa confirmação, o sistema grava o pedido e os itens, registra o pagamento e baixa o estoque como uma operação única no banco. Se alguma parte falhar — por exemplo, se não houver saldo suficiente — a operação é desfeita para não deixar um pedido pela metade. O pagamento e a entrega têm estados separados: um pedido pode estar pago e ainda estar em preparo.

**Ação:** se a compra foi confirmada, mostre-a em **Meus Pedidos**. Se não foi, use um pedido fictício já existente para mostrar o acompanhamento.

### 3:45–4:25 — Samuel: equipe e balcão

> Na área da equipe, pedidos avançam uma etapa por vez: aberto, em preparo, enviado e entregue. Um cancelamento permitido registra o motivo e devolve o estoque uma única vez. Para atendimento presencial existe a venda de balcão: selecionamos cliente, produto e quantidade; ela registra retirada imediata, pagamento simulado em dinheiro e baixa de estoque.

**Ação:** se já estiver com uma sessão de vendedor preparada, mostre a lista de pedidos **ou** a tela de venda de balcão. Se precisar trocar de usuário e isso estiver demorando, explique o fluxo sem trocar de tela.

### 4:25–5:00 — Samuel: gestão e fechamento

> O administrador também tem estoque, cadastros, dashboard, relatórios e exportação CSV. Os relatórios separam vendas confirmadas de pedidos cancelados ou pendentes. Este é um protótipo desktop para demonstrar regras de operação: pagamentos, entrega e avisos são locais e simulados. Como próximo passo para uso comercial, seria necessário integrar serviços reais e hospedar o banco em um servidor compartilhado. Obrigado.

## Como o sistema funciona, em palavras simples

1. **Telas:** a interface é Java Swing. A loja do cliente também é uma janela desktop; não é um site.
2. **Regras e gravação:** DAOs concentram as consultas e gravações PostgreSQL. Os modelos representam dados como produto, cliente e pedido; serviços/utilitários tratam validações, sessão, dinheiro, datas e transações.
3. **Venda:** a compra guarda cabeçalho, itens, pagamento e movimentos de estoque relacionados. Uma transação confirma o conjunto todo ou desfaz tudo em caso de erro.
4. **Estoque:** a baixa só ocorre se houver quantidade disponível. O cancelamento permitido gera devolução uma vez e mantém o histórico da operação.
5. **Pedido:** estado de pagamento é diferente do estado de entrega. Equipe avança a entrega em etapas; cliente acompanha seu próprio pedido.
6. **Banco:** PostgreSQL guarda dados e relacionamentos. Migrations V001 a V006 criam/evoluem o esquema; a demonstração usa dados fictícios.

## Perguntas prováveis do professor

**Por que o projeto existe?**  
Para centralizar, no cenário de uma pequena loja, catálogo, clientes, estoque, pedidos e relatórios e demonstrar como essas operações podem seguir regras consistentes.

**É um e-commerce disponível na internet?**  
Não. É um aplicativo desktop em Java Swing. A loja é uma tela do próprio aplicativo, sem servidor web.

**O pagamento é real?**  
Não. PIX, crédito, débito e dinheiro registram estados de uma simulação. Não há integração bancária, cobrança ou emissão fiscal. Boleto pode ficar pendente até confirmação pela equipe.

**Como evitam pedido sem estoque ou estoque negativo?**  
A baixa é condicional e acontece junto com pedido/itens dentro da mesma transação. Se não houver saldo ou outra etapa falhar, ocorre rollback. Os testes também verificam compras concorrentes pela última unidade.

**E se alguém clicar duas vezes em confirmar?**  
A interface bloqueia a ação enquanto grava e o banco usa um identificador único da tentativa de compra. Repetir a confirmação não deve gerar outra venda.

**O que acontece quando um pedido é cancelado?**  
O cancelamento só é permitido nos estados previstos. Quando permitido, registra motivo e responsável, cancela o pagamento simulado e devolve o estoque uma vez; o histórico não é apagado.

**Por que o ranking de produtos pode diferir do faturamento?**  
O ranking soma o valor bruto dos itens; o faturamento usa o total final das vendas válidas, depois de descontos. Um cancelamento ou pagamento pendente não entra no faturamento.

**Como as senhas são protegidas?**  
O sistema persiste hashes PBKDF2 com salt, não a senha em texto puro. As senhas listadas neste roteiro são apenas credenciais fictícias da demonstração.

**Duas pessoas em computadores diferentes veem a mesma venda?**  
Não na configuração atual: cada computador cria um banco local independente. Para compartilhar dados em tempo real seria necessário configurar um PostgreSQL centralizado em um servidor e conectar as máquinas a ele.

**Como vocês validaram o projeto?**  
Com testes automatizados em PostgreSQL real temporário, testes de regras/rollback/concorrência, inicialização de 23 instâncias de telas e roteiros de interface para os fluxos principais. Isso cobre os cenários testados, não significa que o sistema foi testado em toda combinação possível de computador.

## Se o tempo apertar

Mostre apenas catálogo/busca → carrinho/checkout → acompanhamento de pedido. Mencione venda de balcão, estoque e relatórios sem trocar de perfil. Prefira uma explicação curta e um fluxo concluído a tentar abrir todas as telas.
