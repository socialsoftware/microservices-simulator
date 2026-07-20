package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ScenarioExecutorCli {
    public static void main(String[] args) throws Exception {
        Map<String, String> options = parse(args);
        validateInvocation(options);
        String springProfiles = options.getOrDefault("spring-profiles", "");
        if (!springProfiles.isBlank()) {
            System.setProperty("spring.profiles.active", springProfiles);
        }
        Class<?> applicationClass = Class.forName(options.get("spring-application-class"));
        try (ConfigurableApplicationContext context = SpringApplication.run(applicationClass, args)) {
            ScenarioRuntimeContext runtimeContext = new SpringScenarioRuntimeContext(context);
            ScenarioExecutorOptions executorOptions = new ScenarioExecutorOptions(
                    Path.of(options.get("package-path")),
                    Path.of(options.get("output-path")),
                    options.get("fault-scenario-id"),
                    Boolean.parseBoolean(options.getOrDefault("dry-run", "false")),
                    options.get("application-base"),
                    options.get("application-id"),
                    options.get("spring-application-class"),
                    springProfiles,
                    options.get("maven-profile"));
            ScenarioExecutionReport report = new ScenarioExecutor().execute(executorOptions, runtimeContext);
            System.out.println("Scenario executor selected " + report.faultScenarioId()
                    + " status=" + report.terminalStatus()
                    + " conformance=" + report.scheduleConformance());
            report.actualActions().forEach(action -> System.out.println(
                    "action " + action.actualPosition() + " " + action.kind() + " " + action.actionId()
                            + " " + action.status()));
            System.exit(exitCodeFor(report.terminalStatus()));
        }
    }

    static int exitCodeFor(String terminalStatus) {
        return switch (terminalStatus == null ? "" : terminalStatus) {
            case "SUCCESS", "COMPENSATED", "PARTIAL_COMPENSATED", "DRY_RUN" -> 0;
            default -> 1;
        };
    }

    static Map<String, String> parse(String[] args) {
        Map<String, String> parsed = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--")) continue;
            String key = args[i].substring(2);
            String value = i + 1 < args.length && !args[i + 1].startsWith("--") ? args[++i] : "true";
            parsed.put(key, value);
        }
        return parsed;
    }

    static void validateInvocation(Map<String, String> options) {
        require(options, "spring-application-class");
        require(options, "package-path");
        require(options, "fault-scenario-id");
        require(options, "output-path");
        Set<String> supportedExecutorOptions = Set.of(
                "spring-application-class", "spring-profiles", "application-base", "application-id",
                "maven-profile", "package-path", "fault-scenario-id", "output-path", "dry-run");
        options.keySet().stream()
                .filter(key -> !supportedExecutorOptions.contains(key) && !key.contains("."))
                .findFirst()
                .ifPresent(key -> {
                    throw new IllegalArgumentException("Unsupported executor option --" + key
                            + "; execute one persisted --fault-scenario-id");
                });
    }

    private static void require(Map<String, String> options, String key) {
        if (!options.containsKey(key) || options.get(key).isBlank()) {
            throw new IllegalArgumentException("Missing required --" + key);
        }
    }

    private record SpringScenarioRuntimeContext(ConfigurableApplicationContext context) implements ScenarioRuntimeContext {
        @Override
        public Object bean(Class<?> type) {
            return context.getBean(type);
        }
    }
}
