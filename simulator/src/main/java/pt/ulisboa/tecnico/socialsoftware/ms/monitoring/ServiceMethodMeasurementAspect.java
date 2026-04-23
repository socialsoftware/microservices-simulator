package pt.ulisboa.tecnico.socialsoftware.ms.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Aspect
@Component
public class ServiceMethodMeasurementAspect {
    private static final String ENABLED = "simulator.service-method-profiling.enabled";
    private static final String OUTPUT_FILE = "simulator.service-method-profiling.output-file";
    private static final String DEFAULT_OUTPUT_FILE = "/src/test/resources/service-method-profiling.txt";
    private static final ThreadLocal<Integer> nestingDepth = ThreadLocal.withInitial(() -> 0);
    private static final Object write_lock = new Object();

    private final HardwareCounterService hardwareCounterService;
    private final Environment environment;

    public ServiceMethodMeasurementAspect(Environment environment) {
        this.environment = environment;
        this.hardwareCounterService = HardwareCounterService.getInstance();
        clearOutputFile();
        appendMeasurementLine(String.format(
                "SERVICE_METHOD_PROFILE status=boot enabled=%s hwAvailable=%s outputFile=%s",
                isProfilingEnabled(),
                hardwareCounterService.isAvailable(),
                resolveOutputPath()));
    }

    private boolean isProfilingEnabled() {
        return environment.getProperty(ENABLED, Boolean.class, false);
    }

    private Path resolveOutputPath() {
        return Path.of(environment.getProperty(OUTPUT_FILE, DEFAULT_OUTPUT_FILE));
    }

    @Around("execution(public * pt.ulisboa.tecnico.socialsoftware.quizzes.microservices..service.*.*(..))")
    public Object profileServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!isProfilingEnabled()) {
            return joinPoint.proceed();
        }

        int depth = nestingDepth.get();
        nestingDepth.set(depth + 1);
        boolean owner = depth == 0;
        long startThreadId = Thread.currentThread().threadId();
        long[] startValues = null;
        if (owner) {
            hardwareCounterService.startThreadCounters();
            startValues = hardwareCounterService.readValues();
        }

        try {
            return joinPoint.proceed();
        } finally {
            nestingDepth.set(depth);
            if (owner) {
                long endThreadId = Thread.currentThread().threadId();
                String serviceClass = joinPoint.getTarget().getClass().getSimpleName();
                String methodName = joinPoint.getSignature().getName();

                if (!hardwareCounterService.isAvailable()) {
                    appendMeasurementLine(String.format(
                            "SERVICE_METHOD_PROFILE status=hardware-unavailable service=%s method=%s",
                            serviceClass, methodName));
                } else if (startValues == null || startValues.length < 2) {
                    appendMeasurementLine(String.format(
                            "SERVICE_METHOD_PROFILE status=unavailable service=%s method=%s",
                            serviceClass, methodName));
                } else if (startThreadId != endThreadId) {
                    appendMeasurementLine(String.format(
                            "SERVICE_METHOD_PROFILE status=invalid-thread-hop service=%s method=%s threadStart=%d threadEnd=%d",
                            serviceClass, methodName, startThreadId, endThreadId));
                } else {
                    long[] endValues = hardwareCounterService.readValues();
                    if (endValues == null || endValues.length < 2) {
                        appendMeasurementLine(String.format(
                                "SERVICE_METHOD_PROFILE status=empty service=%s method=%s",
                                serviceClass, methodName));
                    } else {
                        long instructions = Math.max(0, endValues[0] - startValues[0]);
                        long cycles = Math.max(0, endValues[1] - startValues[1]);
                        double cpi = instructions > 0 ? (double) cycles / instructions : 0.0;

                        appendMeasurementLine(String.format(
                                "service=%s method=%s instructions=%d cycles=%d cpi=%s startThreadId=%d endThreadId=%d",
                                serviceClass, methodName, instructions, cycles, cpi, startThreadId, endThreadId));
                    }
                }
            }
        }
    }

    private void appendMeasurementLine(String line) {
        Path outputPath = resolveOutputPath();
        String content = line + System.lineSeparator();
        synchronized (write_lock) {
            try {
                Path parent = outputPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(outputPath, content,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to write service method profiling metrics to file " + outputPath, e);
            }
        }
    }

    private void clearOutputFile() {
        Path outputPath = resolveOutputPath();
        synchronized (write_lock) {
            try {
                Path parent = outputPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(outputPath, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to clear service method profiling file " + outputPath, e);
            }
        }
    }
}
