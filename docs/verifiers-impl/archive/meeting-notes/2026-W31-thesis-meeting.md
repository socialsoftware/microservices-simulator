# Reunião de tese — 2026-W31

## Escrita

- Concluída uma primeira versão da introdução do artigo, centrada no problema, espaço combinatório e contribuições.
- Tratados no texto os comentários do orientador aos Capítulos 2 e 4 da tese:
  - Capítulo 2 reorganizado entre microsserviços, DDD, concorrência, Sagas e simulador;
  - Capítulo 4 reorganizado entre problema, estratégia, arquitectura e implementação;
  - figuras e terminologia alinhadas com `WorkloadPlan` e `FaultScenario`.
- A tese compila; os comentários continuam abertos no Overleaf para validação do orientador.

## Implementação e evidência

- Melhorada a materialização dos inputs de Quizzes, comparando com o ponto de partida:
  - baseline: 587 inputs aceites e 94 marcados como materializáveis; numa amostra de 20 cenários sem falhas, apenas 4 executaram com sucesso (20%);
  - após preservar os campos dos DTOs construídos por helpers: 732 inputs aceites e 82 candidatos estáticos mais conservadores, dos quais 80 passaram o preflight;
  - após corrigir a reposição de valores inteiros: 82/82 workloads `SETUP_READY`;
  - os 164 FaultScenarios estão ligados a workloads `SETUP_READY`, mas ainda não foram todos executados.
- Os valores 4/20 e 82/82 medem níveis diferentes: o primeiro resulta da execução completa de uma amostra; o segundo valida setup, materialização e arranque, sem executar todos os steps.
- Implementada a primeira métrica de impacto:
  - `ImpactV1 = invariantViolationCount`;
  - uma rejeição de `Aggregate.verifyInvariants()` gera um sinal estruturado;
  - falhas injectadas, abortos e compensações não contam, por si só, como impacto.
- Evidência recolhida:
  - um teste real de Quizzes produz exactamente uma violação de invariante;
  - um cenário Quizzes sem falha termina `SUCCESS / EXACT`;
  - uma falha injectada real termina `COMPENSATED / EXACT`, é marcada como `REALIZED` e tem impacto `0`;
  - o caminho completo no executor até impacto `1` está provado com uma fixture genérica.
- Limitação actual: o caso positivo de Quizzes ainda não é reproduzido pelo executor porque o catálogo não representa/materializa toda a sequência multi-Saga com processamento do evento.

## Próximos passos

- Rever com o orientador a introdução do artigo e as alterações aos Capítulos 2 e 4.
- Escrever a solução proposta e o trabalho relacionado do artigo.
- Fechar a lacuna de catálogo/materialização necessária para obter um caso Quizzes end-to-end com impacto positivo.
- Ligar a métrica validada à pesquisa genética apenas depois dessa evidência end-to-end estar bem delimitada.

## Notas

- Ver event processing, sagas emitem eventos em alguns passos - oq fazer no executor? Idealmente conseguimos interleave a execução dos eventos com o restante cenario, podiamos ainda "falhar" este processamento? E compensar as sagas em si? Como evitar a recorrencia? Como evitar q sagas comecem sagas q comecam sagas... todas estas que nao estao no plano original
- Temos que processar estes eventos para garantir q a execução no ambiente simulação que o ScenarioExecutor cria é igual a executar no simulador base - o domínio mantêm-se consistente.
- EventHandling (ver e.g update user -> coursexecution update student name -> tournament reage ao evento)
- Eventos e sagas que deles proveem nao podem ser compensados, nem falhar - tal como as compensações

