package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.EventUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EnableDisableEventsController;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionRepository;

public final class Oracle {
    private enum DbBackend {
        H2, POSTGRES
    }

    /**
     * Default seed for the ReadySet pick in {@code ScheduleExecutor}. A fixed seed
     * keeps a given test case reproducible across runs; {@link #setSchedulerSeed}
     * lets a test driver vary it to explore different concrete schedule
     * realizations for the same inter-dependencies set.
     */
    private static final long DEFAULT_SCHEDULER_SEED = 42L;

    /** Bean name for the simulator's base aggregate repository. */
    private static final String AGGREGATE_REPOSITORY_BEAN_NAME = "aggregateRepository";

    private static final String DB_NAME_PREFIX = "oracledb_";
    private static final String DB_IMAGE = "postgres:15-alpine";
    private static final String DB_USERNAME = "oracle";
    private static final String DB_PASSWORD = "postgres";
    private static final String ORACLE_DB_PROPERTY = "oracle.db";

    /** Exact Spring property names checked to enforce isolation. */
    private static final Set<String> ISOLATION_CONTROLLED_PROPERTIES = Set.of(
            "spring.main.web-application-type",
            "spring.profiles.active",
            "spring.jpa.hibernate.ddl-auto",
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.datasource.driver-class-name",
            "spring.jpa.properties.hibernate.dialect",
            "server.port",
            "server.address",
            "grpc.server.port");

    /** Property-name prefixes checked to enforce isolation for external services and server settings with varying suffixes. */
    private static final List<String> ISOLATION_CONTROLLED_PROPERTY_PREFIXES = List.of(
            "spring.cloud.", "spring.rabbitmq.", "eureka.", "grpc.", "server.");

    /**
     * Isolated campaigns always use an in-memory H2 database owned by their
     * Oracle instance. A non-isolated fidelity pass may opt into a tool-owned
     * PostgreSQL container with {@code -Doracle.db=postgres}.
     * <p>
     * The oracle executes each schedule strictly sequentially on a single thread
     * ({@code ScheduleExecutor}), with the background event scheduler stopped.
     * Transactions
     * therefore never overlap in wall-clock time. The consistency guarantees
     * under test are
     * enforced at the application layer (semantic locks +
     * {@code CentralizedVersionService}
     * version checks), not by the database isolation level. H2 is thus
     * accuracy-equivalent here while avoiding external resource dependencies.
     */
    private final @Nullable PostgreSQLContainer<?> postgres;
    private final Class<?> springAppClass;
    private final List<String> springAppBaseArgs;
    private final IsolationMode isolationMode;
    private final String databaseName;

    private @Nullable String[] springAppArgs;
    private @Nullable ConfigurableApplicationContext springContext;
    private long schedulerSeed = DEFAULT_SCHEDULER_SEED;
    private List<Integer> schedulerChoicePrefix = List.of();
    private Set<SemanticLockId> ignoredSemanticLocks = Set.of();

    private @Nullable Set<EventHandling> eventHandlings;
    private @Nullable DeferredEventApplicationService defEventAppService;
    private @Nullable TracingSagaUnitOfWorkService tracingUowService;
    private @Nullable InterInvariantsProvider interInvariantProvider;

    public Oracle(Class<?> springAppClass, List<String> springAppBaseArgs) {
        this.isolationMode = IsolationMode.fromSystemProperty();
        validateIsolationArguments(isolationMode, springAppBaseArgs);

        DbBackend backend = databaseBackend(isolationMode);
        this.databaseName = DB_NAME_PREFIX + UUID.randomUUID().toString().replace("-", "");
        this.postgres = backend == DbBackend.POSTGRES
                ? new PostgreSQLContainer<>(DB_IMAGE)
                        .withDatabaseName(databaseName)
                        .withUsername(DB_USERNAME)
                        .withPassword(DB_PASSWORD)
                : null;

        this.springAppClass = springAppClass;
        this.springAppBaseArgs = List.copyOf(springAppBaseArgs);
    }

    private static DbBackend databaseBackend(IsolationMode isolationMode) {
        String configuredBackend = System.getProperty(ORACLE_DB_PROPERTY, "h2");
        if (isolationMode == IsolationMode.REQUIRED) {
            if (!"h2".equalsIgnoreCase(configuredBackend)) {
                throw incompatibleWithIsolation("system property '" + ORACLE_DB_PROPERTY + "="
                        + configuredBackend + "'");
            }
            return DbBackend.H2;
        }
        return "postgres".equalsIgnoreCase(configuredBackend) ? DbBackend.POSTGRES : DbBackend.H2;
    }

    private static void validateIsolationArguments(IsolationMode isolationMode, List<String> springAppBaseArgs) {
        if (isolationMode == IsolationMode.UNSUPPORTED) {
            return;
        }

        // Check explicit JVM properties and Spring arguments against the exact names and prefixes below; reject settings that select shared services or listening servers.
        for (String property : ISOLATION_CONTROLLED_PROPERTIES) {
            String configuredValue = System.getProperty(property);
            if (configuredValue != null && !isRequiredIsolationValue(property, configuredValue)) {
                throw incompatibleWithIsolation("system property '" + property + "=" + configuredValue + "'");
            }
        }
        // Check JVM properties whose names begin with one of the controlled prefixes.
        for (String prefix : ISOLATION_CONTROLLED_PROPERTY_PREFIXES) {
            System.getProperties().stringPropertyNames().stream()
                    .filter(property -> property.startsWith(prefix))
                    .filter(property -> !isRequiredIsolationValue(property, System.getProperty(property)))
                    .findFirst()
                    .ifPresent(property -> {
                        throw incompatibleWithIsolation(
                                "system property '" + property + "=" + System.getProperty(property) + "'");
                    });
        }

        // Apply the same checks to command-line arguments, which can override defaults.
        for (String argument : springAppBaseArgs) {
            String property = argumentProperty(argument);
            if (property != null && isIsolationControlledProperty(property)
                    && !isRequiredIsolationValue(property, argumentValue(argument))) {
                throw incompatibleWithIsolation("Spring argument '" + argument + "'");
            }
        }
        
        // TODO Could also be relevant to check environment variables and application config files.
    }

    private static boolean isIsolationControlledProperty(String property) {
        return ISOLATION_CONTROLLED_PROPERTIES.contains(property)
                || ISOLATION_CONTROLLED_PROPERTY_PREFIXES.stream().anyMatch(property::startsWith);
    }

    /** Returns true only for explicitly allowed values; all other values are rejected. */
    private static boolean isRequiredIsolationValue(String property, @Nullable String value) {
        return switch (property) {
            case "spring.main.web-application-type" -> "none".equalsIgnoreCase(value);
            case "spring.profiles.active" -> "sagas,local,test,oracle".equals(value);
            case "spring.jpa.hibernate.ddl-auto" -> "create-drop".equalsIgnoreCase(value);
            // Disables an optional RSocket endpoint; it cannot make the Oracle
            // reachable and is required by the existing test JVM configuration.
            case "spring.cloud.function.rsocket.enabled" -> "false".equalsIgnoreCase(value);
            default -> false;
        };
    }

    private static IllegalArgumentException incompatibleWithIsolation(String conflictingSetting) {
        return new IllegalArgumentException(
                "Consistency isolation requires tool-owned H2, local Oracle profiles, and no listening server; "
                        + conflictingSetting + " is incompatible. Remove it or rerun with -D"
                        + IsolationMode.PROPERTY + "=unsupported.");
    }

    /** Extracts the property name from {@code --name=value}; returns null for non-option arguments. */
    private static @Nullable String argumentProperty(String argument) {
        if (!argument.startsWith("--")) {
            return null;
        }
        int assignment = argument.indexOf('=');
        return assignment < 0 ? argument.substring(2) : argument.substring(2, assignment);
    }

    /** Extracts the value from a Spring {@code --name=value} argument, returns null when absent. */
    private static @Nullable String argumentValue(String argument) {
        int assignment = argument.indexOf('=');
        return assignment < 0 ? null : argument.substring(assignment + 1);
    }

    private String[] generateFinalSpringArgs() {
        if (isolationMode == IsolationMode.UNSUPPORTED) {
            if (postgres != null) {
                return mergeArgsWithPriority(datasourceArgs(), springAppBaseArgs).toArray(new String[0]);
            }
            return springAppBaseArgs.toArray(new String[0]);
        }

        List<String> springPriorityArgs = new ArrayList<>(List.of(
                "--spring.main.allow-bean-definition-overriding=true",
                "--spring.profiles.active=sagas,local,test,oracle",
                "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--spring.main.web-application-type=none"));

        springPriorityArgs.addAll(datasourceArgs());

        List<String> finalArgsList = mergeArgsWithPriority(springPriorityArgs, springAppBaseArgs);
        return finalArgsList.toArray(new String[0]);
    }

    private List<String> datasourceArgs() {
        if (postgres != null) {
            // PostgreSQL testcontainer
            return List.of(
                    "--spring.datasource.url=" + postgres.getJdbcUrl(),
                    "--spring.datasource.username=" + postgres.getUsername(),
                    "--spring.datasource.password=" + postgres.getPassword(),
                    "--spring.datasource.driver-class-name=" + postgres.getDriverClassName(),
                    "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect");
        } else {
            // In-memory H2
            return List.of(
                    "--spring.datasource.url=jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1",
                    "--spring.datasource.username=sa",
                    "--spring.datasource.password=sa",
                    "--spring.datasource.driver-class-name=org.h2.Driver",
                    "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect");
        }
    }

    private static <T> void overrideSpringBean(
            Class<T> originalBeanClass,
            Class<? extends T> replacementBeanClass,
            ConfigurableApplicationContext context) {

        String beanName = Introspector.decapitalize(originalBeanClass.getSimpleName());
        var registry = (BeanDefinitionRegistry) context.getBeanFactory();
        var definition = new RootBeanDefinition(replacementBeanClass);
        definition.setPrimary(true);
        registry.registerBeanDefinition(beanName, definition);
    }

    public void init() {
        if (springContext != null && springContext.isActive()) {
            throw new IllegalStateException("Spring application is already running.");
        }

        if (postgres != null) {
            postgres.start();
        }

        SpringApplication app = new SpringApplication(springAppClass);
        springAppArgs = generateFinalSpringArgs();

        app.addInitializers(ctx -> {
            overrideSpringBean(EventApplicationService.class, DeferredEventApplicationService.class, ctx);
            overrideSpringBean(SagaUnitOfWorkService.class, TracingSagaUnitOfWorkService.class, ctx);
        });

        springContext = app.run(springAppArgs);

        getBean(TracingSagaUnitOfWorkService.class).configureIgnoredSemanticLocks(ignoredSemanticLocks);

        // stop periodic events scheduling handlers, to favor
        // DeferredEventApplicationService more determinisitc testing capabilities
        var eventsSchedulerController = getBean(EnableDisableEventsController.class);
        eventsSchedulerController.stopSchedule();

        // Fail fast at startup if a required bean is missing or of the wrong type.
        getRequiredBeans();
    }

    public void shutdown() {
        if (springContext != null) {
            springContext.close();
            springContext = null;
        }

        if (postgres != null) {
            postgres.stop();
        }

    }

    List<String> effectiveSpringAppArgs() {
        if (springAppArgs == null) {
            // Effective arguments exist only after init has constructed them.
            throw new IllegalStateException("Oracle has not started.");
        }
        return List.of(springAppArgs);
    }

    public void restart() {
        shutdown();
        init();
    }

    private void clearDatabase() {
        AggregateIdRepository aggregateIdRepository = getBean(AggregateIdRepository.class);

        // Select the simulator repository by name to avoid ambiguity,
        // since application repositories may also extend AggregateRepository.
        AggregateRepository aggrRepository = getBean(AGGREGATE_REPOSITORY_BEAN_NAME, AggregateRepository.class);

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
                    "Cannot fetch bean [%s] : Context is inactive.".formatted(beanClass.getName()));
        }
        return context.getBean(beanClass);
    }

    /**
     * Use a name to be unambiguous when multiple application beans share the
     * requested type.
     */
    private <T> T getBean(String beanName, Class<T> beanClass) {
        ConfigurableApplicationContext context = springContext;
        if (context == null || !context.isActive()) {
            throw new IllegalStateException(
                    "Cannot fetch bean [%s] : Context is inactive.".formatted(beanName));
        }
        return context.getBean(beanName, beanClass);
    }

    public <T> Map<String, T> getBeansOfType(Class<T> beansClass) {
        ConfigurableApplicationContext context = springContext;
        if (context == null || !context.isActive()) {
            throw new IllegalStateException(
                    "Cannot fetch beans of type [%s] : Context is inactive.".formatted(beansClass.getName()));
        }
        return context.getBeansOfType(beansClass);
    }

    private void getRequiredBeans() {
        ConfigurableApplicationContext context = Objects.requireNonNull(springContext);
        EventUtils.validateEventHandlingRegistration(
                Arrays.stream(context.getBeanDefinitionNames())
                        .map(context::getType).filter(Objects::nonNull).toList());

        EventApplicationService eventAppService = getBean(EventApplicationService.class);
        if (!(eventAppService instanceof DeferredEventApplicationService defEventAppService)) {
            throw new IllegalStateException(
                    "Bean for [%s] must be an instance of [%s], but got: [%s]"
                            .formatted(EventApplicationService.class.getName(),
                                    DeferredEventApplicationService.class.getName(),
                                    eventAppService.getClass().getName()));
        }
        this.defEventAppService = defEventAppService;

        var uowService = getBean(SagaUnitOfWorkService.class);
        if (!(uowService instanceof TracingSagaUnitOfWorkService tracingUowService)) {
            throw new IllegalStateException(
                    "Bean for [%s] must be an instance of [%s], but got: [%s]"
                            .formatted(SagaUnitOfWorkService.class.getName(),
                                    TracingSagaUnitOfWorkService.class.getName(),
                                    uowService.getClass().getName()));
        }
        this.tracingUowService = tracingUowService;

        Map<String, EventHandling> eventHandlingBeans = getBeansOfType(EventHandling.class);
        eventHandlings = Set.copyOf(eventHandlingBeans.values());

        try {
            interInvariantProvider = getBean(InterInvariantsProvider.class);
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalStateException("""
                    SpringBoot application [%s] is missing bean implementation for [%s].
                    Please add a component in the application's 'src/test/java/...' matching this blueprint:

                    @Component
                    @Profile("oracle")
                    public class TestInterInvariantsProvider implements InterInvariantsProvider { ... }
                    """.formatted(springAppClass.getName(), InterInvariantsProvider.class.getName()));
        }
    }

    private TestResult executeSchedule(
            Map<FunctionalityId, WorkflowFunctionality> functionalities,
            StepDependencies interDependencies) {

        // Re-resolve the required beans for THIS run instead of trusting the values
        // cached at init(). Useful for external "bean overriding", e.g., for tests.
        getRequiredBeans();

        try (DeferredEventApplicationService.CaptureSession captureSession = defEventAppService.beginCapture();
                TracingSagaUnitOfWorkService.TraceSession traceSession = tracingUowService.beginTrace()) {

            ScheduleExecutor scheduleExecutor = new ScheduleExecutor(
                    functionalities,
                    interInvariantProvider.getInterInvariants(),
                    interDependencies,
                    tracingUowService,
                    traceSession,
                    captureSession,
                    eventHandlings,
                    schedulerSeed,
                    schedulerChoicePrefix);

            return scheduleExecutor.execute();
        }
    }

    public Oracle setSchedulerSeed(long schedulerSeed) {
        this.schedulerSeed = schedulerSeed;
        return this;
    }

    /**
     * Replays scheduler choice positions while available, then resumes seeded
     * random scheduling. Positions are mapped into each run's current ready set.
     */
    public Oracle setSchedulerChoicePrefix(List<Integer> schedulerChoicePrefix) {
        this.schedulerChoicePrefix = List.copyOf(schedulerChoicePrefix);
        return this;
    }

    /**
     * Drains all scheduled event deliveries until no registered handler can observe
     * more work.
     */
    public void establishQuiescentState() {
        getRequiredBeans(); // Re-resolve and validate required beans before draining setup effects.
        BaselineEventDrainer.drainToQuiescence(eventHandlings, defEventAppService);
    }

    /**
     * Configures semantic-lock acquisitions that the oracle must skip.
     * May be changed between schedules; changing it while a schedule runs is
     * unsupported and not guarded here (in-flight schedule could observe either the
     * old or new configuration).
     */
    public Oracle setIgnoredSemanticLocks(Set<SemanticLockId> ignoredSemanticLocks) {
        this.ignoredSemanticLocks = Set.copyOf(ignoredSemanticLocks);

        ConfigurableApplicationContext context = springContext;
        if (context != null && context.isActive()) {
            getBean(TracingSagaUnitOfWorkService.class).configureIgnoredSemanticLocks(this.ignoredSemanticLocks);
        }
        return this;
    }

    public TestResult runTest(Supplier<TestCase> setupInitialState) {
        Consumer<TestResult> noBeforeCleanupHook = result -> {
            // do nothing
        };
        return runTest(setupInitialState, noBeforeCleanupHook);
    }

    public TestResult runTest(Supplier<TestCase> setupInitialState, Consumer<TestResult> beforeCleanupHook) {
        try {
            TestCase testCase = setupInitialState.get();
            configureDeterministicWorkflowPlans(testCase.getFunctionalities());

            TestResult result = executeSchedule(
                    testCase.getFunctionalities(),
                    testCase.getInterDependencies());

            beforeCleanupHook.accept(result);
            return result;
        } finally {
            clearDatabase();
        }
    }

    /** Oracle schedules workflow steps itself; keep dependency-equivalent plan ties stable. */
    private static void configureDeterministicWorkflowPlans(
            Map<FunctionalityId, WorkflowFunctionality> functionalities) {

        for (WorkflowFunctionality functionality : functionalities.values()) {
            if (functionality.getWorkflow() instanceof SagaWorkflow sagaWorkflow) {
                sagaWorkflow.setDeterministicPlanOrder(true);
            }
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
