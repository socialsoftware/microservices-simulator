# Quizzes: inventário mestre dos resultados locais

**Atualização de 28 setembro:** a expansão e a retoma na proteina07 terminaram dentro das respetivas reservas. Há agora **320 workloads com mapas completos conhecidos**, dos quais **316 qualificados** para a seleção alargada (os outros quatro são os diagnósticos de preparação históricos). Os 144 mapas novos foram auditados localmente contra os catálogos completos, os hashes e os cinco critérios. A seleção qualificada tem **18.858 cenários**, ou **6.396 sem os três workloads gigantes**; mantém **87 famílias distintas**. Só dois controlos novos sem score conjunto completo continuam excluídos. [Inventário unificado e limites](master-inventory-2026-09-28.json) · [Resumo da retoma](../../../../verifiers/target/cluster-proteina07-expansion-2026-09-27/resume-audited-summary-2026-09-28.json). Estes são resultados medidos; ainda não são a amostra congelada nem uma comparação controlada de runtimes.

**Seleção atual para replay — 26 setembro:** **172 workloads / 87 famílias / 16.280 cenários**. Os 17 pendentes foram requalificados: 85 cenários, 16 positivos e 69 negativos, todos com os cinco scores disponíveis, sem alterar as avaliações antigas. Treze usam fixtures reduzidas explícitas; quatro usam preparação atualizada, incluindo uma expansão explícita do helper de criação de pergunta omitido pelo extrator. Os quatro diagnósticos continuam excluídos. [Inventário da seleção](evaluation-inventory-2026-09-26.json) · [Requalificação](preparation-requalification-2026-09-26.json).

Com **os cinco critérios ativos**, esta seleção contém **10.888 positivos / 4.004 negativos / 1.388 sem score conjunto**. A tabela histórica abaixo reúne scores guardados com configurações diferentes: 519 cenários antes pontuados tinham lost update com peso zero e cobertura incompleta. Ao ativá-lo, 173 positivos e 346 negativos ficam sem score conjunto; os seus critérios individualmente válidos continuam disponíveis. Não houve mudança de medição nesses 519. Os totais positivos por critério mantêm-se. O novo replay usa a mesma configuração explícita para todas as variantes; a validação com runtime comum no cluster continua pendente.

**Replay atual concluído e auditado:** sete variantes × seis objetivos (conjunto e cada critério isolado) × seeds 1, 11, 29, 47 e 73 × 8.000 escolhas = **210 runs / 1.680.000 escolhas offline**. Estados frios, feedback reavaliado, progresso reconstruído só das escolhas anteriores, candidatos sem repetição e prefixos do GA iguais dentro de cada objetivo/seed. A média de score conjunto é maior em H0; só estrutura encontra mais cenários positivos, mas perde em dependências e leituras compensadas. Estrutura + progresso supera só estrutura na média conjunta. Todos encontram os três eventos no objetivo isolado; os tempos de descoberta distinguem-nos. [Resultados atuais, provas e limites](replay-multiseed-2026-09-26.json).

**Desempate corrigido para as próximas runs:** os três mapas dominantes têm os mesmos inputs e preparação, mas ordens de passos diferentes; as features sem ordem são idênticas. Num controlo posterior de seed 1, apenas inverter a prioridade entre esses três em só estrutura muda o score conjunto de 13.773 para 18.095 e dependências de 1.397 para 3.495. Não é uma nova variante vencedora: demonstra sensibilidade à prioridade fixa dos IDs. As runs principais foram preservadas. A regra comum pela seed está implementada e validada no novo runner; falta repetir a comparação formal com ela, rever a representação da ordem e validar a medição num runtime comum no cluster. [Validação](seeded-ties-and-expansion-2026-09-27.json).

**Controlo GA versus aleatório — 27 setembro, concluído e auditado:** sete variantes, cinco seeds, score conjunto; 8.000 escolhas no inventário completo e 3.000 sem os três gigantes. São 140 runs de replay (105 novas e 35 reutilizadas), sem novos resultados do Quizzes. As seis políticas adaptativas superam a alocação uniforme na média final em ambas as coleções com os dois métodos; a ordem entre políticas muda: H0 tem a maior média com GA no inventário completo (17.116,4), SP com pesquisa aleatória (16.557,6). Sem gigantes, só estrutura recolhe todo o score disponível (1.320) com ambos os métodos nas cinco seeds; a coleção de 3.818 cenários precisa de expansão antes da avaliação com 4.000 escolhas. Existe uma proposta ainda não executada de 146 ordens adicionais em 23 famílias, com estimativa por casos semelhantes de cerca de 6.800 cenários totais. Os desempates originais foram mantidos para isolar a pesquisa local; o controlo não encerra a avaliação final. [Resultados, provas e proposta](local-search-control-2026-09-27.json).

### Inventário máximo histórico (176 workloads)

**Estado (26 setembro 2026):** inventário máximo dos resultados locais acessíveis, **não** a amostra final do paper. Ainda não houve seleção por tamanho, resultado ou família. Os 96 workloads do [inventário anterior](../transfer-inventory-2026-09-21/RESULTS.md) estão incluídos. A procura dirigida por critérios raros acrescentou os mapas de `FindQuestionByAggregateId + UpdateQuestion`, `FindQuiz + UpdateTopic + UpdateTournament`, três variantes de `DeleteTopic` com ou sem entrega do evento e um workload de `RemoveQuestion`.

| Medida | Total |
| --- | ---: |
| Workloads distintos | 176 (96 anteriores + 80 adicionais) |
| Famílias de Sagas distintas | 87 |
| Cenários distintos com tentativa e resultado guardados | 16.296 |
| Positivos / negativos / sem score conjunto | 11.061 / 4.366 / 869 |
| Referências examinadas | 219 |

Os três workloads de `AddParticipant + LeaveTournament + RemoveTournament + UpdateTournament` ocupam **12.462 cenários (76,5%)**. Logo, a dimensão em cenários não equivale a diversidade de histórias. Todos os 176 têm um cenário sem falha com score disponível; catorze desses controlos são positivos, o que é permitido pelo desenho atual da experiência.

| Critério | Cenários positivos | Workloads com positivo | Famílias com positivo | Sem pontuação |
| --- | ---: | ---: | ---: | ---: |
| Dependência eliminada | 4.105 | 22 | 9 | 471 |
| Resíduo de operação falhada | 11.149 | 56 | 23 | 767 |
| Evento entregue por resolver | 3 | 3 | 3 | 471 |
| Leitura exposta a compensação | 4.571 | 11 | 4 | 585 |
| Atualização copiada perdida | 24 | 4 | 3 | 990 |

Os positivos são **cenários**, não defeitos independentes. Um cenário pode ser positivo em mais de um critério. `Sem pontuação` significa cobertura insuficiente ou execução sem avaliação válida, não score zero.

O mapa novo de `UpdateQuestion + FindQuestionByAggregateId` tem oito cenários: um positivo de evento no controlo sem falha, seis negativos e um sem resultado por falha do processo antes de produzir relatório. É outra família, mas repete o mecanismo de `UpdateQuestion` já observado.

O novo workload `FindQuiz + UpdateTopic + UpdateTournament` tem **176 cenários executados**: 104 positivos e 72 negativos na pontuação conjunta, sem scores indisponíveis. Há 14 positivos de atualização copiada perdida, todos na escrita normal do Tournament, incluindo o controlo sem falha. Doze dos 14 também têm resíduo de operação falhada; os outros dois são positivos só pela atualização perdida. O mecanismo continua a ser a cópia do nome de Topic no Tournament, agora numa família que inclui a leitura do Quiz.

O teste `UpdateTournamentTest.delete a topic already used by a tournament` confirmou uma segunda causa de evento por resolver: `DeleteTopicEvent` é entregue ao Tournament, mas `TournamentService.removeTopic()` termina sem persistir a remoção. As três variantes geradas têm três cenários cada, todos `COMPLETE` e com score disponível. Na variante com entrega ao Tournament, o controlo sem falha é positivo em evento por resolver e dependência eliminada; as outras duas variantes, com entrega à Question ou sem entrega, têm dependência eliminada mas zero eventos por resolver. Estes três controlos não representam três defeitos independentes.

O teste `UpdateTournamentTest.remove a question already used by a tournament quiz` passou, mas o gerador não ligou `DeleteQuestionEvent` ao Quiz nesse workload. O diagnóstico do extrator é `RECURSIVE_EVENT_ROUTE_UNSUPPORTED`: a Saga consumidora `RemoveQuizQuestion` pode publicar `InvalidateQuizEvent`, e a regra atual exclui por inteiro essa primeira rota. Há também uma incompatibilidade no Quizzes: `DeleteQuestionEvent` usa o ID do Course como publisher, enquanto `QuizSubscribesDeleteQuestion` subscreve o ID da Question; no controlo medido são 1 e 8. O handler ainda passa o ID do publisher como se fosse o da Question. Portanto, a rota omitida não deve ser presumida uma entrega válida. Os quatro cenários medidos só cobrem a Saga `RemoveQuestion`: um positivo de dependência eliminada sem falha, dois negativos e um sem score. O último termina consistentemente em `COMPENSATION_FAILED` com `EXPLICIT_COMPENSATION_FAILED`, tanto com dois como com um trabalhador.

Na procura dirigida, a combinação `UpdateQuestion + FindQuiz` não gerou um WorkloadPlan ligado e, portanto, não foi contada como negativo. Uma tentativa de medir o mapa de 176 cenários com oito trabalhadores produziu 16 falhas operacionais de arranque/timeout; essa sessão foi preservada fora do inventário. A referência escolhida foi repetida de raiz com quatro trabalhadores e **176/176 execuções `COMPLETE`**.

## Medição das provas de rotas — 26 setembro

As provas `DeleteCourseExecution → Tournament` e `DisenrollStudent → Tournament` originaram **11 workloads / 41 cenários**, todos com os cinco scores disponíveis. A ronda acrescenta **4 cenários positivos de dependência eliminada e 4 de resíduo de operação falhada**; nenhum novo positivo dos outros três critérios. As famílias `RemoveCourseExecution` e `RemoveStudentFromCourseExecution` já existiam no inventário; o total continua em 87.

**Quatro workloads / 16 cenários são diagnósticos de preparação:** o gerador escolheu só o `setup()` comum e omitiu a segunda execução e as duas remoções de alunos no `given`. Os controlos compensam antes de publicar o evento e todos os seus scores são zero. Foram mantidos no inventário máximo, mas estão identificados para exclusão da avaliação final. Os restantes **7 workloads / 25 cenários** usam a preparação pretendida. Um helper de medição recompõe as ações extraídas do próprio teste, pela ordem de origem; não alterámos o gerador de produção nem os critérios. A correção genérica de seleção foi implementada depois desta medição: o gerador regenera os mesmos sete IDs preparados, sem o helper especial. A auditoria do restante inventário acompanha a seleção para avaliação.

Os quatro controlos preparados de remoção terminam `SUCCESS/EXACT`: cada um tem dependência eliminada no controlo normal e resíduo numa falha do último passo. Os três mapas de remoção de aluno são negativos. Novas rotas entregues não implicam novos eventos por resolver. [Dados, provas de entrega e IDs de diagnóstico](measurement-2026-09-26.json).

## Ficheiros

Os CSV/JSONL abaixo preservam o inventário máximo histórico de 176 workloads e as configurações dos scores então guardados. Para a seleção e configuração atuais, usar os recibos no início deste documento.

- [workloads.csv](workloads.csv): mapa legível, uma linha por workload, com família, teste de origem quando recuperável e contagens por critério.
- [positive-workloads.csv](positive-workloads.csv): para cada critério, os workloads que deram positivos, ordenados pela respetiva contagem.
- [families.csv](families.csv): agregação por conjunto de Sagas.
- [workloads.jsonl](workloads.jsonl): metadados, controlo, versão e fonte escolhida de cada workload.
- [scenarios.jsonl.gz](scenarios.jsonl.gz): chave, vetor, execução e resultado de cada cenário distinto.
- [references.jsonl](references.jsonl): todas as 219 referências, incluindo repetições e hashes.
- [conflicts.jsonl](conflicts.jsonl): 251 chaves com avaliações divergentes, todas no mesmo workload grande (`242d3ef997…`).
- [summary.json](summary.json): totais para máquinas. `python3 build.py` reconstrói os ficheiros a partir das referências locais.

Para cada workload, o inventário escolhe uma referência que contém todas as chaves de cenário encontradas localmente. Nos 96 anteriores, respeita a referência já escolhida no inventário de 21 setembro. Assim, no workload `242d3ef997…` usa a avaliação v3 aprovada: a versão anterior fica visível em `conflicts.jsonl`, mas não é contada duas vezes. Os outros cenários repetidos têm a mesma avaliação guardada. As 219 referências têm chaves de candidato, observação e fitness correspondentes; os hashes e contagens das 96 referências anteriores foram novamente conferidos.

## O que significa “runtime” aqui

É a imagem Docker, o classpath e os hashes dos ficheiros compilados/dependências usados numa tentativa. Há oito assinaturas nos resultados escolhidos:

- **83 workloads:** imagem Docker do cluster (`3b5f7c…`), com 3.376 ficheiros registados.
- **65 workloads:** imagem Docker local (`0aab59…`). Os mesmos 3.376 ficheiros têm os mesmos hashes de conteúdo do grupo anterior; a imagem e os caminhos da máquina diferem. Isso, por si só, não demonstra diferença nos resultados.
- **10 workloads:** mesma imagem do cluster, com 19 classes adicionais sobrepostas do avaliador de leituras compensadas. Aqui mudou efetivamente a medição desse critério.
- **3 workloads:** imagem local com um novo snapshot sobreposto de classes da aplicação, simulador e verificador. Inclui os testes recentes; devem ser analisados separadamente até verificarmos equivalência.
- **3 workloads:** a mesma imagem local e snapshot, com uma sobreposição adicional apenas das classes compiladas de `UpdateTournamentTest` para o novo teste `DeleteTopic`.
- **1 workload:** a mesma imagem local e snapshot, com outra sobreposição das classes de `UpdateTournamentTest` que inclui o teste `RemoveQuestion`.

- **7 workloads novos:** imagem local e mesmas classes de produção do snapshot anterior; sobreposição dos novos testes e helper de seleção.
- **4 workloads novos preparados:** mesmas classes de produção, testes e helper adicional de preparação extraída da feature.

Esta proveniência fica por workload. O inventário reúne o que foi observado, mas **não afirma que os oito grupos constituem uma única experiência controlada**. Também não afirma que todas as referências cobrem o catálogo completo de cenários possível para cada workload.

## Verificação dos arquivos e limpeza

Os arquivos locais de resultados de proteina01 contêm 20 referências; todas estão extraídas com o mesmo SHA-256. Os quatro arquivos locais de resultados de proteina06 contêm 12 referências e **nenhuma chave de cenário ausente** do inventário. O arquivo local do mapa de 5.184 casos contém a mesma referência byte a byte; o arquivo `ga-500x3` não contém uma referência de mapa. Os arquivos de inputs/runtime não contêm referências de resultados.

Também consultei os arquivos guardados só nos hosts, sem os alterar. Em proteina06, `overnight-results.tar.gz` contém **75 referências de 75 IDs**, todos já no inventário e sem chaves de cenário novas; as **1.237 pontuações sobrepostas** coincidem com as escolhidas localmente. `overnight-supplement.tar.gz` não contém referências. Em proteina01, os arquivos `results`, `retry64` e `diversity` contêm referências cujos caminhos já estão na extração verificada local. Esta passagem cobre as referências de mapas acessíveis; não transforma tentativas brutas sem mapa numa pontuação.

Os documentos de pilotos antigos ficam classificados como histórico no [índice de investigação](../../research/README.md); movê-los ou apagá-los quebraria ligações sem acrescentar resultados. Os diretórios antigos `ga-500x3` e `full-map-5184` já tiveram relatórios brutos arquivados e removidos numa manutenção anterior, segundo o [recibo dessa operação](../../../../verifiers/target/evidence-storage-2026-09-19/README.md). Nenhum resultado bruto foi apagado nesta passagem.

## Antes da avaliação final — histórico do primeiro replay

**Preparação em 27 setembro:** desempate comum pela seed validado em 21 runs curtas;
146 novas ordens de execução em 23 famílias existentes foram preparadas para execução no cluster.
Estimativa por casos semelhantes: mais 2.999 cenários, chegando a cerca de 6.817 sem os
três gigantes / 19.279 com eles. Não são novos totais medidos: mantêm-se 3.818 / 16.280.
[Recibo de validação e preparação](seeded-ties-and-expansion-2026-09-27.json).

**Primeira reserva concluída:** proteina07, oito workers, VM de 32 vCPU / 48 GB. O guard parou
novas execuções às 03:20, fez backup íntegro às 03:21 e desligou a VM às 03:22 de Lisboa,
antes do fim da reserva às 04:00. Dos 146 workloads preparados, 142 deram mapas completos;
dois controlos não tiveram score conjunto completo, um mapa ficou com 113/160 cenários
medidos e outro com 72/76. Nesse fecho, as tentativas incompletas ficaram preservadas no
arquivo e fora dos totais.
O arquivo e as referências foram verificados por hash; os 142 mapas completos foram novamente
avaliados no Mac contra os seus catálogos. [Recibo operacional](cluster-expansion-2026-09-27.json).

**Retoma concluída na mesma VM, 28 setembro:** os dois mapas interrompidos chegaram a
160/160 e 76/76 cenários. Foram executados só os 51 em falta; os 185 ficheiros de tentativa
anteriores permanecem byte a byte iguais nos dois arquivos. O novo arquivo (82 MB) foi
verificado por hash, as identidades e os cinco scores foram reavaliados no Mac, e a VM
ficou desligada antes do fim da segunda reserva. Estes dois mapas somam 236 cenários,
120 positivos e 116 negativos no score conjunto; os 120 positivos são apenas resíduos de
operação falhada. Os dois controlos sem score completo permanecem excluídos. A retoma
usou o limite de CPU configurado de 2 por contentor; a campanha anterior teve ajustes
operacionais para 4 em parte das tentativas, pelo que a proveniência efetiva do runtime
continua a merecer revisão antes de claims finais.

Decidir como uniformizar ou estratificar as versões de execução/medição; qualificar os workloads por diversidade e cobertura; só depois congelar a amostra e o orçamento. O inventário máximo serve para fazer essa escolha, não a antecipa.

**Primeiro replay histórico em 26 setembro:** 155 workloads / 16.195 cenários. Os quatro diagnósticos (16 cenários) ficaram de fora; outros 17 workloads (85 cenários) aguardavam revisão porque a receita de preparação mudou ou ficou ambígua no gerador corrigido. Esses 17 estão agora requalificados na seleção atual acima. Os sete workloads preparados regeneram os mesmos IDs sem o helper especial. [Recibo inicial de preparação](preparation-qualification-2026-09-26.json).

Os hashes das classes de evidência efetivamente escolhidas pelo classpath distinguem **três grupos de medição**, reunindo as oito assinaturas de imagem/setup/helper. A comparação inicial usa os mesmos mapas guardados para todas as variantes; harmonização da medição e confirmação no cluster continuam pendentes. [Proveniência das classes](measurement-class-provenance-2026-09-26.json). O runner é `verifiers/experiments/allocator-expanded-replay/run.py`; a seleção e a validação de 155 mapas estão em `verifiers/target/allocator-expanded-replay-full-2026-09-26/`. Esta seleção serve o replay exploratório, mantendo o inventário máximo intacto.

**Primeira seed concluída:** sete variantes, 8.000 escolhas cada, cinco critérios juntos. Os sete estados iniciais são frios e os prefixos de candidatos do GA local coincidem entre variantes para cada workload. Só estrutura encontrou 6.351 positivos / score 13.769; progresso local, 6.109 / 17.710; híbrido completo, 6.152 / 17.833. O uniforme encontrou os três positivos de eventos e os 24 de atualização perdida, contra zero de ambos no só estrutura. São resultados desta seed e deste benchmark histórico. As 35 runs curtas por critério apenas verificam a execução do runner; a comparação grande por critério e mais seeds continuam pendentes. [Resultados e limites](replay-seed1-2026-09-26.json).

Nas duas variantes finais, `run_fast.py` omite produtos por zero mantendo a ordem de `math.fsum`; 200 casos numéricos e 128 decisões por variante foram idênticos ao cálculo original. Esta aceleração está identificada nos recibos, e os tempos de implementação desta ronda não servem para comparar os algoritmos. A auditoria inicial também registou sensibilidade do empate do híbrido a uma unidade de precisão numérica, para revisão antes da comparação formal.
