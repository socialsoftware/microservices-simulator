package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;

public class Oracle {
    private final Class<?> springAppClass;
    private final String[] springAppArgs;
    private @Nullable ConfigurableApplicationContext springContext;

    public Oracle(Class<?> springAppClass, String[] springAppArgs) {
        this.springAppClass = springAppClass;
        this.springAppArgs = Arrays.copyOf(springAppArgs, springAppArgs.length);
    }

    public void init() {
        if (springContext != null && springContext.isActive()) {
            throw new IllegalStateException("Spring application is already running.");
        }
        springContext = SpringApplication.run(springAppClass, springAppArgs);
    }

    public void shutdown() {
        if (springContext != null) {
            springContext.close();
            springContext = null;
        }
    }

    public void restart() {
        shutdown();
        init();
    }

    public <T> T getBean(Class<T> beanClass) {
        ConfigurableApplicationContext context = springContext;
        if (context == null || !context.isActive()) {
            throw new IllegalStateException("Cannot fetch bean: Context is inactive.");
        }
        return context.getBean(beanClass);
    }

    private TestResult executeSchedule(
            List<WorkflowFunctionality> functionalities,
            Map<FlowStep, Set<FlowStep>> interDependencies) {

        ScheduleExecutor scheduleExecutor = new ScheduleExecutor(functionalities, interDependencies);
        return scheduleExecutor.execute();
    }

    public TestResult runTest(TestCase testCase) {
        testCase.setup().run();
        try {
            return executeSchedule(testCase.functionalities(), testCase.interDependencies());
        } finally {
            testCase.tearDown().run();
        }
    }
}