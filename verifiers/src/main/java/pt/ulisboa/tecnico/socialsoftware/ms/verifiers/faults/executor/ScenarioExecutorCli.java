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
        int exitCode;
        try (ConfigurableApplicationContext context = SpringApplication.run(applicationClass, args)) {
            ScenarioRuntimeContext runtimeContext = new SpringScenarioRuntimeContext(context);
            ScenarioExecutor executor = new ScenarioExecutor();
            if (enabled(options, "preflight")) {
                ScenarioSetupPreflightOptions preflightOptions = new ScenarioSetupPreflightOptions(
                        Path.of(options.get("package-path")),
                        Path.of(options.get("output-path")),
                        options.get("application-base"),
                        options.get("application-id"),
                        options.get("spring-application-class"),
                        springProfiles,
                        options.get("maven-profile"));
                ScenarioSetupPreflightReport report = executor.preflight(preflightOptions, runtimeContext);
                System.out.println("Scenario setup preflight candidates=" + report.candidateCount()
                        + " participants=" + report.participantCount()
                        + " status=" + report.terminalStatus());
                report.workloads().forEach(workload -> {
                    System.out.println("workload " + workload.workloadPlanId() + " " + workload.status());
                    workload.participants().forEach(participant -> System.out.println(
                            "participant " + participant.sagaInstanceId() + " "
                                    + (participant.setupReady() ? "SETUP_READY" : "SETUP_FAILED")
                                    + " saga=" + participant.sagaFqn()));
                    workload.blockers().forEach(blocker -> System.out.println(
                            "blocker " + blocker.reason() + " " + blocker.message()));
                });
                exitCode = report.successful() ? 0 : 1;
            } else {
                ScenarioExecutorOptions executorOptions = new ScenarioExecutorOptions(
                        Path.of(options.get("package-path")),
                        Path.of(options.get("output-path")),
                        options.get("fault-scenario-id"),
                        enabled(options, "dry-run"),
                        options.get("application-base"),
                        options.get("application-id"),
                        options.get("spring-application-class"),
                        springProfiles,
                        options.get("maven-profile"));
                ScenarioExecutionReport report = executor.execute(executorOptions, runtimeContext);
                System.out.println("Scenario executor selected " + report.faultScenarioId()
                        + " status=" + report.terminalStatus()
                        + " conformance=" + report.scheduleConformance());
                report.actualActions().forEach(action -> System.out.println(
                        "action " + action.actualPosition() + " " + action.kind() + " " + action.actionId()
                                + " " + action.status()));
                exitCode = exitCodeFor(report.terminalStatus());
            }
        }
        System.exit(exitCode);
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
        require(options, "output-path");
        validateBooleanOption(options, "preflight");
        validateBooleanOption(options, "dry-run");
        boolean preflight = enabled(options, "preflight");
        if (preflight) {
            if (options.containsKey("fault-scenario-id")) {
                throw new IllegalArgumentException("--preflight checks manifest-declared workload candidates; "
                        + "do not pass --fault-scenario-id");
            }
            if (enabled(options, "dry-run")) {
                throw new IllegalArgumentException("--dry-run is an execution mode and cannot be combined with --preflight");
            }
        } else {
            require(options, "fault-scenario-id");
        }
        Set<String> supportedExecutorOptions = Set.of(
                "spring-application-class", "spring-profiles", "application-base", "application-id",
                "maven-profile", "package-path", "fault-scenario-id", "output-path", "dry-run", "preflight");
        options.keySet().stream()
                .filter(key -> !supportedExecutorOptions.contains(key) && !key.contains("."))
                .findFirst()
                .ifPresent(key -> {
                    throw new IllegalArgumentException("Unsupported executor option --" + key
                            + "; execute one persisted --fault-scenario-id");
                });
    }

    private static void validateBooleanOption(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value != null && !"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException("--" + key + " must be exactly 'true' or 'false'");
        }
    }

    private static boolean enabled(Map<String, String> options, String key) {
        return "true".equals(options.get(key));
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
