# Inventário para estudar transferência — 21 setembro 2026

**Arquivo histórico.** Estes 96 workloads estão incluídos no [inventário mestre atual](../workload-master-inventory-2026-09-25/README.md); os totais abaixo descrevem apenas a seleção de 21 setembro.

## Resultado

Há **96 identificadores distintos de workload**, **45 conjuntos de tipos de Saga** e
**14.464 cenários nas referências inventariadas**. 32 workloads têm positivos com os
cinco critérios e 64 têm apenas zeros. Os 96 workloads não são 96 histórias independentes:
incluem variantes de inputs e ordens para o mesmo conjunto de Sagas. Não são o total
possível do Quizzes nem uma amostra aleatória da aplicação.

A coleção inclui os 73 mapas completos da campanha anterior, 19 referências combinadas
verificadas da continuação, o mapa de 3.360 casos e os mapas anteriores de 5.184, 186 e 72.
O mapa de 3.918 e a ordem tardia de Topic usam a referência v3 já aprovada; as outras
referências mantêm a versão original. Os scores dos dois perfis foram recalculados das
observações locais, preservando indisponíveis. Não houve novas execuções, extração de
arquivos grandes nem nova verificação dos relatórios brutos de todas as campanhas.

As chaves dos candidatos e das observações coincidem. 85 contagens foram cruzadas nesta
passagem com os totais completos guardados ou o protocolo dos 73 mapas; para as restantes
11, a inclusão apoia-se nas referências completas e na verificação anterior das campanhas.
As versões e caminhos exatos estão no [inventário](inventory.json). Os identificadores
não se repetem entre as referências escolhidas; isso não prova independência semântica.

## Separação proposta, ainda não executada

Para começar com um ambiente comum, selecionar o grupo com o mesmo descritor de runtime e
mapas com menos de 500 cenários. São **81 workloads, 41 conjuntos de Sagas, 1.511 cenários**.
A regra usa ambiente e tamanho, sem escolher pelos resultados do GA.

| Papel | Sagas por workload | Workloads | Conjuntos de Sagas | Cenários | Positivos, cinco critérios |
| --- | --- | ---: | ---: | ---: | ---: |
| Aprendizagem | 1–2 | 66 | 37 | 485 | 78 |
| Teste | 3 | 15 | 4 | 1.026 | 516 |

Nenhum conjunto de Sagas do teste aparece na aprendizagem. Todas as Sagas usadas no teste
aparecem individualmente em pelo menos um workload de aprendizagem. Isto testa transferência
para novas combinações de Sagas conhecidas, não para Sagas inteiramente desconhecidas.
O mesmo descritor de runtime é um critério de agrupamento de proveniência; não transforma
medições de datas diferentes numa experiência simultânea controlada.

Os workloads de teste são:

| Sagas | Workloads | Cenários | Positivos |
| --- | ---: | ---: | ---: |
| AddParticipant + FindTournament + UpdateTournament | 10 | 916 | 516 |
| AddParticipant + FindTournament + UpdateStudentName | 2 | 24 | 0 |
| AddParticipant + GetCourseExecutionById + UpdateStudentName | 1 | 12 | 0 |
| AnonymizeStudent + GetCourseExecutionById + RemoveStudentFromCourseExecution | 2 | 74 | 0 |

A composição torna esta uma experiência inicial possível, mas concentrada: os positivos
estão num único conjunto. Com dependências eliminadas apenas, **todos os 1.026 casos de
teste têm score zero**. Esse perfil não informa a descoberta de positivos nesta separação;
não se justifica repetir a mesma comparação com esse objetivo.

## O que o modelo consegue distinguir

Reconstruí os perfis estruturais com o código atual. Toda a informação de Sagas, interações
e eventos necessária está disponível localmente; os campos usados para interações com o
mesmo ID são consistentes nos ficheiros consultados. Os dez workloads de
AddParticipant/FindTournament/UpdateTournament têm **o mesmo perfil estrutural inicial**.
Diferem na ordem, mas o modelo atual não codifica a ordem normal como feature. O progresso
observado pode distingui-los mais tarde. Portanto, a experiência pode mostrar prioridade
para essa combinação de Sagas, mas não antecipação da melhor ordem dentro dela.

## Próximo passo concreto

Montar os inputs compactos para as quatro variantes propostas (estrutura/progresso,
com/sem aprendizagem anterior), congelar os conjuntos acima e o orçamento de aprendizagem
e teste antes de correr. Há referências completas e metadados locais suficientes; só 11
pastas inventariadas têm o pacote completo no formato diretamente consumido pelo CLI atual.
As restantes usam metadados compactos de campanhas anteriores. É necessário adaptar a leitura
ou materializar um pacote mínimo verificado, sem inventar interações ou misturar fontes.
Isto é trabalho local, não uma razão para pedir compute ao cluster.

A escolha dos conjuntos deve ficar congelada antes da comparação. A informação dos scores
acima serve para explicitar o alcance, não para procurar uma separação que favoreça o modelo.
Um teste posterior com outro conjunto contendo positivos daria mais abrangência. Os mapas
grandes e os três workloads Topic ficam disponíveis como extensões separadas; não devem ser
misturados silenciosamente por terem outras versões de runtime/avaliação.

[Separação exata](proposed-split.json) · [Resumo](summary.json) · [Metadados locais](metadata-availability.json)

## Inventário por conjunto de Sagas

| Conjunto de Sagas | Workloads | Cenários | Workloads com positivos (cinco critérios) |
| --- | ---: | ---: | ---: |
| ActivateUser | 2 | 4 | 0 |
| AddParticipant | 2 | 6 | 0 |
| AddParticipant + AnonymizeStudent | 2 | 28 | 0 |
| AddParticipant + FindTournament | 2 | 12 | 0 |
| AddParticipant + FindTournament + UpdateStudentName | 3 | 36 | 0 |
| AddParticipant + FindTournament + UpdateTournament | 10 | 916 | 10 |
| AddParticipant + GetCourseExecutionById + UpdateStudentName | 2 | 24 | 0 |
| AddParticipant + LeaveTournament + RemoveTournament | 1 | 72 | 1 |
| AddParticipant + LeaveTournament + RemoveTournament + UpdateTournament | 3 | 12462 | 3 |
| AddParticipant + LeaveTournament + UpdateTournament | 1 | 186 | 1 |
| AddParticipant + UpdateStudentName | 1 | 6 | 0 |
| AddParticipant + UpdateTournament | 4 | 136 | 4 |
| AddStudent | 2 | 6 | 0 |
| AddStudent + GetCourseExecutionById | 2 | 12 | 0 |
| AddStudent + RemoveStudentFromCourseExecution | 2 | 18 | 0 |
| AddStudent + UpdateStudentName | 3 | 18 | 0 |
| AnonymizeStudent | 2 | 6 | 0 |
| AnonymizeStudent + GetCourseExecutionById | 2 | 16 | 0 |
| AnonymizeStudent + GetCourseExecutionById + RemoveStudentFromCourseExecution | 3 | 110 | 0 |
| AnonymizeStudent + RemoveStudentFromCourseExecution | 2 | 32 | 0 |
| CancelTournament | 2 | 6 | 0 |
| CreateTournament | 2 | 16 | 0 |
| CreateTournament + GetCourseExecutionById | 1 | 16 | 0 |
| CreateTournament + UpdateStudentName | 2 | 42 | 0 |
| CreateUser | 2 | 4 | 0 |
| DeactivateUser | 2 | 4 | 0 |
| FindQuestionByAggregateId | 1 | 2 | 0 |
| FindQuiz | 1 | 2 | 0 |
| FindTournament | 2 | 4 | 0 |
| FindTournament + RemoveTournament | 3 | 24 | 3 |
| FindTournament + UpdateTournament | 1 | 12 | 1 |
| GetCourseExecutionById | 2 | 4 | 0 |
| GetCourseExecutionById + RemoveStudentFromCourseExecution | 2 | 12 | 0 |
| GetCourseExecutionById + UpdateStudentName | 2 | 8 | 0 |
| GetCourseExecutions | 2 | 4 | 0 |
| GetCourseExecutionsByUser | 1 | 2 | 0 |
| LeaveTournament | 1 | 3 | 0 |
| RemoveCourseExecution | 3 | 12 | 3 |
| RemoveStudentFromCourseExecution | 2 | 6 | 0 |
| RemoveTournament | 2 | 8 | 2 |
| SolveQuiz | 1 | 8 | 0 |
| StartQuiz | 2 | 8 | 0 |
| UpdateStudentName | 2 | 4 | 0 |
| UpdateTopic + UpdateTournament | 3 | 141 | 3 |
| UpdateTournament | 1 | 6 | 1 |
