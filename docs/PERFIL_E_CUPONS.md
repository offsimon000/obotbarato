# Perfil e cupons — parte inicial da etapa 7

Desktop Swing/PostgreSQL, mantendo o tema branco/azul. Este bloco conclui perfil e cupons; avaliações, notificações e dashboard ainda estão pendentes.

## Perfil do cliente

Loja → **Meu perfil**: nome, CPF opcional, e-mail de acesso, telefone e endereço. CPF e telefone mantêm máscaras; campos vazios opcionais ficam NULL. E-mail é normalizado; CPF/e-mail duplicados e valores inválidos são recusados.

Salvar exige a **senha atual**, inclusive ao mudar só os dados. Nova senha e confirmação são opcionais; vazias preservam a senha. Se preenchidas, precisam conferir e atender ao limite de 8 a 128 caracteres. O armazenamento mantém PBKDF2. O DAO valida o titular e a ativação no banco, bloqueia o cadastro e verifica a senha atual antes de atualizar. Dados e troca de senha são atômicos; duplicidade não deixa senha alterada parcialmente.

Salvar revoga códigos de recuperação/ativação pendentes desse cliente. A sessão atual permanece ativa; próximos logins usam o novo e-mail/senha. O nome da saudação e os dados usados como padrão em novos checkouts são atualizados. Uma entrega já gravada conserva seu snapshot; editar o endereço do perfil não muda pedidos anteriores. Endereço de checkout já preenchido continua sendo revisado pelo comprador.

Erros preservam o preenchimento; botões ficam bloqueados durante salvamento. Campos de senha são limpos após sucesso. Nenhum perfil de outra conta é consultado ou editado.

## Administração de cupons

Painel admin → **Cupons**: cadastrar, editar, ativar/desativar, definir percentual e início/fim. Vendedor não gerencia cupons; cliente só valida um código para a própria compra.

- Código: 3 a 32 letras ASCII, números, hífen ou sublinhado; normalizado em maiúsculas; único.
- Percentual: maior que zero até 100%, com até duas casas decimais.
- Datas obrigatórias, brasileiras e mascaradas; fim não pode preceder início.
- Cupom válido precisa estar ativo e a data do banco deve estar entre início e fim, inclusive ambos os dias.
- Não há exclusão física pela tela; códigos/valores já aplicados permanecem no pedido.
- Um cupom por compra. Reutilização em outras compras é permitida enquanto válido, sem limite global/por cliente nesta versão. Não existe desconto de frete: a entrega segue como simulação gratuita.

A demonstração instala **APRESENTA10**, com 10%, válido por 30 dias a partir da primeira criação. Reexecutar o seed não reativa, muda percentual ou estende validade de um cupom existente.

## Combinação e confirmação da compra

Cupom é aplicado primeiro ao subtotal; desconto de pagamento incide sobre o restante. Cada etapa usa BigDecimal, arredondamento HALF_UP e centavos.

| Exemplo | Cupom | Pagamento | Total |
|---|---|---|---|
| Subtotal R$ 100, cupom 10%, PIX | R$ 10 | R$ 9 (10% dos R$ 90 restantes) | R$ 81 |
| Subtotal R$ 99,99, cupom 10%, débito | R$ 10 | R$ 4,50 (5% dos R$ 89,99 restantes) | R$ 85,49 |
| Cupom 100% | Todo o subtotal | Zero | Zero, nunca negativo |

No pagamento do checkout, digite o código e clique **Aplicar cupom**. Campo preenchido sem validação bloqueia confirmação. Depois de aplicado, o código fica bloqueado para edição; **Remover** libera o campo e recalcula valores. PIX/débito continuam podendo ser escolhidos para revisar o total combinado.

Na confirmação, o servidor revalida ativação, período e percentual, com bloqueio da linha do cupom durante a transação. Percentual diferente do mostrado exige reaplicação/revisão; não muda o valor silenciosamente. Se o cupom expirar ou mudar, remova e aplique novamente, ou compre sem cupom. Pedido, itens, descontos, pagamento, estoque e histórico continuam atômicos.

UUID protege repetição: uma compra já gravada retorna seus valores originais, mesmo que o cupom tenha sido editado/desativado depois. Cupom não reserva nem consome um estoque separado. Cancelamento mantém o código/percentual/descontos e segue devolvendo estoque uma vez.

## Banco e relatórios

`V005__perfil_e_cupons.sql` cria cupons, referência opcional do pedido e snapshots de código, percentual, desconto do cupom e do pagamento. Desconto total é a soma dos dois; total continua subtotal menos desconto. Restrições impedem valores negativos e descontos maiores que o subtotal.

Migrations anteriores permanecem intactas. V5 preserva pedidos/valores antigos, atribuindo o desconto anterior ao pagamento e zero ao cupom. Perfil usa as colunas existentes de clientes. Faturamento por período continua somando o total líquido de pedidos não cancelados com pagamento confirmado. Receita bruta no ranking continua identificada como tal até a revisão completa da etapa 8.

## Como testar

1. Abra `Iniciar OBotBarato.cmd`; cliente de demonstração: `cliente@demo.local` / `cliente123`.
2. Abra Meu perfil; altere telefone/endereço e confirme com a senha atual. Tente senha incorreta: nada deve mudar. Para testar troca de e-mail/senha, prefira criar outra conta, preservando as credenciais da demonstração.
3. Em uma conta de teste, preencha nova senha e confirmação; salve. Saia e autentique com os dados novos. A senha anterior deve ser recusada.
4. Como admin (`admin` / `admin123`), abra Cupons. Confira APRESENTA10, crie um cupom, edite percentual/datas e desative/reative. Tente código repetido, percentual zero/acima de 100 e validade invertida: deve recusar.
5. Cliente: compre e vá ao pagamento. Digite APRESENTA10 → Aplicar cupom → selecione PIX. Confira os dois descontos e o total. Por exemplo, produto R$ 100 resulta em R$ 81.
6. Remova o cupom, confira recalcular e aplique de novo. Confirme; detalhes do pedido devem guardar código/percentual e descontos separados.
7. Edite/desative esse cupom como admin. Pedido antigo deve manter os valores. Nova compra com cupom inativo deve ser recusada. Vendedor consulta pedidos/pagamentos, mas não possui manutenção de cupons.

## Verificação

`scripts/testar-integracao.ps1`: 66 cenários anteriores + 14 de perfil/cupons (80), evolução até V5, construção de 19 telas e fluxo Swing de perfil, senha, aplicar/remover/reaplicar cupom, PIX, entrega atualizada e duplo clique. Evidência: `validacao-perfil-cupons.txt`; capturas `validacao-ui/perfil-cliente.png`, `cupons-admin.png` e `checkout-cupom*.png`.

`scripts/verificar-base.ps1`: registros próprios verificam perfil/senha e compra cancelada com subtotal R$ 23, cupom R$ 2,30, desconto PIX R$ 2,07 e total R$ 18,63 após reinício e restauração. Remove somente suas fixtures, inclusive o cupom temporário. O catálogo e as contas reais permanecem preservados.
