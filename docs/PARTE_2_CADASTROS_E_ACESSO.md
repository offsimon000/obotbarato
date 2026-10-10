# Parte 2 — cadastros, acesso e catálogo

Implementação da etapa 4 do plano. Desktop Java 17/Swing/PostgreSQL, mantendo o tema visual existente. Guia de teste: `COMO_TESTAR_PARTES_1_E_2.md`.

## Matriz de permissões

| Operação | Admin | Vendedor | Cliente |
|---|---|---|---|
| Gerenciar usuários/perfis | Sim | Não | Não |
| Gerenciar categorias/produtos/imagens | Sim | Não | Não |
| Consultar catálogo | Sim | Sim | Sim |
| Consultar estoque, histórico e alertas | Sim | Sim | Disponibilidade no catálogo |
| Movimentação manual / definir mínimo | Sim | Não | Não |
| Cadastrar/editar clientes | Sim | Sim | Autocadastro |
| Desativar/reativar cliente | Sim | Não | Não |
| Emitir código de acesso de cliente | Sim | Não | Não |
| Registrar venda | Sim | Sim | Compra própria |
| Consultar relatórios e pedidos gerais | Sim | Sim | Não |
| Gerenciar cupons | Sim | Não | Não |
| Editar perfil e trocar senha | Na sessão do titular | Na sessão do titular | Somente próprio e com senha atual |
| Publicar/editar avaliação | Não | Não | Produto adquirido em pedido próprio entregue/pago |
| Consultar/marcar notificações | Equipe, leitura individual | Equipe, leitura individual | Somente próprias |
| Histórico pessoal e favoritos | Na sessão de cliente | Na sessão de cliente | Somente próprios |
| Avançar entrega simulada | Sim, uma etapa por vez | Sim, uma etapa por vez | Não |
| Cancelar pedido | Antes do envio | Antes do envio | Somente próprio e aberto |

`Acesso` revalida identidade, ativação e perfil no banco. DAOs e telas aplicam a matriz; consultas públicas de catálogo não expõem dados de clientes. `Sessao.isAdminAtivo()` identifica apenas admin, e `Sessao.sair()` limpa identidades e fecha as janelas antes de abrir o login. O banco possui credencial técnica compartilhada pelo desktop; esta matriz protege as operações do aplicativo, não é um isolamento por usuário do PostgreSQL.

## Senhas e atendimento

Novas senhas: 8 a 128 caracteres, PBKDF2-HMAC-SHA256, 600.000 iterações, salt aleatório de 16 bytes e resultado de 32 bytes. Formato: `pbkdf2$iterações$saltBase64$hashBase64`. A implementação usa o Java padrão; valor de referência: [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). Hashes MD5 e SHA256+salt antigos são verificados e migrados no login, inclusive senhas antigas curtas. Falhas de migração são propagadas. Senha vazia na edição de usuário preserva a atual.

`UsuarioDAO` serializa alterações de usuários com advisory lock e impede que o conjunto termine sem administrador ativo. Login é normalizado em minúsculas e tem índice único independente de caixa; perfis permitidos continuam admin/vendedor. Desativação preserva o registro.

O cadastro administrativo de cliente não cria senha. O admin identifica o titular por atendimento e emite um código aleatório de 128 bits em `CodigoAcessoDAO`. Entrega manual pelo atendente: não há e-mail/SMS integrado. O banco guarda somente SHA256 do código, prazo de 30 minutos, contador de tentativas e emissor. Novo código substitui o anterior. Cinco códigos incorretos com formato válido bloqueiam o uso; código expirado ou usado não redefine senha. Código e alteração de senha são consumidos na mesma transação, com bloqueio das linhas. Conhecer CPF/e-mail não basta para redefinir senha. O autocadastro válido cria senha e acesso imediatamente.

## Cadastros e catálogo

`Validacao` é compartilhada entre manutenção e autocadastro: limites de campos, e-mail normalizado, CPF opcional com dígitos verificadores, telefone opcional com DDD + 8/9 dígitos e endereço opcional. Campos opcionais vazios tornam-se NULL. Índices normalizados de e-mail/CPF impedem duplicidade com diferença de caixa/máscara. A V2 falha atomicamente se uma base externa contiver duplicidades incompatíveis; esses dados precisam ser revisados antes de migrar, sem remoção automática.

`MascaraCampo` usa DocumentFilter e respeita inserção/seleção/exclusão; fixa/celular são formatados conforme quantidade de dígitos.

Categorias têm `ativo`. Admin cria/edita/desativa/reativa. Desativação é recusada se existem produtos ativos vinculados. Produto ativo precisa de categoria ativa; as duas operações usam o mesmo advisory lock. Manutenção mostra também inativos; loja mostra somente produto e categoria ativos. Clientes inativos não autenticam ou compram; admin pode reativá-los preservando senha e histórico.

Imagem PNG/JPEG opcional é validada por tamanho (até 5 MB) e dimensões (até 4096 pixels por lado), guardada em BYTEA e exibida proporcionalmente com placeholder se ausente. Não depende de caminho absoluto da máquina. Produto + saldo inicial + mínimo + movimento de abertura são confirmados juntos.

## Estoque e evolução do banco

`V002__cadastros_e_acesso.sql` adiciona categoria ativa, imagem, índices normalizados, `acessos_cliente` e `movimentos_estoque`. Não altera a V1 nem reseta demonstração. Saldo anterior recebe um registro de abertura, sem inventar operações anteriores. A instalação da demonstração também registra abertura dos novos produtos.

`EstoqueDAO` aplica quantidade positiva em entrada/saída, saldo e mínimo não negativos. Movimentações manuais e ajustes são de admin; consultas são da equipe. Cada movimento guarda data, tipo, delta com sinal, saldo, motivo e responsável. A baixa por venda participa da transação de `PedidoDAO`: falha não deixa estoque/histórico/pedido parcial. Alterar mínimo é configuração, não movimento de saldo. O alerta usa o mínimo salvo de cada produto.

## Limites desta entrega

Parte 2 fecha a manutenção e o acesso. Compra/pagamento estão concluídos na etapa 5 (COMPRA_E_PAGAMENTO_SIMULADO.md); transições e cancelamento na etapa 6 (GESTAO_E_CANCELAMENTO_PEDIDOS.md). Perfil e cupons estão concluídos (PERFIL_E_CUPONS.md). Avaliações e notificações também estão concluídas (AVALIACOES_E_NOTIFICACOES.md); dashboard e revisão final seguem pendentes. Parte das consultas antigas ainda roda no EDT; sua reorganização faz parte da etapa 8. Catálogo/saldos passaram a uma consulta em segundo plano nos ajustes de 09/10, descritos em AJUSTES_INTERFACE_E_DATAS.md. A documentação acadêmica final ainda será regenerada com todos os módulos concluídos.
