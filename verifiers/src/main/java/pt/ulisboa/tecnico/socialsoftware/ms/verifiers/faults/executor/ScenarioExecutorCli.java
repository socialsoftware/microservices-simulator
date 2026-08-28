package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator;

import java.nio.file.Path;
import java.util.List;
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
        ScenarioSetupPreflightOptions preflightOptions = enabled(options, "preflight")
                ? preflightOptions(options, springProfiles) : null;
        if (enabled(options, "preflight") && !enabled(options, "preflight-worker")) {
            ScenarioExecutor.PreflightPlan plan = new ScenarioExecutor().preflightPlan(preflightOptions);
            if (!plan.sourceSetupWorkloadIds().isEmpty()) {
                int status = new ScenarioSetupPreflightProcessOrchestrator().run(args, preflightOptions, plan);
                System.exit(status);
                return;
            }
        }

        Class<?> applicationClass = Class.forName(options.get("spring-application-class"));
        int exitCode;
        try (EventReplayCoordinator.Activation replayMode = activateReplayMode();
             ConfigurableApplicationContext context = SpringApplication.run(applicationClass, args)) {
            ScenarioRuntimeContext runtimeContext = new SpringScenarioRuntimeContext(context);
            ScenarioExecutor executor = new ScenarioExecutor();
            if (enabled(options, "preflight")) {
                ScenarioSetupPreflightReport report = enabled(options, "preflight-worker")
                        ? executor.preflightIsolatedAttempt(preflightOptions, runtimeContext,
                        Set.copyOf(List.of(options.get("preflight-workload-ids").split(","))))
                        : executor.preflight(preflightOptions, runtimeContext);
                System.out.println("Scenario setup preflight candidates=" + report.candidateCount()
                        + " participants=" + report.participantCount()
                        + " status=" + report.terminalStatus());
                report.workloads().forEach(workload -> {
                    System.out.println("workload " + workload.workloadPlanId() + " " + workload.status());
                    workload.participants().forEach(participant -> System.out.println(
                            "participant " + participant.sagaInstanceId() + " "
                                    + (participant.setupReady() ? "SETUP_READY" : "SETUP_FAILED")
                                    + " saga=" + participant.sagaFqn()));
                    printSourceSetup(workload.sourceSetup());
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
                        options.get("maven-profile"),
                        options.containsKey("impact-output-path")
                                ? Path.of(options.get("impact-output-path"))
                                : null);
                ScenarioExecutionReport report = executor.execute(executorOptions, runtimeContext);
                System.out.println("Scenario executor selected " + report.faultScenarioId()
                        + " status=" + report.terminalStatus()
                        + " conformance=" + report.scheduleConformance());
                printSourceSetup(report.sourceSetup());
                report.actualActions().forEach(action -> System.out.println(
                        "action " + action.actualPosition() + " " + action.kind() + " " + action.actionId()
                                + " " + action.status()));
                exitCode = exitCodeFor(report.terminalStatus());
            }
        }
        System.exit(exitCode);
    }

    private static ScenarioSetupPreflightOptions preflightOptions(
            Map<String, String> options, String springProfiles) {
        return new ScenarioSetupPreflightOptions(
                Path.of(options.get("package-path")),
                Path.of(options.get("output-path")),
                options.get("application-base"),
                options.get("application-id"),
                options.get("spring-application-class"),
                springProfiles,
                options.get("maven-profile"));
    }

    private static void printSourceSetup(ScenarioExecutionReport.SourceSetup setup) {
        if (setup == null) return;
        System.out.println("source setup status=" + setup.status()
                + " pendingEventsCleared=" + setup.pendingEventsCleared());
        setup.actions().forEach(action -> System.out.println(
                "setup action " + action.orderIndex() + " " + action.actionId()
                        + " " + action.status()
                        + (action.retainedResultId() == null ? "" : " result=" + action.retainedResultId())
                        + (action.aggregateId() == null ? "" : " aggregateId=" + action.aggregateId())));
        setup.participantBindings().forEach(binding -> System.out.println(
                "setup binding " + binding.inputVariantId() + "#" + binding.argumentIndex()
                        + " <- " + binding.sourceActionId()
                        + (binding.propertyName() == null ? "" : "." + binding.propertyName())
                        + " result=" + binding.retainedResultId()
                        + " value=" + binding.resolvedValue()));
    }

    static EventReplayCoordinator.Activation activateReplayMode() {
        System.setProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY, "true");
        return EventReplayCoordinator.activate();
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
        validateBooleanOption(options, "preflight-worker");
        validatePathOption(options, "impact-output-path");
        boolean preflight = enabled(options, "preflight");
        if (preflight) {
            if (options.containsKey("fault-scenario-id")) {
                throw new IllegalArgumentException("--preflight checks manifest-declared workload candidates; "
                        + "do not pass --fault-scenario-id");
            }
            if (enabled(options, "dry-run")) {
                throw new IllegalArgumentException("--dry-run is an execution mode and cannot be combined with --preflight");
            }
            if (options.containsKey("impact-output-path")) {
                throw new IllegalArgumentException("--impact-output-path is an execution output and cannot be combined with --preflight");
            }
            if (enabled(options, "preflight-worker")) {
                require(options, "preflight-workload-ids");
            } else if (options.containsKey("preflight-workload-ids")) {
                throw new IllegalArgumentException("--preflight-workload-ids is internal to isolated preflight workers");
            }
        } else {
            require(options, "fault-scenario-id");
            if (enabled(options, "preflight-worker") || options.containsKey("preflight-workload-ids")) {
                throw new IllegalArgumentException("isolated preflight worker options require --preflight");
            }
        }
        Set<String> supportedExecutorOptions = Set.of(
                "spring-application-class", "spring-profiles", "application-base", "application-id",
                "maven-profile", "package-path", "fault-scenario-id", "output-path", "impact-output-path",
                "dry-run", "preflight", "preflight-worker", "preflight-workload-ids");
        options.keySet().stream()
                .filter(key -> !supportedExecutorOptions.contains(key) && !key.contains("."))
                .findFirst()
                .ifPresent(key -> {
                    throw new IllegalArgumentException("Unsupported executor option --" + key
                            + "; execute one persisted --fault-scenario-id");
                });
    }

    private static void validatePathOption(Map<String, String> options, String key) {
        String value = options.get(key);
        if (options.containsKey(key) && (value == null || value.isBlank() || "true".equals(value))) {
            throw new IllegalArgumentException("--" + key + " requires an explicit path value");
        }
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

        @Override
        public <T> List<T> beans(Class<T> type) {
            return List.copyOf(context.getBeansOfType(type).values());
        }
    }
}
