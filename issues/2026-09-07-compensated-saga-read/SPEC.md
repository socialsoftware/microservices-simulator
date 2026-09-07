# Observação de leitura de criação posteriormente compensada

Estado: **proposta para discussão; execução não aprovada**. Fundamentação:
[DIAGNOSIS.md](DIAGNOSIS.md). Esta fatia limita o padrão geral de leitura de efeito
posteriormente compensado à criação seguida de eliminação lógica.

## 1. O que é

Diagnóstico complementar do Saga/local ScenarioExecutor que identifica quando uma
resposta entrega a uma Saga B a revisão criada por outra Saga A e a compensação dessa
criação persiste depois a sua eliminação. Relata exposição, sem exigir dano ou escrita
posterior de B. Não é uma leitura de transação local ainda não confirmada.

Termo proposto para o glossário durante a implementação: **exposição a criação
posteriormente compensada (future)** — entrega comprovada de uma revisão criada por
outra Saga, anterior à eliminação confirmada pela compensação do passo criador.

## 2. Objetivos

1. Produzir um finding auditável por evidência exata de criação → entrega → compensação.
2. Distinguir um negativo avaliado de cobertura ausente, ambígua ou não suportada.
3. Preservar comportamento da aplicação e métricas ImpactV1/ImpactV2.

## 3. Não objetivos

Updates repostos, dano final, dependências de campos, dirty reads clássicos, lost updates,
write skew, serializabilidade, propagação de eventos, novos inputs/schedules, binding de
resultados entre participantes durante medição, gRPC/stream/TCC e mudanças em GA/score.
Não reconstruir um grafo arbitrário de DTOs ou instalar um novo framework de traces.

## 4. Requisitos funcionais

- **FR-1 — Entrega real.** Registar apenas respostas efetivamente devolvidas por uma
  chamada local bem-sucedida, após desserialização quando ativa. Cada adaptador declara
  classes exatas do comando e resultado, tipo persistente e extração tipada da identidade
  e revisão do agregado exterior. É um contrato de proveniência auditado/testado; não
  uma inferência por nomes `get*`, campos `id/version`, IDs coincidentes ou versão da UoW.
  Não atribuir ao agregado exterior as versões de referências aninhadas.
- **FR-2 — Identidade e autoria.** Conservar tentativa, workload/scenario, participante,
  ação/ocorrência, fase, comando, contrato/versão do adaptador, identidade lógica tipada,
  runtime type e revisão retornada. Exigir junção única com a escrita confirmada de A.
  Colisões de tipo, mesma revisão ambígua e autoria desconhecida são lacunas.
- **FR-3 — Predicado positivo.** Exigir: (a) ausência anterior coberta e escrita FORWARD
  de A que cria X/v sem predecessor, com lifecycle ACTIVE; (b) entrega de X/v a B≠A no
  FORWARD, antes de A concluir com sucesso; (c) posteriormente, escrita RECOVERY de A
  que persiste X/c DELETED, predecessor exato X/v; (d) essa ação pertence à compensação
  explícita do mesmo passo/ocorrência produtor, comprovada pelos registos de execução.
  A sequência deve ser criação confirmada < entrega < eliminação confirmada. Uma mera
  falha de A, execução de checkpoint ou libertação de lock é insuficiente.
- **FR-4 — Sem requisito de dano.** B pode não escrever, terminar antes da compensação
  ou mais tarde falhar/compensar. Não apagar uma exposição já provada por causa do seu
  resultado final, de uma recriação posterior ou de um uso histórico legítimo. A
  observação descreve um acontecimento, não um estado proibido no horizonte final.
- **FR-5 — Negativos e lacunas.** Aplicar a matriz abaixo. Preservar factos e motivo de
  cada desconhecido. Não inferir eliminação física de uma linha ausente nem a relação
  de compensação apenas pela mesma Saga. Cadeias com revisão intermédia ficam fora da
  primeira prova; não escolher arbitrariamente um escritor.
- **FR-6 — Cobertura.** Declarar contratos cobertos, chamadas observadas, falhas sem
  entrega, comandos sem adaptador e percursos excluídos. A primeira cobertura inclui
  lookup exterior singular de Quiz e Tournament e fixtures dummyapp. Leituras internas,
  listas, predicados, referências aninhadas, reuso em memória sem chamada e consumidores
  de eventos não têm cobertura global. Ausência de eventos não significa ausência de leituras.
- **FR-7 — Isolamento.** Excluir setup, observadores e probes. Autoria ausente/mismatched
  não pode virar B. Conter falhas de coleta e não alterar resultado, retries, locks,
  transações ou DTOs da aplicação. Lacunas exclusivas deste diagnóstico não degradam
  a cobertura nem os scores dos três checks ImpactV2.
- **FR-8 — Persistência e repetibilidade.** Guardar sidecar separado, com evidência e
  junções auditáveis, ordenação estável e IDs derivados da tentativa/ocorrências.
  Exposições repetidas do mesmo par A/B e revisão compensada formam um finding com
  referências a todas as entregas; leitores diferentes permanecem distinguíveis.
- **FR-9 — Prova.** Qualificar o predicado e os negativos em Spock com dummyapp primeiro;
  demonstrar entrega direta/serializada no framework e a cadeia Quizzes num harness
  controlado, incluindo um leitor que não persiste efeitos. Demonstrar separadamente
  persistência pelo executor normal, equivalência observador ligado/desligado e custo.

| Situação | Veredicto |
| --- | --- |
| Criação X/v de A → B recebe X/v → compensação do passo criador elimina X/v | `OBSERVED` |
| Mesma sequência; B só lê e termina sem escrever | `OBSERVED` |
| A termina bem; X muda normalmente depois | `NOT_OBSERVED`, com evidência suficiente |
| B recebe outra revisão, comprovadamente não produzida por A | Sem finding atribuído a A |
| Eliminação antes da chamada; B recebe erro | Sem entrega, sem finding |
| A falha; compensação só liberta lock ou elimina outro objeto | `NOT_OBSERVED`, se a evidência confirma isso |
| Só se sabe que A falhou/compensou; faltam escritas/revisões | `UNKNOWN` |
| Update reposto, revisão intermédia, provenance ausente/ambígua | `UNKNOWN` / razão de cobertura, nunca zero global |
| Consulta do observador ou setup | Excluída da população de leitores |

## 5. Arquitetura

Framework recolhe entregas tipadas e reutiliza a evidência de escritas confirmadas;
verifier faz a junção determinística com ações/recuperação e escreve o diagnóstico.
Adaptadores residem na integração diagnóstica da aplicação, com o mesmo contrato genérico.
Não introduzir classes Quizzes no verificador nem alterar os DTOs públicos para telemetria.
Manter as fronteiras visitor → analysis state → adapter → scenario → dynamic.
**(assumption)** A prova cobre o runtime local síncrono e revisões de dados que respeitam
o versionamento do framework; escritas diretas fora dessas fronteiras não ganham cobertura
por se ter instalado o hook de retorno.

## 6. Modelo de dados

Sidecar proposto: `<execution>.saga-read-exposure.json`, schema
`microservices-simulator.saga-read-exposure.v1`. Guarda referência/hash do manifesto e
do relatório de execução, attempt/workload/scenario IDs, âmbito/contratos, entregas,
escritas necessárias e fontes de baseline/checkpoint, findings e lacunas. O tipo runtime
desambigua a identidade persistente atual; row IDs e timestamps não são chaves causais.

Separar validade da execução, cobertura da coleta (`COMPLETE_WITHIN_SCOPE`, `PARTIAL`,
`UNAVAILABLE`) e veredictos individuais. Prefixos interrompidos podem conservar um
`OBSERVED` já provado, mas não sustentar ausência completa. `observedExposureCount` é
contagem diagnóstica de findings, um limite inferior quando parcial; é null quando não
houve medição utilizável. Nunca guardar `impactScore` nem somar ao ImpactV2. Cada sidecar
é autocontido para as suas provas e explicita dependências de evidência indisponíveis.

## 7. Modelo de segurança

Código de aplicação e adaptadores tipados são a fronteira de confiança da proveniência.
Não avaliar texto, chamar mutações ou varrer getters arbitrários como oráculo. Guardar
só identidade/revisão e factos necessários; não duplicar payloads pessoais dos DTOs.

## 8. Operação

Opt-in proposto `microservices.simulator.saga-read-exposure.enabled=true`, desligado
por omissão, sem novo sidecar quando desligado. Quando ativo, reutiliza a coleta de
escritas existente; se esta estiver indisponível/desligada,
o diagnóstico diz `UNAVAILABLE`, sem a ativar implicitamente. Desligar reverte ao custo
atual; não há migração de dados. Setup/preflight não fabricam leituras medidas.

## 9. Evolução futura

Uma extensão a updates exigirá contrato sobre a projeção/efeito reposto e a sua relação
com o retorno, mais controles de escrita concorrente e compensação parcial. A geração
normal do par que consome um ID produzido durante medição é trabalho separado.

## 10. Decisões abertas

| # | Decisão | Recomendação | Alternativa | Impacto |
| --- | --- | --- | --- | --- |
| 1 | Âmbito a aprovar | Primeira fatia criação eliminada e adaptadores explícitos | Incluir updates/proveniência geral já | Reabre contrato de efeito e instrumentação; ver os dois exemplos no DIAGNOSIS |

Os documentos definem a proposta concreta, não uma aprovação presumida. O checkpoint
de implementação é o [PLAN](PLAN.md).
