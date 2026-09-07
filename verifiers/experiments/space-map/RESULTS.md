# Mapa estrutural de espaços adicionais — 7 de setembro de 2026

**Concluído:** oito workloads adicionais, 159 execuções de descoberta e 16 repetições
estáveis, com 175 IDs de tentativa distintos. O melhor candidato adicional de
calibração é AddParticipant/SolveQuizAsync; a interpretação do seu residual DELETED
e os limites de diversidade ficam explícitos abaixo.

## Resultado da descoberta

As **159 primeiras execuções** produziram 119 avaliações COMPLETE: **111 com score
zero e oito com score um**. As restantes 40 execuções foram incompletas, todas com
avaliação INVALID e score nulo. O setup terminou com sucesso e os bindings formaram
uma rede de objetos partilhados nas 159 tentativas.

Depois de agrupar IDs com o mesmo vetor e as mesmas ações, foram tentadas **117
sequências distintas**: 94 COMPLETE, das quais 88 com zero e seis positivas; 23
inválidas. Os positivos são seis sequências em dois workloads, não seis defeitos.

| Grupo | IDs COMPLETE / inválidos | Sequências COMPLETE / inválidas | IDs positivos / sequências positivas |
|---|---:|---:|---:|
| w01 | 8 / 6 | 6 / 3 | 0 / 0 |
| w02 | 4 / 6 | 3 / 3 | 0 / 0 |
| w03 | 4 / 6 | 3 / 3 | 0 / 0 |
| w04 | 8 / 4 | 6 / 2 | 0 / 0 |
| w05 | 7 / 10 | 6 / 6 | 0 / 0 |
| w06 | 24 / 8 | 22 / 6 | 0 / 0 |
| w07 | 32 / 0 | 24 / 0 | 4 / 3 |
| w08 | 32 / 0 | 24 / 0 | 4 / 3 |

`DELETED_DEPENDENCY` e `UNRESOLVED_DELIVERED_EVENT` ficaram a zero em todas as
avaliações COMPLETE. Os oito scores um vêm exclusivamente de
`FAILED_OPERATION_RESIDUAL`, num único `SagaQuizAnswer` por tentativa. Os controlos
sem falha dos seis grupos com eventos foram inválidos por
`SELECTED_SUBSCRIBER_NOT_FOUND`. Os casos que completaram nesses grupos tiveram
score zero. Nas comparações amostradas com o mesmo vetor, diferentes recuperações
não produziram diferenças de score ou validade; isto não cobre as recuperações
ainda não tentadas.

### O sinal novo e a sua utilidade para pesquisa

Em **w07**, o controlo sem falha é SUCCESS/EXACT. Os três vetores positivos são
`000000001`, `010000001` e `100000001`: todos falham em `solveQuizStep`, depois da
criação de QuizAnswer. As compensações previstas concluem, mas o objeto fica
persistido como DELETED, face à ausência no baseline. A métrica atual conta essa
diferença. O resultado não demonstra, por si só, um defeito funcional nem sinergia
entre as duas Sagas.

O universo fixo de w07 tem **três positivos em 24 vetores** e foi coberto integralmente
ao nível das ações. Neste universo, o sinal reduz-se à falha no último passo de
SolveQuizAsync: há uma minoria positiva, mas não um landscape de recuperação complexo.
Não se generaliza esta frequência a outros inputs, schedules ou aplicações.

Em **w08**, os três vetores positivos amostrados são `00000000001`, `00010000001`
e `10010000001`, com o mesmo tipo de residual. O controlo sem falha termina
PARTIAL_COMPENSATED/DEVIATED: AddStudent encontra um estudante já inscrito pelo setup.
A avaliação é COMPLETE pelo contrato atual, mas este controlo é menos adequado
para atribuir efeitos à interação pretendida das três Sagas. Os outros 48 vetores
canónicos não foram avaliados.

**Recomendação concreta:** usar w07 — workload
`8d4c46bcb8700b2fd70e4911d4a5730f6d374a185c2b307ef629d3b5f55dccf0`, nove passos,
sem eventos, 24 sequências canónicas — como próximo universo adicional de
calibração da pesquisa, com uma identidade por sequência de ações. O benchmark
RemoveTournament/AddParticipant continua útil como referência separada. Estes
espaços pequenos não sustentam ainda uma comparação convincente entre GA e métodos
simples. Antes de uma avaliação maior, importa qualificar controlos/recetores e
resolver a equivalência de IDs; a relevância do residual DELETED para o objetivo
de defeitos deve ser mantida explícita. Não foi implementado GA nem alterada a métrica.

## Seleção e universo medido

A seleção foi estrutural e anterior aos novos scores. O gerador produziu 3 820
workloads, abaixo do limite global de 50 000, e 11 086 cenários iniciais. Existiam
1 999 workloads com duas ou três Sagas, setup derivado de fonte materializável e
cenários eager, distribuídos por 20 combinações de Sagas. Foram escolhidas oito
combinações, incluindo inscrição, atualização de nomes e resolução de quizzes,
além de anonimização e remoção de estudantes.

O catálogo é deliberadamente limitado a **10 inputs por Saga**: a contabilidade
regista 899 inputs encontrados, 218 aceites, 591 excluídos por esse limite,
86 pelo modo de origem e quatro pela política de inputs. Portanto, não representa
todos os inputs Quizzes. A seleção atravessa combinações distintas e não usa um
prefixo global de workloads; o viés do limite de inputs continua a existir.

Dentro de cada combinação, preferiu-se o maior número de entregas de evento e
escolheu-se um workload entre IDs ordenados, com seed `9072026 + índice do grupo`.
Essa preferência não garante subscritores elegíveis em runtime. Nenhum caso é
substituído depois de falhar. Cada workload mantém setup, inputs, schedule normal
e entregas fixos. Os workloads derivados podem recombinar ocorrências de métodos
do mesmo contexto de teste; não são apresentados como a reprodução textual de um
único método de teste.

| Grupo | Participantes | Passos / eventos | Vetores pedidos / canónicos | IDs gerados / medidos | Sequências distintas geradas / tentadas |
|---|---|---:|---:|---:|---:|
| w01 | AddStudent + RemoveStudentFromCourseExecution | 4 / 2 | 9 / 9 | 14 / 14 | 9 / 9 |
| w02 | AddStudent + UpdateStudentName | 3 / 2 | 6 / 6 | 10 / 10 | 6 / 6 |
| w03 | UpdateStudentName + AddParticipant | 3 / 2 | 6 / 6 | 10 / 10 | 6 / 6 |
| w04 | RemoveStudentFromCourseExecution + UpdateStudentName | 3 / 2 | 6 / 6 | 12 / 12 | 8 / 8 |
| w05 | GetCourseExecutionById + UpdateStudentName + AddParticipant | 4 / 2 | 12 / 12 | 17 / 17 | 12 / 12 |
| w06 | AnonymizeStudent + GetCourseExecutionById + RemoveStudentFromCourseExecution | 5 / 2 | 18 / 18 | 42 / 32 | 36 / 28 |
| w07 | AddParticipant + SolveQuizAsync | 9 / 0 | 24 / 24 | 34 / 32 | 24 / 24 |
| w08 | AddStudent + AddParticipant + SolveQuizAsync | 11 / 0 | 24 / 72 | 36 / 32 | 24 / 24 |

Os 105 pedidos foram aceites pelo serviço existente e produziram **125 sequências
de ações**, sem atingir o cap 10 000. Juntamente com os cenários eager, o pacote
contém **175 IDs** para esses vetores. Os primeiros cinco grupos
foram enumerados integralmente; nos restantes escolheu-se um cenário por vetor e
preencheu-se o orçamento 32 com amostragem sem reposição. Todos os vetores sem
falha e com uma falha isolada foram preservados. São vetores canónicos com no
máximo uma falha por participante, não todos os vetores binários.

Em w08, os outros 48 vetores canónicos não foram pedidos: 24 sequências não é o
tamanho do universo integral desse workload. Dezasseis IDs gerados ficaram sem
execução (10 em w06, dois em w07 e quatro em w08). A cobertura de sequências é
**117/125**: as oito sequências ainda não medidas pertencem a w06. A amostra é
estratificada por vetor e por ID, não uniforme sobre as sequências possíveis.
Cobertura significa pelo menos uma tentativa; uma execução pode interromper-se
antes de cumprir todas as ações.

### IDs diferentes com as mesmas ações

A seleção original deduplicava por ID. Durante a campanha verificou-se que 50
pares de IDs eager/on-demand têm o mesmo workload, vetor e sequência compacta de
ações. Os 159 IDs selecionados correspondem a 117 sequências distintas; 42
execuções de descoberta têm, por isso, ações equivalentes a outro ID selecionado.
Estas não são apresentadas como diversidade adicional. A equivalência foi detetada
depois da seleção: mantiveram-se o protocolo e todas as execuções, acrescentando
uma segunda contagem sem alterar o pacote ou substituir casos.

O agrupamento conserva a identidade do workload, o vetor e a ordem integral das
ações; apenas ignora o ID do FaultScenario. Os relatórios dos IDs equivalentes são
comparados separadamente. Em w07 e nos 24 vetores pedidos de w08, essa análise
mostra cobertura completa das sequências, apesar da amostragem de IDs.

## Contrato de leitura dos resultados

Uma avaliação COMPLETE pode acompanhar SUCCESS, COMPENSATED ou PARTIAL_COMPENSATED,
com conformidade EXACT ou DEVIATED. Não significa que todas as Sagas terminaram
com sucesso. Apenas COMPLETE tem score numérico; os restantes resultados mantêm
score nulo. A distribuição das três condições usa apenas avaliações COMPLETE.
Um positivo conta um FaultScenario, não um defeito distinto nem gravidade.

O runner conserva a saída não-zero como PROCESS_FAILURE. A análise adicional
valida os relatórios que o executor conseguiu produzir e distingue execução
inválida, avaliação INVALID, falha do setup, ausência de subscritor e falha de
domínio não injetada. Nenhum destes resultados é convertido em zero.

As repetições são separadas da descoberta: uma execução adicional de até três
representantes por grupo, escolhidos pelo menor ID de cenário em cada assinatura
distinta de estado/score/condições, conforme a regra congelada. A comparação inclui
o resumo semântico dos relatórios e o motivo de paragem. Não prova repetibilidade
de cenários que só foram executados uma vez.

## Repetibilidade, custo e prova

As **16 repetições** coincidiram com as assinaturas semânticas e motivos de paragem
originais: oito zeros, dois positivos e seis execuções inválidas. São dois
representantes por workload. As 42 comparações adicionais entre IDs com ações
equivalentes também coincidiram. Isto qualifica esses representantes e equivalências,
sem demonstrar repetibilidade dos restantes cenários executados uma única vez.

A geração levou **23,88 s** e a preparação on-demand **224,08 s**. A campanha,
incluindo as verificações finais, levou **2604,49 s — 43 min 24 s**. A mediana por
tentativa foi **28,26 s**, com intervalo **16,90–51,41 s**. Compilação foi reutilizada;
os tempos incluem arranque, leitura do pacote, setup, execução e carga concorrente
do host. Não isolam o custo das ações nem constituem uma previsão geral de throughput.

Não houve timeout ou erro de validação de relatório. As 40 saídas não-zero da
descoberta correspondem aos hard stops de evento documentados, com relatórios
INVALID válidos, não a score indisponível convertido em zero. A descoberta teve
83 conformidades EXACT, 36 DEVIATED e 40 INCOMPLETE. As avaliações COMPLETE
incluem SUCCESS, COMPENSATED e PARTIAL_COMPENSATED, conforme o contrato.

Passaram **24 testes Python distintos**: 12 de amostragem/análise e 12 de regressão
do runner reutilizado, além da validação real dos 105 pedidos e dos 175 conjuntos
de relatórios. Build, pacote, scripts medidos e dependências foram verificados
antes/depois; a imagem manteve a mesma identidade nas leituras de início e fim.

## Versão e evidência

A campanha usa o build preparado de `combined-event-deliveries/run-02`, com fontes
de produção iguais às de `ebea5526d`. A única diferença de fonte no início era o
teste posterior `GroovyClosureBoundarySpec`. O novo diagnóstico de leituras de
criações compensadas, implementado noutra tarefa, não pertence à versão medida.
Build, pacote e dependências são verificados antes/depois; alterações posteriores
do checkout ficam identificadas separadamente.

Não se alterou Java de produção, domínio Quizzes, condições ImpactV2, score ou
política de anomalias. `RequestBatch.java` é apenas um lançador experimental do
serviço on-demand existente. Cada tentativa usa container, JVM e H2 novos, com
concorrência dois e timeout de 120 segundos. Não há GA nem adaptação da descoberta
ao score observado.

Os logs, relatórios completos, pacote, planos e hashes estão em
`verifiers/target/space-map/`. A [evidência compacta](../../../docs/verifiers-impl/evidence/space-map-2026-09-07/map.md)
inclui a [seleção](../../../docs/verifiers-impl/evidence/space-map-2026-09-07/selection.json),
[avaliações e repetições](../../../docs/verifiers-impl/evidence/space-map-2026-09-07/assessment-summary.json),
[tentativas](../../../docs/verifiers-impl/evidence/space-map-2026-09-07/full-attempts.csv)
e [prova de integridade](../../../docs/verifiers-impl/evidence/space-map-2026-09-07/proof.json).
A reprodução está no README; SPEC/PLAN/HANDOFF e as
[propostas de qualificação](../../../issues/2026-09-07-space-map/FINDINGS.md) ficam na issue.

## Limites de qualificação observados

O caso sem falha de w01 materializa o setup e liga as duas Sagas à mesma Execution
e ao mesmo User. Contudo, o setup já inscreveu o estudante; a ação `enrollStudentStep`
do participante AddStudent volta a tentar inscrevê-lo e produz uma falha de domínio
não injetada. Mais tarde, uma entrega termina com `SELECTED_SUBSCRIBER_NOT_FOUND`.
O resultado é execução incompleta e avaliação INVALID, não score zero. A partilha
de objetos é real, mas isso não basta para obter um controlo sem falha útil.

São duas fronteiras diferentes a qualificar em trabalho futuro: o estado inicial
produzido quando uma ação de setup também é promovida a participante, e a existência
de um recetor elegível para **cada** entrega escolhida. Este trabalho não removeu
ações do setup, não fabricou objetos recetores e não alterou a política de eventos
para fazer os casos passar. Uma mudança nesses contratos requer uma proposta própria.

## Corrective qualification — 7 September 2026

The approved follow-up keeps every original result above immutable. It corrects the
generic setup boundary for an exact facade call that is both in `setup()` and selected
as a measured participant: only the strict prefix before that call is replayed, and the
prefix may bind compatible later participants from the same source class.

The regenerated `w01` equivalent confirms the distinction. Its setup creates the course
execution and user and activates the user, but does not enroll the user. Setup succeeds,
AddStudent and RemoveStudent complete all four forward steps, and only then does the first
QuizAnswer delivery stop with `SELECTED_SUBSCRIBER_NOT_FOUND`. The earlier unassigned
`already enrolled` failure is removed; the receiver gap is not hidden.

A source-backed `UpdateStudentName -> Tournament` control completed `SUCCESS / EXACT`.
Its single trigger-fault variant completed `COMPENSATED / EXACT` and reported the event
as `MASKED_BY_TRIGGER_FAULT`. This proves the selected route when its receiver is present.
It does not imply that the missing receivers in `w01`–`w06` can be synthesized from their
different fixtures. `w08` remains without one coherent setup because its selected setup
targets cannot all be preceded by a single replayable prefix.

The six original event groups account for all forty INVALID attempts: 6/6/6/4/10/8 in
`w01`–`w06`. Besides the corrected repeated AddStudent state in `w01` and `w02`, `w05`
has its independent unassigned AddParticipant invariant failure. Every group also selects
at least one route whose QuizAnswer or Tournament receiver is absent. The compact
[qualification evidence](../../../docs/verifiers-impl/evidence/space-map-setup-qualification-2026-09-07/map.md)
records the regenerated IDs, package/report hashes, and bounded three-run Docker proof.
