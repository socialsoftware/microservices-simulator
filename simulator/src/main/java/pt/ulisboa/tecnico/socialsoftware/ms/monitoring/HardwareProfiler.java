package pt.ulisboa.tecnico.socialsoftware.ms.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Aspect
@Component
@ConditionalOnProperty(name = "simulator.hardware-profiling.enabled", havingValue = "true", matchIfMissing = false)
public class HardwareProfiler {
    @Value("${simulator.hardware-profiling.output-file:#{null}}")
    private String OUTPUT_FILE;
    private static final String DEFAULT_OUTPUT_FILE = "/src/test/resources/service-method-profiling.txt";

    private final HardwareCounterService hardwareCounterService = HardwareCounterService.getInstance();
    private static final Object write_lock = new Object();
    private static final ThreadLocal<Integer> nestingDepth = ThreadLocal.withInitial(() -> 0);

    public HardwareProfiler() {
    }

    @PostConstruct
    public void init() {
        clearOutputFile();
    }

    private Path resolveOutputPath() {
        if (OUTPUT_FILE != null) {
            return Path.of(OUTPUT_FILE);
        }
        return Path.of(DEFAULT_OUTPUT_FILE);
    }

    @Around("execution(public * pt.ulisboa.tecnico.socialsoftware.quizzes.microservices..service.*.*(..))")
    public Object profileServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        /*
         * Nesting depth is used to prevent counting nested service calls
         * If a service A() invokes A->B() and A->C()
         * B() and C() should not count toward A() #instructions since they
         * are diferent services
         */
        int depth = nestingDepth.get();
        nestingDepth.set(depth + 1);
        boolean atTopLevel = (depth == 0);

        long startThreadId = Thread.currentThread().threadId();
        long[] startValues = null;
        if (atTopLevel) {
            hardwareCounterService.startThreadCounters();
            startValues = hardwareCounterService.readValues();
        }

        try {
            // Execute Service
            return joinPoint.proceed();
        } finally {
            nestingDepth.set(depth);

            if (atTopLevel) {
                long endThreadId = Thread.currentThread().threadId();
                String serviceClass = joinPoint.getTarget().getClass().getSimpleName();
                String methodName = joinPoint.getSignature().getName();

                // Validation Checks
                boolean hardwardCounterIsUnavailable = (!hardwareCounterService.isAvailable());
                boolean failedInitialReading = (startValues == null || startValues.length < 2);
                boolean startAndEndThreadDiffer = (startThreadId != endThreadId);

                if (hardwardCounterIsUnavailable) {
                    appendMeasurementLine("FAILED: status=hardware-counter-service-unavailable");
                } else if (failedInitialReading) {
                    appendMeasurementLine(String.format(
                            "FAILED: status=invalid-init-reading service=%s method=%s", serviceClass, methodName));
                } else if (startAndEndThreadDiffer) {
                    appendMeasurementLine(String.format(
                            "FAILED: status=invalid-thread-hop service=%s method=%s threadStart=%d threadEnd=%d",
                            serviceClass, methodName, startThreadId, endThreadId));

                } else {
                    long[] endValues = hardwareCounterService.readValues();
                    boolean failedFinalReading = (endValues == null || endValues.length < 2);

                    if (failedFinalReading) {
                        appendMeasurementLine(String.format(
                                "status=invalid-final-reading service=%s method=%s", serviceClass, methodName));
                    } else {
                        // Report Instruction Counting
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
        if (OUTPUT_FILE == null) {
            System.err.println("HardwareProfilerError: No output-file found");
            return;
        }

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
