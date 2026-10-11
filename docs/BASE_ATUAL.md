# Estado da entrega em 11/10/2026

Sistema desktop Java Swing com PostgreSQL, para apresentação em 28/10/2026. Implementados: cadastros e três perfis, catálogo com imagens, estoque e movimentos, loja/favoritos/carrinho, checkout e pagamentos simulados, venda de balcão, gestão/histórico/cancelamento, perfil/cupons, avaliações, notificações, dashboard, relatórios e CSV.

A busca de produtos ignora acentos, caixa e espaços extras; aceita fragmentos e palavras em qualquer ordem. Venda de balcão tem seleção de produto visível e tabela sem sobreposição. Cadastro indica obrigatório/opcional, datas têm máscara brasileira, valores estão em reais e telas secundárias possuem Voltar.

## Execução e dados

Para uso sem o editor, abra **Executar OBotBarato** na Área de Trabalho ou na pasta do projeto. `scripts/criar-atalho.ps1` recria os atalhos locais; `scripts/abrir-desktop.ps1` prepara o ambiente e inicia javaw sem console permanente. Como alternativa de desenvolvimento, abra OBotBarato.code-workspace no VS Code e use F5. As duas formas iniciam TelaLoginUnificado e preservam a base e seus dados. Não há lançadores CMD ou arquivos NetBeans/Ant na entrega. Pré-requisitos e tarefas: EXECUTAR_VS_CODE.md e README.

O catálogo de demonstração possui 30 produtos. A carga diária acrescenta 20 clientes e 60 pedidos fictícios, com 18 avaliações e 40 favoritos; não duplica nem repõe estoque ao ser repetida. Guia: SIMULACAO_USO_DIARIO.md. Valores do dashboard variam conforme novas operações.

## Código documentação e testes

71 fontes Java de produção; sete modelos reais; 16 tabelas de negócio e schema_migrations. Migrations V001 a V006 preservadas. DICIONARIO_BANCO.md lista o esquema consultado, MATRIZ_RASTREABILIDADE.md liga requisitos à implementação. Documentação acadêmica, PDF e slides estão em final; diagramas PNG e PlantUML em diagramas; conteúdo editável da documentação em fontes.

A regressão de 11/10/2026 aprovou os 102 cenários dos módulos, oito cenários de busca, verificações de evolução, catálogo, 23 instâncias de telas e fluxos Swing incluindo balcão. A carga fictícia passou em nove cenários próprios. Evidências e comandos estão nos guias e nos arquivos validacao-*.txt. Backup, reinício e restauração são verificáveis com scripts/verificar-base.ps1, que usa apenas suas fixtures.

O roteiro de revisão, ensaio e envio posterior ao GitHub está em REVISAO_E_APRESENTACAO.md. O pacote inclui fontes, configuração portátil, driver, SQL, dados fictícios, documentos e JAR; exclui configurações reais, senhas e banco local. Pagamentos e entregas são simulações acadêmicas. Não há site público, emissão fiscal, integração bancária/transportadora/e-mail/SMS ou devolução após envio.
