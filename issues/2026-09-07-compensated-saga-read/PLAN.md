# Plano proposto — observação de criação lida e compensada

**Pendente de decisão sobre o SPEC e aprovação explícita de implementação.**

## 1. Ambiente e modo de execução

Rota documental, execução revista por marcos. Esta investigação alterou apenas os três
documentos da issue; permite commit local no branch `fault-analysis/scenarios`. Preservar
`note-04-09-2026.md` e trabalho concorrente. Sem push, merge, PR ou worktree nesta tarefa.
Para execução futura, usar o checkout autorizado e builds de validação em snapshot
isolado; reavaliar a coexistência com a campanha antes de qualquer alteração. Não usar
targets/cache partilhados, nem `mvn clean` no verifiers principal. JDK21 e Maven por módulo.

Não há pausa de protótipo visual. O gate é aceitar o âmbito e este plano; mudanças para
updates, política de score ou geração do par com binding runtime voltam ao utilizador.

## 2. Estratégia de implementação

1. Acrescentar um contrato de adaptador tipado para resposta singular exterior e um hook
   de entrega no retorno de `LocalCommandGateway.send`. Desembrulhar apenas `SagaCommand`
   conhecido para escolher o contrato do payload; usar o resultado realmente retornado
   em ambos os modos de serialização. Não usar o ID pedido como substituto do devolvido.
2. Reutilizar o escopo de autoria do executor, as escritas `afterCommit` e os snapshots
   existentes. Capturar uma ordem de observação comum ou posições de ações/ocorrências
   que provem a ordem causal; não comparar contadores de coletores independentes.
3. Acumular factos de leitura num diagnóstico separado e juntar criação/entrega/tombstone
   ao checkpoint exato. Compor a coleta no único escopo de observador já existente,
   sem instalar um segundo holder concorrente nem duplicar consultas de escrita.
   Preservar a separação entre lacunas de leituras e as usadas pelo ImpactV2.

Os adaptadores iniciais de Quizzes declaram `GetQuizByIdCommand → QuizDto` e
`GetTournamentByIdCommand → TournamentDto`, apenas a revisão exterior. A identidade
persistente vem de mapeamento tipado explícito consistente com `PersistentStateObserver`;
colisões não se resolvem retirando o prefixo “Saga” nem adivinhando pelo nome do DTO.
Uma revisão retornada sem proveniência auditável fica desconhecida. Não corrigir serviços,
DTOs aninhados ou semantic locks para conseguir o resultado desejado.

## 3. Marcos

### M0 — Provar a entrega exata antes de construir a regra

**Resultado:** FR-1, FR-2, FR-6 e FR-7. Um retorno medido tem identidade e revisão do
objeto entregue; uma leitura interna diferente não o substitui.

**Fronteiras/âncoras:** integração de observação em
`simulator/src/main/java/…/ms/messaging/local/LocalCommandGateway.java`, contrato de
adaptador/escopo, testes de gateway e fixtures dummyapp. Os DTOs atuais de dummyapp não
têm revisão: acrescentar fixture própria positiva e preservar um DTO sem revisão como
negativo; dummyapp continua source-only, não passa a ser uma aplicação runtime.

**Prova antes de continuar:** retorno direto e JSON; wrapper Saga; fonte interna v2 mas
DTO retornado v1 (registar v1); ID/revisão ausentes; tipo ambíguo; serviço ou semantic-lock
handler falha após construir DTO (nenhuma entrega); tentativa/retry com erro seguida
de sucesso (apenas entregas reais); observador/setup sem autoria de aplicação; callback
falha sem mudar o resultado. Um teste tem nomes/campos sem convenção para excluir heurísticas.

### M1 — Diagnóstico auditável e sidecar no executor

**Resultado:** FR-3 a FR-8. O sidecar separa positivos, negativos e lacunas e não altera
nenhum score. Ativação é opt-in; leitura sem coleta de escrita produz indisponibilidade.

**Fronteiras/âncoras:** `ImpactEvidence`, `ImpactWriterContext`,
`SagaUnitOfWorkService.registerCommittedWriteObservation`, `ImpactV2EvidenceCollector`,
`ScenarioExecutor`, `ScenarioExecutionReport.ActionOutcome` e um assessor diagnóstico
separado. Não mudar a definição/catálogos de FaultScenario ou o assessor ImpactV2.

**Preflight:** verificar contra o checkout atual a junção entre ação de compensação e
ocorrência produtora, incluindo fallback runtime; referências ambíguas permanecem gaps.
Confirmar que nenhum erro específico do diagnóstico entra na lista de gaps ImpactV2.

**Prova antes de continuar:** Spock em `verifiers/src/test/groovy`, dummyapp-first, para
toda a matriz SPEC, baseline sem cobertura, rollback local, reload falhado, tombstone
com predecessor errado, escritor concorrente, checkpoint errado, atualização reposta,
read posterior à eliminação, leitor sem escrita e leitor que posteriormente compensa.
Verificar deduplicação, dois leitores, ordenação estável, restart da tentativa, prefixo
interrompido e sidecar sem substituir ficheiros de pacote/execução. Comparar ImpactV1,
ImpactV2, conformance e resultados da aplicação com o diagnóstico ligado/desligado.

### M2 — Qualificar Quizzes e documentar o limite real

**Resultado:** FR-9 e documentação de cobertura. O mesmo assessor suporta dummyapp e
Quizzes através de adaptadores, sem branches de domínio na regra.

**Fronteiras/âncoras:** integração diagnóstica/testes Quizzes, o harness existente
`verifiers/experiments/impact-three-cases/ImpactExperiment.java`, teste
`CreateTournamentStartQuizRecoveryWindowExploratoryTest`, e documentação canónica
`docs/verifiers-impl/current-state.md`/`roadmap.md`. Adicionar o termo ao glossário e
retirar `(future)` apenas quando implementado. Não editar a nota pessoal/reunião.

**Prova antes de concluir:** em JVM/Spring/H2 novos via Docker/snapshot, repetir positivo,
compensação antes da leitura e sucesso de A. Acrescentar A cria → B `FindQuiz` recebe e
termina sem escrever → A compensa. Este último é caminho executável sustentado pelo
código, **ainda não reproduzido nesta investigação**. Observar o DTO no retorno genérico,
conferindo-o com o DTO retido no workflow e as revisões persistidas. Validar o lookup
exterior de Tournament e a referência Quiz aninhada sem revisão como caso não coberto.

Demonstrar escrita do sidecar pelo executor normal com um workload já executável
(`FindQuiz` sobre resultado de setup serve de controlo, não positivo de A). A prova
positiva usa o harness para fornecer a B o ID realmente produzido; não alegar que é um
FaultScenario normalmente gerado. Publicar hashes, fontes/versões, logs e checks num
HANDOFF. Não é condição desta fatia reparar o binding runtime do catálogo.

## 4. Validação e custo

Usar primeiro os testes de gateway/observador do módulo simulator e os novos Spock
focados em verifiers; depois a composição Quizzes. Reutilizar o procedimento Docker
da investigação existente num snapshot coordenado, sem competir com a campanha.
Só correr regressão mais ampla perante alterações que a justifiquem.

Medir a diferença ligado/desligado em execuções equivalentes, com warmup declarado e
repetições fixadas antes de olhar os tempos. Reportar duração, número de respostas,
escritas, bytes do sidecar e memória retida. Expectativa a validar: trabalho linear no
número de factos, índices por identidade/revisão, sem consulta extra por leitura; a
coleta de escritas existente continua a dominar o custo de persistência. Não há ainda
medição deste mecanismo nem orçamento numérico aprovado. Um limite de retenção deve
produzir `PARTIAL/TRUNCATED`, nunca descartar factos silenciosamente.

## 5. Riscos e alternativas

- **DTO com revisão enganadora ou incompleta:** adaptador é contrato de integração,
  não prova automática de linhagem de campos. Testes confrontam fonte, DTO e resposta
  desserializada; sem contrato fiável não há finding. Evitar expansão reflexiva do grafo.
- **Sem um hook universal:** os bypasses declarados impedem uma conclusão global.
  É aceitável `COMPLETE_WITHIN_SCOPE` com âmbito explícito, nunca “todas as leituras”.
- **Compensação parcial/escritas intermédias:** exigir tombstone e predecessor direto
  ligados ao checkpoint produtor; conservar unknown. Não adicionar heurísticas de igualdade.
- **Interferência no observador atual:** opt-in, composição de callback e equivalência
  dos resultados/ImpactV2 são critérios de aceitação; recuar a integração se falharem.
- **Catálogo não liga o ID produzido a B:** conservar prova controlada e controlo de
  integração separado. Implementar um novo mecanismo de inputs requer outro plano.

Checkpoint: aprovar apenas o diagnóstico de criação eliminada com adaptadores tipados,
o hook no resultado final e esta prova em três marcos. A implementação continua pendente.
