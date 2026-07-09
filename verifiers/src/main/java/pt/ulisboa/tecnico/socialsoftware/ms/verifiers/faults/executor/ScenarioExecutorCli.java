package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.util.*;

public class ScenarioExecutorCli {
    public static void main(String[] args) throws Exception {
        Map<String, String> options = parse(args);
        require(options, "spring-application-class");
        require(options, "catalog-path");
        require(options, "output-path");
        String springProfiles = options.getOrDefault("spring-profiles", "");
        if (!springProfiles.isBlank()) {
            System.setProperty("spring.profiles.active", springProfiles);
        }
        Class<?> applicationClass = Class.forName(options.get("spring-application-class"));
        try (ConfigurableApplicationContext context = SpringApplication.run(applicationClass, args)) {
            ScenarioRuntimeContext runtimeContext = new SpringScenarioRuntimeContext(context);
            ScenarioExecutorOptions executorOptions = new ScenarioExecutorOptions(null,
                    Path.of(options.get("catalog-path")),
                    Path.of(options.get("output-path")),
                    options.get("scenario-id"),
                    options.get("fault-vector"),
                    Boolean.parseBoolean(options.getOrDefault("dry-run", "false")),
                    options.get("application-base"),
                    options.get("application-id"),
                    options.get("spring-application-class"),
                    springProfiles,
                    options.get("maven-profile"));
            ScenarioExecutionReport report = new ScenarioExecutor().execute(executorOptions, runtimeContext);
            System.out.println("Scenario executor selected " + report.scenarioPlanId() + " status=" + report.terminalStatus());
            report.participants().forEach(participant -> participant.stepOutcomes().forEach(step ->
                    System.out.println("participant " + participant.sagaInstanceId() + " step " + step.scheduleOrder() + " " + step.runtimeStepName() + " " + step.status())));
            System.exit(exitCodeFor(report.terminalStatus()));
        }
    }

    static int exitCodeFor(String terminalStatus) {
        return switch (terminalStatus == null ? "" : terminalStatus) {
            case "SUCCESS", "COMPENSATED", "PARTIAL_COMPENSATED", "DRY_RUN" -> 0;
            default -> 1;
        };
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> parsed = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--")) continue;
            String key = args[i].substring(2);
            String value = i + 1 < args.length && !args[i + 1].startsWith("--") ? args[++i] : "true";
            parsed.put(key, value);
        }
        return parsed;
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
