# Notas para a reunião de tese — 2026-W23

Data: 2026-06-02

Âmbito: mudanças e enquadramento desde terça-feira, 2026-05-26, às 12:00 em Portugal. Não há commits novos nesse intervalo; esta nota cruza as alterações locais atuais com a direção de trabalho para discutir na reunião.

## Resumo numa frase

Esta semana consolidou-se o executor como primeiro POC estreito e, mais importante, começou-se a transformar o trabalho num problema de avaliação: comparar o catálogo gerado com uma baseline brute-force limitada, perceber onde se perdem cenários executáveis, e preparar a passagem para escrita da dissertação, execução em escala e pesquisa com algoritmo genético.

## O que mudou de facto

- O PRD do `ScenarioExecutor` POC ficou fechado em `issues/scenario-executor-poc/prd.md`.
- As 8 fatias do PRD estão implementadas em `issues/scenario-executor-poc/done/`.
- O QA do PRD está marcado como `PASS` em `issues/scenario-executor-poc/qa-report.md`.
- Existe código novo em `verifiers/src/main/java/.../faults/executor/` para ler catálogos, selecionar cenários, materializar argumentos suportados, arrancar a aplicação alvo e escrever relatórios de execução.
- Existem testes em `verifiers/src/test/groovy/.../faults/executor/` para seleção, materialização, falhas estruturadas, CLI/orquestração e comportamento do executor.
- Foi adicionada documentação de uso em `docs/verifiers-impl/scenario-executor-poc.md`.
- O `docker-compose.yml` tem agora um serviço `scenario-executor` para correr o executor contra um catálogo existente.
- `ScenarioGeneratorApplication` passou a poder ser desligada por propriedade (`verifiers.application.enabled=false`), para o executor arrancar o contexto da aplicação alvo sem ativar a aplicação Spring do verificador dentro desse processo.
- O glossário foi atualizado com a linguagem de comparação: universo brute-force, universo input-bound, espaço type-level, universo interagente, catálogo gerado, accounting lens, schedule strategy e execução fault-free.
- Foi clarificada uma limitação importante: `SEGMENT_COMPRESSED` ainda não é compressão de segmentos ao estilo da tese. Para claims públicas e baselines, usar `SERIAL` ou `ORDER_PRESERVING_INTERLEAVING`.
- Foi preparado um guião separado sobre a possível arquitetura comum entre profiling/performance e enriquecimento runtime.

## Evidência que posso citar

- QA do PRD: `issues/scenario-executor-poc/qa-report.md` com veredito `PASS`.
- Testes do executor registados no QA: `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec' -DfailIfNoTests=false`, `PASS`, 10 testes, 0 falhas, 0 erros.
- Smoke real do Quizzes registado no QA e na documentação: cenário `GetCourseExecutionsFunctionalitySagas`, passo `getCourseExecutionsStep`, estado final `SUCCESS`.
- Constraint preservada: sem alterações em `applications/quizzes/src/` para o POC do executor.

## O que não avançou muito

- Não aumentou significativamente o número de cenários que conseguem ser efetivamente reproduzidos/replayed.
- O executor continua a ser um POC estreito, não um replay arbitrário do catálogo.
- O smoke real continua centrado num caso simples do Quizzes: `GetCourseExecutionsFunctionalitySagas`, com o passo `getCourseExecutionsStep` a terminar com `SUCCESS`.
- Ainda não há execução multi-saga, geração de fixtures/base de dados, injeção de falhas, scoring de impacto no domínio, nem pesquisa automática.
- A comparação brute-force ainda está na fase de enquadramento/contabilidade e terminologia; não deve ser apresentada como um gerador brute-force completo já implementado.

## Como enquadrar na reunião

Na semana passada o resultado principal era: o catálogo deixou de ser apenas descritivo, porque já existe um primeiro executor que consegue pegar num cenário real, materializar o que sabe, arrancar a aplicação alvo e executar um passo da saga.

Esta semana o avanço é menos sobre aumentar imediatamente a cobertura de replay e mais sobre tornar o trabalho avaliável. Ou seja, começar a responder a perguntas como:

- qual é o universo total de cenários que uma baseline brute-force limitada teria de considerar;
- qual é a parte desse universo que o gerador atual emite;
- qual é a parte que é admissível para o executor atual;
- qual é a parte que já foi executada fault-free;
- e em que ponto da pipeline cada cenário é perdido.

Esta mudança é importante porque transforma a implementação num argumento de tese. A contribuição já não é só "gerei cenários"; passa a ser "defini um espaço de cenários, gerei uma parte desse espaço com critérios de interação, comecei a ligar esses cenários a execução real, e agora consigo medir as limitações".

## Baseline e brute force

A comparação que está a emergir deve separar três coisas que antes eram fáceis de misturar:

- `Type-level shape space`: contagem explicativa sobre classes de saga e footprints de agregados, antes de exigir inputs concretos.
- `Input-bound brute-force universe`: baseline bounded que só conta cenários com inputs concretos aceites, tuples compatíveis e schedules possíveis.
- `Generated interacting catalog`: aquilo que o gerador atual realmente emite depois de aplicar interação, compatibilidade de inputs, estratégia de scheduling e caps.

Isto ajuda a evitar uma comparação injusta. O brute force para a avaliação não deve contar vetores de falhas nesta fase; as falhas são semântica de execução futura aplicada a um cenário, não parte da identidade estática do `ScenarioPlan`.

Também convém dizer que o gerador antigo de CSVs de comportamento não é uma baseline de cenário no sentido da tese. Esse gerador expande opções manuais de funcionalidades/passos para runs, mas não descobre automaticamente conjuntos de sagas, inputs, interações por agregados nem schedules.

## Executor e performance

O próximo salto técnico é tratar o executor como um conceito executável e escalável, não apenas como uma demo manual.

Pontos a discutir:

- o executor atual já define o caminho `catálogo -> input -> saga -> executeUntilStep(...) -> relatório`;
- para algoritmo genético, cada avaliação vai precisar de executar um cenário com uma configuração de falhas;
- se cada execução for pesada, a pesquisa pode demorar horas por aplicação;
- por isso, convém pensar cedo em isolamento, paralelismo e custo por execução;
- a arquitetura deve permitir múltiplos executores em paralelo, sem escrever no mesmo artefacto e sem depender de estado global escondido.

A ligação com o guião de profiling/performance é útil aqui: uma camada comum de observações runtime poderia servir tanto para métricas de performance como para evidência semântica do verificador.

## Escrita da dissertação

Mesmo sem a parte final de ML implementada, já há material suficiente para começar capítulos ou secções da dissertação:

- arquitetura do simulador e do módulo `verifiers`;
- pipeline `visitor/* -> ApplicationAnalysisState -> scenario/adapter/* -> scenario/* -> dynamic/*`;
- extração estática de sagas, passos, comandos, footprints e inputs;
- geração determinística de `ScenarioPlan`s;
- enriquecimento dinâmico como sidecar;
- receitas estruturadas de inputs;
- primeiro executor POC e as suas limitações;
- terminologia de avaliação: brute-force universe, interacting universe, generated catalog e scenario-space accounting.

A vantagem de começar já é que a escrita vai expor inconsistências de linguagem e de claims antes de a avaliação final ficar fechada.

## Machine learning e algoritmo genético

A investigação de ML deve começar pelo algoritmo genético de forma pragmática:

- indivíduo: configuração de falhas aplicada a um cenário selecionado;
- gene: escolha fault-free/fault/delay/falha por slot executável;
- fitness: impacto observável no domínio, não apenas anomalias infraestruturais;
- custo: número de execuções reais necessárias para avaliar uma população;
- risco: explosão combinatória se o executor não for eficiente e paralelizável.

O ponto para a reunião não é dizer que o GA está implementado. É perguntar se esta decomposição faz sentido: primeiro tornar cenários executáveis, depois definir impacto de domínio, depois pesquisar no espaço de fault configurations dentro de cada cenário.

## Testagem e aplicações de avaliação

A `dummyapp` atual continua útil, mas para um papel específico: quase unit tests dos visitors, materializer, geração de cenários e edge cases mecânicos.

Para avaliação mais convincente, faz sentido discutir uma segunda camada de testes/aplicação:

- uma aplicação compilável e executável dentro do ecossistema do simulador;
- com domínio realista, mas menor e mais controlável que o Quizzes;
- com invariantes de domínio claros;
- com fixtures ou setup reprodutível;
- com cenários desenhados para exercitar falhas, compensações e inconsistências de estado;
- que possa ser apresentada como "um programador usa o simulador e os verifiers para testar o seu sistema".

Pergunta concreta ao professor: devemos evoluir a `dummyapp` para algo mais executável, criar uma nova aplicação fixture, ou usar o Quizzes como alvo principal e aceitar a complexidade de fixtures/base de dados?

## O que dizer verbalmente

"Desde a última reunião não aumentei muito o número de cenários que consigo reproduzir/replay. O executor continua a ser um POC estreito. Mas fechei esse POC como PRD, com testes e QA, e agora comecei a virar o trabalho para a avaliação: definir qual é a baseline brute-force justa, separar o universo total de cenários do catálogo que o gerador emite, e perceber onde é que os cenários se perdem até chegarem a execução. Isto está a tornar as limitações mais explícitas, que é bom para a tese. Também acho que já há material suficiente para começar a escrever a parte da arquitetura, enumeração de cenários e pipeline atual. A seguir quero discutir duas decisões: como ligar isto a algoritmo genético sem tornar a execução demasiado lenta, e que tipo de aplicação de teste devemos usar para além da dummyapp, que hoje é muito boa para unit tests dos visitors mas não representa uma aplicação real compilável com domínio e invariantes."

## Perguntas para o orientador

1. A comparação principal deve ser contra o universo input-bound brute-force, contra o universo interagente, ou contra ambos com papéis diferentes?
2. Faz sentido excluir vetores de falhas da contagem de cenários nesta fase e tratá-los como espaço de execução/search separado?
3. O executor POC já é suficiente como marco de arquitetura para começar a escrever a dissertação até ao estado atual?
4. Antes de expandir replay coverage, devo investir em scenario-space accounting para explicar onde se perdem candidatos?
5. Para o algoritmo genético, a unidade de pesquisa deve ser "fault configurations dentro de um cenário" ou "cenário + fault configuration" em conjunto?
6. Devemos desenhar desde já execução paralela de cenários, ou esperar até haver fault injection e scoring?
7. A `dummyapp` deve continuar apenas como fixture mecânica, ou devemos criar uma aplicação compilável mais realista para avaliação controlada?
8. O Quizzes deve ser o alvo principal de avaliação, ou deve ser complementado por uma aplicação menor com invariantes e fixtures mais explícitos?

## Claim safety

- Dizer que existe um primeiro executor POC, não um executor genérico.
- Dizer que a cobertura de replay ainda é estreita.
- Dizer que brute-force/accounting está a ser definido, não que já está completamente implementado.
- Dizer que `SEGMENT_COMPRESSED` não deve ser usado como claim de compressão real.
- Dizer que GA, scoring de impacto e bandit prioritization continuam como próximos passos.
- Dizer que a `dummyapp` é boa para mecânica do verificador, mas não deve ser vendida como avaliação de domínio real.
