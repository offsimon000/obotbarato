# Validação da entrega em 11/10/2026

Versão desktop destinada à revisão dos autores e apresentação em 28/10/2026. Instituição, curso, autores e orientação foram mantidos conforme a confirmação dos autores. Não foi emitido um registro fictício de aprovação pela banca.

## Resultados

| Verificação | Resultado e alcance |
|---|---|
| Compilação | 71 fontes de produção com Java 17; JAR e driver relativo gerados |
| Regressão dos módulos | 102 cenários numerados aprovados em PostgreSQL real |
| Busca | Oito cenários, parâmetros SQL e filtro Swing aprovados; busca oleo também conferida na base de uso sem alterar vendas |
| Carga fictícia | Nove cenários de autorização, atomicidade, valores, datas e reaplicação aprovados |
| Interface | Construção de 23 instâncias e fluxos Swing; balcão validado em três tamanhos e com falha/rollback/nova tentativa |
| VS Code | JSON e dependências das tarefas válidos; tarefas anteriores ao F5 executadas; entrada oficial aberta em VM depurada por JDI com os mesmos caminhos/configuração |
| Banco e backup | Cópia extraída prepara base nova; catálogo e carga diária instalados; reinício e restauração em banco novo preservam dados das fixtures |
| Pacote | Extraído fora da pasta de desenvolvimento; hashes conferidos; compilação, autenticação, telas e busca aprovadas; cluster temporário encerrado |
| Documentação | 22 requisitos funcionais e três não funcionais ligados ao código, tabelas e testes; esquema consultado com 16 tabelas de negócio e uma técnica |
| Diagramas | 12 fontes PlantUML e figuras geradas; nomes reais, chaves e fluxos atuais |
| Word e PDF | 33 páginas renderizadas e conferidas individualmente, com sumário, tabelas e figuras legíveis |
| Slides | 13 slides editáveis, notas de fala, fontes verificadas, validação estrutural/importação e revisão visual de todos os slides |

A verificação do depurador reproduz os parâmetros da configuração do editor; não automatiza a tecla F5 dentro da interface do VS Code. Na revisão, os autores devem abrir o workspace e conferir o F5 e o ensaio pessoal. As extensões Java já estão instaladas nesta máquina.

## Evidências e reprodução

Os fontes dos testes estão em test. scripts/testar-integracao.ps1 executa os módulos, evolução, busca e fluxos Swing em cluster isolado. scripts/testar-simulacao.ps1 verifica a carga em base nova. scripts/verificar-base.ps1 verifica persistência, reinício e restauração com fixtures próprias. ExecucaoDepurador.java verifica abertura da entrada oficial por JDI sem cadastrar ou vender.

Evidências dos módulos: ultimo-teste-integracao.txt, validacao-parte2.txt, validacao-compra.txt, validacao-pedidos.txt, validacao-perfil-cupons.txt, validacao-avaliacoes-avisos.txt, validacao-relatorios-navegacao.txt, validacao-interface.txt, validacao-venda-balcao.txt, validacao-busca.txt, validacao-catalogo.txt e validacao-simulacao-uso.txt. Capturas de tela estão em validacao-ui. A execução fora da pasta é registrada em validacao-pacote.txt.

## Condições da entrega

Windows, JDK 17 ou posterior e PostgreSQL UTF-8 são pré-requisitos. O pacote inclui código, JAR, JDBC, SQL, configuração de exemplo, catálogo, scripts, testes e documentos; não inclui Java/PostgreSQL instaláveis, senha real do banco, cluster local ou backup com dados de uso. Cada cópia nova prepara seu ambiente e recebe a demonstração pela tarefa manual.

Pagamento/entrega são simulações; a loja é desktop, notificações são internas e algumas consultas de manutenção são síncronas. Integrações financeiras/fiscais/logísticas, site público e devolução após envio ficam fora do escopo. Os testes não garantem ausência absoluta de falhas em qualquer ambiente. Não há falha conhecida aberta nos fluxos verificados da entrega.

O roteiro para revisão, apresentação e envio posterior ao GitHub está em REVISAO_E_APRESENTACAO.md. A revisão textual e o ensaio dos autores precedem o envio; nenhum push é feito na preparação.
