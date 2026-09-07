# Leitura de uma criação de Saga depois compensada

Investigação documental de 2026-09-07, com código inspecionado em
`ebea5526d419141e0511757d9e7e5e2f7ad0d1f5`. **Proposta; implementação não aprovada.**
O [SPEC](SPEC.md) e o [PLAN](PLAN.md) delimitam a primeira fatia recomendada.

## Conclusão

É viável observar genericamente **uma resposta que entrega a B a revisão criada por
A, seguida de uma eliminação efetivamente persistida pela compensação dessa criação**.
O ponto de observação recomendado é o retorno bem-sucedido de
`LocalCommandGateway.send`, incluindo o resultado depois de desserializado. Faltam um
contrato tipado de extração da proveniência da resposta e a junção diagnóstica; os hooks
atuais não fornecem essa prova. A regra e o coletor podem ser independentes da aplicação;
os contratos de projeção dos seus DTOs precisam de adaptadores explícitos e testados.

Isto prova exposição a um efeito posteriormente compensado. Não exige que B escreva
ou sofra dano, não altera ImpactV2 e não classifica uma simples mudança posterior como
anomalia. A escrita de A já confirmou uma **transação local**: a Saga ainda não terminou.
Não se deve chamar a isto dirty read clássico sem mudar explicitamente essa fronteira.

## Percurso real e cobertura

Os caminhos abaixo são relativos a `src/main/java` dos módulos indicados.

| Fronteira inspecionada | Facto e consequência |
| --- | --- |
| simulator: `…/ms/transaction/sagas/aggregate/SagaAggregateRepository.java` | `findNonDeletedSagaAggregate` escolhe a revisão máxima do ID e rejeita a revisão DELETED; não usa a versão da UnitOfWork como snapshot de leitura. A consulta admite INACTIVE. |
| simulator: `…/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java` | `aggregateLoadAndRegisterRead` regista acesso READ, mas `aggregateLoad` não o faz e `registerRead` apenas devolve o agregado. `UnitOfWork.version` é mutável e não é a versão lida. |
| Quizzes: `…/microservices/quiz/service/QuizService.java#getQuizById`, `…/quiz/aggregate/QuizDto.java` | A transação SERIALIZABLE lê o agregado e a fábrica constrói um DTO que copia o ID e a **versão do Quiz**. `SagasQuizFactory.createQuizDto` delega no construtor. Não há um `AggregateDto` comum nestes DTOs. |
| simulator: `…/ms/transaction/sagas/messaging/SagaCommandHandler.java` | Verifica apenas os estados proibidos configurados, chama o serviço e, depois, regista o semantic lock pedido. Uma falha nessa parte ainda impede a resposta. Observar só a construção do DTO seria prematuro. |
| simulator: `…/ms/messaging/local/LocalCommandGateway.java`, `LocalCommandService.java`, `CommandResponse.java` | Há retorno direto e retorno JSON. O segundo desserializa o resultado, combina a UnitOfWork e verifica o erro antes de devolver. O hook proposto observa este resultado final, não apenas o objeto escolhido na base de dados. `application-test.yaml` de Quizzes ativa serialização. |
| Quizzes: `…/answer/coordination/sagas/StartQuizFunctionalitySagas.java`, `…/answer/service/QuizAnswerService.java#startQuiz` | `getQuizStep` guarda o DTO no workflow. Mais tarde, `startQuizStep` envia-o ao serviço de respostas, que constrói `AnsweredQuiz` sem voltar a consultar o Quiz. É cache de aplicação; o registo da entrega original basta para a exposição, mas não prova que campos B usou depois. |
| Quizzes: `QuizService#getAvailableQuizzes`, `TournamentService#getOpenedTournamentsForCourseExecution`/`getClosedTournamentsForCourseExecution`, `ExecutionService#getCourseExecutionsByUserId` | Usam consultas/listas, `aggregateLoad`, filtros e `registerRead`; nem todos os acessos aparecem no hook READ. Consultas de existência, como `QuizAnswerRepository.existsByQuizIdAndStudentId`, também escapam. São leituras de predicados, não necessariamente revisões devolvidas. |
| Quizzes: `…/tournament/aggregate/TournamentDto.java`, `TournamentQuiz.java#buildDto` | O DTO exterior copia a revisão do Tournament. O Quiz aninhado copia o ID mas atribui a si próprio a versão ainda nula. Não é possível inferir uma leitura exata do Quiz a partir dessa resposta nem do ID aninhado. Esta proposta não corrige o mapeamento. |

Não foram encontradas anotações de cache de aplicação nos fontes inspecionados. Isso
não prova ausência de caches Hibernate ou de futuros percursos diretos. Um retorno
tipado com revisão histórica pode ser observado como tal; uma reutilização posterior
do DTO sem nova chamada não cria outra leitura. Consultas diretas, escalares, listas,
projeções compostas, predicados, eventos e gRPC/stream ficam fora da primeira cobertura.
O carregamento interno de um agregado também pode alimentar uma escrita ou um DTO
diferente: não é, sozinho, prova do resultado entregue.

## Escrita, identidade e compensação

`SagaUnitOfWorkService.registerChanged` atribui a revisão e regista um callback
`afterCommit`. `PersistentStateObserver.snapshotVersion` recarrega essa revisão exata
em `REQUIRES_NEW`. `ImpactEvidence` já conserva identidade lógica, revisão, runtime
type, predecessor e autor. `ImpactWriterContext` distingue tentativa, participante,
ação e fases `FORWARD`/`RECOVERY`; eventos têm autoria própria. Os nomes das classes
ou o relógio/versionamento da UnitOfWork não substituem estas identidades.

`ScenarioExecutionReport.ActionOutcome` fornece checkpoint, `sourceScheduledStepId`,
ocorrência e subresultados da recuperação. Permite ligar uma escrita RECOVERY à
compensação do passo produtor, em vez de assumir que qualquer escrita da mesma Saga
desfaz todas as anteriores. A junção proposta exige ainda tombstone confirmado cujo
predecessor seja **a revisão criada e entregue**, com ausência anterior demonstrada.
Revisão concorrente/intermédia, predecessor ambíguo ou escrita não confirmada tornam
o candidato desconhecido. Não se usa a hipótese de que versões consecutivas diferem em 1.

O rollback implícito/commit de Saga altera semantic locks por `entityManager.merge`,
sem passar por `registerChanged` nem criar revisão de dados. Libertar um lock ou concluir
um checkpoint não prova inversão do efeito. As leituras do `PersistentStateObserver`
são consultas JPA próprias; devem continuar excluídas, com escopo explícito de observador
mesmo que no futuro passem pelo gateway.

## Evidência Quizzes e limite de execução

Reinspecionaram-se os seis JSON existentes de `verifiers/target/impact-three-cases/run-*`
e os testes/código que os produziram. Não houve nova execução Docker/Maven nesta tarefa.
Todos os checks guardados estão positivos. Uma verificação read-only adicional confirmou
as junções criação/revisão devolvida, o predecessor da eliminação e a ordem dos positivos,
bem como os controlos de sucesso. Isto é uma auditoria de evidência anterior,
não uma requalificação do checkout atual.

| Caso existente | Sequência comprovada | Resultado esperado da proposta |
| --- | --- | --- |
| `create-split-start` (duas execuções) | Baseline da fatia sem Q; A cria Quiz 9/v18 sem predecessor; B recebe v18; compensação de `generateQuizStep` deixa v19 DELETED, predecessor v18 | Uma exposição; a posterior QuizAnswer ativa não é necessária para a regra |
| `create-success-overlap-control` (duas execuções) | B recebe a mesma v18; A termina com sucesso; Quiz permanece ativo | Sem finding deste padrão |
| `create-early-compensation` | A elimina Q antes de B tentar ler; a chamada de B falha | Sem entrega, sem finding |
| `remove-fault-recovery-alone` | Q já existe no baseline; uma única Saga elimina-o no forward e recupera apenas o lock | Fora do padrão; o efeito final pertence a outra análise |

O positivo guardado tem SHA-256
`6d1cd3a9a61ab285c04c0a15cf0415161ca514f5a9db3bfd039b02fc8b0d5087`
(`run-1/create-split-start.json`); o controlo de sucesso tem
`011d805ece7662d4283903824b070ccee7f877db787d1474dd487e282bd49180`.
A fonte durável do método é
[ImpactExperiment.java](../../verifiers/experiments/impact-three-cases/ImpactExperiment.java),
com resultados em [RESULTS.md](../../verifiers/experiments/impact-three-cases/RESULTS.md)
e teste `CreateTournamentStartQuizRecoveryWindowExploratoryTest` em Quizzes.

O harness obtém o ID realmente criado antes de construir B. **Não há prova de que o
catálogo/executor atual consiga materializar esta ligação entre resultado de A durante
medição e argumento inicial de B.** O executor materializa/inicia todos os participantes
antes dos passos. Não se deve adivinhar o próximo ID nem mover a criação para setup:
isso mudaria o produtor medido. A primeira prova positiva Quizzes será pelo harness
controlado existente; um `FindQuiz` após setup é apenas controlo de integração normal.
Não se demonstrou descoberta deste Quiz pela UI normal de Tournament.

## Criação eliminada versus atualização reposta

**Recomendação: começar apenas pela criação eliminada.** Dois exemplos delimitam a decisão:

- A cria Q/v18; B recebe essa revisão; a compensação do passo criador persiste Q/v19
  DELETED com predecessor v18. O efeito de existência foi desfeito sem precisar de
  uma regra de negócio sobre a utilização posterior de Q.
- A muda Tournament de duas para três perguntas; B recebe a revisão nova; a compensação
  repõe duas, mas pode alterar também tópicos/datas ou deixar outro campo diferente.
  Seria preciso definir que projeção/efeito foi lido e reposto. Uma revisão nova ou a
  igualdade de um único campo não prova compensação integral da revisão entregue.

O [experimento de updates](../../verifiers/experiments/impact-updates/RESULTS.md) demonstrou
reposição da projeção selecionada na compensação normal e ausência de reposição num
mutante no-op. Não tinha B. A qualificação posterior documentou diferenças adicionais
no estado persistente: não generalizar a igualdade daquela projeção à totalidade do
Tournament. Updates ficam `UNSUPPORTED_EFFECT_KIND`, com evidência retida, não zero.

A discussão necessária é aceitar este âmbito inicial e os adaptadores tipados de
respostas. Cobrir updates, eliminar adaptadores com nova instrumentação de proveniência,
ou produzir já este par num pacote normal aumenta o âmbito e precisa de nova decisão.
Nada aqui propõe uma política de score ou um oráculo LLM.
