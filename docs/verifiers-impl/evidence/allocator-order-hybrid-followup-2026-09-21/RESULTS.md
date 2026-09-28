# Alocação entre workloads: complexidade, orçamento e critérios

A informação estrutural ajuda numa das preferências estudadas, mas o progresso observado é suficiente — e por vezes melhor — noutras. Juntar as características da ordem ao modelo híbrido não acrescentou um ganho final relevante. Estes resultados justificam comparar estratégias de alocação, sem tratar a variante mais complexa como vencedora por definição.

## Perguntas e desenho

A comparação completa quatro combinações: modelo partilhado ou híbrido, cada um com estrutura apenas ou com estrutura e ordem. Assim, podemos testar o valor de acrescentar ordem mantendo o modelo, e o valor dos parâmetros privados mantendo as características. UCB independente, contextual só com progresso, distribuição equilibrada e escolha aleatória de workload permanecem como alternativas.

As mesmas duas coleções são estudadas com duas preferências. A primeira atribui peso um aos cinco critérios. A segunda atribui peso um a dependências eliminadas e leituras compensadas, zero aos restantes. Expressa o interesse na consistência das dependências e nas leituras expostas a compensação. Foi escolhida pela existência de observações desses critérios antes das novas pesquisas; não pela vantagem de um método. O [inventário por componente](pre-search-component-inventory.json) inclui também as componentes sem positivos.

- 15 workloads com três Sagas: 1.026 cenários, mediana de 38 por workload. Há 516 positivos com os cinco critérios e 76 com a segunda preferência; estes 76 correspondem a leituras compensadas em seis workloads.
- 66 workloads com uma ou duas Sagas: 485 cenários, mediana de quatro por workload. Há 78 positivos com os cinco critérios e seis com a segunda preferência; estes seis correspondem a dependências eliminadas em quatro workloads que incluem RemoveTournament.
- Os 81 workloads permanecem incluídos. Cada pesquisa começa sem aprendizagem nem histórico do GA, usa 256 escolhas e uma das 30 sementes. População oito, mutação 0,3, exploração e regularização um; sem cooldown ou ajuste de parâmetros.
- A nova preferência entra no GA e no alocador durante a pesquisa. Não é uma nova pontuação aplicada posteriormente às mesmas escolhas. O resultado de um cenário só é revelado quando é escolhido.

São 540 pesquisas novas e 420 pesquisas anteriores reutilizadas mediante verificação. A matriz completa tem 960 pesquisas, todas sobre resultados guardados, sem novas execuções da aplicação. As referências têm score disponível para os dois perfis. As coleções já tinham sido examinadas; esta é uma comparação controlada nas mesmas referências, não um teste numa aplicação nova.

## Impacto acumulado após 256 escolhas

Médias de 30 sementes. Comparar métodos dentro de cada coluna; a definição da score muda entre preferências. Na segunda preferência, cada positivo observado tem score um, pelo que impacto e número de positivos coincidem. Os números de positivos e percentis de todas as pesquisas estão em [summary.json](summary.json).

| Método | 15 workloads: cinco critérios | 66 workloads: cinco critérios | 15 workloads: dependências e leituras | 66 workloads: dependências e leituras |
| --- | ---: | ---: | ---: | ---: |
| Equilibrado | 115,00 | 24,47 | 13,40 | 4,40 |
| Workload aleatório | 113,43 | 26,13 | 13,53 | 4,63 |
| UCB independente | 182,83 | 37,47 | 19,80 | 5,47 |
| Contextual só com progresso | 175,37 | 60,27 | 30,53 | 4,00 |
| Contextual com estrutura | 171,37 | 43,57 | 21,33 | 6,00 |
| Contextual com estrutura e ordem | 176,13 | 48,37 | 20,90 | 6,00 |
| Híbrido | 186,43 | 47,43 | 25,23 | 5,73 |
| Híbrido com ordem | 186,50 | 47,43 | 25,23 | 5,73 |

## O que podemos concluir

**A preferência pode inverter a vantagem da estrutura.** Nos 66 workloads, com os cinco critérios, só progresso obtém 60,27 de impacto e o contextual estrutural 43,57. Com a segunda preferência, o estrutural encontra todos os seis positivos em cada uma das 30 sementes; só progresso encontra quatro em média. A diferença estrutural menos progresso passa de −16,70 [−22,03; −11,20] para +2,00 [+1,53; +2,47]. São seis casos concretos, não uma evidência ampla de generalização.

A distribuição das escolhas explica parte do resultado observado. Com os cinco critérios, só progresso dedica em média 139,63 escolhas a workloads que contêm positivos, contra 105,60 do estrutural. Na segunda preferência, o estrutural executa todos os 24 cenários dos quatro workloads que contêm os seis positivos; só progresso gasta 19,87 escolhas nesses workloads. Estas contagens são diagnósticos posteriores e nunca entram nas características fornecidas aos métodos.

**Positivos raros não tornam automaticamente a estrutura mais útil.** Nos 15 workloads, com a segunda preferência, só progresso encontra 30,53 dos 76 positivos, contra 21,33 do estrutural, 25,23 do híbrido e 13,53 da escolha aleatória. Só progresso atribui em média 206,20 escolhas aos seis workloads que contêm positivos, enquanto o estrutural atribui 150. O resultado é compatível com concentrar melhor o orçamento a partir do feedback observado; não estabelece uma causa isolada para todas as diferenças entre modelos.

**Combinar ordem e híbrido não justifica acrescentar complexidade nesta avaliação.** Com os cinco critérios nos 15 workloads, a combinação obtém 186,50 contra 186,43 do híbrido: diferença +0,07 [−0,30; +0,37]. Nas outras três condições, ambos têm o mesmo impacto final em cada semente. As escolhas não são iguais: só no perfil dos cinco critérios diferem 1.579 escolhas de workload na coleção de 15 e 4.798 na de 66. As características foram usadas, mas mudar o caminho não acrescentou impacto final. Isto não prova equivalência geral entre os métodos.

**Uma diferença para um modelo complexo não basta para justificar outro ainda mais complexo.** Nos 15 workloads com todos os critérios, a combinação supera o contextual só com progresso em 11,13 [6,77; 15,20], mas o UCB independente já obtém 182,83. A diferença para essa alternativa é apenas 3,67 [−0,53; 7,73]. Nestes dados, a vantagem não é suficientemente clara para escolher a combinação apenas pela maior média.

**O orçamento faz parte da conclusão.** Na mesma coleção e preferência, só progresso tem a maior média nas 16, 32 e 64 escolhas; o híbrido passa à frente nas 128 e 256. Com a segunda preferência nos 15 workloads, só progresso lidera nos cinco pontos. Nos 66 workloads, lidera nos cinco pontos com todos os critérios, mas o estrutural fica à frente nas 128 e 256 quando o objetivo são os seis casos de dependências eliminadas. Todos os pontos foram fixados antes da pesquisa; 256 continua a ser o resultado principal.

![Médias por orçamento e preferência](allocation-preferences.png)

A figura mostra cinco alternativas práticas. A tabela inclui os oito métodos e o JSON preserva todos os pontos e intervalos. As curvas mostram médias, não intervalos. Os intervalos citados são bootstrap emparelhado entre sementes, 95%, 10.000 reamostragens, sem correção por comparações múltiplas. Descrevem variabilidade da pesquisa nestas coleções, não variabilidade entre aplicações. Uma diferença de orçamento cobre frações diferentes das duas coleções: 256 escolhas são cerca de 25% e 53% dos respetivos catálogos.

## Como apresentar no paper e na dissertação

A conclusão principal pode ser: distribuir o orçamento com base nos resultados é útil, mas o benefício da informação estrutural depende daquilo que se procura; observações de progresso podem fornecer uma alternativa mais simples e competitiva. A comparação das preferências no mesmo grupo de 66 workloads dá um exemplo claro, acompanhado pela coleção de 15 para mostrar que raridade, por si só, não determina a escolha.

No paper, usar uma comparação compacta de uniformidade, adaptação independente, progresso e estrutura. A experiência com o híbrido e a ordem pode ser resumida como análise dos componentes: não houve ganho adicional consistente que justifique apresentar a combinação como abordagem principal. A dissertação pode incluir a matriz completa, curvas, equações e análise de alocação. A seleção antecipada de uma estratégia para uma aplicação nova continua uma decisão de configuração; estes dados não são uma regra que identifique automaticamente o vencedor.

Não são necessários novos algoritmos nem mais execuções destas referências para responder às perguntas fixadas. O próximo passo é rever a apresentação e escolher quais os métodos que ficam em destaque, preservando os resultados completos como suporte.

## Verificação e limites técnicos

- Revisão independente dos 960 resultados: todos os recibos, identidades e scores conferem; 245.760 escolhas foram verificadas diretamente contra as contagens e cobertura dos critérios originais. As 540 pesquisas novas correspondem a 138.240 dessas consultas, não a cenários novos.
- Todas as atualizações de progresso, checkpoints e métricas acumuladas foram recalculadas. As 37.750 sequências locais comparadas coincidem nos prefixos comuns entre métodos: cada GA faz as mesmas escolhas locais quando recebe o mesmo histórico e os mesmos pesos.
- Cinco testes focados passam. Trinta verificações adicionais confirmam que terminar nas escolhas 16 ou 64 preserva o prefixo da pesquisa de 256, incluindo decisões, contexto e recompensa.
- A implementação matemática do híbrido e os sete contadores da ordem são os anteriormente verificados. A ordem representa acessos potenciais ao mesmo tipo de agregado; não prova identidade do objeto nem ocorrência de uma anomalia. Mantêm-se as 16 limitações de conhecimento estático identificadas nos metadados originais.
- Fontes, referências e resultados anteriores permaneceram inalterados. Os desempates determinísticos e os parâmetros de exploração estão fixos. O custo de decisão guardado exclui preparação e execução da aplicação; não é uma medição controlada do tempo total.

## Artefactos

[Protocolo](protocol.json) · [Perguntas](QUESTIONS.md) · [Manifesto](manifest.json) · [Resultados completos](summary.json) · [Diagnóstico de alocação](allocation-diagnostics.json) · [Verificação independente](independent-verification.json) · [Descrição das coleções](collection-description.json) · [Código e comandos](../../../../verifiers/experiments/allocator-order-hybrid-followup/README.md).
