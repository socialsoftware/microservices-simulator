# Quizzes: auditoria inicial das características e piloto de componentes

**Diagnóstico histórico.** O [inventário mestre atual](../evidence/workload-master-inventory-2026-09-25/README.md) reúne os workloads executados; os números abaixo continuam a descrever só este piloto.

**Estado:** diagnóstico de desenho, 23 setembro 2026. Usa resultados guardados
e coleções já examinadas; não é confirmação de eficácia nem congela a nova amostra.
Não foram executados novos cenários na aplicação.

## Universo e suporte dos critérios

O [inventário existente](../evidence/transfer-inventory-2026-09-21/RESULTS.md)
contém 96 workloads completos, 45 conjuntos de Sagas e 14.464 cenários. Recalculei
cada um dos cinco perfis isolados com `fitness.assess` a partir das observações
guardadas, aplicando as regras de validade e cobertura de cada critério:

| Critério isolado | Positivos | Negativos (score zero) | Sem score | Workloads/famílias com positivo |
| --- | ---: | ---: | ---: | ---: |
| Dependência eliminada | 4.059 | 9.939 | 466 | 9 / 4 |
| Resíduo de operação falhada | 10.683 | 3.033 | 748 | 32 / 11 |
| Evento entregue por resolver | 0 | 13.998 | 466 | 0 / 0 |
| Leitura exposta a compensação | 4.567 | 9.317 | 580 | 9 / 3 |
| Atualização copiada perdida | 2 | 13.996 | 466 | 1 / 1 |

Estes totais contam cenários, não defeitos independentes. Muitos positivos dos
mapas grandes pertencem à mesma família de quatro Sagas. No painel anterior de
81 workloads, há seis dependências positivas nos 66, 76 leituras positivas nos
15, e nenhum positivo de evento ou atualização copiada perdida. Logo, a nova
amostra tem de procurar *famílias e mecanismos* adicionais para os dois critérios
sem suporte; multiplicar inputs/ordens das famílias atuais não resolve isso.
O caso de evento sem positivo pode ser um resultado real do âmbito atual; não
iremos fabricar um positivo para fechar uma tabela.

## O que chega ao modelo

O vetor estrutural atual usa, antes de observar qualquer resultado: número de
Sagas, pares, interações e eventos; nomes de Sagas e pares; IDs, evidência,
acessos e formas de interações; rotas e presença de eventos. O vetor de progresso
usa apenas o histórico revelado daquele workload: tentativas, frações de score
conhecido/desconhecido, fração positiva, média e melhor score. O modelo estrutural
histórico é **estrutura mais progresso**. A variante `S-shared` retira o
progresso do vetor, mas continua a aprender coeficientes com as rewards observadas.

Nos 81 workloads, a estrutura tem 70 coordenadas e 54 vetores iniciais
distintos; 44 workloads estão em 17 grupos de perfil repetido. Dez workloads de
`AddParticipant + FindTournament + UpdateTournament` têm o mesmo vetor inicial,
apesar de catálogos de 36 a 160 cenários e 12 a 104 positivos. O vetor P inicial
tem sete coordenadas e é idêntico para todos. Assim, a estrutura não distingue
antecipadamente ordens/inputs dentro dessa família; o progresso observado pode
separar os percursos posteriormente.

Com exploração e ridge iguais a um, a norma inicial do vetor P é 1; a da
estrutura varia entre 1,50 e 2,67. O UCB estrutural começa portanto com um
bónus de incerteza maior e variável. O primeiro controlo `unit` igualou esse
bónus, mas também dividiu o **bias** por um valor diferente em cada workload.
Isso altera o ponto de partida do modelo; os seus resultados são apenas
diagnósticos. Num controlo corrigido, `cal`, o bias fica em 1, as coordenadas
estáticas restantes têm norma 1 e a exploração é ajustada analiticamente para
igualar o bónus inicial de P/S/SP e, separadamente, de H0/H1. Igualar o arranque
não iguala as atualizações nem a capacidade dos modelos.

Os pares de Sagas são derivados dos nomes das Sagas, mas permitem à regressão
representar uma combinação, não apenas a soma de efeitos individuais. A marca
`event:present` e alguns contadores são parcialmente redundantes com outras
coordenadas; isso pode funcionar como limiar no modelo linear. Os IDs de
interação podem ser específicos demais para transferir entre famílias. Por ora,
conservar estes grupos para comparabilidade e testar a sua utilidade com
ablação/permutação no painel de desenvolvimento da nova amostra. A ordem
continua uma análise separada: os seus acessos são potenciais por tipo de
agregado, não prova de encontro do mesmo objeto.

## Piloto executado

Acrescentei três políticas só no [adaptador experimental](../../../verifiers/experiments/allocator-component-pilot/README.md):
`S-shared`, `P-local` e `H0`. H0 usa bias partilhado e exatamente o mesmo vetor
local de progresso do H1 existente. Executei também P/SP/H1 existentes e os três
controlos `unit`. Cada uma das **nove variantes** correu do zero nos painéis de
66 e 15 workloads, com os perfis cinco critérios e dependência+leitura, seeds
1–3 e 32 escolhas: **108 pesquisas, 3.456 escolhas**. Mesmo GA local, mapas,
feedback e reward para cada comparação. O orçamento curto serve para ver
arranque, escala, implementação e custo; não permite escolher vencedores.

Exemplo do diagnóstico com cinco critérios nos 66 workloads, seeds 1–3:

| Variante | Workloads visitados em 32 escolhas | Positivos encontrados |
| --- | --- | --- |
| P-partilhado | 5, 3, 6 | 5, 13, 5 |
| S-partilhado bruto | 26, 29, 32 | 3, 3, 0 |
| SP-partilhado bruto | 27, 28, 29 | 4, 3, 0 |
| P-local | 13, 14, 14 | 4, 3, 4 |
| H0 | 15, 16, 14 | 4, 1, 4 |
| H1 bruto | 27, 31, 32 | 4, 1, 0 |
| SP com estrutura unitária | 23, 19, 12 | 5, 4, 5 |
| H1 com estrutura unitária | 25, 19, 17 | 5, 2, 3 |

Isto mostra que as variantes novas executam e que mudar a escala muda o caminho
em algumas condições. Repeti também as três variantes `cal` nas mesmas duas
coleções, dois perfis e três seeds: mais 36 pesquisas e 1.152 escolhas. O teste
confirma bias fixo e bónus inicial emparelhado; os caminhos continuam diferentes.
Três seeds nos mesmos workloads não medem generalização
entre famílias. As escolhas, previsão, incerteza, progresso antes da observação
e score ficam nos três JSONs de replay em
`verifiers/target/allocator-component-pilot-2026-09-23/seed-{1,2,3}-budget-32-traces.json`.
Também ficaram os hashes de percurso para comparação exata.

## Verificação e decisão provisória

Passaram quatro testes focados: P-local atualiza apenas o seu workload; H0 e H1
têm o mesmo progresso local; S não depende do progresso; e a normalização dá
norma inicial um. Repeti as 108 pesquisas: os hashes dos percursos coincidiram.
Recalculei contagem de escolhas por workload, score total, nulos e comprimento
dos 108 logs; todos fecham em 32 decisões por pesquisa. Não há feedback nulo
nestes dois perfis/painéis, pelo que o piloto não testa a política de cooldown.

Para a experiência maior, proponho avaliar o controlo `cal` junto da escala
original, mantendo o bias fixo. Para atribuir diferenças ao **conteúdo** da
estrutura, também precisamos de um controlo com perfis estruturais permutados,
mas com a mesma dimensão e escala; P vs SP por si só muda também a capacidade
do modelo. A ordem e ablação de grupos ficam para análises secundárias
fixadas no desenvolvimento. Antes de congelar essa escolha, falta
auditar e selecionar uma amostra Quizzes com mais famílias e suporte por
critério, verificar versões e estimar custo de novos mapas completos. Em
particular, não podemos prometer comparação de descoberta para evento entregue
por resolver com os mapas atuais, nem uma inferência robusta para atualização
copiada perdida com apenas dois positivos de uma família.
