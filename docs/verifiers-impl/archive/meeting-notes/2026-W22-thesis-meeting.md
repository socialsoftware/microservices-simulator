# Notas para a reunião de tese — 2026-W22

Data: 2026-05-26

Âmbito: mudanças desde quinta-feira, 2026-05-21, sem contar esse dia. Como não há commits novos nesse intervalo, esta nota resume as alterações locais atuais.

## Resumo numa frase

O verificador já não fica só na geração de cenários: agora existe um primeiro executor pequeno que lê um cenário gerado, reconstrói os dados de entrada que consegue, arranca a aplicação alvo e executa passos reais da saga.

## O que mudou desde quinta-feira

- Foi criado o primeiro protótipo do `ScenarioExecutor`.
- Foi escrito um PRD para esse protótipo em `issues/scenario-executor-poc/prd.md`.
- As 8 fatias desse PRD foram implementadas e movidas para `issues/scenario-executor-poc/done/`.
- O QA desse protótipo está marcado como `PASS` em `issues/scenario-executor-poc/qa-report.md`.
- O executor consegue ler `scenario-catalog-enriched.jsonl` quando existe.
- Se não houver catálogo enriquecido, o executor usa `scenario-catalog.jsonl`.
- O executor aceita um `--scenario-id`, que é o id do `ScenarioPlan`.
- Sem `--scenario-id`, o executor escolhe de forma determinística o primeiro cenário simples que consegue executar.
- O protótipo só suporta cenários de uma saga, com uma ordem simples de passos.
- O executor reconstrói alguns dados de entrada a partir das receitas já exportadas pelo verificador.
- Já há suporte para literais, DTOs simples, construtores, setters, listas, sets, maps, `toSet`, resultados de helpers e acesso simples a propriedades.
- O executor também consegue obter alguns argumentos do ambiente de execução: `SagaUnitOfWorkService`, `CommandGateway` e `SagaUnitOfWork`.
- Valores não resolvidos continuam bloqueados. O executor não inventa dados.
- O executor arranca a aplicação alvo num processo/JVM separado, com a classpath da aplicação alvo.
- O Quizzes não passou a ser dependência de compilação do módulo `verifiers`.
- O Quizzes não precisou de alterações no código ou nos testes para este teste.
- O executor chama `executeUntilStep(...)` para cada passo agendado.
- O protótipo não chama `resumeWorkflow(...)`.
- O executor escreve um relatório próprio com o estado final, cenário escolhido, saga, dados de entrada, passos executados e bloqueios.
- Falhas de seleção, materialização, arranque e execução de passos ficam separadas no relatório.

## Teste real no Quizzes

Foi executado um cenário real gerado pelo verificador para o Quizzes.

- Catálogo usado: `verifiers/target/structured-input-recipes-quizzes-smoke/quizzes-20260520-175058-455/scenario-catalog.jsonl`.
- `ScenarioPlan` id: `2f0c64a371fcd65b5a38f294ccbda93a42df060c3d1e5b7dcedf43568abcf661`.
- Saga: `GetCourseExecutionsFunctionalitySagas`.
- Passo executado: `getCourseExecutionsStep`.
- Resultado: `SUCCESS`.
- Relatório: `/tmp/opencode/quizzes-execution-report-get-course-executions.json`.

Isto prova uma coisa concreta: pelo menos um cenário que saiu do catálogo conseguiu chegar ao ambiente de execução do Quizzes e executar um passo real da saga.

## Como dizer isto na reunião

Na semana passada eu tinha o catálogo de cenários e as receitas dos dados de entrada. Ou seja, o verificador já conseguia dizer: "este é um cenário possível" e, em alguns casos, "é assim que se pode reconstruir o dado de entrada usado pela saga".

Desde sexta-feira, dei o passo seguinte. Fiz um executor pequeno que pega num cenário do catálogo, tenta reconstruir os dados de entrada, arranca a aplicação alvo e chama os passos da saga pela ordem gerada pelo verificador.

Ainda é um protótipo estreito. Não executa qualquer cenário, não cria fixtures de base de dados e não faz injeção de falhas. Mas já funcionou num caso real do Quizzes: o cenário de `GetCourseExecutionsFunctionalitySagas` foi materializado, a saga foi criada, e o passo `getCourseExecutionsStep` terminou com sucesso.

A forma simples de explicar é: antes eu tinha o mapa e algumas receitas. Agora já consegui cozinhar uma dessas receitas e dar um passo real no percurso.

## O que isto prova

- O catálogo já pode ser usado como entrada de uma execução real, pelo menos num caso simples.
- As receitas dos dados de entrada não são só documentação: algumas já são suficientes para criar objetos em execução.
- O caminho `catálogo -> dados de entrada -> saga -> executeUntilStep(...) -> relatório` já existe.
- O executor consegue falhar de forma explicável quando encontra algo que ainda não suporta.
- O protótipo mantém a fronteira certa: o executor escreve os seus próprios relatórios e não altera o catálogo estático nem os ficheiros laterais de enriquecimento dinâmico.

## O que não devo prometer ainda

- Não é execução genérica de todos os cenários.
- Não há ainda geração automática de fixtures/base de dados.
- Não há execução multi-saga.
- Não há injeção de falhas.
- Não há geração de CSV de comportamento.
- Não há ainda scoring de impacto no domínio.
- Não há ainda pesquisa automática para escolher falhas ou cenários.
- O resultado do executor ainda não é evidência de enriquecimento dinâmico e não é juntado de volta ao catálogo enriquecido.

## Evidência de validação

- `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec' -DfailIfNoTests=false`: PASS, 10 testes, 0 falhas, 0 erros.
- Teste com CLI separada no Quizzes: PASS, estado final `SUCCESS`.
- QA do PRD: `issues/scenario-executor-poc/qa-report.md` marcado como `PASS`.
- Verificação de fronteira: sem alterações em `applications/quizzes/src/` para este protótipo.

## Nota paralela sobre profiling e registos em runtime

Também foi preparado um guião para discutir a ligação entre o meu trabalho e o trabalho de profiling/performance do Martim.

A ideia simples é esta:

- AOP com `@Around` é bom para medir ou envolver chamadas técnicas, como serviços, comandos, capacidade e delays.
- Os meus hooks internos continuam a fazer sentido quando preciso de contexto da saga, como o nome do passo, o `inputVariantId` e a unidade de trabalho.
- A proposta limpa é ter uma camada comum de observações em runtime, em vez de dois mecanismos separados.
- Essa camada poderia emitir eventos comuns para performance e para enriquecimento de cenários.

## Próximos passos sugeridos

1. Decidir se este protótipo já é suficiente como marco de "primeira execução real de cenário".
2. Melhorar o executor com uma estratégia clara para fixtures/base de dados.
3. Aumentar a cobertura de receitas de dados de entrada só com casos reais que apareçam nos catálogos.
4. Depois disso, avançar para falhas, comportamento observado e scoring.
5. Se fizer sentido juntar com o trabalho do Martim, definir primeiro o formato comum dos eventos em runtime.
