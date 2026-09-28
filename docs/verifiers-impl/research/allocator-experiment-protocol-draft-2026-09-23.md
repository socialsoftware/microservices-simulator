# Experiência de alocação entre workloads — rascunho do protocolo

**Base de resultados atual:** [inventário mestre de workloads](../evidence/workload-master-inventory-2026-09-25/README.md). O protocolo e a amostra final continuam por congelar.

**Estado:** rascunho de desenho, 23 setembro 2026. Não é um protocolo congelado,
uma seleção de workloads, uma autorização de execução, nem uma conclusão sobre o
melhor método. Primeiro definimos as perguntas e as regras; depois auditamos a
amostra possível no Quizzes, estimamos o custo e fechamos IDs, parâmetros e
orçamentos antes de observar os novos resultados comparativos.

A [auditoria inicial e piloto local de componentes](allocator-component-pilot-2026-09-23.md)
já verificaram as variantes em falta nos painéis existentes e mostraram que a
escala inicial das características estruturais altera os percursos. Um primeiro
controlo de norma alterou também o bias; o controlo corrigido preserva-o e
emparelha o bónus inicial, mas ainda exige análise de sensibilidade e permutação
da estrutura. A nova amostra, as famílias e os orçamentos continuam por definir
e este documento permanece rascunho.

## Objetivo e âmbito

Queremos perceber se, com um orçamento global limitado, a escolha adaptativa do
próximo workload encontra mais cedo cenários de impacto e que informação explica
as escolhas. Uma decisão dá uma tentativa a um WorkloadPlan ainda ativo; a pesquisa
local escolhe um FaultScenario ainda não observado. A reward é a score configurada
desse cenário, revelada só depois da tentativa. Os resultados serão comparações
entre métodos na mesma coleção e no mesmo perfil de reward, não uma regra universal
para escolher antecipadamente um vencedor.

As experiências de 21 setembro são dados de desenvolvimento, já examinados.
Mostraram resultados dependentes do perfil e da coleção; não isolam completamente
o significado das características estruturais da escala da exploração. Os 96
workloads inventariados representam 45 conjuntos de Sagas e incluem ordens/inputs
relacionados, não 96 histórias independentes. A nova confirmação exigirá famílias
fixadas antes de observar as comparações novas.

## Perguntas e variantes principais

Todos os métodos recebem exatamente o mesmo catálogo, a mesma sequência de seeds,
o mesmo orçamento, o mesmo perfil de reward e a mesma política local em cada
comparação. Partem sem aprendizagem anterior. Sem score, a tentativa conta no
orçamento mas não atualiza o modelo nem entra como pai no GA; score zero é um
resultado **negativo observado** e atualiza normalmente.

| Nome de trabalho | Informação/modelo para escolher o workload | Estado atual |
| --- | --- | --- |
| Equilibrado | Volta pelos workloads ativos, sem aprendizagem | Existe |
| Aleatório | Sorteia um workload ativo | Existe |
| UCB independente | Média e bónus de exploração próprios de cada workload | Existe |
| P-partilhado | Um LinUCB sobre bias e seis características de progresso observado, aprendido em todos os workloads | Existe |
| S-partilhado | Um LinUCB sobre características estáticas de estrutura, sem características de progresso | A acrescentar |
| SP-partilhado | Um LinUCB sobre estrutura **e** progresso observado | Existe; chamado «estrutural» nos resultados anteriores |
| P-local | Um LinUCB de progresso separado para **cada** workload, sem coeficientes partilhados | A acrescentar; não é o UCB independente |
| H0 | Híbrido com bias partilhado e progresso local por workload, sem tokens estruturais | A acrescentar |
| H1 | Híbrido com estrutura partilhada e progresso local por workload | Existe; chamado «híbrido» nos resultados anteriores |

P-local mantém, para cada workload, a sua previsão e incerteza. Um W2 ainda não
visitado começa sem recompensa aprendida, mas com incerteza positiva: continua
elegível e pode ter prioridade sobre W1 depois das observações em W1. Não há
obrigação de visitar todos os workloads antes de esgotar um orçamento curto.
Registar quantos ficaram por visitar. Os desempates e a regra de cold start serão
determinísticos e idênticos entre variantes sempre que a arquitetura o permita.

Comparações principais, declaradas antes dos resultados:

1. **P-partilhado vs SP-partilhado:** benefício de acrescentar estrutura ao mesmo
   LinUCB com progresso partilhado.
2. **S-partilhado vs SP-partilhado:** benefício de acrescentar progresso ao mesmo
   LinUCB estrutural partilhado.
3. **P-partilhado vs P-local:** efeito de partilhar a aprendizagem de progresso,
   mantendo as mesmas características e família de modelo.
4. **H0 vs H1:** benefício dos tokens estruturais no mesmo desenho híbrido.
5. **SP-partilhado vs H1:** comparação prática entre progresso partilhado e
   progresso local com estrutura; muda a localização dos parâmetros, pelo que
   deve ser interpretada como comparação de arquiteturas, não como isolamento
   de uma única característica.
6. Comparar cada método adaptativo com UCB independente e com os dois métodos
   sem aprendizagem, para quantificar se a complexidade traz ganho útil.

As características de **ordem** ficam numa análise secundária e entram em pares
iguais salvo essa adição: SP vs SP+ordem e H1 vs H1+ordem. A ordem atual descreve
acessos potenciais a tipos de agregado, não identidade provada do objeto nem
ocorrência de anomalia. O tratamento de resultados sem score (por exemplo,
cooldown) constitui outra pergunta secundária, com pares próprios; não o ligar
apenas a algumas variantes da comparação principal.

## Perfis de reward e comportamentos encontrados

Executar desde o início o GA e o alocador sob **seis perfis predefinidos**:

1. Os cinco critérios com peso um.
2. Só `DELETED_DEPENDENCY`.
3. Só `FAILED_OPERATION_RESIDUAL`.
4. Só `UNRESOLVED_DELIVERED_EVENT`.
5. Só `COMPENSATED_READ_EXPOSURE`.
6. Só `LOST_COPIED_UPDATE`.

O perfil muda o feedback dado **ao GA e ao alocador**; não basta voltar a somar
as scores de um percurso obtido com outros pesos. Em cada perfil, positivo
significa score disponível maior que zero; negativo significa score disponível
igual a zero; sem score é uma terceira categoria. Reportar também as cinco
componentes observadas no mesmo cenário para distinguir coocorrência de tipos de
impacto. Não chamar a seis cenários positivos «seis defeitos independentes».

Além dos critérios, criar uma taxonomia auditável dos comportamentos de aplicação
e das razões dos findings a partir dos relatórios. Fixar as regras de codificação
antes da comparação final; mostrar exemplos e contagens, e admitir a classe
«não classificado» quando a evidência não chega. A taxonomia é para interpretação
posterior e nunca entra nas características fornecidas aos métodos.

Um perfil sem positivos num painel não sustenta uma comparação de descoberta nesse
painel. Preservar esse resultado zero no inventário, mas não apresentar diferenças
de score como evidência de eficácia para esse critério. Se um mecanismo raro
precisar de coleção enriquecida, declarar seleção, prevalência e alcance dessa
coleção separadamente da amostra estrutural geral.

## Amostra e painéis: regras a completar após auditoria ao Quizzes

A auditoria seguinte construirá o universo elegível de WorkloadPlans e estimará
quantos catálogos completos e execuções são necessários. A seleção deve usar
informação anterior ao resultado do fault scenario: conjunto de Sagas, operações,
interações, eventos, ordens, proveniência de inputs e tamanho **enumerado** do
catálogo. Incluir workloads de tamanhos pequenos, médios e grandes; não deixar
que centenas de variantes de uma única família dominem a amostra. Registar todas
as exclusões e os controlos de admissão. Não selecionar novos workloads porque
um método neles ganhou ou perdeu.

O painel principal admite controlos sem falha com score positivo ou uma compensação
de base quando há resultado medido e score completo nos critérios ativos. O score
do controlo fica oculto até o seu cenário ser escolhido, altura em que conta no
orçamento. Classificar os desvios de base na análise; não converter medições
incompletas em negativos.

A ambição é crescer para **centenas de workloads**, mas o alvo final depende da
diversidade e do custo medidos na auditoria. Contaremos sobretudo famílias de
Sagas e mecanismos de impacto distintos, não apenas IDs ou cenários. Antes de
fechar um número, estimar precisão com um piloto e verificar se há suporte útil
para cada um dos cinco critérios. Separar painéis de desenvolvimento, usados para
validar características e calibrar parâmetros, de painéis de confirmação com
famílias de Sagas reservadas. Workloads que diferem só em input/ordem pertencem
ao mesmo grupo de separação. Se a recolha ficar apenas no Quizzes, limitar as
conclusões a essa aplicação e às famílias estudadas.

Preferir mapas completos para replay e para conhecer o denominador de positivos
e negativos. Fixar versão do gerador, executor, assessor, catálogo, inputs e
runtime por painel; referências de versões diferentes não serão misturadas sem
uma comparação de compatibilidade explícita. Guardar resultados sem score e
repetições de execução num subconjunto destinado a medir possível variação do
runtime; um único resultado guardado por cenário não mede essa variação.

Os painéis terão regras explícitas para número de workloads, distribuição de
tamanhos, composição de famílias e orçamento. O orçamento principal e os
checkpoints devem permitir estudar descoberta precoce e posterior sem esgotar
imediatamente os catálogos pequenos. Fixaremos números depois de ver o inventário
de tamanhos e o custo, **antes** de executar os modelos nos painéis reservados.

## Comparabilidade, seeds e controlos

Todos os métodos que aprendem usam seleção por previsão mais incerteza. Os
modelos estruturais têm mais coordenadas que P-partilhado; os mesmos valores
numéricos de exploração e regularização não implicam incerteza inicial igual.
No desenvolvimento, inspecionar a distribuição da prioridade inicial, escolher
uma normalização e uma regra de calibração comuns, e congelá-las. Guardar uma
análise de sensibilidade predefinida com os valores originais. Incluir um controlo
em que os perfis estruturais são permutados entre workloads comparáveis, mantendo
dimensão e escala, para verificar se o conteúdo da estrutura contribui além da
geometria das características. A permutação é apenas controlo analítico, não
um método candidato para uso real.

A seed global cria uma seed local estável por `(seed, workload ID)`. Essa seed
controla a sequência do GA naquele workload; o método aleatório de alocação
também a usa diretamente. Os UCBs e híbridos não sorteiam workloads no passo
de seleção. A mesma seed e o mesmo perfil devem produzir prefixos locais
iguais quando dois métodos dão o mesmo número de tentativas ao mesmo workload.
Seeds diferentes testam sensibilidade ao GA, **não** criam novas famílias de
workloads. Fixar lista de seeds e regra de desempate no protocolo final.

Na análise principal, todos usam o mesmo GA local. Num segundo fator, repetir
um subconjunto fixado das variantes centrais com pesquisa local uniforme sobre
cenários ainda não vistos. Comparar o efeito do alocador sob as duas políticas
locais, mantendo recompensa, seed, catálogo e orçamento. Esta análise testa a
interação entre alocador e GA; não altera a conclusão da análise principal por
escolha posterior de pares favoráveis.

## Métricas e análise

Métrica principal por perfil e orçamento: impacto configurado acumulado e número
de cenários positivos únicos encontrados. Reportar também fração dos positivos
conhecidos no catálogo, quando o mapa é completo, e curvas de descoberta. Por
critério e comportamento: contagem de cenários encontrados, primeira descoberta,
coocorrências e exemplos de decisões que levaram às descobertas. Por percurso:
escolhas e scores por workload/família, negativos observados, resultados sem
score, workloads nunca visitados, exaustão e evolução de previsão e incerteza.
Não confundir escolhas em negativos com escolhas sem resultado.

Comparar métodos com as mesmas seeds e painéis, reportando diferenças
emparelhadas, intervalos, vitórias e perdas. A inferência principal respeitará
o agrupamento por família de Sagas/painel; dezenas de seeds na mesma família
não serão tratadas como dezenas de aplicações independentes. Declarar antes
quais comparações e orçamentos são primários; os restantes gráficos e contrastes
são exploratórios. Não inferir equivalência de um intervalo que inclua zero.

## Portões antes de congelar e executar

1. Auditar Quizzes: universo elegível, estrutura, tamanhos completos, cobertura
   dos cinco critérios, mecanismos distintos, resultados sem score, versões de
   referência e custo de execução/armazenamento.
2. Rever a construção das características, seleção, atualização, GA e pontuação;
   corrigir qualquer erro comprovado e preservar referências antigas com versão.
3. Fazer um piloto **apenas de desenho** para estimar custo, diversidade,
   variância e escalas de exploração. Não usar os painéis reservados para ajustar
   as variantes.
4. Fechar um manifesto com IDs, regras de seleção, código e hashes, perfis,
   modelos, parâmetros, seeds, orçamentos, métricas, comparações e exclusões.
5. Verificar prefixos do GA, cálculo independente das rewards, separação entre
   resultados selecionados e ocultos, e reprodutibilidade de uma seed por método.
6. Só depois recolher/combinar os novos mapas e correr a comparação congelada.
   A nova campanha de aplicação/cluster e o custo respetivo são uma decisão
   separada; este rascunho não os inicia.

## Fontes de partida

- [Comportamento implementado](../current-state.md#bounded-cross-workload-allocator)
- [Direção acordada em reunião](../reunioes/2026-09-22.md#3-bandit--escolhas-e-resultados)
- [Resultados exploratórios atuais](../evidence/allocator-order-hybrid-followup-2026-09-21/RESULTS.md)
- [Inventário dos mapas completos](../evidence/transfer-inventory-2026-09-21/RESULTS.md)
