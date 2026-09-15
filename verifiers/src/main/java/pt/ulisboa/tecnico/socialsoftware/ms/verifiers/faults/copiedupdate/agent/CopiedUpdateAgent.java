package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.copiedupdate.agent;

import static net.bytebuddy.matcher.ElementMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.utility.JavaModule;

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate.CopiedUpdateObservation;

import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/** Only inferred constructor bodies are instrumented; framework boundaries have native hooks. */
public final class CopiedUpdateAgent {
    public static final String CONTRACTS_PROPERTY = "simulator.copied-update.contracts";

    @SuppressWarnings("unchecked")
    public static void premain(String arguments, Instrumentation instrumentation) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(System.getProperty(CONTRACTS_PROPERTY)));
            var manifest = new ObjectMapper().readTree(bytes);
            if (!"copy-contracts.v1".equals(manifest.path("schema").asText()))
                throw new IllegalArgumentException("Unsupported copy contract manifest");
            List<Map<String, Object>> contracts =
                    new ObjectMapper().convertValue(manifest.path("contracts"), List.class);
            Path sourceRoot = Path.of(System.getProperty("simulator.copied-update.source-root"))
                    .toAbsolutePath().normalize();
            for (Map<String, Object> contract : contracts) {
                var sources = (List<Map<String, String>>) contract.get("sourceFiles");
                if (sources == null || sources.isEmpty())
                    throw new IllegalArgumentException("Missing copy-contract source provenance");
                for (var source : sources) {
                    Path path = sourceRoot.resolve(source.get("path")).normalize();
                    if (!path.startsWith(sourceRoot)) throw new IllegalArgumentException("Invalid source path");
                    String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(Files.readAllBytes(path)));
                    if (!actual.equals(source.get("sha256")))
                        throw new IllegalArgumentException("Copy-contract source mismatch: " + source.get("path"));
                }
            }
            String[] targets =
                    contracts.stream()
                            .map(c -> (String) c.get("targetType"))
                            .distinct()
                            .toArray(String[]::new);
            Set<String> targetNames = Set.of(targets);
            for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
                if (targetNames.contains(loaded.getName()))
                    CopiedUpdateObservation.instrumentationFailed(
                            loaded.getName(), "TARGET_ALREADY_LOADED");
            }
            new AgentBuilder.Default()
                    .with(
                            new AgentBuilder.Listener.Adapter() {
                                @Override
                                public void onError(
                                        String name,
                                        ClassLoader loader,
                                        JavaModule module,
                                        boolean loaded,
                                        Throwable failure) {
                                    CopiedUpdateObservation.instrumentationFailed(
                                            name, failure.getClass().getName());
                                }
                            })
                    .type(namedOneOf(targets))
                    .transform(
                            (builder, type, loader, module, domain) ->
                                    builder.visit(
                                            Advice.to(ConstructorCopy.class)
                                                    .on(isConstructor().and(takesArguments(1)))))
                    .installOn(instrumentation);
            CopiedUpdateObservation.instrumented(
                    contracts,
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (Exception | LinkageError failure) {
            CopiedUpdateObservation.instrumentationFailed("agent", failure.getClass().getName());
        }
    }

    public static class ConstructorCopy {
        @Advice.OnMethodExit
        public static void exit(
                @Advice.This Object target, @Advice.AllArguments Object[] arguments) {
            CopiedUpdateObservation.copied(target, arguments);
        }
    }
}
