# Guião para conversa com o orientador sobre profiling e enriquecimento runtime

## Objetivo

Este guião prepara uma conversa curta com o orientador sobre como juntar o trabalho de performance/profiling do Martim com o trabalho de enriquecimento runtime do catálogo de cenários.

## Guião curto

"Estive a olhar para o branch do Martim, especialmente para a parte de instrumentação no `simulator`. Pelo que percebi, o trabalho dele de performance está bastante centrado em aspetos Spring AOP. Por exemplo, o `HardwareProfiler` usa um `@Around` sobre métodos públicos de serviços em `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices..service.*.*(..)`, lê contadores de hardware antes e depois do `joinPoint.proceed()`, e escreve métricas como instruções, ciclos e CPI. O `CapacityManager` usa uma ideia parecida para adquirir e libertar capacidade com semáforos à volta dos métodos dos serviços. E o `ImpairmentHandler` envolve o `CommandGateway.send(...)` para criar spans de comandos e, quando configurado, delegar no `NetworkManager` a introdução de delays antes e depois do envio."

"A ligação com o meu trabalho é que eu também preciso de evidência runtime, mas a minha evidência é mais semântica do que performance-oriented. No meu caso, estou a registar eventos como `STEP_STARTED` e `STEP_FINISHED` no `ExecutionPlan`, `COMMAND_SENT` no `LocalCommandGateway`, e `AGGREGATE_ACCESSED` no `SagaUnitOfWorkService`. Depois o verificador lê estes eventos em JSONL e junta-os ao catálogo estático de cenários."

"Por isso, acho que a junção limpa não deve ser 'o meu tracer contra o profiler dele'. Devíamos pensar numa camada comum de observabilidade runtime. Os aspetos dele com `@Around` são muito bons para preocupações transversais, como chamadas a serviços, envio de comandos, capacidade, delays de rede e profiling de hardware. Os meus hooks internos continuam a fazer sentido onde só o simulador conhece o contexto semântico, como o nome do passo da saga, o `inputVariantId`, a versão da unidade de trabalho e o acesso ao agregado."

"A proposta concreta seria termos um `RuntimeObservationRecorder` comum e um esquema comum de evento. Do meu lado, os eventos seriam coisas como `STEP_STARTED`, `STEP_FINISHED`, `COMMAND_SENT`, `AGGREGATE_ACCESSED` e, no futuro, talvez `INVARIANT_VIOLATION`. Do lado do Martim, poderiam ser `SERVICE_PROFILED`, `CAPACITY_WAITING`, `CAPACITY_ACQUIRED`, `CAPACITY_RELEASED` e `NETWORK_DELAY_INJECTED`. O ponto importante é os dois anexarem o mesmo contexto: teste, cenário, `inputVariantId`, funcionalidade, invocação da funcionalidade, passo, comando, serviço origem, serviço destino e, se possível, `traceId`/`spanId`."

"Também reparei em algumas coisas que valia a pena limpar em conjunto. O pointcut do profiling está hard-coded para os serviços do `quizzes`, por isso talvez devêssemos torná-lo configurável ou mais genérico para aplicações do simulador. O `HardwareProfiler` e o `CapacityManager` envolvem o mesmo tipo de método, portanto convinha definir a ordem dos aspetos explicitamente com `@Order`. E acho importante que falhas no profiling ou escrita de reports nunca partam o comportamento da aplicação; no meu recorder de evidência dinâmica eu tento engolir esses erros para preservar a execução dos testes."

"O split que eu proporia é: usar `@Around` para interceção genérica e transversal; manter hooks internos do simulador para semântica de domínio; e emitir tudo para uma stream estruturada comum de observações runtime. Assim, a tese do Martim consome métricas de performance e a minha consome enriquecimento de cenários a partir da mesma base de evidência, em vez de termos dois mecanismos incompatíveis."

## Porque e que eu nao estou a usar `@Around`

Eu nao estou a usar `@Around` principalmente porque a minha evidencia precisa de contexto semantico que ja existe dentro do fluxo do simulador, mas que nao e necessariamente visivel a partir de uma chamada generica interceptada por AOP.

No caso do Martim, o objetivo e medir ou alterar uma fronteira transversal: "quando um metodo de servico e chamado", "quando um comando e enviado", "quando uma capacidade deve ser adquirida". Isto encaixa bem em `@Around`, porque ele so precisa de envolver a chamada original:

```java
@Around("execution(public * pt.ulisboa.tecnico.socialsoftware.quizzes.microservices..service.*.*(..))")
public Object profileServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    long[] startValues = hardwareCounterService.readValues();
    try {
        return joinPoint.proceed();
    } finally {
        long[] endValues = hardwareCounterService.readValues();
        // escreve instructions, cycles, CPI
    }
}
```

No meu caso, eu preciso de saber coisas como: qual e o passo da saga, qual e a funcionalidade, qual e a versao da `UnitOfWork`, qual e o `inputVariantId`, e quando exatamente um passo comeca e acaba. Esse contexto esta dentro do `ExecutionPlan`, por isso o hook atual fica no ponto onde o simulador ja tem essa informacao:

```java
DynamicEvidenceContext.Scope scope = DynamicEvidenceContext.enterStep(
        funcName,
        functionalityClassFqn,
        functionalityClassSimpleName,
        stepName,
        unitOfWorkVersion);

DynamicEvidenceRecorderHolder.recordStepStarted(context);
CompletableFuture<Void> stepFuture = step.execute(unitOfWork);
return stepFuture.whenComplete((ignored, error) ->
        DynamicEvidenceRecorderHolder.recordStepFinished(context, outcome, error));
```

Isto e o que chamo um hook interno: nao e um aspeto externo que envolve qualquer metodo por padrao; e uma chamada explicita no ponto do simulador onde a semantica do dominio esta disponivel.

Outro exemplo: para o Martim, interceptar `CommandGateway.send(...)` com `@Around` faz sentido para atrasos de rede ou tracing transversal:

```java
@Around("execution(public * pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway.send(..)) && args(command)")
public Object wrapSend(ProceedingJoinPoint joinPoint, Command command) throws Throwable {
    // pode medir, atrasar, registar span
    return joinPoint.proceed();
}
```

No meu lado, para enriquecer cenarios, o comando nao e so uma chamada: e evidencia de que um passo enviou um comando especifico, com uma funcionalidade e uma atribuicao de input. Por isso o registo atual acontece no gateway, mas delega para o contexto dinamico ativo:

```java
DynamicEvidenceRecorderHolder.recordCommandSent(command, dynamicEvidenceProperties);
```

E, para acessos a agregados, o ponto certo nao e um metodo generico de servico, mas sim o momento em que o `SagaUnitOfWorkService` sabe que um agregado foi escrito e que versao recebeu:

```java
aggregate.setVersion(commitVersion);
entityManager.merge(aggregate);
unitOfWork.setVersion(commitVersion);
recordAggregateAccess("WRITE", aggregate, unitOfWork, "SagaUnitOfWorkService.registerChanged");
```

Portanto, eu nao diria que `@Around` e melhor ou pior. Eu diria que serve para outro nivel de observacao. AOP e bom para fronteiras tecnicas transversais; hooks internos sao melhores quando a evidencia depende de conceitos internos do simulador e do dominio.

## Pontos técnicos para mencionar se houver tempo

- `@Around` é o advice mais flexível do Spring AOP: corre código antes e depois da chamada original.
- `ProceedingJoinPoint.proceed()` é a chamada real ao método interceptado.
- Se o advice não chamar `proceed()`, o método original não executa.
- Se chamar `proceed()` dentro de `try/finally`, consegue medir duração, registar erros, injetar delays ou garantir libertação de recursos.
- Spring AOP só intercepta chamadas via proxy Spring; não cobre chamadas privadas, construtores, self-invocation no mesmo bean, nem certos casos com métodos finais.
- Métricas thread-local, como os contadores de hardware, precisam de cuidado com execução assíncrona e thread hops.
- O meu uso de hooks internos nao e uma rejeicao de AOP; e uma escolha porque o ponto de observacao precisa de contexto semantico que o pointcut nao ve diretamente.

## Ideia de arquitetura conjunta

- `RuntimeObservationContext`: contexto partilhado com teste, cenário, funcionalidade, passo, comando e tracing.
- `RuntimeObservationRecorder`: API única para emitir eventos sem quebrar comportamento de domínio.
- `RuntimeObservationEvent`: evento estruturado com `kind`, `timestamp`, `context` e `payload`.
- Export JSONL para o verificador juntar com o catálogo de cenários.
- Export OpenTelemetry para traces e análise de performance.
- Reports textuais ou CSV como views derivadas, não como formato primário de integração.
