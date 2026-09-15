package experiment.inferred;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import java.lang.instrument.Instrumentation;
import static net.bytebuddy.matcher.ElementMatchers.*;

/** Generic experimental hooks; application constructor targets come from extracted contracts. */
public final class Agent {
    public static void premain(String arg,Instrumentation instrumentation)throws Exception {
        Probe.load(System.getProperty("experiment.copyContracts"));
        String[] targets=Probe.contracts.stream().map(c->(String)c.get("targetType")).distinct().toArray(String[]::new);
        new AgentBuilder.Default().with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
            .type(namedOneOf(targets)).transform((b,t,l,m,d)->b.visit(Advice.to(Copy.class).on(isConstructor().and(takesArguments(1)))))
            .type(named("pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway"))
            .transform((b,t,l,m,d)->b.visit(Advice.to(Gateway.class).on(named("send").and(takesArguments(1)))))
            .type(named("pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandService"))
            .transform((b,t,l,m,d)->b.visit(Advice.to(Inbound.class).on(named("send").and(takesArguments(1)))))
            .type(named("pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService"))
            .transform((b,t,l,m,d)->b.visit(Advice.to(Register.class).on(named("registerChanged").and(takesArguments(2)))))
            .installOn(instrumentation);
    }
    public static class Copy {@Advice.OnMethodExit public static void exit(@Advice.This Object target,@Advice.AllArguments Object[] args){Probe.copied(target,args);}}
    public static class Gateway {
        @Advice.OnMethodEnter public static void enter(@Advice.Argument(0) Object command){Probe.begin(command);}
        @Advice.OnMethodExit(onThrowable=Throwable.class) public static void exit(@Advice.Return Object result,@Advice.Thrown Throwable failure){Probe.end(result,failure);}
    }
    public static class Inbound {@Advice.OnMethodEnter public static void enter(@Advice.Argument(0) Object command){Probe.inbound(command);}}
    public static class Register {@Advice.OnMethodExit public static void exit(@Advice.Argument(0) Object aggregate){Probe.registered(aggregate);}}
}
