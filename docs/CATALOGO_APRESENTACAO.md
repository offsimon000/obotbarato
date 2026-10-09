# Catálogo de apresentação — 09/10/2026

Catálogo fictício de cosméticos para a demonstração acadêmica do OBotBarato: **30 produtos, cinco categorias, seis itens por categoria**. Preços definidos para o projeto, sem representar cotações comerciais. São 855 unidades de estoque inicial, com mínimo 5 em cada produto.

Todas as imagens são ilustrativas, geradas pelo recurso integrado **imagegen**, com uma chamada por produto. Arquivos originais selecionados em `catalogo/imagens`; cadastro e conjunto completo dos prompts em `catalogo/produtos.json`; arquivo de importação em `catalogo/produtos.tsv`. Os arquivos foram copiados para o projeto e as imagens também são gravadas no PostgreSQL, acompanhando os backups. Não dependem de links externos para aparecer na loja.

A linha visual dos prompts foi fotografia de embalagem cosmética fictícia, enquadramento de catálogo, fundo branco, sombra suave, sem marcas reais, texto legível ou marca d'água. Cada prompt especifica a embalagem, material, cor e capacidade do item correspondente.

## Produtos

| Código do catálogo | Produto | Categoria | Preço | Estoque inicial |
|---|---|---|---|---|
| OBB-001 | Aurora Floral Eau de Parfum 100 ml | Perfumes | R$ 129,90 | 24 |
| OBB-002 | Essência Amadeirada Eau de Parfum 100 ml | Perfumes | R$ 149,90 | 20 |
| OBB-003 | Brisa Cítrica Eau de Toilette 100 ml | Perfumes | R$ 109,90 | 28 |
| OBB-004 | Jardim de Rosas Eau de Parfum 75 ml | Perfumes | R$ 119,90 | 18 |
| OBB-005 | Noite Âmbar Eau de Parfum 100 ml | Perfumes | R$ 169,90 | 16 |
| OBB-006 | Body Splash Flor de Algodão 200 ml | Perfumes | R$ 49,90 | 35 |
| OBB-007 | Batom Velvet Vermelho Clássico 3,5 g | Maquiagem | R$ 39,90 | 32 |
| OBB-008 | Batom Nude Rosado 3,5 g | Maquiagem | R$ 39,90 | 30 |
| OBB-009 | Gloss Cristal Transparente 6 ml | Maquiagem | R$ 29,90 | 36 |
| OBB-010 | Máscara de Cílios Preto Intenso 8 ml | Maquiagem | R$ 44,90 | 25 |
| OBB-011 | Paleta de Sombras Tons Neutros 12 cores | Maquiagem | R$ 89,90 | 18 |
| OBB-012 | Blush Compacto Pêssego 5 g | Maquiagem | R$ 34,90 | 28 |
| OBB-013 | Creme Facial Conforto 50 g | Cuidados com a Pele | R$ 59,90 | 25 |
| OBB-014 | Gel de Limpeza Suave 150 ml | Cuidados com a Pele | R$ 39,90 | 30 |
| OBB-015 | Água Micelar Essencial 200 ml | Cuidados com a Pele | R$ 34,90 | 32 |
| OBB-016 | Sérum Facial Toque de Seda 30 ml | Cuidados com a Pele | R$ 69,90 | 22 |
| OBB-017 | Esfoliante Facial Gentil 75 g | Cuidados com a Pele | R$ 44,90 | 24 |
| OBB-018 | Balm Labial Baunilha 10 g | Cuidados com a Pele | R$ 19,90 | 40 |
| OBB-019 | Shampoo Brilho Diário 300 ml | Cabelos | R$ 29,90 | 40 |
| OBB-020 | Condicionador Toque Macio 300 ml | Cabelos | R$ 32,90 | 36 |
| OBB-021 | Máscara Capilar Nutritiva 250 g | Cabelos | R$ 49,90 | 24 |
| OBB-022 | Leave-in Leve 150 ml | Cabelos | R$ 39,90 | 28 |
| OBB-023 | Óleo para Pontas Argan 60 ml | Cabelos | R$ 44,90 | 26 |
| OBB-024 | Creme para Pentear Cachos 300 ml | Cabelos | R$ 34,90 | 34 |
| OBB-025 | Loção Corporal Baunilha 200 ml | Corpo e Banho | R$ 39,90 | 30 |
| OBB-026 | Sabonete Líquido Floral 300 ml | Corpo e Banho | R$ 24,90 | 42 |
| OBB-027 | Creme para Mãos Algodão 75 g | Corpo e Banho | R$ 19,90 | 38 |
| OBB-028 | Esfoliante Corporal Coco 200 g | Corpo e Banho | R$ 39,90 | 24 |
| OBB-029 | Kit Sabonetes Lavanda 3 unidades | Corpo e Banho | R$ 29,90 | 28 |
| OBB-030 | Sais de Banho Lavanda 250 g | Corpo e Banho | R$ 34,90 | 22 |

## Instalação e conservação

O catálogo foi instalado manualmente na base local depois de backup e validação em base isolada. Produtos anteriores são **desativados**, mantendo IDs, nomes, imagens, saldos, favoritos e referências de pedidos existentes. Não se apagam vendas nem se renomeiam itens de compras anteriores.

O lançador normal não importa nem repõe o catálogo. Para instalar em uma nova base local gerenciada, execute na pasta oficial:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/instalar-catalogo.ps1
```

O script faz backup antes de alterar dados e usa somente a configuração do ambiente local gerenciado. Não admite overrides `DB_*`. A ferramenta confere imagens/quantidades e aplica produtos, estoques, abertura de movimentações e desativação dos anteriores numa única transação. Uma falha desfaz a importação. Não há nova migration nem alteração das V1/V2/V3/V4.

Reexecutar com os nomes/categorias originais reutiliza os IDs, não duplica produtos, não repõe estoque e não sobrescreve as edições dos itens encontrados. Nomes/categorias funcionam como identificadores desta importação; se tiverem sido alterados depois, confira os cadastros antes de reexecutar. A operação substitui o conjunto ativo: outros produtos fora deste arquivo ficam inativos. Alterações normais depois da instalação devem ser feitas em Produtos/Estoque.

## Consultar e testar

Abra `Iniciar OBotBarato.cmd` e entre como cliente. A loja deve mostrar 30 produtos ao limpar filtros. Use as categorias para navegar; **Ver detalhes** mostra imagem maior, descrição, preço e saldo. Favoritos e carrinho continuam revalidando disponibilidade. Admin mantém os produtos pela tela Produtos, incluindo os antigos inativos.

`test/CatalogoApresentacao.java` usa apenas `obotbarato_test`: simula falha no arquivamento depois da criação, confere rollback, 30 imagens distintas, seis produtos por categoria, pedidos preservados e reexecução sem reposição de saldo. Também carrega a loja e aciona Ver detalhes, sem mostrar janelas ao usuário. O script de integração inclui essa verificação ao final. Evidência: `docs/validacao-catalogo.txt`; capturas da loja, detalhes e visão dos 30 itens: `docs/validacao-ui/catalogo-*.png`.
