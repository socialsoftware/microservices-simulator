# Primeira comparação de pesquisa — 6 de setembro de 2026

**Concluído: 114/114 avaliações COMPLETE, em processos novos, com 114 IDs de execução
distintos.** Não houve timeout, falha de infraestrutura, relatório inválido ou divergência
entre repetições comparáveis. A conformidade foi EXACT em 111 tentativas e DEVIATED em
três; ambas eram aceites pelo contrato congelado. COMPLETE descreve a avaliação, não o
sucesso de todos os participantes: houve SUCCESS, COMPENSATED e PARTIAL_COMPENSATED.

O runner agora permite avaliar candidatos sem conhecer o resultado esperado, preservando
o modo anterior de regressão. A comparação executou uma ordem determinística e cinco
permutações aleatórias sem reposição, com seeds 11, 29, 47, 71 e 101. Todos os candidatos,
ordens, horizontes e orçamentos foram congelados antes dos novos scores. Não há GA,
fitness adaptativa, reutilização de resultados como novas tentativas ou ranking global
entre workloads.

## Descoberta por orçamento

Positivo significa **ImpactV2 COMPLETE com score > 0**. Cada descoberta conta um
FaultScenario distinto; variantes de recuperação podem expor a mesma condição. Estes
números não contam defeitos distintos nem medem gravidade.

| Workload / horizonte fixo | Universo | Orçamento por estratégia | Primeiro positivo: determinística | Primeiro positivo: random, 5 seeds | Positivos no orçamento: determinística / random |
| --- | ---: | ---: | --- | --- | --- |
| RemoveTournament/AddParticipant, sem ações de evento | 29 cenários / 12 vetores | 12 | Tentativa 4 | 1–3; mediana 2 | 9 / 5–9; mediana 6 |
| CreateQuiz, três passos, sem ações de evento | 4 cenários | 4 | Não encontrado | Não encontrado nos cinco seeds | 0 / 0 |
| AnonymizeStudent, dois passos + evento selecionado | 3 cenários | 3 | Não encontrado | Não encontrado nos cinco seeds | 0 / 0 |

A [tabela completa e as curvas discretas por orçamento](../../../docs/verifiers-impl/evidence/search-baselines-2026-09-06/discovery.md)
mostram todas as tentativas até ao limite, sem parar no primeiro positivo. No benchmark:

| Estratégia | Seed | Primeiro positivo | Positivos distintos em 12 tentativas |
| --- | ---: | ---: | ---: |
| Determinística | — | 4 | 9 |
| Random | 11 | 2 | 6 |
| Random | 29 | 3 | 6 |
| Random | 47 | 1 | 5 |
| Random | 71 | 1 | 9 |
| Random | 101 | 3 | 6 |

Nesta amostra, random chegou mais cedo ao primeiro positivo; a ordem determinística
encontrou tantos cenários positivos como o melhor seed e mais do que os restantes.
Não há uma superioridade geral demonstrada. O benchmark é pequeno e conhecido, com
15 de 29 cenários positivos na referência histórica: encontrar um positivo é fácil.
Não foi escolhido um seed vencedor depois dos resultados.

## Cobertura, repetibilidade e limites

As 72 tentativas online do benchmark visitaram **26/29 candidatos**. Três ficaram por
visitar, três tiveram uma única observação e 23 tiveram duas ou mais observações,
sempre semanticamente estáveis. Não se apresentam os 14 zeros/15 scores dois históricos
como uma nova qualificação completa dos 29. A
[comparação posterior com a referência](../../../docs/verifiers-impl/evidence/search-baselines-2026-09-06/reference-check-summary.json)
verificou as 72 tentativas já realizadas: zero diferenças e **zero novas execuções**.
Os IDs e as ações dos 29 também coincidem com a referência estrutural corrigida.

Os **sete candidatos adicionais** foram executados seis vezes cada, todos COMPLETE e
com score zero. A seleção incluiu todos os cenários declarados daqueles workloads,
incluindo falhas intermédias ausentes dos anteriores pares controlo/última falha.
Os zeros foram conservados; não se substituíram workloads para obter resultados positivos.
São espaços executáveis e repetíveis, mas não discriminam uma seleção adaptativa nesta
amostra. No espaço de três candidatos, os cinco seeds produziram apenas três ordens
aleatórias distintas; todas foram executadas realmente, sem aumentar artificialmente
o número de ordens diferentes.

Os universos ficam limitados aos FaultScenarios persistidos: zero/falhas simples nos
singles; no benchmark, no máximo uma falha por participante, com variantes de recuperação
e cap 20. O orçamento 12 seleciona cenários, não uma representação garantida de cada
vetor. Os caps de geração e os 36 candidatos elegíveis estão na
[declaração congelada](../../../docs/verifiers-impl/evidence/search-baselines-2026-09-06/experiment-summary.json).
Não é uma exploração de todos os vetores binários, interleavings ou inputs do catálogo.
A entrega de evento de AnonymizeStudent não foi removida nem comparada com um horizonte
mais curto para aumentar o score.

## Custo e reprodução

A preparação do pacote levou **14,38 s**, reutilizando o build existente e pedindo apenas
os seis vetores em falta através do CLI atual. As execuções online e verificações de
integridade levaram **1317,02 s — cerca de 21 min 57 s**. A mediana por tentativa foi
23,66 s, com intervalo 16,57–29,19 s. Estes tempos incluem arranque/processo e não isolam
o custo das ações. Compilação não foi repetida.

Houve no máximo dois percursos em paralelo, cada um sequencial internamente, com
container/JVM/H2 novos por tentativa e limites de 2 CPUs/3 GiB por container. O timeout
individual foi 180 s; não houve teto temporal global nem retries. Carga, ordem de
execução e contenção tornam os tempos descritivos; a comparação principal usa orçamento
de tentativas, não tempo de parede.

Com a preparação local já retida, executar na raiz:

```sh
python3 verifiers/experiments/search-baselines/search.py run \
  --experiment verifiers/target/search-baselines/prepared/experiment.json \
  --output verifiers/target/search-baselines/run-02
```

O destino deve ser novo. O [README](README.md) documenta preparação, seeds, contratos,
resumo e limitações de reprodução sem os artefactos locais. Os relatórios completos
ficam em `verifiers/target/search-baselines/`; a evidência compacta em Git inclui
[114 linhas por tentativa](../../../docs/verifiers-impl/evidence/search-baselines-2026-09-06/attempts.csv),
[resumo e repetibilidade](../../../docs/verifiers-impl/evidence/search-baselines-2026-09-06/summary.json),
[hashes dos artefactos](../../../docs/verifiers-impl/evidence/search-baselines-2026-09-06/artifact-hashes.json)
e [prova de validação](../../../docs/verifiers-impl/evidence/search-baselines-2026-09-06/proof.json).
Passaram 13 testes de pesquisa e os 12 testes de regressão do runner.

Na revisão final foi corrigido um detalhe do wrapper: os 72 relatórios do benchmark
usam provider e omitem legalmente `sourceSetup`. Isso deixara um diagnóstico de
metadados, embora os scores já estivessem corretamente validados. Esses diagnósticos
permanecem visíveis nos artefactos/CSV. A versão final trata o campo opcional ausente
como indisponível e rejeita metadados presentes malformados sem conservar score.
Duas verificações adicionais em processos novos (provider e source setup) passaram;
são separadas das 114 tentativas da comparação. A revalidação dos 114 scores não mudou.

O snapshot medido permaneceu inalterado. Durante a campanha, outra tarefa acrescentou
um teste de caracterização ao checkout; essa diferença foi registada sem invalidar o
snapshot. Não houve alterações de produção, métrica, Quizzes, nota da reunião ou IDE.

## O que falta para um GA

1. Definir o objetivo: rapidez até ao primeiro positivo, cobertura de cenários positivos
   ou maximização do score existente são critérios diferentes, como esta comparação mostra.
2. Definir um genótipo que represente falhas **e escolhas de recuperação**, com operadores
   que produzam candidatos válidos e uma política explícita de duplicados e avaliações
   incompletas. O avaliador real e os estados de resultado já estão disponíveis.
3. Comparar a política adaptativa com estas baselines sob o mesmo orçamento e horizonte,
   num espaço suficientemente informativo. Os dois espaços planos adicionais servem
   como controlos; o benchmark pequeno e conhecido, por si só, não demonstra vantagem de GA.
