# Alocação entre workloads: ordem e modelo híbrido

As duas variantes melhoram a média face ao contextual estrutural anterior. O híbrido tem a maior média no grupo de 15 workloads; o contextual que usa apenas progresso continua à frente no grupo de 66. Não há um vencedor comum aos dois grupos.

## Experiência

- Seleção e comparações fixadas em [protocol.json](protocol.json) antes das novas pesquisas: todos os 15 workloads com três Sagas do teste anterior e todos os 66 workloads com uma ou duas Sagas da aprendizagem anterior, agora avaliados separadamente e sem aprendizagem prévia.
- O primeiro grupo contém 1.026 cenários, 516 positivos e 510 zeros. O segundo contém 485, dos quais 78 positivos e 407 zeros. Todos têm score disponível nesta referência e com estes pesos.
- Sete métodos, 30 sementes, 256 escolhas por pesquisa. Os cinco critérios têm peso um; população 8, mutação 0,3, exploração uniforme de candidatos ainda não vistos. Nenhuma regra de cooldown.
- Cada escolha consulta exclusivamente o resultado guardado do cenário escolhido. Os estados de aprendizagem e os GAs começam vazios em cada pesquisa. São 420 pesquisas e 107.520 consultas a resultados guardados, sem novas execuções da aplicação.
- Os grupos já tinham sido observados. O segundo verifica as novas comparações noutra coleção, mas não é um conjunto de dados nunca antes examinado. Não houve procura de hiperparâmetros nem seleção de workloads pelos resultados novos.

## O que mudou

**Ordem:** sete características adicionais contam padrões potenciais read→write, write→read, write→write entre participantes, leituras entre escritas de outros participantes e os três padrões que envolvem entregas de eventos. Cada contagem n é representada por n/(1+n). As características vêm dos acessos identificados nos passos e recetores de eventos; nenhum resultado de execução entra na extração. As dez ordens antes indistinguíveis da família AddParticipant + FindTournament + UpdateTournament passam a ter cinco vetores diferentes.

**Híbrido:** os coeficientes estruturais são partilhados; os coeficientes do progresso e um termo constante são específicos de cada workload. O modelo é a forma híbrida de LinUCB, com regularização e exploração iguais a um. Este braço usa as características estruturais originais, sem as novas características da ordem. Assim, cada comparação altera uma dimensão da abordagem.

Os novos modos são injetáveis na experiência com resultados guardados. Os nomes, comportamento e defaults do alocador existente e do dispatcher live permanecem iguais.

## Resultados após 256 escolhas

Cada célula mostra **positivos / impacto acumulado**, em média nas 30 sementes. Impacto acumulado é a métrica principal; positivos são cenários com score maior que zero, não bugs diferentes.

| Método | 15 workloads / 1.026 cenários | 66 workloads / 485 cenários |
| --- | ---: | ---: |
| Distribuição equilibrada | 100.43 / 115.00 | 20.07 / 24.47 |
| Workload aleatório | 98.80 / 113.43 | 21.50 / 26.13 |
| UCB independente | 150.40 / 182.83 | 32.73 / 37.47 |
| Contextual só com progresso | 150.20 / 175.37 | 57.30 / 60.27 |
| Contextual estrutural anterior | 144.73 / 171.37 | 38.23 / 43.57 |
| Contextual com ordem | 148.67 / 176.13 | 43.23 / 48.37 |
| Híbrido com estrutura | 155.33 / 186.43 | 43.73 / 47.43 |

No grupo de 15, o híbrido sobe de 171,37 para 186,43 de impacto face ao contextual anterior (+8,8%). Porém, o UCB independente já obtém 182,83: a diferença para essa alternativa é apenas 3,60, cerca de 2%. No grupo de 66, acrescentar ordem sobe de 43,57 para 48,37 (+11,0%), enquanto o contextual só com progresso obtém 60,27.

## Variabilidade

Intervalos abaixo: bootstrap de diferenças entre sementes emparelhadas, 10.000 reamostragens, 95%. São condicionais a estas coleções e configuração, sem correção por comparações múltiplas; não medem variabilidade entre aplicações.

| Grupo | Comparação com o contextual anterior | Diferença de impacto [intervalo] | Diferença de positivos [intervalo] |
| --- | --- | ---: | ---: |
| 15 workloads | Adicionar ordem | +4.77 [-2.27, +11.40] | +3.93 [-0.87, +8.17] |
| 15 workloads | Modelo híbrido | +15.07 [+8.80, +21.30] | +10.60 [+6.73, +14.27] |
| 66 workloads | Adicionar ordem | +4.80 [+3.10, +6.87] | +5.00 [+3.33, +7.03] |
| 66 workloads | Modelo híbrido | +3.87 [-1.07, +8.80] | +5.50 [+0.37, +10.60] |

As diferenças mais claras face ao contextual anterior são o híbrido no grupo de 15 e a ordem no grupo de 66. As outras duas diferenças médias de impacto têm intervalos que incluem zero.

Uma [comparação exploratória adicional](exploratory-baseline-contrasts.json) confirma o limite prático: o ganho de impacto do híbrido face ao UCB independente no grupo de 15 tem intervalo [-0,67; +7,83]. No grupo de 66, o híbrido fica 12,83 pontos abaixo do contextual só com progresso, com intervalo [-16,00; -9,40]. Esta comparação adicional não fazia parte dos contrastes principais fixados.

![Médias das 30 sementes](allocation-comparison.png)

As curvas mostram médias; [summary.json](summary.json) preserva também percentis 10 e 90 nos resultados finais, checkpoints e contrastes emparelhados.

## Verificação e limites

- 131 testes do módulo e três testes do estudo passam. A revisão independente do híbrido resolve diretamente o sistema completo de regressão: três regularizações, 360 previsões e erro máximo 6,66e-15 nos coeficientes, estimativas e incerteza.
- Os sete contadores da ordem foram recalculados independentemente nos 81 workloads, incluindo 33 entregas de eventos. Todos coincidem. As associações a Saga/passo/recetor estão disponíveis nos 81; 16 mantêm uma limitação registada de análise do dispatch de updateQuizStep.
- As características referem-se a tipos de agregado e acessos extraídos. Não provam identidade do objeto, execução do acesso nem uma anomalia. SagaCommand pode representar uma escrita do protocolo/lock; não foi reclassificado manualmente como escrita de negócio. Ausência de um acesso nos metadados não prova que ele não exista.
- Todas as 420 pesquisas têm recibos verificados. Os estados iniciais coincidem entre métodos. As 16.069 sequências locais comparadas coincidem enquanto têm o mesmo comprimento observado: a diferença vem de onde o alocador gasta escolhas, não de outro GA.
- O custo local médio de seleção/atualização do híbrido foi 0,44 s e 2,71 s por pesquisa nos dois grupos; com ordem, 0,51 s e 2,93 s. Isto exclui preparação, leitura dos resultados e aplicação; não é um benchmark controlado de tempo total.
- O número fixo de 256 escolhas cobre frações diferentes das duas coleções. Os efeitos devem ser comparados dentro de cada coleção. As sementes variam a pesquisa local e a escolha aleatória; os desempates determinísticos por workload não foram alterados.

## Decisão proposta

Manter a pesquisa em dois níveis e comparar seriamente as alternativas simples. A ordem tem utilidade medida numa coleção; o híbrido melhora o contextual anterior na outra, mas nenhum justifica ainda substituir todos os métodos por um único default. Não combinar automaticamente as duas alterações, alterar os pesos, abrir outra campanha ou escrever uma conclusão definitiva no paper para obter uma vitória maior.

Próximo passo: rever estes resultados com André e escolher o âmbito da apresentação. Uma experiência adicional só deve responder a uma dúvida concreta, por exemplo a interação entre ordem e híbrido; não repetir mais cenários da aplicação para testar estas mesmas políticas.

## Artefactos

- [Protocolo](protocol.json), [inventário](collection-inventory.json), [resultados](summary.json), [verificação completa](verification.json).
- [Metadados e características](order-inputs.json), [revisão da ordem](order-independent-review.json), [revisão matemática do híbrido](hybrid-independent-review.json), [reprodução das baselines](baseline-review.json).
- [Código e comandos](../../../../verifiers/experiments/allocator-order-hybrid/README.md). Traces compactos: `verifiers/target/allocator-order-hybrid-2026-09-21/`, organizados por coleção/método/semente.
- Os hashes das referências originais foram revalidados. O runner das baselines precede apenas o acréscimo da verificação de hash dos metadados novos; a versão exata foi preservada em `sources/baseline-runner.py`. Nenhum ciclo de decisão, reward ou GA foi alterado entre braços.
