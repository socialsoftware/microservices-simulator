# Reunião de tese — 2026-W30

## Objectivos da última reunião

- Começar a escrever o artigo, com foco inicial na introdução, solução proposta e trabalho relacionado.
- Reformular o catálogo de cenários para que os vectores de falhas e as intercalações de compensações façam parte da semântica reprodutível dos cenários, em vez de serem apenas escolhas feitas durante a execução.
- Definir o novo modelo do catálogo antes de alterar o gerador ou o executor.

## Progresso desta semana

### Revisão da tese

- Recolhi e tratei de forma sistemática os 26 comentários por resolver e as 11 alterações registadas pelo orientador nos Capítulos 2 e 4.
- Reorganizei o Capítulo 2 para separar arquitectura de microsserviços, DDD e responsabilidades de consistência, concorrência e comportamento de Sagas, e testes e observabilidade do simulador.
- Reorganizei o Capítulo 4 em torno da definição do problema e requisitos, estratégia da solução, arquitectura e implementação no simulador.
- Movi o conteúdo específico da tese sobre o espaço de cenários, o algoritmo genético e o contextual bandit do background genérico para a solução.
- Adicionei uma figura com as intercalações do exemplo e redesenhei a figura da arquitectura offline/online para distinguir componentes de processamento de artefactos de dados.
- Actualizei os capítulos adjacentes para manter a coerência do documento.
- Compilei e inspeccionei com sucesso a tese resultante, com 62 páginas e sem referências ou citações por resolver.
- Mantive por resolver os comentários do orientador no Overleaf, em vez de os fechar sem validação.

### Artigo

- Produzi uma primeira versão da introdução, com aproximadamente 950 palavras e um estilo orientado para o EuroSys.
- A introdução apresenta o problema da análise de falhas em Sagas e nas suas intercalações, o espaço combinatório de cenários, a separação entre cenários estruturais e pesquisa local, e as contribuições previstas.
- Adicionei a introdução e as respectivas referências bibliográficas ao Overleaf em modo de revisão, para que as alterações possam ser inspeccionadas, aceites ou rejeitadas.
- Confirmei que esta versão compila.
- As secções da solução proposta e do trabalho relacionado ainda não foram escritas.

### Catálogo e replay de cenários com compensações

- Implementei e integrei o trabalho do catálogo com compensações.
- Substituí o catálogo anterior, com um único nível, por dois níveis semânticos relacionados:
  - um `WorkloadPlan` regista participantes, inputs, o schedule forward, evidência de conflitos, pontos de injecção de falhas e checkpoints de compensação;
  - um `FaultScenario` regista um vector de falhas atribuído e um schedule concreto de acções forward e de compensação.
- As compensações podem ser executadas antes, entre ou depois das acções restantes das Sagas activas, preservando o replay determinístico e a ordem forward residual original.
- Adicionei:
  - geração eager limitada para o vector sem falhas e para todos os vectores com uma única falha;
  - selecção determinística de schedules de recuperação sob um limite configurável;
  - persistência on demand de vectores arbitrários com múltiplas falhas;
  - replay exacto das acções forward e de compensação persistidas;
  - commit automático dos participantes e registo explícito do seu ciclo de vida;
  - separação entre falhas de domínio, falhas de infra-estrutura e falhas de compensação;
  - relatórios de execução orientados às acções.
- A execução limitada sobre Quizzes produziu:
  - 2 000 WorkloadPlans;
  - 12 WorkloadPlans materializáveis;
  - 60 vectores de falhas eager;
  - 84 FaultScenarios;
  - um cenário executado com intercalação de compensações.
- A validação passou com 96 testes do simulador, 509 testes do verificador e 509 testes através do fluxo Docker do verificador.
- A revisão final também resolveu lacunas de integridade e replay relacionadas com checksums dos packages, classificação explícita de falhas, retenção conservadora de checkpoints de compensação, mutação de packages entre processos e ocorrências repetidas de steps em runtime.

## Estado dos objectivos

- Introdução do artigo: primeira versão para revisão concluída.
- Solução proposta do artigo: ainda não escrita.
- Trabalho relacionado do artigo: ainda não escrito.
- Modelo do catálogo com compensações: concluído.
- Geração do catálogo e replay persistido: concluídos.
- Evidência limitada de intercalações de compensações: concluída.
- Impact scoring e detecção de anomalias: não implementados.
- Pesquisa genética e alocação contextual: não implementadas.

## Próximo trabalho

1. Escrever a secção da solução proposta do artigo com base no modelo implementado de `WorkloadPlan` e `FaultScenario`.
2. Escrever a secção de trabalho relacionado sobre injecção de falhas distribuídas, testes de Sagas, detecção de anomalias de concorrência e exploração guiada do espaço de testes.
3. Rever com o orientador a nova organização dos capítulos da tese, o texto e as figuras.
4. Aumentar o número e a diversidade de WorkloadPlans e FaultScenarios materializáveis, resolvendo os bloqueios actuais de inputs e materialização e medindo a cobertura resultante.
5. Definir a evidência e o cálculo de impacto antes de implementar o algoritmo genético ou a alocação contextual.
6. Começar a análise de impacto com invariantes fortes da aplicação e depois experimentar vários invariantes e outras famílias de anomalias ou oráculos.
7. Ligar o impacto validado à pesquisa genética local e, posteriormente, à priorização contextual de cenários.

## Questões para discussão

1. O modelo com os dois níveis `WorkloadPlan` e `FaultScenario` é adequado como modelo central apresentado tanto no artigo como na tese?
    R: Sim
2. Na próxima semana, a prioridade deve ser concluir as secções da solução proposta e do trabalho relacionado do artigo ou implementar o primeiro oráculo de impacto baseado em invariantes?
    R: Escrita :)
3. A baseline eager limitada — um vector sem falhas e todos os vectores com uma única falha, gerando cenários com múltiplas falhas on demand — é uma metodologia experimental adequada?
    R: Sim

## Novos requisitos resultantes da reunião

- A execução de um cenário não deve continuar até ao fim quando ocorre uma falha inesperada num dos seus steps.
- Um vector de falhas não deve atribuir mais do que uma falha à mesma instância de Saga, uma vez que a primeira falha torna inalcançáveis os seus steps seguintes.
