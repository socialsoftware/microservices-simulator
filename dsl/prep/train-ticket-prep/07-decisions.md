# 07 — Decisões de Design (train-ticket → Nebula)

**Data:** 2026-08-04. Fecha os gaps abertos em `SUMMARY.md § Next steps` e nos ficheiros `03`, `04`, `05`.

Cada decisão regista a **escolha**, a **justificação** e o **impacto no modelo**. Este ficheiro é
a fonte para o Cap. 4 da dissertação (*Design da reprodução*).

> Contexto transversal: o train-ticket **não tem event bus, não tem foreign keys e não impõe
> integridade referencial em lado nenhum**. Toda a integridade abaixo é **introduzida pelo modelo
> Nebula** — é uma diferença deliberada face à fonte, não uma reprodução dela, e é um dos
> resultados reportáveis da tese.

---

## D1 — Snapshots do Order são congelados na reserva

**Escolha:** as projeções que o `Order` guarda de `Contacts`, `Station` e `TrainType` são
**congeladas no momento da reserva**. O Order subscreve apenas os `*DeletedEvent` (integridade),
**nunca** os `*UpdatedEvent`.

**Justificação:** uma encomenda é um registo financeiro/histórico. O bilhete tem de continuar a
dizer o nome do passageiro e as estações **tal como estavam quando foi comprado**. É também o que
a fonte faz na prática — o `ts-order-service` copia `contactsName`/`from`/`to`/`trainNumber` para
a linha da encomenda e nunca mais lhes toca.

**Impacto:** elimina metade do event wiring do Order. Sem `subscribe ContactsUpdatedEvent`,
`StationUpdatedEvent`, `TrainTypeUpdatedEvent`. Fecha o gap `03 g3`.

---

## D2 — Referências passam a ser por `aggregateId`; nomes tratados como imutáveis

**Escolha:** todas as ligações cross-aggregate passam a **projeções por `aggregateId`**
(`Entity X from Y`), que trazem automaticamente `<y>AggregateId` + `<y>Version` + `<y>State`. O
`name` continua presente como campo mapeado, mas apenas como **cópia congelada para leitura**.
Nomes de `Station` e `TrainType` são tratados como **imutáveis**.

**Justificação:** a fonte liga serviços por *nome* (`trainTypeName`, `startStationName`,
`stations[]` como `List<String>`) sem qualquer FK. Isso torna a propagação de renomeações
impossível de garantir e a integridade referencial inexistente. Passar a `aggregateId` dá
integridade real; tratar nomes como imutáveis dispensa propagação de renomeações — que a fonte
também nunca implementou.

**Impacto:**
- `Route.stations`: `List<String>` → `List<RouteStation>` (projeção de Station).
- `Trip`: perde `stationsName`; `startStation`/`terminalStation` já eram projeções.
- Nenhum `*UpdatedEvent` é publicado ou subscrito em todo o modelo — **só existem
  `*DeletedEvent`**, ao serviço da integridade. Fecha os gaps `03 g1`, `03 g2` e `04`.

---

## D3 — Disponibilidade de lugares é uma guarda Layer 2, não um inter-invariante

**Escolha:** o cálculo do `ts-seat-service` (contar encomendas vendidas contra a capacidade do
`TrainType`) é implementado como **guarda síncrona Layer 2 em `OrderService`**, dentro da saga
`PreserveOrder`, e **não** como inter-invariante nem como agregado `Seat`.

**Justificação:** pela árvore de decisão de `docs/concepts/decision-guide.md` — a regra tem de ser
verificada **sincronamente** (vender dois bilhetes para o mesmo lugar não é recuperável por
eventual consistency) e a contagem lê **apenas a tabela do próprio Order**. A capacidade
(`economyClass` / `confortClass`) chega no `TrainTypeDto` obtido num passo anterior da saga, ou
seja não há leitura de tabela alheia — respeita as restrições R1/R2. O `Seat` não é agregado:
confirmado que não tem `@Entity` na fonte, é puro cálculo.

**Impacto:** os passos de leitura de `Trip`/`TrainType` da saga levam `setForbiddenStates`
(Layer 3) para impedir escritas concorrentes durante a reserva; a contagem propriamente dita é um
método de repositório em `OrderRepository`. Fecha os gaps `04`/`05` sobre seat availability.

---

## D4 — Políticas `onDelete`

**Escolha:**

| Consumidor → Referenciado | Política | Justificação |
|---|---|---|
| `Contacts` → `User` | **cascade** | Um contacto não existe fora da conta que o criou. |
| `Route` → `Station` | **prevent** | Apagar uma estação parte todas as rotas que a usam. |
| `Trip` → `TrainType` | **prevent** | Há viagens agendadas com esse tipo de comboio. |
| `Trip` → `Route` | **prevent** | Há viagens agendadas nessa rota. |
| `Trip` → `Station` (início/fim) | **prevent** | Idem. |
| `PriceConfig` → `TrainType` | **cascade** | Uma regra de preço para um comboio que já não existe é lixo. |
| `PriceConfig` → `Route` | **cascade** | Idem. |
| `Order` → `User` | **prevent** | Encomendas são registos financeiros; não se perdem. |
| `Order` → `Contacts` | **prevent** | Idem — o bilhete tem de manter o passageiro identificado. |
| `Order` → `Trip` / `TrainType` / `Station` | **prevent** | Idem. |
| `Payment` → `Order` / `User` | **prevent** | Registo financeiro. |

**Justificação:** a regra que aplico é — *cascade* quando o agregado a jusante **não tem
significado próprio** sem o de montante (contacto sem conta, tarifa sem comboio); *prevent* em
tudo o que seja registo histórico ou dado de referência partilhado.

**Impacto:** o `message` é obrigatório no grammar do Nebula mesmo para `cascade`
(`ReferenceConstraint` exige `onDelete` **e** `message`). Fecha o gap `04`.

---

## Decisões de modelação adicionais

Surgiram ao escrever os `.nebula` e ficam registadas pelas mesmas razões.

### D5 — Códigos numéricos passam a enums tipados

`status`, `seatClass`, `documentType` e `gender` são `int` na fonte (`documentType = 1` significa
bilhete de identidade). Passam a enums em `shared-enums.nebula`: `OrderStatus`, `SeatClass`,
`DocumentType`, `Gender`, mais `TripType` e `PaymentType`.

**Porquê:** elimina invariantes de números mágicos (`documentType >= 0 && documentType <= 3`) e
torna a guarda D3 legível — `seatClass` mapeia diretamente para `economyClass` ou `confortClass`.

### D6 — `Trip.tripId` achatado em `tripType` + `tripNumber`

A fonte usa um value object `@Embeddable TripId { Type type; String number }` (ex.: `G1234`). O
Nebula não tem `@Embeddable`, logo achata-se em dois campos: `TripType tripType` +
`String tripNumber`. Resolve o `// TODO` que estava em `trip.nebula`.

### D7 — `Route.distances` absorvido na projeção da estação

A fonte tem duas listas paralelas — `List<String> stations` e `List<Integer> distances` — cuja
correspondência posicional ninguém valida. Passam a uma única lista ordenada de projeções:

```
Entity RouteStation from Station {
    map name as stationName
    Integer stationOrder
    Integer distanceFromStart
}
```

**Porquê:** o invariante de alinhamento entre as duas listas estava assinalado em `05` como
possivelmente inexprimível no grammar. Fundir as listas **elimina o invariante** em vez de o
tentar exprimir. `Route.startStation`/`endStation` desaparecem por serem deriváveis (primeiro e
último elemento) — menos denormalização, menos inconsistência possível.

### D8 — `Order.price` passa de `String` para `Double`

A fonte guarda o preço como `String`. Passa a `Double`, tal como `User.balance`,
`PriceConfig.*Rate` e `Payment.amount`.

**Porquê:** a saga `PayOrder` faz aritmética sobre estes valores (debitar saldo, calcular
reembolso). Aritmética sobre `String` seria conversão em todo o lado.

### D9 — Âmbito do ciclo de vida: `User.balance` + agregado `Payment`

O `ts-inside-payment-service` tem `Balance { userId, balance }` e o `ts-payment-service` tem
`Payment { orderId, userId, price }`. O `Balance` é absorvido como campo `balance` no `User`
(é uma relação 1:1 com a conta, não um agregado próprio); o `Payment` fica como agregado, porque
tem ciclo de vida próprio e é o registo histórico de cada movimento.

**Porquê:** dá à saga `PayOrder` três agregados a coordenar — `Order`, `User`, `Payment` — com
compensação real (repor o saldo), que é exatamente o material de validação do Cap. 6.

---

## Decisões forçadas por limitações do gerador

Descobertas ao compilar o código gerado (a validação do DSL passa limpa — só aparecem no `mvn compile`).
São material para o Cap. 5, secção sobre as lacunas do Nebula.

### D10 — `TrainType` renomeado para `Train`

`unified-type-resolver.ts:104` decide se um tipo é enum **só pelo nome**, com a regex
`/^[A-Z][a-zA-Z]*(Type|State|Role|Status|Method|Kind|Mode|Level|Priority)$/`, sem nunca consultar
os `SharedEnums` declarados. Um agregado chamado `TrainType` é classificado como enum e recebe um
`import ...shared.enums.TrainType;` que não existe.

**Escolha:** renomear o agregado para `Train` e as projeções para `OrderTrain` / `TripTrain` /
`PriceConfigTrain`. Nenhum outro nome do modelo cai nos nove sufixos-armadilha.

### D11 — Nomes de agregado de uma só palavra

Para `Aggregate PriceConfig`, o comando gerado declara `getPriceconfigDto()` mas o handler chama
`getPriceConfigDto()` — inconsistência de capitalização em nomes multi-palavra. Verificado que
renomear para uma palavra só (ex.: `Fare`) elimina o erro.

**Estado:** ainda **não aplicado** — `PriceConfig` mantém o nome da fonte (`ts-price-service`).
Decidir se se troca o vocabulário do domínio por causa de um bug do gerador.

### D12 — ⚠ EM ABERTO: dupla referência ao mesmo agregado

`Order` referencia `Station` duas vezes (`fromStation`, `toStation`) e `Trip` também
(`startStation`, `terminalStation`). O gerador assume **uma referência por par de agregados, com o
nome do campo igual ao nome do agregado em minúsculas** — é por isso que `answers/` e `teastore/`
nunca batem no problema (`user.userAggregateId`, `execution.executionAggregateId`). Resultado:

- `StationService.deleteStation` recebe o bloco de verificação duas vezes → variáveis locais duplicadas;
- a subscrição gera `getFromStationAggregateId()` quando o getter real da projeção é `getStationAggregateId()`.

**Não há contorno limpo ao nível do modelo** — origem/destino e início/fim são essenciais ao domínio.
As opções são desistir da proteção de apagamento numa das referências, ou corrigir o gerador.

---

## Resumo do impacto no modelo

| | Antes | Depois |
|---|---|---|
| Agregados | 7 | **9** (+ PriceConfig, + Payment) |
| Enums partilhados | 1 | **6** |
| Blocos `Events` | 0 | **8** |
| Blocos `References` | 0 | **7** (16 constraints) |
| `interInvariant` | 0 | **14** |
| Eventos publicados | 0 declarados | **7** `*DeletedEvent` (nenhum `*UpdatedEvent`, por D1+D2) |
