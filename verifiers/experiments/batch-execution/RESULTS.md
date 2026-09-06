# Execução em lote — 6 de setembro de 2026

Estado: concluído. **36/36 tentativas passaram**, sem timeout, falha de infraestrutura,
relatório inválido ou divergência semântica. Foram observados 36 IDs de execução
distintos. A seleção foi congelada antes do lançamento: seis casos já conhecidos de
um único fixture Quizzes, repetidos três vezes com concorrência 1 e três com
concorrência 2. A [seleção compacta](../../../docs/verifiers-impl/evidence/batch-execution-2026-09-06/selection-summary.json)
retém os IDs, hashes do pacote e expectativas anteriores.

## O que ficou disponível

Um runner Python pequeno, fora do pipeline Java, executa uma seleção explícita com
repetições, timeout individual e concorrência 1/2. Reutiliza `run-prepared.sh`, o build
e o pacote existentes. Cada tentativa continua a usar container/JVM e H2 em memória
novos. Compilar uma vez e lançar processos novos já era possível; o novo trabalho
automatiza o lote, congela expectativas e identidades, conserva as falhas e mede os
custos. Não foi introduzida uma otimização no executor ou no domínio.

Para repetir a mesma comparação, a partir da raiz, com a seleção local já congelada:

```sh
python3 verifiers/experiments/batch-execution/run.py run \
  --selection verifiers/target/batch-execution/selection.json \
  --output verifiers/target/batch-execution/run-02 \
  --concurrency 1 2 --repetitions 3 --timeout 180
```

O diretório de saída deve ser novo. O [README](README.md) explica como congelar outra
seleção, preparar novamente os artefactos quando necessário e gerar o resumo de tempos.
Não existe teto temporal global, conforme a correção explícita do utilizador. O timeout
individual é de 180 segundos; não há retries automáticos.

## Tempos e estabilidade

Máquina: Apple M4, 10 CPUs, 16 GiB de RAM; Docker Linux/aarch64 com 10 CPUs e cerca de
7,65 GiB disponíveis. Cada container foi limitado a 2 CPUs/3 GiB, com heap de 1,5 GiB.
O limite máximo simultâneo da campanha foi de dois containers. O grupo sequencial
precedeu o grupo concorrente; não foram excluídas tentativas de aquecimento.

| Medição | Concorrência 1 | Concorrência 2 |
| --- | ---: | ---: |
| Tentativas válidas/planeadas | 18/18 | 18/18 |
| Tempo total do grupo | 207,41 s | 173,39 s |
| Mediana por tentativa | 11,34 s | 19,23 s |
| Mínimo–máximo por tentativa | 11,14–12,60 s | 17,40–21,77 s |
| Mediana de arranque Spring, segundo o log | 8,38 s | 14,74 s |
| Mediana de setup, segundo o relatório | 0,73 s | 1,15 s |
| Execuções válidas por hora, extrapoladas do grupo | 312,4 | 373,7 |

A campanha completa demorou **386,79 s — cerca de 6 min 27 s**. Inclui 3,57 s de
verificação/preparação inicial e as verificações finais. O comando separado de seleção
não está incluído. Compilação e geração foram reutilizadas e não foram cronometradas
nesta campanha; não se atribui custo zero a uma preparação nova.

O arranque Spring domina os tempos observados. Os timers existentes distinguem esse
arranque e o setup; o restante tempo mistura lançamento Docker/JVM, leitura do pacote,
ações, avaliação, escrita e encerramento. Não há uma medição isolada do custo das ações
do cenário. O setup de provider não foi necessário nestes casos e reportou zero.

A concorrência 2 reduziu o tempo do grupo em **16,4%** e aumentou o débito observado em
**19,6%**, enquanto aumentava a duração individual. A ordem fixa dos grupos, as caches,
a carga da máquina e a dimensão da amostra limitam esta comparação. Não é uma prova
estatística de aceleração nem uma estimativa de todos os workloads do catálogo.

As 30 execuções de sucesso e seis compensações mantiveram EXACT/COMPLETE. Os scores
previstos foram 2 na base, 1 em cada subscritor isolado, 0 nas duas ordens combinadas
e 1 na compensação. As contagens das três condições, os resultados das ações e as
contagens de estados de ciclo de vida coincidiram em todas as repetições. Os IDs locais
e timestamps não foram usados como critério de igualdade entre processos.

## Implicação para pesquisa

Como ponto de partida operacional para **casos de custo semelhante**, reservar cerca
de **250 avaliações/hora com concorrência 1** ou **300/hora com concorrência 2** deixa
aproximadamente 20% de margem face a esta amostra. É uma margem de planeamento, não um
intervalo de confiança ou uma garantia. Assim, 100 avaliações sugerem cerca de 24/20
minutos; 1000 sugerem cerca de 4 h/3 h 20 min, respetivamente, mais preparação nova e
eventuais falhas. Estas estimativas não impõem qualquer limite ao runner.

A execução sequencial é uma base simples para iniciar a comparação determinística/
aleatória. A concorrência 2 pode aumentar o débito quando a máquina tem margem. O
próximo passo é escolher workloads úteis para a pesquisa e medir uma pequena amostra
desses workloads antes de fixar o orçamento comparável das baselines e do GA. A
seleção presente mede repetição e custo; não explora nem qualifica todo o espaço.

## Prova e limites

- [Resultados das 36 tentativas](../../../docs/verifiers-impl/evidence/batch-execution-2026-09-06/results.json),
  [tempos](../../../docs/verifiers-impl/evidence/batch-execution-2026-09-06/measurements.json) e
  [plano anterior ao lançamento](../../../docs/verifiers-impl/evidence/batch-execution-2026-09-06/plan.json).
- [12 testes Python](../../../docs/verifiers-impl/evidence/batch-execution-2026-09-06/unit-tests.log)
  passaram: timeout, falha de processo/lançamento, relatório ausente/malformado,
  identidade incompatível, zero válido, divergência, alterações de pacote/JARs,
  preservação e revalidação de artefactos. Nenhum código Java mudou; não se apresentam
  XML Maven antigos como prova nova.
- Um [teste real de timeout Docker](../../../docs/verifiers-impl/evidence/batch-execution-2026-09-06/timeout-check.json)
  terminou em 2,16 s e não deixou o seu container. Não executou um cenário e está fora
  do denominador de desempenho. Duas
  [verificações adicionais da versão final do runner](../../../docs/verifiers-impl/evidence/batch-execution-2026-09-06/final-check-results.json)
  — sucesso e compensação, em paralelo — também passaram; não foram misturadas nas 36.
- [Proveniência e verificações](../../../docs/verifiers-impl/evidence/batch-execution-2026-09-06/proof.json):
  1014 fontes, 2669 ficheiros do build preparado, manifesto e oito artefactos de pacote,
  306 dependências Maven, imagem e launcher. As verificações antes/depois passaram;
  os JARs simulator/verifier da cache coincidem com os do build preparado.
- Os relatórios completos e logs permanecem em `verifiers/target/batch-execution/`.
  O [inventário de hashes](../../../docs/verifiers-impl/evidence/batch-execution-2026-09-06/artifact-hashes.json)
  permite verificar a sua identidade. A evidência compacta em Git ocupa cerca de 203 KiB.
  A seleção executável completa e o build permanecem locais; para outro checkout sem
  esses artefactos é necessário preparar e congelar uma nova seleção compatível.

Não houve alteração à métrica, política de impacto, anomalias, GA, Quizzes, nota da
reunião, IDE ou containers alheios. A nota pessoal não versionada foi preservada. O
commit é local na branch `fault-analysis/scenarios`; não houve push, merge ou PR.
