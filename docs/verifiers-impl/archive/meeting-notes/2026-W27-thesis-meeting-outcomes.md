# Notas da reunião de tese — 2026-W27 — outcomes

## Contexto

Estas notas registam os resultados da reunião de hoje. São histórico de orientação da tese, não fonte de verdade da implementação actual. Antes de citar estado técnico, confirmar contra [`../../current-state.md`](../../current-state.md), [`../../roadmap.md`](../../roadmap.md) e [`../../evidence.md`](../../evidence.md).

## Direcção acordada

A prioridade passa a ser tornar a pipeline avaliável do ponto de vista experimental, em vez de continuar apenas a aumentar cobertura estática.

O próximo corte deve concentrar-se em:

1. usar os testes happy-path existentes para obter cenários normais e plausíveis;
2. restringir os interleavings a casos que fazem sentido semanticamente, sobretudo quando há evidência de que as Sagas actuam sobre o mesmo agregado/instância;
3. executar cenários suficientes para recolher logs e estado final;
4. definir métricas de impacto com base em anomalias observáveis;
5. decidir uma estratégia inicial simples para injecção de falhas antes de avançar para GA.

## 1. Interleavings a partir de happy paths normais

O catálogo não deve tratar todos os interleavings geráveis como igualmente úteis para a avaliação.

Próximo foco:

- partir de inputs vindos de testes happy-path reais;
- preferir combinações em que as Sagas partilham evidência de agregado relevante;
- quando possível, exigir evidência de mesma instância de agregado, não apenas mesmo tipo;
- usar type-only como fallback conservador, mas não o vender como conflito exacto;
- evitar gastar esforço em interleavings independentes que dificilmente geram anomalias de domínio.

Formulação segura para a tese:

> Os testes existentes são usados como fonte de workloads normais. A geração de cenários tenta explorar variações concorrentes/falhadas desses workloads, priorizando combinações com evidência de interacção em agregados partilhados.

Implicação técnica:

- melhorar aggregate-instance/key binding onde isso muda a utilidade dos cenários;
- manter a segment compression como redução prática, mas explicitar que ela depende da qualidade da evidência de conflitos extraída.

## 2. Anomalias e impact metric a partir dos logs

A métrica de impacto deve combinar sinais observáveis de execução, em vez de depender apenas de “houve exception”.

Sinais candidatos:

- dirty reads ou leituras de estado intermédio/inconsistente;
- violações de invariantes de domínio;
- divergência entre estado esperado e estado final observado;
- compensações incompletas, falhadas ou divergentes;
- Sagas abortadas ou presas em estado não terminal;
- exceptions não tratadas;
- erros em traces/logs quando ligados a uma consequência de domínio.

Ordem recomendada de prioridade:

1. **Invariantes violadas** — sinal mais forte e mais defensável para a tese.
2. **Divergência de estado final** — útil quando existe oracle simples a partir do happy path.
3. **Compensation divergence/failure** — muito relevante para Sagas.
4. **Dirty reads/anomalias de isolamento** — úteis quando os logs permitem reconstruir reads/writes e versões.
5. **Exceptions/log errors** — manter como sinal auxiliar, não como métrica principal.

Formulação segura:

> A impact metric deve privilegiar efeitos de domínio: invariantes quebradas, estado final inconsistente e compensações incorrectas. Anomalias infra-estruturais ou exceptions só contam fortemente quando implicam uma consequência observável no domínio.

Questão em aberto:

- quais invariantes serão implementadas primeiro: invariantes genéricas derivadas de estado/logs, ou invariantes específicas declaradas para Quizzes?

Decisão provisória:

- começar por poucas invariantes explícitas e fáceis de validar;
- usar logs/traces para detectar padrões como dirty reads, mas não bloquear a tese à espera de uma taxonomia completa de anomalias.

## 3. Injecção de falhas: brute force vs GA

Não começar directamente por uma GA complexa sem uma baseline simples.

Plano recomendado:

1. implementar injecção de falhas com uma representação simples: bit vector sobre passos schedulados;
2. criar uma baseline exaustiva pequena para cenários com poucos passos;
3. para cenários maiores, usar uma baseline heurística/limitada:
   - single-point failures;
   - falhas em passos com writes;
   - combinações pequenas de falhas próximas dos conflict anchors;
4. só depois introduzir GA como optimização local sobre o mesmo espaço de fault vectors.

Razão:

> A tese precisa primeiro de uma baseline executável e mensurável. A GA só é defensável depois de existir executor, fault model e impact score estáveis.

Formulação segura:

> Brute force é usado como baseline em espaços pequenos e como referência conceptual. Para espaços maiores, a pesquisa local com GA procura configurações de falhas de maior impacto sob orçamento limitado.

## 4. Escrita da tese

A escrita deve começar já, em paralelo com a implementação.

Capítulos/secções que podem avançar agora:

- problema e motivação;
- contexto de Sagas, consistência e fault injection;
- arquitectura geral da solução;
- cenário generator e catálogo determinístico;
- extracção estática a partir de código e testes;
- dynamic evidence como enriquecimento sidecar;
- redução de interleavings por conflitos/segment compression;
- limitações actuais e ameaças à validade.

Secções que devem ficar como desenho + avaliação futura até haver implementação:

- executor genérico;
- injecção de falhas completa;
- impact metric final;
- GA/local search;
- bandit/priorização.

Regra para evitar overclaiming:

> Separar claramente o que está implementado, o que é POC, e o que é desenho planeado para completar a avaliação.

## Próximas acções

1. Definir o menor conjunto de cenários executáveis baseados em happy paths normais.
2. Priorizar cenários com evidência de mesmo agregado/instância quando disponível.
3. Escolher 2–3 invariantes iniciais para a impact metric.
4. Definir o formato mínimo de relatório de execução: schedule, fault vector, logs relevantes, estado final, violações detectadas e score.
5. Implementar fault injection primeiro como baseline simples antes de GA.
6. Começar a escrever os capítulos estáveis da tese, usando os docs actuais como material de apoio mas validando claims contra o estado actual.

## Frase-resumo

> O próximo marco da tese deve ser uma pipeline experimental mínima: workloads happy-path transformados em cenários plausíveis, interleavings filtrados por interacção em agregados, execução com falhas simples, logs/estado final convertidos numa métrica de impacto, e só depois pesquisa GA para escalar além da baseline pequena.
