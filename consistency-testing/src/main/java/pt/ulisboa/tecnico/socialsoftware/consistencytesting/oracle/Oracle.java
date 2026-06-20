package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.beans.Introspector;
import java.util.ArrayList;
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
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EnableDisableEventsController;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionRepository;

public final class Oracle {
    private enum DbBackend {
        H2, POSTGRES
    }

    private static final String DB_IMAGE = "postgres:15-alpine";
    private static final String DB_NAME = "oracledb";
    private static final String DB_USERNAME = "oracle";
    private static final String DB_PASSWORD = "postgres";

    /**
     * Persistence backend for the app-under-test. Defaults to in-memory
     * {@link DbBackend#H2}.
     * <p>
     * The oracle executes each schedule strictly sequentially on a single thread
     * ({@code ScheduleExecutor}), with the background event scheduler stopped.
     * Transactions
     * therefore never overlap in wall-clock time, so PostgreSQL's
     * {@code SERIALIZABLE}
     * conflict-detection is never actually exercised — the consistency guarantees
     * under test are
     * enforced at the application layer (semantic locks +
     * {@code CentralizedVersionService}
     * version checks), not by the database isolation level. H2 is thus
     * accuracy-equivalent here
     * while avoiding per-query Postgres-over-Docker round-trips (~tens of ms each)
     * and container
     * startup.
     * <p>
     * Set {@code -Doracle.db=postgres} to run against the production dialect for a
     * fidelity pass.
     */
    private static final DbBackend DB_BACKEND = "postgres".equalsIgnoreCase(System.getProperty("oracle.db", "h2"))
            ? DbBackend.POSTGRES
            : DbBackend.H2;

    private final @Nullable PostgreSQLContainer<?> postgres; // null unless DB_BACKEND == POSTGRES
    private final Class<?> springAppClass;
    private final List<String> springAppBaseArgs;

    private @Nullable String[] springAppArgs;
    private @Nullable ConfigurableApplicationContext springContext;

    public Oracle(Class<?> springAppClass, List<String> springAppBaseArgs) {
        postgres = DB_BACKEND == DbBackend.POSTGRES
                ? new PostgreSQLContainer<>(DB_IMAGE)
                        .withDatabaseName(DB_NAME)
                        .withUsername(DB_USERNAME)
                        .withPassword(DB_PASSWORD)
                : null;

        this.springAppClass = springAppClass;
        this.springAppBaseArgs = List.copyOf(springAppBaseArgs);
    }

    private static String[] generateFinalSpringArgs(
            @Nullable PostgreSQLContainer<?> postgres,
            List<String> springAppBaseArgs) {

        List<String> springPriorityArgs = new ArrayList<>(List.of(
                "--spring.main.allow-bean-definition-overriding=true",
                "--spring.profiles.active=sagas,local,test",
                "--spring.jpa.hibernate.ddl-auto=create-drop"));

        springPriorityArgs.addAll(datasourceArgs(postgres));

        List<String> finalArgsList = mergeArgsWithPriority(springPriorityArgs, springAppBaseArgs);
        return finalArgsList.toArray(new String[0]);
    }

    private static List<String> datasourceArgs(@Nullable PostgreSQLContainer<?> postgres) {
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
                    "--spring.datasource.url=jdbc:h2:mem:" + DB_NAME + ";DB_CLOSE_DELAY=-1",
                    "--spring.datasource.username=sa",
                    "--spring.datasource.password=sa",
                    "--spring.datasource.driver-class-name=org.h2.Driver",
                    "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect");
        }
    }

    public void init() {
        if (springContext != null && springContext.isActive()) {
            throw new IllegalStateException("Spring application is already running.");
        }

        if (postgres != null) {
            postgres.start();
        }

        SpringApplication app = new SpringApplication(springAppClass);
        springAppArgs = generateFinalSpringArgs(postgres, springAppBaseArgs);

        // override EventApplicationService bean with DeferredEventApplicationService
        app.addInitializers(ctx -> {
            String targetBeanName = Introspector.decapitalize(EventApplicationService.class.getSimpleName());
            var registry = (BeanDefinitionRegistry) ctx.getBeanFactory();
            var def = new RootBeanDefinition(DeferredEventApplicationService.class);

            def.setPrimary(true);
            registry.registerBeanDefinition(targetBeanName, def);
        });

        springContext = app.run(springAppArgs);

        // stop periodic events scheduling handlers, to favor
        // DeferredEventApplicationService more determinisitc testing capabilities
        var eventsSchedulerController = springContext.getBean(EnableDisableEventsController.class);
        eventsSchedulerController.stopSchedule();
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

    <T> T getBean(Class<T> beanClass) {
        ConfigurableApplicationContext context = springContext;
        if (context == null || !context.isActive()) {
            throw new IllegalStateException(
                    "Cannot fetch bean [%s] : Context is inactive.".formatted(beanClass.getName()));
        }
        return context.getBean(beanClass);
    }

    <T> Map<String, T> getBeansOfType(Class<T> beansClass) {
        ConfigurableApplicationContext context = springContext;
        if (context == null || !context.isActive()) {
            throw new IllegalStateException(
                    "Cannot fetch beans of type [%s] : Context is inactive.".formatted(beansClass.getName()));
        }
        return context.getBeansOfType(beansClass);
    }

    private TestResult executeSchedule(
            Map<FunctionalityId, WorkflowFunctionality> functionalities,
            StepDependencies interDependencies) {

        EventApplicationService baseEventAppService = getBean(EventApplicationService.class);
        if (!(baseEventAppService instanceof DeferredEventApplicationService eventAppService)) {
            throw new IllegalStateException(
                    "Bean for [%s] must be an instance of [%s], but got: [%s]"
                            .formatted(EventApplicationService.class.getName(),
                                    DeferredEventApplicationService.class.getName(),
                                    baseEventAppService.getClass().getName()));
        }

        var uowService = getBean(SagaUnitOfWorkService.class);
        Map<String, EventHandling> eventHandlingBeans = getBeansOfType(EventHandling.class);
        List<EventHandling> eventHandlings = new ArrayList<>(eventHandlingBeans.values());

        try (DeferredEventApplicationService.CaptureSession captureSession = eventAppService.beginCapture()) {

            ScheduleExecutor scheduleExecutor = new ScheduleExecutor(
                    functionalities,
                    interDependencies,
                    uowService,
                    captureSession,
                    eventHandlings);

            return scheduleExecutor.execute();
        }
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