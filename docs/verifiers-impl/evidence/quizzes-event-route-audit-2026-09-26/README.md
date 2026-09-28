# Auditoria das rotas de eventos do Quizzes

O [inventário por rota](routes.csv) classifica as **20 rotas de consumo**, para **11 tipos de evento**, no código atual. A auditoria está concluída: cada rota tem produtor, identidade e payload, subscrição e elegibilidade, cadeia de consumo, efeito previsto, cobertura do verificador e prova ou bloqueio explícito. Uma rota é um tipo de evento e um destino; não é um workload nem um cenário positivo. A prova é sobre os casos observados e o código atual, não sobre todos os setups possíveis.

| Prova de entrega ou bloqueio | Rotas |
| --- | ---: |
| Entrega medida em controlo do executor | 12 |
| Publicação normal e entrega em teste da aplicação | 3 |
| Consumidor provado com evento inserido no teste | 1 |
| Percurso normal bloqueado, com prova no código | 4 |

## As duas rotas que faltavam

Os testes dirigidos confirmaram `DeleteCourseExecutionEvent → Tournament`: depois de criar outra execução do mesmo Course e retirar os estudantes da execução a apagar, a operação publica o evento; o Tournament subscreve-o e fica **INACTIVE** numa versão persistida posterior. A query que devolve apenas a última versão ativa deixa de o encontrar, por isso o teste verifica também o histórico persistido.

Confirmaram também `DisenrollStudentFromCourseExecutionEvent → Tournament`: com um estudante já participante, a operação normal publica o evento; a subscrição é elegível, o participante persistido fica **DELETED** e a versão copiada da Execution passa a ser a versão do evento.

Os controlos antigos não refutavam estas rotas. A pesquisa dos 3347 relatórios de execução guardados encontrou 14 tentativas da primeira e 24 da segunda com `NO_ELIGIBLE_SUBSCRIBER`; todas tinham zero Tournament no estado inicial. São 38 tentativas históricas, não 38 workloads independentes.

## Bloqueios e entregas sem efeito

| Rota | Evidência atual |
| --- | --- |
| `DeleteUser → QuizAnswer` | Há handler, mas o QuizAnswer não cria subscrição para este tipo de evento. |
| `DeleteQuestion → Quiz` | Publica o ID do Course; o Quiz subscreve o ID da Question. O handler interpreta ainda o publisher como Question. |
| `QuizAnswerQuestionAnswer → Tournament` | O produtor Saga lê `quizAnswer` antes da primeira atribuição, impedindo a publicação normal. |
| `InvalidateQuiz → Tournament` | Depende da rota DeleteQuestion bloqueada; o serviço do Tournament retorna sem registar alteração. |

Entre as 12 rotas medidas pelo executor, dez mostraram alteração persistida e duas não: `UpdateQuestion → Quiz` (duas entregas) e `DeleteTopic → Tournament` (uma). Nesses três controlos, o recetor continuou elegível no horizonte de avaliação. O código explica a observação: `QuizService.updateQuestion` altera uma cópia sem chamar `registerChanged`; `TournamentService.removeTopic` tem o corpo comentado e retorna `null`. São comportamentos da implementação atual, não falta de entrega.

O teste de `InvalidateQuiz → QuizAnswer` usa um evento inserido manualmente: prova o consumidor, não a publicação normal. `DeleteUser → CourseExecution` tem publicação e entrega normais provadas em teste.

## Consequência para a amostra

O verificador aceita **16** rotas e exclui quatro: duas por emissão condicional e duas por cadeia recursiva. Das 16 aceites, 14 têm entrega demonstrada (12 no executor e duas nos novos testes); duas estão bloqueadas na aplicação. As quatro excluídas incluem duas com consumidor provado em teste. A [extração retida](verifier-routes.txt) coincide exatamente com as 20 linhas do inventário.

As duas novas provas tornam possíveis setups mais completos para a avaliação, mas **ainda não acrescentam cenários medidos nem positivos** ao inventário mestre. O próximo trabalho de amostra é gerar workloads a partir destes testes e qualificá-los com o executor e os cinco critérios. Mantemos os bloqueios etiquetados e os casos entregues sem efeito; não transformamos ausência de recetor em resultado negativo do handler.

## Ler e reproduzir a prova

No CSV, `handler_source_effect` descreve o código e `observed_immediate_application_changes` descreve a execução ou a asserção do teste. `matching_type` compara apenas os tipos de ID; não prova elegibilidade. A regra base exige tipo, ID e versão publicada maior que a subscrita; oito rotas acrescentam uma condição sobre estudante ou participante. As contagens históricas dos controlos não foram alteradas pelos testes novos.

Os [controlos exemplares](controls/) e [relatórios de testes](tests/) foram retidos aqui. Em `applications/quizzes`, executar:

```sh
mvn -Ptest-sagas -Dtest=AnonymizeStudentAndSolveQuizTest,QuizAnswerEventHandlingTest,DeleteUserFromCourseExecutionTest test
```

Resultado no estado auditado: **10 testes, zero falhas e zero erros**. Foram acrescentados apenas os dois testes autorizados; produção e verificador não foram alterados nesta auditoria.
