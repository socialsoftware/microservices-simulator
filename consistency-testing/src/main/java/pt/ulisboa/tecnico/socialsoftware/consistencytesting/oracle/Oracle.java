package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionRepository;

public class Oracle {
    private static final String DB_IMAGE = "postgres:15-alpine";
    private static final String DB_NAME = "oracledb";
    private static final String DB_USERNAME = "oracle";
    private static final String DB_PASSWORD = "postgres";

    private final PostgreSQLContainer<?> postgres; // Spins up a throwaway PostgreSQL testcontainer
    private final Class<?> springAppClass;
    private final List<String> springAppBaseArgs;

    private @Nullable String[] springAppArgs;
    private @Nullable ConfigurableApplicationContext springContext;

    public Oracle(Class<?> springAppClass, List<String> springAppBaseArgs) {
        postgres = new PostgreSQLContainer<>(DB_IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD);

        this.springAppClass = springAppClass;
        this.springAppBaseArgs = List.copyOf(springAppBaseArgs);
    }

    private static String[] generateFinalSpringArgs(
            PostgreSQLContainer<?> postgres,
            List<String> springAppBaseArgs) {
        List<String> springPriorityArgs = List.of(
                "--spring.profiles.active=sagas,local,test",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--spring.datasource.driver-class-name=" + postgres.getDriverClassName(),
                "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
                "--spring.jpa.hibernate.ddl-auto=create-drop");

        List<String> finalArgsList = mergeArgsWithPriority(springPriorityArgs, springAppBaseArgs);
        return finalArgsList.toArray(new String[0]);
    }

    public void init() {
        postgres.start();

        if (springContext != null && springContext.isActive()) {
            throw new IllegalStateException("Spring application is already running.");
        }

        springAppArgs = generateFinalSpringArgs(postgres, springAppBaseArgs);
        springContext = SpringApplication.run(springAppClass, springAppArgs);
    }

    public void shutdown() {
        if (springContext != null) {
            springContext.close();
            springContext = null;
        }

        postgres.stop();
    }

    public void restart() {
        shutdown();
        init();
    }

    private void clearDatabase() {
        AggregateIdRepository aggregateIdRepository = getBean(AggregateIdRepository.class);
        AggregateRepository aggrRepository = getBean(AggregateRepository.class);
        EventRepository eventRepository = getBean(EventRepository.class);

        SagaAggregateRepository sagaAggregateRepository = getBean(SagaAggregateRepository.class);

        VersionRepository versionRepository = getBean(VersionRepository.class);

        aggregateIdRepository.deleteAll();
        aggrRepository.deleteAll();
        eventRepository.deleteAll();
        sagaAggregateRepository.deleteAll();
        versionRepository.deleteAll();
    }

    public <T> T getBean(Class<T> beanClass) {
        ConfigurableApplicationContext context = springContext;
        if (context == null || !context.isActive()) {
            throw new IllegalStateException(
                    "Cannot fetch bean %s : Context is inactive.".formatted(beanClass.getName()));
        }
        return context.getBean(beanClass);
    }

    private TestResult executeSchedule(
            Map<FunctionalityId, WorkflowFunctionality> functionalities,
            StepDependencies interDependencies) {

        var uowService = getBean(SagaUnitOfWorkService.class);
        ScheduleExecutor scheduleExecutor = new ScheduleExecutor(functionalities, interDependencies, uowService);
        return scheduleExecutor.execute();
    }

    public TestResult runTest(Supplier<TestCase> setupInitialState) {
        return runTest(setupInitialState, result -> {
            // No-op beforeCleanupHook by default
        });
    }

    public TestResult runTest(Supplier<TestCase> setupInitialState, Consumer<TestResult> beforeCleanupHook) {
        try {
            TestCase testCase = setupInitialState.get();

            TestResult result = executeSchedule(
                    testCase.getFunctionalities(),
                    testCase.getInterDependencies());

            beforeCleanupHook.accept(result);
            return result;
        } finally {
            clearDatabase();
        }
    }

    /**
     * Merges two lists of arguments, giving precedence to high-priority arguments.
     * <p>
     * If a low-priority argument shares the same property key as a high-priority
     * argument,
     * the low-priority argument is omitted from the final list. The resulting list
     * preserves the order, placing all high-priority arguments first, followed by
     * the non-conflicting low-priority arguments.
     *
     * @param highPriorityArgs the list of high-priority arguments.
     * @param lowPriorityArgs  the list of low-priority arguments.
     * @return A new list containing the merged arguments.
     */
    private static List<String> mergeArgsWithPriority(List<String> highPriorityArgs, List<String> lowPriorityArgs) {
        // Regex to capture the property of the argument
        // (e.g., "--spring.profiles.active=sagas,local,test"),
        // returns the string between "--" and "=" (or end of string)
        Pattern argPropertyPattern = Pattern.compile("^--([^=]+)");

        Function<String, String> getArgProperty = arg -> {
            Matcher matcher = argPropertyPattern.matcher(arg);
            return matcher.find() ? matcher.group(1) : null;
        };

        Set<String> highPriorityProperties = highPriorityArgs.stream()
                .map(getArgProperty)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Function<String, Boolean> argDoesNotOverwriteHighPriorityArg = arg -> {
            return !highPriorityProperties.contains(getArgProperty.apply(arg));
        };

        List<String> nonConflitctingLowPriorityArgs = lowPriorityArgs.stream()
                .filter(argDoesNotOverwriteHighPriorityArg::apply)
                .toList();

        return Stream.concat(highPriorityArgs.stream(), nonConflitctingLowPriorityArgs.stream()).toList();
    }
}