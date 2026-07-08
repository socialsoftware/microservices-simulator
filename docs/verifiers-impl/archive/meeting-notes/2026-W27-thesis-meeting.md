# Notas da reunião de tese — 2026-W27

## Objectivo da reunião

Fazer uma revisão completa do trabalho do verificador/scenario generator até agora: o que estava planeado no capítulo da Solução do PIC2, o que foi efectivamente implementado, como as peças encaixam, o que ainda falta, quais são os riscos principais, e qual deve ser o próximo marco da tese.

Orientação já recebida na reunião anterior:

> Devemos avançar para as próximas fases da tese — execução, injecção de falhas, análise de impacto e eventualmente ML — mesmo que parte da cobertura e genericidade do verificador fique incompleta.

Pedido principal ao orientador nesta reunião:

> Validar o menor corte executável e avaliável para chegar a algo semelhante aos objectivos originais até Outubro, sem continuar preso a melhorar indefinidamente a análise estática.

## Guião proposto para 1 hora

| Tempo | Tópico | Objectivo |
|---:|---|---|
| 0–5 min | Objectivo e enquadramento | Alinhar a reunião: zoom-out, não só debugging. |
| 5–15 min | Plano PIC2 vs estado actual | Mostrar a diferença entre a arquitectura planeada e o que existe. |
| 15–25 min | Principais resultados | Defender as contribuições já implementadas com evidência. |
| 25–35 min | Lacunas e riscos | Explicar honestamente o que ainda não existe e o que está difícil. |
| 35–55 min | Walkthrough técnico por módulo | Mostrar pipeline, artefactos, classes/ideias principais e snippets. |
| 55–60 min | Decisões a pedir | Fechar com perguntas concretas ao orientador. |

## Frase inicial sugerida

> O plano original do PIC2 era uma pipeline autónoma de teste de resiliência: geração estática de cenários, execução com injecção de falhas, cálculo de impacto, pesquisa local com GA e priorização com bandit/RL. O que está implementado até agora é sobretudo a primeira metade: extracção estática, catálogos determinísticos, redução do espaço de schedules, enriquecimento dinâmico e um POC estreito de execução. O risco actual é transformar cenários gerados em experiências executáveis e repetíveis de injecção de falhas.

## 1. Plano original do PIC2

O capítulo da Solução propunha uma pipeline em camadas:

```text
Descrição da aplicação + testes happy-path
        ↓
Scenario Generator
        ↓
Execução de cenários + injecção de falhas
        ↓
Análise de impacto
        ↓
Pesquisa local com Genetic Algorithm
        ↓
Priorização de cenários com Contextual Bandit / RL
```

### Requisitos principais do plano original

- Suportar fault injection em uma ou mais Sagas intercaladas.
- Gerar automaticamente cenários interessantes a partir das interacções entre Sagas.
- Descobrir cenários prejudiciais: violações de invariantes, excepções não tratadas, compensações incompletas ou divergência de estado.
- Ser mais eficiente do que brute force/exploração exaustiva.
- Produzir resultados reprodutíveis.
- Ser agnóstico ao domínio da aplicação, usando estrutura, passos, agregados, inputs e evidência de execução em vez de regras hardcoded.

### Decomposição conceptual proposta

- **Nível alto:** escolher que cenários vale a pena explorar.
  - Planeado: Contextual Multi-Armed Bandit / LinUCB.
- **Nível local:** dentro de um cenário fixo, escolher que combinações de falhas testar.
  - Planeado: Genetic Algorithm sobre bit vectors de falhas.
- **Base necessária:** gerar cenários e executá-los de forma reprodutível.
  - Esta é a parte onde a implementação actual mais avançou.

## 2. Estado actual em uma imagem

```text
Código Java/Groovy da aplicação
        ↓
Extracção estática do verificador
        ↓
Modelo de Sagas, passos, inputs, footprints e conflitos
        ↓
Catálogo determinístico de cenários + accounting
        ↓
Enriquecimento dinâmico opcional com evidência runtime
        ↓
ScenarioExecutor POC para casos single-saga suportados
        ↓
Futuro: execução genérica → fault injection → impact scoring → GA → bandit
```

Resumo honesto:

- A geração estática de cenários está implementada como MVP funcional.
- O enriquecimento dinâmico existe como evidência sidecar.
- A redução do espaço de schedules por segment compression está implementada.
- Existe um ScenarioExecutor POC estreito.
- Ainda não existe execução genérica, multi-saga execution, fault injection gerado, impact scoring, GA ou bandit.

Fontes actuais de verdade:

- [`../../advisor-brief.md`](../../advisor-brief.md)
- [`../../current-state.md`](../../current-state.md)
- [`../../roadmap.md`](../../roadmap.md)
- [`../../evidence.md`](../../evidence.md)
- [`../../reference/scenario-executor.md`](../../reference/scenario-executor.md)

## 3. O que foi implementado

### 3.1 Extracção estática de Sagas e passos

O verificador analisa código de produção Java e testes Groovy/Spock para construir uma representação estática da aplicação.

Consegue extrair:

- Sagas/functionality classes.
- Passos ordenados de workflows.
- Dependências entre passos.
- Dispatch footprints.
- Handlers de comandos.
- Acessos a serviços/repositórios.
- Fases forward/compensation quando detectáveis.
- Topologia de eventos para o shape implementado `EventHandling`/`EventProcessing`.

Claim a defender:

> O verificador já consegue obter estrutura de execução de Sagas a partir de código real, não apenas de fixtures pequenas.

### 3.2 Extracção de inputs a partir de testes

O verificador tenta transformar testes happy-path em variantes de input para cenários.

Implementado:

- Indexação de código Groovy.
- Tracing de construtores de DTOs.
- Detecção de chamadas a facades/functionality methods.
- Tracing através de helper methods.
- Receitas de input orientadas a replay.
- Metadados de ownership/proveniência.
- Classificação de source mode: `SAGAS`, `TCC`, `MIXED`, `UNKNOWN`.

Política actual:

- `SAGAS`: aceite.
- `TCC`: rejeitado diagnostically.
- `MIXED`: rejeitado diagnostically.
- `UNKNOWN`: aceite com warning.

Ponto importante:

> Um input aceite estaticamente não significa que esteja pronto a ser executado. Pode ainda conter placeholders, valores runtime ou dependências Spring que o executor não consegue materializar.

### 3.3 Catálogo determinístico de cenários

O verificador gera artefactos machine-readable para fases futuras.

Artefactos principais:

- `scenario-catalog.jsonl`
- `scenario-catalog-manifest.json`
- `rejected-inputs` / diagnósticos de inputs rejeitados
- `scenario-space-accounting.json`
- relatório HTML para leitura humana

Propriedades importantes:

- IDs determinísticos de `ScenarioPlan`.
- Ordenação estável.
- Modo `COUNT_ONLY` para medir espaços grandes sem materializar todos os planos.
- Separação entre catálogo estático e evidência dinâmica.

Claim a defender:

> O catálogo é o contrato estático. A evidência dinâmica pode enriquecê-lo, mas não redefine a estrutura estática dos cenários.

### 3.4 Geração de cenários e accounting

O verificador gera:

- cenários single-saga;
- candidatos multi-saga bounded;
- evidência de conflito por agregados/footprints;
- contagens do espaço de schedules.

Estratégias relevantes:

- `SERIAL`
- `ORDER_PRESERVING_INTERLEAVING`
- `SEGMENT_COMPRESSED`

Resultado importante em Quizzes:

```text
ORDER_PRESERVING_INTERLEAVING selected total: 218528454
SEGMENT_COMPRESSED selected total: 1019393
```

Interpretação:

> A segment compression reduz substancialmente o espaço seleccionado sob a evidência estática de conflitos. Isto não prova completude semântica; é uma redução estática baseada nos conflitos que o verificador conseguiu extrair.

### 3.5 Enriquecimento dinâmico

O enriquecimento dinâmico corre testes reais da aplicação com hooks do simulador activos e junta a evidência runtime ao catálogo estático.

Implementado:

- Orquestração opcional pelo verificador.
- Execução de um batch Maven com as test classes seleccionadas.
- Escrita de `dynamic-input-map.json` antes do batch dinâmico.
- Emissão runtime de `inputVariantId` quando há correspondência exacta.
- Join conservador com estados como:
  - `MATCHED_EXACT`
  - `MATCHED_HIGH_CONFIDENCE`
  - `MATCHED_PARTIAL`
  - `AMBIGUOUS`
  - `UNMATCHED`
  - `NOT_COVERED`
- Artefactos enriquecidos sidecar:
  - `scenario-catalog-enriched.jsonl`
  - `scenario-catalog-enriched-manifest.json`
  - `dynamic-evidence-join-report.json`

Evidência actual depois da correcção de ownership para fixtures/setup/helpers:

```text
Run: verifiers/target/feature-helper-owner-fix-dynamic-smoke/quizzes-20260630-122219-034/
Scenario records: 584
Test classes selected/passed/failed: 45 / 43 / 2
Dynamic events read: 26815
MATCHED_EXACT: 435
MATCHED_HIGH_CONFIDENCE: 125
MATCHED_PARTIAL: 0
AMBIGUOUS: 0
UNMATCHED: 24
NOT_COVERED: 0
unmatchedReasonCounts:
  FAILED_TEST_CLASS: 8
  NOT_SELECTED_TEST_CLASS: 7
  HELPER_OWNER_MISMATCH: 0
  UNCLASSIFIED: 9
```

Baseline pós-event-semantics antes desta correcção:

```text
MATCHED_EXACT: 291
MATCHED_HIGH_CONFIDENCE: 109
AMBIGUOUS: 0
UNMATCHED: 184
```

Baseline antigo antes da atribuição runtime por `inputVariantId`:

```text
MATCHED_EXACT: 0
MATCHED_HIGH_CONFIDENCE: 2
AMBIGUOUS: 44
UNMATCHED: 20
warningCount: 8238
```

Limitação actual:

> A evidência dinâmica reduziu a ambiguidade do join e resolveu a maior parte do problema de ownership de helpers/setup, mas continua a ser sidecar e não cria novos cenários estáticos. `UNMATCHED=24` mostra que ainda há planos estáticos sem correspondência dinâmica segura nos testes seleccionados.

### 3.6 ScenarioExecutor POC

Existe um executor estreito, owned pelo verificador, que consegue consumir um catálogo ou catálogo enriquecido e executar alguns cenários single-saga suportados.

O POC consegue:

- carregar catálogo estático/enriquecido;
- seleccionar ou receber um `scenario-id`;
- materializar inputs suportados;
- resolver alguns argumentos runtime-owned;
- executar o schedule single-saga;
- escrever um execution report.

Argumentos runtime-owned actualmente suportados:

- `SagaUnitOfWorkService`
- `CommandGateway`
- `SagaUnitOfWork`

Smoke evidence em Quizzes:

```text
Saga: pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.GetCourseExecutionsFunctionalitySagas
Step: getCourseExecutionsStep
Terminal status: SUCCESS
```

Limitação:

> Isto não é execução genérica. Não cobre execução multi-saga, fault injection gerado, CSV behaviour generation, impact scoring, GA ou bandit.

## 4. Evidência principal a mostrar

### 4.1 Contagem estática Quizzes pós-event-semantics

Comparação relevante:

```text
discovered sagas: 65 -> 68
sagas with accepted inputs: 26 -> 36
sagas without accepted inputs: 39 -> 32
accepted input variants: 517 -> 584
selected input-bound scenario total: 517 -> 584
catalog written: 0 -> 0 (COUNT_ONLY, esperado)
staticRecipeReadyInputVariantCount: 0
executorMaterializableInputVariantCount: 94
blockedInputVariantCount: 490
```

Interpretação:

- A semântica de eventos melhorou a cobertura estática.
- O grupo original de Sagas event-driven passou a ter inputs estáticos aceites.
- Ainda há 32 Sagas de Quizzes sem inputs estáticos aceites.
- `acceptedInputVariantCount=584` não significa `executor-ready=584`.
- `executorMaterializableInputVariantCount=94` mede apenas o subconjunto suportado pelo ScenarioExecutor actual.

### 4.2 Redução de schedules

```text
ORDER_PRESERVING_INTERLEAVING: 218528454
SEGMENT_COMPRESSED: 1019393
```

Interpretação:

- A redução é grande e útil para argumentar contra brute force.
- Mas depende da qualidade da extracção estática de conflitos.
- Não deve ser vendida como prova absoluta de equivalência semântica.

### 4.3 ScenarioExecutor smoke

```text
Catalog: verifiers/target/structured-input-recipes-quizzes-smoke/quizzes-20260520-175058-455/scenario-catalog.jsonl
Scenario plan id: 2f0c64a371fcd65b5a38f294ccbda93a42df060c3d1e5b7dcedf43568abcf661
Saga: GetCourseExecutionsFunctionalitySagas
Step: getCourseExecutionsStep
Terminal status: SUCCESS
```

Interpretação:

- Prova que existe uma ponte inicial entre catálogo gerado e execução real.
- Não prova que a execução genérica esteja resolvida.

## 5. O que falta / riscos principais

### 5.1 Materialização e replayability

Este é o problema central actual.

O verificador consegue descrever muitos inputs e cenários, mas executar genericamente exige transformar esses dados em objectos vivos dentro da aplicação:

- DTOs construídos em testes.
- Placeholders de eventos.
- Valores produzidos por setup runtime.
- Dependências Spring.
- `SagaUnitOfWork` e infra-estrutura do simulador.
- Agregados/IDs que podem só existir depois de passos anteriores.

Formulação para a reunião:

> O problema já não é apenas “descobrir cenários”. O problema é decidir que formas de receita de input são suportadas pelo executor e como resolver valores que só existem em runtime.

### 5.2 Exact aggregate-instance binding incompleto

O verificador muitas vezes sabe:

- tipo de agregado;
- modo de acesso (`READ`/`WRITE`);
- passo/Saga que toca no agregado.

Mas nem sempre sabe:

- a instância exacta do agregado;
- a chave concreta;
- se duas Sagas tocam realmente no mesmo objecto ou apenas no mesmo tipo.

Risco:

> Type-only evidence é útil como fallback conservador, mas não deve ser descrita como evidência exacta de conflito na mesma instância.

### 5.3 Dynamic enrichment foi refrescado e o problema principal de helpers/setup baixou muito

O enriquecimento dinâmico foi refrescado contra o catálogo pós-event-semantics e depois novamente após a correcção de ownership para fixtures/setup/helpers. O resultado principal é positivo para a precisão do join:

```text
Antes da correcção de ownership:
MATCHED_EXACT: 291
MATCHED_HIGH_CONFIDENCE: 109
AMBIGUOUS: 0
UNMATCHED: 184

Depois da correcção de ownership:
MATCHED_EXACT: 435
MATCHED_HIGH_CONFIDENCE: 125
AMBIGUOUS: 0
UNMATCHED: 24
```

Interpretação:

- o `inputVariantId` runtime removeu a ambiguidade nesta baseline;
- `inputRole`, `fixtureOrigin`, `callContextMethodName` e owners multi-feature permitiram casar inputs criados em `setup()`/helpers com a feature Spock activa;
- o catálogo enriquecido cobre os 584 planos estáticos como sidecar;
- `UNMATCHED=24` continua a ser uma caveat, mas já não é o problema dominante anterior;
- os 2 test classes falhados são falhas Quizzes já conhecidas, não novos erros de instrumentation.

Distribuição actual dos `UNMATCHED=24`:

```text
FAILED_TEST_CLASS: 8
NOT_SELECTED_TEST_CLASS: 7
HELPER_OWNER_MISMATCH: 0
UNCLASSIFIED: 9
```

Próximo passo possível:

- fazer triagem curta dos `UNCLASSIFIED=9`;
- não avançar automaticamente para runtime-value matching sem confirmar que esses 9 casos precisam mesmo disso;
- não usar dynamic evidence para criar cenários estáticos novos.

### 5.4 Execução genérica ainda não existe

Não implementado:

- executor genérico para qualquer entrada do catálogo;
- execução multi-saga;
- injecção de falhas a partir de schedules gerados;
- geração de behaviour CSV;
- impact scoring;
- Genetic Algorithm;
- bandit/priorização;
- suporte TCC/runtime distribuído/stream/gRPC.

Este ponto deve ser dito explicitamente para evitar overclaiming.

## 6. Walkthrough técnico por módulo

Usar esta secção para a parte detalhada da reunião. A ordem deve seguir a pipeline, não necessariamente a estrutura exacta de packages.

### 6.1 Extracção estática

Objectivo desta fase:

> Transformar código Java da aplicação numa representação intermédia com serviços de domínio, command handlers, Sagas, passos, dependências e footprints de acesso a agregados.

#### Código principal

Entrada da pipeline:

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ScenarioGeneratorApplication.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ApplicationsFileTreeParser.java`

Visitors principais:

- `visitor/CommandHandlerIndexVisitor.java`
- `visitor/ServiceVisitor.java`
- `visitor/CommandHandlerVisitor.java`
- `visitor/WorkflowFunctionalityVisitor.java`

Estado intermédio:

- `state/ApplicationAnalysisState.java`

Building blocks relevantes:

- `buildingblock/ServiceBuildingBlock.java`
- `buildingblock/CommandHandlerBuildingBlock.java`
- `buildingblock/CommandDispatchInfo.java`
- `buildingblock/SagaFunctionalityBuildingBlock.java`
- `buildingblock/SagaStepBuildingBlock.java`
- `buildingblock/StepDispatchFootprint.java`

Specs úteis:

- `ApplicationsFileTreeParserSpec.groovy`
- `ServiceVisitorSpec.groovy`
- `CommandHandlerVisitorSpec.groovy`
- `WorkflowFunctionalityVisitorSpec.groovy`
- `ApplicationAnalysisStateSpec.groovy`

#### Fluxo real de execução

`ScenarioGeneratorApplication.run()` faz a extracção nesta ordem:

```text
1. configurar JavaParser + SymbolSolver
2. descobrir ficheiros da aplicação
3. CommandHandlerIndexVisitor
4. ServiceVisitor
5. CommandHandlerVisitor
6. WorkflowFunctionalityVisitor
7. WorkflowFunctionalityCreationSiteVisitor
8. EventHandlingBridgeVisitor
9. GroovySourceIndex + GroovyConstructorInputTraceVisitor
10. relatório HTML + adaptação para catálogo
```

Nesta subsecção interessa sobretudo os passos 1–6.

#### Descoberta de ficheiros

`ApplicationsFileTreeParser` percorre a árvore da aplicação e guarda mapas:

```text
src/main/java/**/*.java   → javaFilePaths:   FQN -> Path
src/test/groovy/**/*.groovy → groovyFilePaths: FQN -> Path
```

Detalhe importante:

- A aplicação alvo é filtrada por `getJavaFilePathsForApplication(...)`.
- O parser constrói FQNs a partir do caminho relativo depois de `src/main/java/` ou `src/test/groovy/`.
- Isto é simples e determinístico, mas depende da estrutura Maven/Spring convencional.

#### Estado intermédio: `ApplicationAnalysisState`

`ApplicationAnalysisState` é o acumulador central da análise.

Guarda:

```text
services
commandHandlers
sagas
sagaCreationSites
eventDrivenFunctionalityInvocations
groovyConstructorInputTraces
groovyFullTraceResults
interfaceToServices
dispatchTargetFqns
dispatchTargetInterfaceFqns
serviceImplementationCountsByInterface
```

Modelo mental:

> Os visitors não geram cenários directamente. Eles preenchem `ApplicationAnalysisState`. Depois, o adapter de cenários transforma este estado em `SagaDefinition`, `InputVariant` e, finalmente, `ScenarioPlan`.

#### Fase 1: `CommandHandlerIndexVisitor`

Objectivo:

> Descobrir que serviços são realmente usados por command handlers antes de classificar serviços de domínio.

O visitor:

- percorre classes que estendem `CommandHandler`;
- inspecciona field injection e constructor injection;
- resolve tipos com JavaParser/SymbolSolver;
- guarda concrete service FQNs em `state.dispatchTargetFqns`;
- guarda interface FQNs em `state.dispatchTargetInterfaceFqns`;
- conta implementações de interfaces `@Service` em `serviceImplementationCountsByInterface`.

Porque existe esta fase:

> Nem todo `@Service` é um serviço de domínio relevante. Algumas classes são facades/coordination helpers. O index evita que o verificador trate qualquer `@Service` como footprint de domínio.

Limitação:

- Não é um solver completo de Spring profiles.
- Interfaces com múltiplas implementações são tratadas de forma conservadora.

#### Fase 2: `ServiceVisitor`

Objectivo:

> Classificar métodos de serviços de domínio como `READ` ou `WRITE`.

O visitor só admite classes que:

- têm `@Service`;
- injectam `UnitOfWorkService` por construtor ou `@Autowired` field;
- são dispatch targets de command handlers, excepto quando o visitor é usado isoladamente em testes.

A classificação é simples:

```text
se o método público causa registerChanged(...) → WRITE
caso contrário → READ
```

Padrões suportados para detectar `WRITE`:

- chamada directa a `unitOfWorkService.registerChanged(...)`;
- `this.unitOfWorkService.registerChanged(...)`;
- alias local do `UnitOfWorkService`;
- getter trivial que devolve o `UnitOfWorkService`;
- um helper method local que encapsula `registerChanged(...)`.

Output:

```text
ServiceBuildingBlock
  fqn
  methodAccessPolicies: methodSignature -> READ/WRITE
```

Exemplo conceptual:

```text
ItemService.getItem(...)      -> READ
ItemService.createItem(...)   -> WRITE
OrderService.cancelOrder(...) -> WRITE
```

Limitações:

- Não interpreta semântica de negócio.
- Só procura padrões de `registerChanged` no estilo implementado.
- Helper detection é intencionalmente curta; não faz análise interprocedural profunda.
- Se um método altera estado sem passar por estes padrões, pode ser classificado como `READ`.

#### Fase 3: `CommandHandlerVisitor`

Objectivo:

> Mapear cada tipo de `Command` para o serviço/método que o executa e para o agregado afectado.

O visitor:

- encontra classes que estendem `CommandHandler`;
- resolve serviços injectados por field injection ou constructor injection;
- extrai o aggregate type name a partir de `getAggregateTypeName()`;
- percorre métodos privados que recebem um subtype de `Command`;
- encontra a primeira chamada a um serviço conhecido;
- cria um `CommandDispatchInfo`.

Output principal:

```text
Command type FQN
  -> service class
  -> service method signature
  -> aggregate name
  -> access policy READ/WRITE via ServiceBuildingBlock
```

Exemplo conceptual:

```text
CreateItemCommand
  -> ItemService.createItem(...)
  -> aggregate: Item
  -> access: WRITE

GetOrderCommand
  -> OrderService.getOrder(...)
  -> aggregate: Order
  -> access: READ
```

Detalhes suportados:

- field injection;
- constructor injection;
- service injection por interface quando há exactamente uma implementação;
- um nível de delegate method privado.

Limitações:

- Se a interface tem várias implementações, o dispatch é skipped/conservador.
- Só usa a primeira chamada a serviço reconhecido.
- Não resolve Spring profile/environment.
- Não faz call graph profundo.

#### Fase 4: `WorkflowFunctionalityVisitor`

Objectivo:

> Extrair as Sagas, os seus passos e os dispatch footprints de cada passo.

O visitor reconhece uma Saga quando a classe:

- é subclass de `WorkflowFunctionality`;
- declara/injecta `SagaUnitOfWorkService`.

Para cada Saga extrai:

- FQN da classe;
- assinaturas dos construtores;
- `new SagaStep(...)`;
- nome do step;
- dependências/predecessores;
- comandos emitidos dentro da lambda do step;
- dispatches forward;
- dispatches de compensation via `registerCompensation(...)`.

Output conceptual:

```text
SagaFunctionalityBuildingBlock
  fqn
  constructorSignatures
  steps: List<SagaStepBuildingBlock>

SagaStepBuildingBlock
  stepKey
  name
  predecessorStepKeys
  dispatches: List<StepDispatchFootprint>
```

O `StepDispatchFootprint` liga o step ao domínio:

```text
stepKey
commandTypeFqn
aggregateName
accessPolicy: READ/WRITE
phase: FORWARD/COMPENSATION
multiplicity
aggregateKeyText
aggregateKeyConfidence: EXACT/SYMBOLIC
```

Snippet mental importante:

```text
new SagaStep("createItemStep", (...) -> {
    commandGateway.send(new CreateItemCommand(...));
}, getOrderStep)
```

vira aproximadamente:

```text
step: CreateItemFunctionalitySagas::createItemStep
predecessor: CreateItemFunctionalitySagas::getOrderStep
command: CreateItemCommand
aggregate: Item
access: WRITE
phase: FORWARD
```

Como a ordem é preservada:

- a lista `sagaBlock.steps` preserva a ordem de descoberta dos `new SagaStep(...)`;
- dependências explícitas são guardadas em `predecessorStepKeys`;
- fases posteriores usam estes dados para construir `StepDefinition` e schedules.

Como os footprints aparecem:

1. O step contém `new SomeCommand(...)`.
2. `WorkflowFunctionalityVisitor` pergunta a `ApplicationAnalysisState.getCommandDispatchInfo(commandType)`.
3. O command dispatch diz que serviço/método trata o comando.
4. O serviço/método já tem policy `READ`/`WRITE`.
5. O visitor cria um `StepDispatchFootprint`.

#### Multiplicity e aggregate key

O visitor tenta inferir multiplicidade do dispatch:

- `SINGLE` para chamada simples;
- `STATIC_REPEAT` para `for` loops simples com contador estático;
- `PARAMETRIC_REPEAT` para loops dependentes de colecções/runtime.

Também tenta inferir uma chave de agregado quando ela está directamente visível no terceiro argumento do command:

- literal → `EXACT`;
- method call → `SYMBOLIC`;
- variável/DTO/outro caso → fica sem chave exacta.

Isto explica uma limitação recorrente:

> O verificador muitas vezes sabe o tipo de agregado e o modo de acesso, mas não a instância exacta do agregado.

#### Ambiguidade e comportamento conservador

Casos ambíguos importantes:

- command não encontrado no registry → warning e dispatch não é criado;
- referência de dependência de step não resolvida → warning e só são preservadas as dependências resolvidas;
- interface com várias implementações `@Service` → skipped;
- aggregate key não directamente visível → type-only/sem exact key;
- service call não reconhecida → command dispatch incompleto.

Isto é coerente com a filosofia actual:

> Melhor não inventar precisão. Quando a evidência estática não chega, o verificador deve manter o caso conservador ou diagnóstico.

#### Resumo desta fase

Input:

```text
Código Java da aplicação + SymbolSolver
```

Output:

```text
ApplicationAnalysisState com:
- serviços classificados READ/WRITE;
- command handlers mapeados para serviços/métodos/agregados;
- Sagas com steps, dependências e dispatch footprints.
```

Claim correcto:

> A extracção estática constrói uma representação de Sagas e footprints de domínio suficientemente rica para gerar cenários e estimar conflitos.

Claim a evitar:

> A extracção estática não prova que todas as interacções reais foram descobertas, nem resolve genericamente Spring profiles, call graphs profundos ou chaves exactas de agregados.

Perguntas que devo conseguir responder:

- O que é uma Saga para o verificador?
- O que é um step?
- Como é preservada a ordem dos steps?
- De onde vem a evidência de leitura/escrita em agregados?
- Porque é que `CommandHandlerIndexVisitor` vem antes de `ServiceVisitor`?
- O que acontece quando há múltiplas implementações de uma interface?
- Porque é que muitas interacções continuam type-only?

Snippet útil a escolher:

- `WorkflowFunctionalityVisitor.extractStepFootprints(...)`, porque mostra a ligação central: `SagaStep` → `Command` → `CommandDispatchInfo` → `StepDispatchFootprint`.

### 6.2 Extracção de testes/inputs

Objectivo desta fase:

> Transformar testes happy-path em variantes de input que possam ser ligadas a Sagas e, mais tarde, tentadas pelo executor.

Esta fase é a ponte entre “a aplicação tem testes” e “o verificador conhece inputs concretos”.

#### Código principal

Indexação Groovy:

- `state/GroovySourceIndex.java`
- `state/GroovySourceClassMetadata.java`
- `state/GroovySourceMethodMetadata.java`
- `state/GroovySourceFieldMetadata.java`

Tracing de inputs:

- `visitor/GroovyConstructorInputTraceVisitor.java`
- `state/GroovyFullTraceResult.java`
- `state/GroovyConstructorInputTrace.java`
- `state/GroovyTraceArgument.java`
- `state/GroovyWorkflowCall.java`
- `state/GroovyValueRecipe.java`
- `state/GroovyValueMetadata.java`
- `state/GroovyValueResolutionCategory.java`

Classificação do modo de origem:

- `state/SourceModeClassifier.java`
- `state/SourceModeClassification.java`
- `state/SourceMode.java`

Event-origin bridge:

- `visitor/EventHandlingBridgeVisitor.java`
- `buildingblock/EventDrivenFunctionalityInvocation.java`
- `buildingblock/EventDrivenArgumentSource.java`

Mapeamento para receitas de execução:

- `scenario/adapter/InputRecipeMapper.java`
- `scenario/model/InputRecipe.java`
- `scenario/model/InputRecipeNode.java`
- `scenario/model/InputRecipeArgument.java`
- `scenario/model/InputRecipeAssignment.java`

Specs úteis:

- `GroovySourceIndexSpec.groovy`
- `GroovyConstructorInputTraceVisitorSpec.groovy`
- `GroovyConstructorInputTraceVisitorDummyappSpec.groovy`
- `EventHandlingBridgeVisitorDummyappSpec.groovy`
- `SourceModeClassifierSpec.groovy`
- `ApplicationAnalysisScenarioModelAdapterSpec.groovy`

#### Fluxo conceptual

```text
Testes Groovy/Spock
        ↓
GroovySourceIndex
        ↓
GroovyConstructorInputTraceVisitor
        ↓
GroovyFullTraceResult
        ↓
ApplicationAnalysisScenarioModelAdapter
        ↓
InputVariant + InputRecipe
```

#### `GroovySourceIndex`

`GroovySourceIndex` lê `src/test/groovy` e constrói metadados estáticos das classes de teste.

Guarda:

- FQN da classe;
- ficheiro de origem;
- package/imports;
- superclass declarada;
- annotations;
- fields;
- métodos;
- classes internas;
- relação com superclasses também presentes no código-fonte.

Porque isto existe:

> Antes de seguir valores nos testes, o verificador precisa de saber que classes existem, que fields/métodos têm, e como fixtures/base specs se relacionam com specs concretas.

Limitação:

- Isto é parsing estático de Groovy, não execução de testes.
- Não resolve todos os comportamentos dinâmicos de Groovy.

#### `GroovyConstructorInputTraceVisitor`

Este é o visitor grande que tenta seguir de onde vêm os inputs usados para instanciar Sagas ou chamar facades que criam Sagas.

Ele percorre:

- fields inicializados no teste;
- métodos `setup`, `setupSpec` e feature methods;
- assignments locais;
- construtores;
- chamadas a helper methods locais;
- chamadas a facades;
- chamadas event-driven detectadas pelo event bridge;
- chamadas de execução de workflow.

Métodos de execução acompanhados:

```text
executeWorkflow
executeUntilStep
resumeWorkflow
```

O resultado principal é `GroovyFullTraceResult`:

```text
sourceClassFqn
sourceMethodName
sourceBindingName
originKind
sourceExpressionText
sagaClassFqn
sourceMode
sourceModeConfidence
sourceModeEvidence
constructorArguments
workflowCalls
resolutionNotes
traceText
```

Modelo mental:

> Um `GroovyFullTraceResult` é uma explicação estática de “este teste/fixture cria ou invoca esta Saga com estes argumentos”.

#### Origens suportadas

O tracing suporta várias origens:

- construtor directo de Saga;
- variável local que aponta para uma Saga;
- field de teste que contém uma Saga;
- helper method que devolve uma Saga;
- facade method que internamente cria uma Saga;
- event handler call resolvido através da cadeia `EventHandling`/`EventProcessing` implementada.

Exemplos conceptuais:

```groovy
def saga = new CreateItemFunctionalitySagas(...)
saga.executeWorkflow()
```

```groovy
facade.createItem(dto)
```

```groovy
eventHandling.handleSubscribedEvent(...)
```

Todos podem originar traces, desde que o verificador consiga resolver a cadeia.

#### Receitas de valores: `GroovyValueRecipe`

Cada argumento do construtor é descrito por uma receita.

Tipos de receita relevantes:

```text
LITERAL
CONSTRUCTOR
COLLECTION_LITERAL
LOCAL_TRANSFORM
HELPER_CALL_RESULT
PROPERTY_ACCESS
UNRESOLVED_VARIABLE
UNRESOLVED_RUNTIME_EDGE
```

A metadata classifica o estado do valor:

```text
RESOLVED
SOURCE_PLACEHOLDER
INJECTABLE_PLACEHOLDER
RUNTIME_CALL
EVENT_PLACEHOLDER
UNKNOWN_UNRESOLVED
```

Exemplo conceptual:

```text
arg[1]: new ItemDto("name", 123)
  kind: CONSTRUCTOR
  children:
    - LITERAL "name"
    - LITERAL 123
  category: RESOLVED
```

Exemplo bloqueado:

```text
arg[2]: EVENT_FIELD:UserEvent.userId
  kind: UNRESOLVED_VARIABLE
  category: EVENT_PLACEHOLDER
```

#### Source-mode filtering

`SourceModeClassifier` tenta perceber se um teste pertence ao mundo Saga ou TCC.

Evidência usada:

- `@ActiveProfiles("sagas")` ou `@ActiveProfiles("tcc")`;
- `spring.profiles.active` em annotations;
- `@TestConfiguration` com beans que constroem serviços Saga/TCC;
- fields `@Autowired` de `SagaUnitOfWorkService` ou `CausalUnitOfWorkService`;
- fields herdados de superclasses source-backed.

Resultado:

```text
SAGAS
TCC
MIXED
UNKNOWN
```

Política no catálogo:

- `SAGAS` aceite;
- `TCC` rejeitado para catálogo Saga;
- `MIXED` rejeitado para catálogo Saga;
- `UNKNOWN` aceite com warning.

Ponto importante:

> Source-mode filtering não é um solver completo de Spring. É evidência estática suficiente para evitar incluir inputs claramente TCC/mistos no catálogo de Sagas.

#### Event-origin inputs

O `EventHandlingBridgeVisitor` tenta resolver a cadeia:

```text
EventHandling.handleSubscribedEvent(...)
        ↓
EventHandler.handleEvent(...)
        ↓
EventProcessing.someMethod(...)
        ↓
Facade.someMethod(...)
        ↓
new SomeFunctionalitySagas(...)
```

Depois o `GroovyConstructorInputTraceVisitor` consegue criar traces event-origin quando um teste chama a parte event-driven.

Tipos de argumento event-driven:

- `EVENT_FIELD`
- `EVENT_PAYLOAD`
- `EVENT_SUBSCRIBER_AGGREGATE_ID`
- `EVENT_PROCESSING_PARAMETER`
- `EVENT_EXPRESSION`
- `INJECTABLE_FIELD`
- `SOURCE_PLACEHOLDER`
- `RUNTIME_CALL`

Limitação importante:

> Estes inputs podem ser aceites estaticamente, mas muitos ficam bloqueados para execução porque o payload real do evento ainda não é reconstruído. Daí o blocker `EVENT_PAYLOAD_PLACEHOLDER`.

#### `InputRecipeMapper`

O adapter transforma `GroovyValueRecipe` em `InputRecipe`.

A `InputRecipe` é mais próxima do que um executor precisa:

```text
InputRecipe
  schemaVersion
  recipeFingerprint
  executorReady
  blockers
  arguments: List<InputRecipeArgument>
```

Cada `InputRecipeArgument` aponta para uma árvore de `InputRecipeNode`.

Kinds principais de `InputRecipeNode`:

```text
literal
constructor
collection
local_transform
helper_result
property_access
placeholder
event_placeholder
call_result
unresolved
```

O mapper também calcula blockers, por exemplo:

```text
EVENT_PAYLOAD_PLACEHOLDER
MISSING_TARGET_TYPE
PROPERTY_RECEIVER_NOT_READY
TRANSFORM_RECEIVER_NOT_READY
UNRESOLVED_VARIABLE
UNKNOWN_VALUE
CALL_RECEIVER_NOT_READY
UNMATERIALIZABLE_ASSIGNMENT
UNRESOLVED_PLACEHOLDER
UNSUPPORTED_TRANSFORM
UNRESOLVED_RUNTIME_EDGE
```

Diferença essencial:

```text
InputResolutionStatus.REPLAYABLE
```

não significa necessariamente:

```text
inputRecipe.executorReady == true
```

E mesmo `inputRecipe.executorReady=false` pode não significar “impossível para sempre”, porque o ScenarioExecutor pode resolver alguns argumentos runtime-owned.

#### `InputVariant`

O adapter transforma cada trace usável num `InputVariant`.

Campos importantes:

```text
deterministicId
sagaFqn
sourceClassFqn
sourceMethodName
sourceBindingName
resolutionStatus
sourceMode
sourceModeConfidence
sourceModeEvidence
stableSourceText
provenanceText
owners
constructorArgumentSummaries
logicalKeyBindings
warnings
inputRecipe
```

Modelo mental:

> Um `InputVariant` é uma variante estática de input para uma Saga, com origem em testes e com uma receita de reconstrução parcial ou total.

#### Provenance vs ownership

Provenance:

> Explica de onde veio o input e como foi reconstruído.

Ownership:

> Diz que feature methods/testes podem reclamar aquele input quando chega evidência runtime.

Isto importa porque muitos inputs são criados em `setup`, `setupSpec` ou fields partilhados. Nesses casos, a origem textual pode ser uma fixture, mas o runtime event deve ser atribuído ao feature method que usou essa fixture.

#### Logical key bindings

O adapter tenta extrair bindings simples de chaves a partir das recipes:

```text
aggregateId
orderId
id
```

Só valores literais são considerados bindings exactos.

Uso:

- compatibilizar input tuples entre Sagas;
- evitar juntar inputs que claramente falam de IDs diferentes;
- ajudar no raciocínio de shared aggregate instance.

Limitação:

- A extracção de keys ainda é incompleta.
- Muitos casos ficam sem binding exacto.

#### Resumo desta fase

Input:

```text
Groovy tests + ApplicationAnalysisState com Sagas/facades/event bridge
```

Output:

```text
GroovyFullTraceResult
InputVariant
InputRecipe
source-mode diagnostics
```

Claim correcto:

> O verificador consegue recuperar muitas variantes de input a partir de testes happy-path, incluindo direct constructors, facade calls, helpers e o shape event-driven implementado.

Claim a evitar:

> Não devo dizer que todos os testes/inputs são compreendidos nem que inputs aceites são automaticamente executáveis.

Perguntas que devo conseguir responder:

- O que é um `InputVariant`?
- O que é uma `inputRecipe`?
- Porque é que provenance e ownership são conceitos diferentes?
- Porque é que um input pode ser aceite mas continuar bloqueado para execução?
- Porque é que event-origin input não implica event payload materialization?
- O que significa `UNKNOWN` source mode ser aceite com warning?

Snippet útil a escolher:

- Um `InputVariant` com `inputRecipe.arguments` e blockers.
- Ou `InputRecipeMapper.eventPlaceholderNode(...)`, porque mostra porque `EVENT_PAYLOAD_PLACEHOLDER` bloqueia execução.

### 6.3 Catálogo de cenários

Objectivo desta fase:

> Transformar o estado analisado e os inputs aceites num catálogo determinístico de cenários que possa ser usado por relatórios, enriquecimento dinâmico e execução futura.

#### Código principal

Adapter análise → modelo de cenário:

- `scenario/adapter/ApplicationAnalysisScenarioModelAdapter.java`
- `scenario/adapter/InputRecipeMapper.java`
- `scenario/adapter/ScenarioModelAdapterResult.java`

Geração:

- `scenario/ScenarioGenerator.java`
- `scenario/ScenarioGeneratorConfig.java`
- `scenario/ScenarioIdGenerator.java`
- `scenario/InputVariantNormalizer.java`
- `scenario/InputTupleJoiner.java`

Modelo:

- `scenario/model/SagaDefinition.java`
- `scenario/model/StepDefinition.java`
- `scenario/model/StepFootprint.java`
- `scenario/model/InputVariant.java`
- `scenario/model/SagaInstance.java`
- `scenario/model/ScheduledStep.java`
- `scenario/model/FaultSpace.java`
- `scenario/model/ConflictEvidence.java`
- `scenario/model/ScenarioPlan.java`
- `scenario/model/ScenarioGenerationResult.java`

Export:

- `scenario/export/ScenarioCatalogJsonlWriter.java`
- `scenario/model/ScenarioCatalogManifest.java`
- `scenario/model/RejectedInputVariant.java`

Specs úteis:

- `ApplicationAnalysisScenarioModelAdapterSpec.groovy`
- `ScenarioGeneratorSpec.groovy`
- `ScenarioCatalogJsonlWriterSpec.groovy`
- `ScenarioGeneratorApplicationSpec.groovy`

#### Fluxo real no `ScenarioGeneratorApplication`

Depois da extracção:

```text
ApplicationAnalysisState
        ↓
ApplicationAnalysisScenarioModelAdapter.adapt(...)
        ↓
SagaDefinition + InputVariant
        ↓
ScenarioGenerator.generate(...)
        ↓
ScenarioGenerationResult
        ↓
ScenarioCatalogJsonlWriter.write(...)
        ↓
scenario-catalog.jsonl + manifest + rejected-inputs + accounting
```

#### Adapter: `ApplicationAnalysisScenarioModelAdapter`

O adapter faz duas coisas:

1. Converte Sagas extraídas para `SagaDefinition`.
2. Converte traces de inputs para `InputVariant`.

Para Sagas:

```text
SagaFunctionalityBuildingBlock
        ↓
SagaDefinition
```

Para steps:

```text
SagaStepBuildingBlock
        ↓
StepDefinition
```

Para footprints:

```text
StepDispatchFootprint
        ↓
StepFootprint
```

O adapter também transforma `AccessPolicy` em `AccessMode`:

```text
READ  -> READ
WRITE -> WRITE
unknown -> WRITE conservador
```

E transforma chaves de agregado:

```text
sem key visível -> TYPE_ONLY
literal -> EXACT
method call/simbólico -> SYMBOLIC
```

#### Normalização de inputs

`InputVariantNormalizer` decide que inputs entram no catálogo.

Filtra por:

- `sourceMode`;
- `InputPolicy`;
- duplicados por deterministic ID;
- limite `maxInputVariantsPerSaga`.

Regras importantes:

```text
TCC   -> rejected input diagnostic
MIXED -> rejected input diagnostic
SAGAS -> accepted
UNKNOWN -> accepted com warning
```

Input policies:

```text
RESOLVED_ONLY
RESOLVED_OR_REPLAYABLE
ALLOW_PARTIAL
ALLOW_UNRESOLVED
```

A configuração actual por defeito usa:

```text
RESOLVED_OR_REPLAYABLE
```

Ponto importante:

> Inputs rejeitados por source mode não desaparecem silenciosamente. São exportados como diagnostics/rejected-inputs.

#### `ScenarioPlan`

O contrato principal do catálogo é `ScenarioPlan`.

Shape simplificado:

```text
ScenarioPlan
  schemaVersion
  deterministicId
  kind: SINGLE_SAGA | MULTI_SAGA
  sagaInstances
  inputs
  expandedSchedule
  faultSpace
  conflictEvidence
  warnings
```

`ScenarioPlan.SCHEMA_VERSION` actual:

```text
microservices-simulator.scenario-catalog.v2
```

Um single-saga plan contém:

- uma `SagaInstance`;
- um `InputVariant`;
- schedule serial dos steps dessa Saga;
- fault space derivado dos scheduled steps;
- sem `ConflictEvidence`.

Um multi-saga plan contém:

- várias `SagaInstance`s;
- um input tuple;
- um schedule intercalado ou segment-compressed;
- conflict evidence que justifica a combinação.

#### IDs determinísticos

`ScenarioIdGenerator` gera hashes estáveis para:

- `inputVariantId`;
- `sagaInstanceId`;
- `stepDefinitionId`;
- `scheduledStepId`;
- `conflictEvidenceId`;
- `scenarioPlanId`.

O hash inclui apenas campos relevantes e ordenações estáveis.

Razão:

> O mesmo código/input deve gerar o mesmo ID para permitir comparação entre runs, enriquecimento dinâmico e reprodução.

#### `COUNT_ONLY`

`ScenarioGenerator.generate(...)` tem um modo especial:

```text
CatalogWriteMode.COUNT_ONLY
```

Neste modo:

- normaliza inputs;
- calcula counts/accounting;
- não materializa nem escreve os `ScenarioPlan`s;
- `scenariosEmitted=0`;
- `catalogWritten=0` é esperado.

Isto é necessário porque Quizzes pode gerar espaços enormes.

Formulação para a reunião:

> `catalogWritten=0` em COUNT_ONLY não é falha; é a forma segura de medir o espaço sem escrever milhões de cenários.

#### Export

`ScenarioCatalogJsonlWriter` escreve:

```text
scenario-catalog.jsonl
scenario-catalog-manifest.json
scenario-catalog-rejected-inputs.jsonl
scenario-space-accounting.json
```

`scenario-catalog.jsonl`:

- uma linha JSON por `ScenarioPlan`;
- é o contrato estático.

Manifest:

- config efectiva;
- counts;
- paths;
- source-mode counts;
- rejected source-mode reasons.

Rejected inputs:

- `InputVariant` rejeitado;
- razão;
- warnings.

Accounting:

- contagens do espaço total/seleccionado/escrito;
- cobertura por tipo;
- readiness do executor;
- top contributors.

#### Resumo desta fase

Input:

```text
ApplicationAnalysisState
```

Output:

```text
SagaDefinition
InputVariant
ScenarioPlan
scenario-catalog.jsonl
scenario-catalog-manifest.json
scenario-catalog-rejected-inputs.jsonl
scenario-space-accounting.json
```

Claim correcto:

> O catálogo é determinístico, machine-readable e separa cenários aceites, inputs rejeitados e accounting.

Claim a evitar:

> O catálogo não garante que cada cenário é executável. Ele é primeiro um contrato estático de cenário.

Perguntas que devo conseguir responder:

- Qual é o contrato estático do catálogo?
- O que significa `COUNT_ONLY`?
- Porque é que `catalogWritten=0` em `COUNT_ONLY` é esperado?
- Qual é a diferença entre accepted, rejected, blocked, recipe-ready e executor-materializable?
- Porque é que IDs determinísticos são importantes?

Snippet útil a escolher:

- Um `ScenarioPlan` JSONL real ou minimizado.
- Ou `ScenarioIdGenerator.scenarioPlanId(...)` para mostrar a estabilidade dos IDs.

### 6.4 Escalonamento e segment compression

Objectivo desta fase:

> Escolher combinações de Sagas que podem interagir e reduzir o número de schedules redundantes sem tentar enumerar todos os interleavings brutos.

#### Código principal

Conflitos/interacções:

- `scenario/ConflictGraphBuilder.java`
- `scenario/ConnectedSagaSetEnumerator.java`
- `scenario/InputTupleJoiner.java`

Schedules:

- `scenario/ScheduleEnumerator.java`
- `scenario/model/ScheduledStep.java`
- `scenario/model/ConflictEvidence.java`

Accounting:

- `scenario/accounting/ScenarioSpaceAccountingCalculator.java`
- `scenario/accounting/ScenarioSpaceAccountingReport.java`
- `scenario/accounting/ScenarioSpaceAccountingWriter.java`

Specs úteis:

- `ScenarioGeneratorSpec.groovy`
- `ScenarioSpaceAccountingCalculatorSpec.groovy`
- `DummyappAccountingFixtureFoundationSpec.groovy`

#### Conflict graph

`ConflictGraphBuilder` compara footprints de passos entre Sagas diferentes.

Um par de footprints só pode gerar conflito se:

1. tocar no mesmo agregado/identidade conhecida ou fallback permitido;
2. pelo menos um acesso for `WRITE`.

Read/read é ignorado:

```text
READ + READ -> sem conflito
READ + WRITE -> conflito
WRITE + READ -> conflito
WRITE + WRITE -> conflito
```

Tipos de conflict evidence:

```text
WRITE_WRITE
WRITE_READ
READ_WRITE
TYPE_ONLY
SYMBOLIC
UNKNOWN
```

O resultado tem:

```text
adjacency: saga -> sagas vizinhas
conflictCandidates
counts
warnings
```

Modelo mental:

> O grafo liga Sagas que têm pelo menos um par de passos potencialmente conflituoso.

#### Exact, symbolic e type-only

O matching depende da confiança da chave de agregado:

- `EXACT`: key literal igual;
- `SYMBOLIC`: key simbólica/method call, aceitável mas menos precisa;
- `TYPE_ONLY`: só se sabe o tipo de agregado;
- `UNKNOWN`: fallback ainda menos preciso.

`allowTypeOnlyFallback=false`:

- evita edges apenas por tipo.

`allowTypeOnlyFallback=true`:

- permite ligar Sagas por tipo quando não há chave exacta.

Claim correcto:

> Type-only é uma heurística conservadora para não perder interacções potenciais.

Claim a evitar:

> Type-only não prova que duas Sagas tocam a mesma instância.

#### Connected saga sets

`ConnectedSagaSetEnumerator` enumera conjuntos de Sagas até `maxSagaSetSize` e mantém apenas os conectados no grafo.

Exemplo:

```text
A -- B -- C
D isolada
```

Conjuntos úteis até tamanho 2:

```text
[A,B]
[B,C]
```

Conjunto rejeitado:

```text
[A,D]
```

Razão:

> Não vale a pena combinar Sagas sem evidência de interacção, porque os seus interleavings não devem afectar o domínio uma da outra sob a evidência estática actual.

#### Input tuples

`InputTupleJoiner` combina uma variante de input por Saga.

Também verifica compatibilidade de `logicalKeyBindings`.

Exemplo:

```text
Saga A: orderId=1
Saga B: orderId=1
        -> compatível

Saga A: orderId=1
Saga B: orderId=2
        -> incompatível, se ambos têm binding exacto
```

Se uma Saga não tem binding conhecido, a combinação é permitida.

Isto mantém cobertura, mas também mostra a limitação:

> Sem keys exactas, a compatibilidade fica permissiva.

#### Schedule strategies

`ScheduleEnumerator` suporta três estratégias:

```text
SERIAL
ORDER_PRESERVING_INTERLEAVING
SEGMENT_COMPRESSED
```

`SERIAL`:

- executa Sagas por ordem;
- um schedule.

`ORDER_PRESERVING_INTERLEAVING`:

- enumera interleavings que preservam a ordem interna de cada Saga;
- explode combinatoriamente.

Fórmula conceptual:

```text
(totalSteps)! / Π(stepsPorSaga!)
```

`SEGMENT_COMPRESSED`:

- identifica conflict anchors;
- agrupa passos até cada anchor em segmentos;
- enumera interleavings de segmentos, não de todos os passos;
- depois anexa o tail não conflitante serialmente.

#### O que é um conflict anchor?

Um conflict anchor é um step que aparece em pelo menos um `ConflictCandidate` para o conjunto de Sagas em análise.

Exemplo:

```text
Saga A: a1, a2*, a3, a4*
Saga B: b1*, b2, b3*
```

`*` = step com conflito.

Segmentos:

```text
Saga A: [a1,a2*], [a3,a4*]
Saga B: [b1*], [b2,b3*]
```

Em vez de intercalar todos os steps individualmente, intercalam-se estes segmentos.

#### O que a segment compression preserva

Preserva:

- ordem interna de cada Saga;
- presença e ordenação relativa dos anchors dentro da Saga;
- schedules representativos sobre zonas com conflitos estáticos.

Reduz:

- permutations de passos que o verificador considera independentes;
- schedules que só diferem por interleaving de operações sem conflito extraído.

Não prova:

- equivalência semântica completa;
- que não existem conflitos não extraídos;
- que type-only edges são exactas;
- que runtime vai tocar as mesmas instâncias.

Formulação segura:

> Segment compression é uma redução estática baseada em conflict anchors extraídos. É uma optimização prática e uma contribuição de scenario generation, não uma prova formal de completude semântica.

#### Accounting

`ScenarioSpaceAccountingCalculator` calcula o tamanho do espaço sem necessariamente gerar todos os planos.

Calcula:

- Sagas descobertas;
- Sagas com/sem inputs;
- espaço input-bound total;
- espaço seleccionado pelo generator configurado;
- espaço escrito no catálogo;
- coverage strict/broad;
- readiness do executor;
- top contributors.

Também compara schedule count por estratégia:

- `ORDER_PRESERVING_INTERLEAVING`: factorial combinatório;
- `SEGMENT_COMPRESSED`: interleavings dos anchors/segmentos;
- `SERIAL`: 1.

Evidência forte em Quizzes:

```text
ORDER_PRESERVING_INTERLEAVING selected total: 218528454
SEGMENT_COMPRESSED selected total: 1019393
```

Interpretação:

> A redução é grande o suficiente para ser uma contribuição prática. A apresentação deve sempre mencionar que depende da evidência estática de conflitos.

#### Resumo desta fase

Input:

```text
SagaDefinition + InputVariant + StepFootprint
```

Output:

```text
Conflict graph
Connected saga sets
Input tuples
Schedules
ConflictEvidence
Scenario-space accounting
```

Claim correcto:

> O verificador reduz o espaço de cenários usando interacções de agregados e segment compression, mantendo determinismo e limites explícitos.

Claim a evitar:

> Não devo dizer que isto enumera todos os cenários semanticamente prejudiciais.

Perguntas que devo conseguir responder:

- O que é um conflict anchor?
- O que é que segment compression preserva?
- O que é que segment compression não prova?
- Porque é que read/read não cria conflito?
- Como `allowTypeOnlyFallback` muda o grafo?
- Porque é que accounting é separado da escrita do catálogo?

Snippet útil a escolher:

- Um exemplo pequeno de segmentos antes/depois.
- Ou `ScheduleEnumerator.anchorSegments(...)` + `conflictAnchorIndexes(...)`.

### 6.5 Enriquecimento dinâmico

Objectivo desta fase:

> Correr testes reais com instrumentation do simulador e juntar a evidência runtime ao catálogo estático sem alterar o contrato estático.

#### Código principal no verificador

Orquestração:

- `dynamic/DynamicEnrichmentOrchestrator.java`
- `dynamic/DynamicEnrichmentConfig.java`
- `dynamic/DynamicEnrichmentTestClassDiscoveryService.java`
- `dynamic/DefaultProcessRunner.java`
- `dynamic/SurefireTestRunReporter.java`

Input map:

- `dynamic/DynamicInputMapWriter.java`

Leitura/join:

- `dynamic/DynamicEvidenceReader.java`
- `dynamic/DynamicEvidenceJoiner.java`
- `dynamic/model/DynamicEvidenceEvent.java`
- `dynamic/model/DynamicEvidenceJoinStatus.java`
- `dynamic/model/DynamicEvidenceJoinResult.java`
- `dynamic/model/EnrichedScenarioRecord.java`

Export:

- `dynamic/export/EnrichedScenarioCatalogWriter.java`

#### Código principal no simulador

Instrumentation runtime:

- `simulator/.../monitoring/dynamic/DynamicEvidenceTestExecutionListener.java`
- `simulator/.../monitoring/dynamic/DynamicEvidenceJsonlRecorder.java`
- `simulator/.../monitoring/dynamic/DynamicInputMap.java`
- `simulator/.../monitoring/dynamic/DynamicInputMapLoader.java`
- `simulator/.../monitoring/dynamic/DynamicEvidenceContext.java`
- `simulator/.../monitoring/dynamic/DynamicEvidenceRecorderHolder.java`
- `simulator/.../monitoring/dynamic/CommandEvidenceExtractor.java`
- `simulator/.../coordination/ExecutionPlan.java`
- `simulator/.../messaging/local/LocalCommandGateway.java`
- `simulator/.../transaction/sagas/unitOfWork/SagaUnitOfWorkService.java`

Specs úteis:

- `DynamicEvidenceReaderSpec.groovy`
- `DynamicEvidenceJoinerSpec.groovy`
- `DynamicInputMapWriterSpec.groovy`
- `EnrichedScenarioCatalogWriterSpec.groovy`
- specs `*Dynamic*Spec` / `*Enriched*Spec`

#### Fluxo conceptual

```text
scenario-catalog.jsonl
        ↓
DynamicInputMapWriter
        ↓
dynamic-input-map.json
        ↓
Maven test run com simulator.dynamic-evidence.enabled=true
        ↓
dynamic-evidence.jsonl + manifest + surefire reports
        ↓
DynamicEvidenceReader
        ↓
DynamicEvidenceJoiner
        ↓
scenario-catalog-enriched.jsonl + join report
```

#### Orquestração

`DynamicEnrichmentOrchestrator.run(...)` faz:

1. cria o directório de dynamic evidence;
2. selecciona test classes;
3. escreve `dynamic-input-map.json`;
4. corre um batch Maven com as test classes seleccionadas e propriedades de dynamic evidence;
5. guarda `maven-output.log`;
6. recolhe relatórios Surefire;
7. lê `dynamic-evidence.jsonl`;
8. junta eventos aos `ScenarioPlan`s;
9. escreve catálogo enriquecido, manifest e join report.

Comando Maven gerado conceptualmente:

```text
mvn -P<profile> test -Dtest=<classes>
  -Dsimulator.dynamic-evidence.enabled=true
  -Dsimulator.dynamic-evidence.test-context.enabled=true
  -Djunit.platform.listeners.autodetection.enabled=true
  -Dsimulator.dynamic-evidence.output-dir=<dir>
  -Dsimulator.dynamic-evidence.input-map-path=<dir>/dynamic-input-map.json
```

#### `dynamic-input-map.json`

O verificador escreve um mapa para o simulador saber que inputs estáticos podem corresponder a eventos runtime.

Cada entry contém:

```text
inputVariantId
sagaFqn
sourceClassFqn
sourceMethodName
owners
resolutionStatus
sourceMode
stepNameHints
literalArgumentValueHints
constructorArgumentSummaries
expectedAggregateTypes
logicalKeyBindings
scenarioPlanIds
stableSourceText
provenanceText
warnings
```

O simulador usa isto para resolver:

```text
test identity + functionality class FQN + step name
        ↓
inputVariantId exacto, se houver exactamente um candidato
```

Esta é a primeira forma de exact attribution.

#### Eventos runtime emitidos

O simulador emite eventos JSONL como:

```text
STEP_STARTED
COMMAND_SENT
AGGREGATE_ACCESSED
STEP_FINISHED
```

Onde são emitidos:

- `ExecutionPlan` entra/sai de steps e grava `STEP_STARTED`/`STEP_FINISHED`;
- `LocalCommandGateway` grava `COMMAND_SENT`;
- `SagaUnitOfWorkService.registerChanged(...)` grava `AGGREGATE_ACCESSED` para writes;
- `DynamicEvidenceTestExecutionListener` acrescenta identidade do teste;
- `DynamicEvidenceContext` transporta contexto de step e `inputVariantId`.

Importante:

> Dynamic evidence é opt-in e deve ser no-op quando desactivada. Não deve partir a execução normal da aplicação.

#### Join: `DynamicEvidenceJoiner`

O join tem duas vias.

Via 1 — exact por `inputVariantId`:

```text
runtime event tem inputVariantId
        ↓
plano contém esse inputVariantId
        ↓
MATCHED_EXACT
```

Via 2 — fallback por identidade:

Usa:

- nome/FQN da functionality;
- step name;
- test class/method/display name;
- owners do `InputVariant`;
- plano que contém o step.

Estados possíveis:

```text
MATCHED_EXACT
MATCHED_HIGH_CONFIDENCE
MATCHED_PARTIAL
AMBIGUOUS
UNMATCHED
NOT_COVERED
```

Regras de interpretação:

- `MATCHED_EXACT`: runtime deu `inputVariantId` directamente.
- `MATCHED_HIGH_CONFIDENCE`: identidade de teste e plano apontam para um input único.
- `MATCHED_PARTIAL`: há evidência relevante mas falta identidade completa.
- `AMBIGUOUS`: há vários candidatos plausíveis.
- `UNMATCHED`: há eventos mas não casam com o plano.
- `NOT_COVERED`: não há eventos para enriquecer.

#### Outputs sidecar

O writer produz:

```text
scenario-catalog-enriched.jsonl
scenario-catalog-enriched-manifest.json
dynamic-evidence-join-report.json
```

Cada linha enriched contém:

```text
EnrichedScenarioRecord
  scenarioPlanId
  scenarioPlan       // o plano estático embebido
  dynamicEvidence    // join status + observed evidence
```

Ponto central:

> O enriched catalog embrulha o `ScenarioPlan`; não reescreve o `ScenarioPlan` original nem cria cenários novos.

#### Evidência a mostrar

Antes da atribuição directa por `inputVariantId`:

```text
MATCHED_EXACT: 0
MATCHED_HIGH_CONFIDENCE: 2
AMBIGUOUS: 44
UNMATCHED: 20
warningCount: 8238
```

Baseline actual após correcção de ownership de fixtures/setup/helpers:

```text
MATCHED_EXACT: 435
MATCHED_HIGH_CONFIDENCE: 125
MATCHED_PARTIAL: 0
AMBIGUOUS: 0
UNMATCHED: 24
NOT_COVERED: 0
unmatchedReasonCounts:
  FAILED_TEST_CLASS: 8
  NOT_SELECTED_TEST_CLASS: 7
  HELPER_OWNER_MISMATCH: 0
  UNCLASSIFIED: 9
```

Interpretação:

> A atribuição directa e a separação entre proveniência, call context e owner melhoraram muito a precisão do join. O ponto fraco deixou de ser a grande massa de `UNMATCHED=184` causada por helpers/setup e passou a ser um residual pequeno de `UNMATCHED=24`, com 9 casos ainda sem classificação fina.

#### Limitações actuais

- O join ainda não usa suficientemente command fields, aggregate IDs, literal runtime values ou keys para resolver todos os casos.
- ThreadLocal context em steps assíncronos tem limitações já reconhecidas no código.
- Stream/gRPC/distributed/TCC parity não está estabelecida.
- Dynamic enrichment é sidecar: não corrige a estrutura estática.
- A baseline actual tem `UNMATCHED=24`; os `UNCLASSIFIED=9` precisam de triagem antes de justificar uma fase de runtime-value matching.

#### Resumo desta fase

Input:

```text
ScenarioPlan estático + testes reais da aplicação
```

Output:

```text
dynamic-input-map.json
dynamic-evidence.jsonl
scenario-catalog-enriched.jsonl
dynamic-evidence-join-report.json
```

Claim correcto:

> Dynamic enrichment confirma e contextualiza parte do catálogo estático com evidência runtime, melhorando a atribuição sem alterar o contrato estático.

Claim a evitar:

> Não devo dizer que dynamic enrichment torna cenários não descobertos em cenários estáticos, nem que resolve genericamente todos os inputs.

Perguntas que devo conseguir responder:

- O que é que dynamic enrichment adiciona?
- Porque é que não deve criar cenários estáticos novos?
- Como o `inputVariantId` chega ao runtime event?
- Que casos continuam ambíguos?
- Que evidência runtime ainda não é usada no join?

Snippet útil a escolher:

- Um evento JSONL com `inputVariantId`.
- Ou o resumo de `dynamic-evidence-join-report.json`.

### 6.6 POC do ScenarioExecutor

Objectivo desta fase:

> Provar uma ponte inicial entre catálogo gerado e execução real da aplicação, sem ainda prometer execução genérica.

#### Código principal

CLI/runtime:

- `executor/ScenarioExecutorCli.java`
- `executor/ScenarioExecutorOrchestrator.java`
- `executor/ScenarioRuntimeContext.java`
- `executor/ScenarioExecutorOptions.java`

Leitura de catálogo:

- `executor/ScenarioCatalogReader.java`
- `executor/CatalogScenarioRecord.java`

Execução:

- `executor/ScenarioExecutor.java`
- `executor/ScenarioMaterializer.java`
- `executor/ScenarioExecutorMaterializationPolicy.java`
- `executor/ScenarioExecutorReadinessEvaluator.java`
- `executor/ScenarioExecutionReport.java`

Doc de referência:

- `../../reference/scenario-executor.md`

#### Fluxo conceptual

```text
scenario-catalog.jsonl ou scenario-catalog-enriched.jsonl
        ↓
ScenarioCatalogReader
        ↓
seleccionar ScenarioPlan
        ↓
validar shape suportado
        ↓
ScenarioMaterializer materializa argumentos
        ↓
instanciar Saga por reflection
        ↓
criar SagaUnitOfWork
        ↓
executeUntilStep(stepName, unitOfWork) por cada scheduled step
        ↓
scenario-execution-report.json
```

#### CLI

Forma conceptual:

```bash
java -cp <target-app-classpath>:<verifier-classes> \
  pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli \
  --spring-application-class <target.SpringApplication> \
  --spring-profiles local,sagas \
  --catalog-path <run-dir>/scenario-catalog.jsonl \
  --output-path <run-dir>/scenario-execution-report.json \
  --scenario-id <scenario-plan-id>
```

O Docker Compose wrapper prepara classpath, compila a app alvo e corre esta CLI.

#### Leitura de catálogo

`ScenarioCatalogReader` aceita:

- catálogo estático;
- catálogo enriquecido.

Se o ficheiro contém `enriched`, lê `EnrichedScenarioRecord` e usa:

```text
record.scenarioPlan()
record.dynamicEvidence().joinStatus()
```

A execução continua a usar o `ScenarioPlan` estático embebido. O join status só ajuda na auto-selecção.

#### Selecção de cenário

Dois modos:

- `EXPLICIT`: `--scenario-id` fornecido;
- `AUTO`: escolhe o primeiro candidato suportado/materializável.

Prioridade em catálogo enriquecido:

```text
MATCHED_EXACT
MATCHED_HIGH_CONFIDENCE
MATCHED_PARTIAL
AMBIGUOUS
UNMATCHED
NOT_COVERED
sem join status
```

#### Shape suportado

`ScenarioExecutor.validate(...)` só suporta:

```text
ScenarioKind.SINGLE_SAGA
exactamente 1 SagaInstance
InputVariant correspondente presente
stepId convertível para runtime step name
```

Não suporta:

- multi-saga;
- schedules concorrentes reais;
- fault bits;
- behaviour CSV;
- impact scoring;
- GA/bandit.

#### Materialização

`ScenarioMaterializer` consome `InputRecipe`.

Kinds suportados:

```text
literal
placeholder, só se runtime-owned
constructor
collection
local_transform: toSet
helper_result
property_access
```

Kinds bloqueados:

```text
call_result -> UNSUPPORTED_CALL_RESULT
unresolved -> UNRESOLVED_VALUE
event_placeholder -> não chega como executor-ready e bloqueia via EVENT_PAYLOAD_PLACEHOLDER
unsupported recipe kind -> UNSUPPORTED_RECIPE_KIND
```

Argumentos runtime-owned:

```text
SagaUnitOfWorkService
CommandGateway
SagaUnitOfWork
```

Estes são resolvidos pelo executor, não pela recipe:

- beans Spring para `SagaUnitOfWorkService` e `CommandGateway`;
- criação directa de `SagaUnitOfWork` para a functionality.

Isto explica a diferença:

```text
staticRecipeReadyInputVariantCount=0
executorMaterializableInputVariantCount=94
```

Porque:

- a recipe bruta pode não estar totalmente pronta;
- mas o executor sabe fornecer alguns argumentos que pertencem ao runtime.

#### Readiness accounting

`ScenarioExecutorReadinessEvaluator` calcula duas coisas diferentes:

```text
staticRecipeReady
materializable
```

`staticRecipeReady`:

- vem de `inputRecipe.executorReady`;
- não aplica overrides runtime-owned.

`materializable`:

- simula a política actual do executor;
- considera runtime-owned como resolvíveis;
- reporta blockers reais para o POC.

Esta separação é uma melhoria importante para a explicação da tese.

#### Execução dos steps

Para cada scheduled step:

1. extrai runtime step name de `stepId`;
2. chama por reflection:

```text
executeUntilStep(String stepName, UnitOfWork unitOfWork)
```

3. grava `StepOutcome`:

```text
COMPLETED
FAILED
DRY_RUN
```

Terminal statuses possíveis incluem:

```text
SUCCESS
DRY_RUN
SELECTION_FAILED
UNSUPPORTED_SCENARIO
MATERIALIZATION_FAILED
STARTUP_FAILED
STEP_EXECUTION_FAILED
```

#### Execution report

`ScenarioExecutionReport` contém:

```text
schemaVersion
terminalStatus
catalogPath
catalogKind
selectionMode
selectionReason
requestedScenarioPlanId
scenarioPlanId
sagaInstanceId
sagaFqn
inputVariantId
stepOutcomes
skippedCandidateCounts
blockers
```

Isto é útil porque diferencia:

- falha de selecção;
- shape não suportado;
- falha de materialização;
- falha de startup;
- falha durante step.

#### Smoke evidence

Smoke Quizzes:

```text
Scenario plan id: 2f0c64a371fcd65b5a38f294ccbda93a42df060c3d1e5b7dcedf43568abcf661
Saga: GetCourseExecutionsFunctionalitySagas
Step: getCourseExecutionsStep
Terminal status: SUCCESS
```

Interpretação:

> O POC prova que pelo menos um `ScenarioPlan` gerado consegue atravessar catálogo → materialização → Spring runtime → execução de step. Não prova execução genérica.

#### Resumo desta fase

Input:

```text
ScenarioPlan estático/enriquecido + Spring app alvo
```

Output:

```text
scenario-execution-report.json
console trace de steps
```

Claim correcto:

> Existe uma ponte inicial e demonstrável entre o catálogo e execução real para um subconjunto single-saga materializável.

Claim a evitar:

> Não devo apresentar isto como executor genérico nem como fault-injection engine.

Perguntas que devo conseguir responder:

- Porque é que este POC executa apenas single-saga suportado?
- O que bloqueia event-origin inputs?
- Porque é que `staticRecipeReady=0` não contradiz `executorMaterializable=94`?
- Que decisão falta: consumir JSONL directamente ou gerar behaviour CSV como adapter?
- Qual é o próximo corte mínimo para transformar o POC numa baseline de fault injection?

Snippet útil a escolher:

- Command shape do executor.
- Ou parte de `ScenarioExecutionReport` com `terminalStatus`, `stepOutcomes` e `blockers`.

### 6.7 Snippets curtos para mostrar

Objectivo desta secção:

> Ter exemplos pequenos para explicar cada parte sem transformar a reunião numa leitura de código.

#### Snippet 1 — ordem da pipeline no `ScenarioGeneratorApplication`

Ficheiro:

```text
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ScenarioGeneratorApplication.java
```

Excerto conceptual:

```java
CommandHandlerIndexVisitor index = new CommandHandlerIndexVisitor();
ServiceVisitor services = new ServiceVisitor();
CommandHandlerVisitor handlers = new CommandHandlerVisitor();
WorkflowFunctionalityVisitor workflows = new WorkflowFunctionalityVisitor();
WorkflowFunctionalityCreationSiteVisitor creationSites = new WorkflowFunctionalityCreationSiteVisitor();
EventHandlingBridgeVisitor eventBridge = new EventHandlingBridgeVisitor();
GroovyConstructorInputTraceVisitor groovy = new GroovyConstructorInputTraceVisitor();
```

Explicação curta:

> Primeiro descubro dispatch targets, depois classifico serviços, depois mapeio comandos, depois extraio Sagas, depois ligo facades/eventos, e só no fim extraio inputs dos testes.

#### Snippet 2 — step → command → footprint

Ficheiro:

```text
visitor/WorkflowFunctionalityVisitor.java
```

Excerto conceptual:

```java
state.getCommandDispatchInfo(creation.getType()).ifPresent(info -> {
    StepDispatchFootprint dispatch = new StepDispatchFootprint(
        stepKey,
        commandTypeFqn,
        info.aggregateName(),
        info.accessPolicy(),
        phase,
        inferDispatchMultiplicity(creation),
        inferAggregateKeyText(creation),
        inferAggregateKeyConfidence(creation));
    stepBlock.addDispatch(dispatch);
});
```

Explicação curta:

> Um `SagaStep` contém um `Command`. O command handler diz que agregado e serviço esse comando toca. Daí nasce o footprint do step.

#### Snippet 3 — source mode filtering

Ficheiro:

```text
scenario/InputVariantNormalizer.java
```

Excerto conceptual:

```java
return switch (sourceMode) {
    case TCC -> SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG;
    case MIXED -> SOURCE_MODE_MIXED_REJECTED_FOR_SAGA_CATALOG;
    case SAGAS, UNKNOWN -> null;
};
```

Explicação curta:

> Inputs claramente TCC/mistos são rejeitados diagnosticamente. Inputs `UNKNOWN` ficam aceites com warning para não perder cobertura.

#### Snippet 4 — event payload placeholder

Ficheiro:

```text
scenario/adapter/InputRecipeMapper.java
```

Excerto conceptual:

```java
return InputRecipeNode.builder("event_placeholder")
    .executorReady(false)
    .blockers(List.of("EVENT_PAYLOAD_PLACEHOLDER"))
    .placeholderPurpose("event_payload")
    .build();
```

Explicação curta:

> O verificador consegue descobrir que o input vem de um evento, mas o executor ainda não sabe reconstruir o payload real. Por isso o input pode ser aceite estaticamente e bloqueado para execução.

#### Snippet 5 — shape do `ScenarioPlan`

Ficheiro:

```text
scenario/model/ScenarioPlan.java
```

Shape simplificado:

```java
record ScenarioPlan(
    String deterministicId,
    ScenarioKind kind,
    List<SagaInstance> sagaInstances,
    List<InputVariant> inputs,
    List<ScheduledStep> expandedSchedule,
    FaultSpace faultSpace,
    List<ConflictEvidence> conflictEvidence,
    List<String> warnings)
```

Explicação curta:

> O `ScenarioPlan` é o contrato estático: que Sagas, que inputs, que schedule, que fault space e que conflitos justificam o cenário.

#### Snippet 6 — conflict graph ignora read/read

Ficheiro:

```text
scenario/ConflictGraphBuilder.java
```

Excerto conceptual:

```java
if (left.accessMode() == READ && right.accessMode() == READ) {
    readReadIgnored++;
    continue;
}
```

Explicação curta:

> Duas leituras sobre o mesmo agregado não são consideradas conflito porque não devem alterar o resultado observável por si só.

#### Snippet 7 — segment compression por anchors

Ficheiro:

```text
scenario/ScheduleEnumerator.java
```

Excerto conceptual:

```java
List<List<StepDefinition>> segments = anchorSegments(
    input.steps(),
    conflictAnchorIndexes(input, conflictCandidates));
```

Exemplo para explicar no quadro:

```text
Saga A: a1, a2*, a3, a4*
Saga B: b1*, b2, b3*

Segmentos:
Saga A: [a1,a2*], [a3,a4*]
Saga B: [b1*], [b2,b3*]
```

Explicação curta:

> Intercalo segmentos que terminam em passos conflituosos, em vez de intercalar todos os passos independentes individualmente.

#### Snippet 8 — dynamic input attribution

Ficheiro:

```text
simulator/src/main/java/.../monitoring/dynamic/DynamicInputMap.java
```

Excerto conceptual:

```java
candidates = inputs.stream()
    .filter(entry -> entry.matches(testIdentity, functionalityClassFqn, stepName))
    .toList();

if (candidateIds.size() == 1) {
    return matched(inputVariantId);
}
```

Explicação curta:

> Runtime attribution só escreve `inputVariantId` quando a identidade do teste, a classe da functionality e o step apontam para uma variante única.

#### Snippet 9 — join exact por `inputVariantId`

Ficheiro:

```text
dynamic/DynamicEvidenceJoiner.java
```

Excerto conceptual:

```java
if (!exactEvents.isEmpty()) {
    return record(plan, MATCHED_EXACT, matchedIds, exactEvents, List.of());
}
```

Explicação curta:

> Quando o runtime já traz `inputVariantId`, o join deixa de depender de heurísticas e passa para `MATCHED_EXACT`.

#### Snippet 10 — executor só aceita single-saga

Ficheiro:

```text
executor/ScenarioExecutor.java
```

Excerto conceptual:

```java
if (plan.kind() != SINGLE_SAGA || plan.sagaInstances().size() != 1) {
    blocker("UNSUPPORTED_SCENARIO_SHAPE");
}
```

Explicação curta:

> O POC é intencionalmente estreito. Multi-saga e fault injection ainda são próximos passos, não comportamento actual.

#### Snippet 11 — runtime-owned arguments

Ficheiro:

```text
executor/ScenarioExecutorMaterializationPolicy.java
```

Excerto conceptual:

```java
SagaUnitOfWorkService
CommandGateway
SagaUnitOfWork
```

Explicação curta:

> Estes argumentos pertencem ao runtime, não ao teste. O executor pode fornecê-los mesmo quando a recipe estática não está totalmente pronta.

#### Snippet 12 — relatório do executor

Ficheiro:

```text
executor/ScenarioExecutionReport.java
```

Shape simplificado:

```java
record ScenarioExecutionReport(
    String terminalStatus,
    String scenarioPlanId,
    String sagaFqn,
    String inputVariantId,
    List<StepOutcome> stepOutcomes,
    Map<String,Integer> skippedCandidateCounts,
    List<Blocker> blockers)
```

Explicação curta:

> Este report distingue selecção, materialização, startup e execução de steps. Isso é essencial para a futura baseline de fault injection.

## 7. Como correr / demonstrar

### 7.1 Testes focados do verificador

```bash
cd verifiers
mvn test -Dtest=SourceModeClassifierSpec,GroovySourceIndexSpec
mvn test -Dtest=GroovyConstructorInputTraceVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,ScenarioGeneratorSpec
mvn test -Dtest=ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,ApplicationsFileTreeParserSpec
```

### 7.2 Testes focados de dynamic enrichment

```bash
cd verifiers
mvn test -Dtest=DynamicEvidenceReaderSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec -DfailIfNoTests=false
mvn test -Dtest='*Dynamic*Spec,*Enriched*Spec' -DfailIfNoTests=false
```

### 7.3 Baseline dinâmica Quizzes via Docker Compose

A baseline dinâmica deve correr com o perfil `test` activo, senão as classes async com `@SpringBootTest` podem falhar por falta de datasource/dialect.

```bash
CATALOG_PATH=/dev/null \
MEDIUM_MEM_LIMIT=2g \
MEDIUM_MEM_RESERVATION=1g \
MEDIUM_CPUS=2 \
docker compose run --rm -T \
  -e SPRING_PROFILES_ACTIVE=test,sagas,local \
  -e VERIFIERS_APPLICATION_BASE_DIR=quizzes \
  -e VERIFIERS_OUTPUT_ROOT=/reports/2026-06-29-dynamic-baseline-test-profile \
  -e VERIFIERS_SCENARIO_CATALOG_ENABLED=true \
  -e VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=WRITE_PLANS \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=1 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_CATALOG_SCENARIOS=2000 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=100000 \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=true \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_ALLOW_PARTIAL_TEST_RUN=true \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_INCLUDE_TEST_DIRS=pt/ulisboa/tecnico/socialsoftware/quizzes/sagas \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_EXCLUDE_TEST_DIRS=pt/ulisboa/tecnico/socialsoftware/quizzes/causal,pt/ulisboa/tecnico/socialsoftware/quizzes/tcc \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_EXCLUDE_TEST_CLASSES=CreateTournamentDynamicEvidenceSmokeTest,DynamicEvidenceDisabledSmokeTest \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_PER_TEST_TIMEOUT_SECONDS=1800 \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_MAVEN_PROFILE=test-sagas \
  fault-analysis-scenario-gen
```

`CATALOG_PATH=/dev/null` só existe porque o mesmo `docker-compose.yml` também define `scenario-executor`, que exige essa variável durante a interpolação do Compose.

### 7.4 ScenarioExecutor via Docker Compose

Exemplo pinned Quizzes smoke:

```bash
CATALOG_PATH=/reports/structured-input-recipes-quizzes-smoke/quizzes-20260520-175058-455/scenario-catalog.jsonl \
SCENARIO_ID=2f0c64a371fcd65b5a38f294ccbda93a42df060c3d1e5b7dcedf43568abcf661 \
docker compose run --rm scenario-executor
```

Exemplo configurável:

```bash
APPLICATION_BASE_DIR=quizzes \
SPRING_APPLICATION_CLASS=pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator \
MAVEN_PROFILE=test-sagas \
SPRING_PROFILES=test,sagas,local \
CATALOG_PATH=/reports/some-run/scenario-catalog.jsonl \
OUTPUT_PATH=/reports/scenario-executor/custom-report.json \
docker compose run --rm scenario-executor
```

### 7.5 Onde procurar artefactos

Directórios típicos:

```text
verifiers/target/<application>-<timestamp>/
verifiers/target/<run-name>/<application>-<timestamp>/
```

Artefactos importantes:

```text
scenario-catalog.jsonl
scenario-catalog-manifest.json
scenario-space-accounting.json
scenario-catalog-enriched.jsonl
dynamic-evidence-join-report.json
maven-output.log
analysis-report.html
scenario-execution-report.json
```

## 8. O que não devo reivindicar

Não dizer que:

- o executor é genérico;
- multi-saga runtime execution está implementado;
- fault injection gerado está implementado;
- impact scoring está implementado;
- GA/local search está implementado;
- bandit/RL prioritization está implementado;
- TCC, stream, gRPC ou distributed runtime parity estão estabelecidos;
- accepted static input significa executor-ready;
- dynamic enrichment redefine o catálogo estático;
- segment compression prova completude semântica absoluta.

Dizer em vez disso:

> A contribuição implementada é a frente da pipeline: extracção estática, geração determinística de catálogos, accounting/redução de schedules, enriquecimento dinâmico sidecar e uma ponte inicial para execução. A orientação agora é congelar o verificador num nível suficiente e construir o menor caminho credível para execução, fault injection e análise de impacto.

## 9. Decisões/perguntas a pedir ao orientador

Decisão de alto nível já assumida:

> Não continuar indefinidamente a aumentar cobertura e genericidade do verificador. Avançar para as próximas fases da tese, mesmo com cobertura incompleta, para ainda conseguir entregar execução, fault analysis e algum componente de pesquisa/priorização até Outubro.

Perguntas úteis que ainda ficam:

1. Qual é o menor executor/fault-injection baseline aceitável para a tese?
   - single-saga primeiro?
   - multi-saga bounded só para um subconjunto?
   - apenas inputs materializáveis?
2. Para a tese, o ScenarioExecutor tem de ser genérico ou pode ser uma baseline estreita mas bem justificada?
3. Que partes do verificador devem ser consideradas “good enough” e congeladas?
4. A segment compression é uma contribuição suficiente para a parte de scenario generation, se for apresentada com as suas limitações?
5. Qual deve ser o primeiro impact metric aceitável?
   - violações de invariantes;
   - excepções não tratadas;
   - compensações incompletas;
   - divergência de estado final;
   - aborts de Sagas.
6. Qual é o scope mínimo realista para GA/bandit até Outubro?
   - GA num cenário fixo?
   - GA em alguns cenários materializáveis?
   - bandit como protótipo sobre rewards já calculados?
7. Devemos gerar behaviour CSV como formato intermédio ou consumir JSONL directamente no executor?
8. É aceitável usar type-only conflict evidence como fallback conservador nas experiências iniciais?
9. Como avaliar honestamente o trabalho se a execução genérica ficar limitada?

## 10. Próximos passos prováveis

Direcção assumida — avançar execução/fault injection:

1. Definir o contrato mínimo do ScenarioExecutor.
2. Escolher JSONL directo vs behaviour CSV adapter.
3. Implementar uma baseline repetível para cenários single-saga materializáveis.
4. Adicionar fault bits simples aos passos executados.
5. Produzir um execution report estável com sucesso/falha/exception/estado terminal.
6. Adicionar primeira métrica de impacto simples.
7. Implementar GA local sobre um cenário fixo ou pequeno conjunto de cenários.
8. Só depois considerar bandit/priorização sobre cenários.

Trabalho de verificador que ainda pode valer a pena, mas deve ser bounded:

1. Fazer um refresh dinâmico curto para não usar números obsoletos.
2. Classificar as 32 Sagas sem inputs aceites apenas se isso desbloquear execução/avaliação.
3. Melhorar event payload materialization apenas para shapes necessários à baseline.
4. Melhorar aggregate-instance key binding apenas onde afecta os cenários escolhidos.

Recomendação a discutir:

> Congelar a ambição de cobertura genérica. Fazer apenas melhorias cirúrgicas no verificador quando desbloquearem a baseline executável. O foco deve passar para execução repetível, fault injection, impact scoring e, pelo menos, uma versão pequena de GA/local search.

## 11. Lacunas pessoais de entendimento a esclarecer durante a preparação

Antes da reunião devo conseguir explicar sem hesitar:

- O que é exactamente um `ScenarioPlan`.
- O que é um `InputVariant`.
- A diferença entre `accepted`, `blocked`, `staticRecipeReady` e `executorMaterializable`.
- Como event-origin inputs são descobertos.
- Porque é que event payload placeholders bloqueiam materialização.
- Como o dynamic join decide `MATCHED_EXACT` vs `AMBIGUOUS`.
- Como segment compression reduz interleavings.
- O que o ScenarioExecutor POC executa realmente.
- Qual é a menor baseline executável que ainda serve a avaliação da tese.
