package pt.ulisboa.tecnico.socialsoftware.ms.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Aspect
@Component
@Profile("profiler")
public class HardwareProfiler {
    private final HardwareCounterService hardwareCounterService = HardwareCounterService.getInstance();
    private static final ThreadLocal<Integer> nestingDepth = ThreadLocal.withInitial(() -> 0);

    private static final ThreadLocal<Integer> dbNestingDepth = ThreadLocal.withInitial(() -> 0);

    private static final ThreadLocal<long[]> dbTaxAccumulator = ThreadLocal.withInitial(() -> new long[] { 0L, 0L });
    private static final ThreadLocal<long[]> nestedServiceTaxAccumulator = ThreadLocal
            .withInitial(() -> new long[] { 0L, 0L });

    private final ConcurrentLinkedQueue<Measurement> measurements = new ConcurrentLinkedQueue<>();
    private static final Logger logger = LoggerFactory.getLogger(HardwareProfiler.class);

    public static final class Measurement {
        private final String serviceClass;
        private final String methodName;
        private final String status;
        private final long instructions;
        private final long cycles;
        private final double cpi;
        private final long startThreadId;
        private final long endThreadId;

        public Measurement(String serviceClass, String methodName, String status,
                long instructions, long cycles, double cpi,
                long startThreadId, long endThreadId) {
            this.serviceClass = serviceClass;
            this.methodName = methodName;
            this.status = status;
            this.instructions = instructions;
            this.cycles = cycles;
            this.cpi = cpi;
            this.startThreadId = startThreadId;
            this.endThreadId = endThreadId;
        }

        public String getServiceClass() {
            return serviceClass;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getStatus() {
            return status;
        }

        public long getInstructions() {
            return instructions;
        }

        public long getCycles() {
            return cycles;
        }

        public double getCpi() {
            return cpi;
        }

        public long getStartThreadId() {
            return startThreadId;
        }

        public long getEndThreadId() {
            return endThreadId;
        }
    }

    public List<Measurement> getMeasurementsSnapshot() {
        return new ArrayList<>(measurements);
    }

    public void clearMeasurements() {
        measurements.clear();
    }

    private void recordMeasurement(String serviceClass, String methodName, String status,
            long instructions, long cycles, double cpi,
            long startThreadId, long endThreadId) {
        measurements.add(new Measurement(serviceClass, methodName, status, instructions, cycles, cpi,
                startThreadId, endThreadId));
    }

    @Around("execution(* pt.ulisboa.tecnico.socialsoftware..*Repository+.*(..)) || execution(* jakarta.persistence.EntityManager.*(..))")
    public Object profileRepositoryMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        int depth = dbNestingDepth.get();
        dbNestingDepth.set(depth + 1);
        boolean isTopLevelRepo = (depth == 0);

        long[] start = isTopLevelRepo ? hardwareCounterService.readValues() : null;
        try {
            return joinPoint.proceed();
        } finally {
            dbNestingDepth.set(depth);
            if (isTopLevelRepo) {
                long[] end = hardwareCounterService.readValues();
                if (start != null && end != null && start.length >= 2 && end.length >= 2) {
                    long diffInst = Math.max(0, end[0] - start[0]);
                    long diffCycles = Math.max(0, end[1] - start[1]);

                    long[] currentTax = dbTaxAccumulator.get();
                    currentTax[0] += diffInst;
                    currentTax[1] += diffCycles;
                }
            }
        }
    }

    @Around("execution(public * pt.ulisboa.tecnico.socialsoftware.*.microservices..service.*.*(..))")
    public Object profileServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        int depth = nestingDepth.get();
        nestingDepth.set(depth + 1);
        boolean atTopLevel = (depth == 0);

        long startThreadId = Thread.currentThread().threadId();

        if (atTopLevel) {
            hardwareCounterService.startThreadCounters();
            dbTaxAccumulator.set(new long[] { 0L, 0L });
            nestedServiceTaxAccumulator.set(new long[] { 0L, 0L });
        }

        long[] startValues = hardwareCounterService.readValues();

        // Snapshot the taxes at the start of this specific method
        long startTaxInst = dbTaxAccumulator.get()[0];
        long startTaxCycles = dbTaxAccumulator.get()[1];

        long startNestedInst = nestedServiceTaxAccumulator.get()[0];
        long startNestedCycles = nestedServiceTaxAccumulator.get()[1];

        try {
            // Execute Service
            return joinPoint.proceed();
        } finally {
            nestingDepth.set(depth);

            long endThreadId = Thread.currentThread().threadId();
            String serviceClass = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();

            // Validation Checks
            boolean hardwardCounterIsUnavailable = (!hardwareCounterService.isAvailable());
            boolean failedInitialReading = (startValues == null || startValues.length < 2);
            boolean startAndEndThreadDiffer = (startThreadId != endThreadId);

            if (hardwardCounterIsUnavailable) {
                recordMeasurement(serviceClass, methodName, "hardware-counter-service-unavailable", 0, 0, 0.0,
                        startThreadId, endThreadId);
                logger.error("FAILED: status=hardware-counter-service-unavailable");
            } else if (failedInitialReading) {
                recordMeasurement(serviceClass, methodName, "invalid-init-reading", 0, 0, 0.0,
                        startThreadId, endThreadId);
                logger.error("FAILED: status=invalid-init-reading service={} method={}", serviceClass, methodName);
            } else if (startAndEndThreadDiffer) {
                recordMeasurement(serviceClass, methodName, "invalid-thread-hop", 0, 0, 0.0,
                        startThreadId, endThreadId);
                logger.error("FAILED: status=invalid-thread-hop service={} method={} threadStart={} threadEnd={}",
                        serviceClass, methodName, startThreadId, endThreadId);

            } else {
                long[] endValues = hardwareCounterService.readValues();
                boolean failedFinalReading = (endValues == null || endValues.length < 2);

                if (failedFinalReading) {
                    recordMeasurement(serviceClass, methodName, "invalid-final-reading", 0, 0, 0.0,
                            startThreadId, endThreadId);
                    logger.error("FAILED: status=invalid-final-reading service={} method={}", serviceClass,
                            methodName);
                } else {
                    // Report Instruction Counting
                    long instructions = Math.max(0, endValues[0] - startValues[0]);
                    long cycles = Math.max(0, endValues[1] - startValues[1]);

                    long endTaxInst = dbTaxAccumulator.get()[0];
                    long endTaxCycles = dbTaxAccumulator.get()[1];
                    long taxInstForThisMethod = Math.max(0, endTaxInst - startTaxInst);
                    long taxCyclesForThisMethod = Math.max(0, endTaxCycles - startTaxCycles);

                    long endNestedInst = nestedServiceTaxAccumulator.get()[0];
                    long endNestedCycles = nestedServiceTaxAccumulator.get()[1];
                    long nestedInstForThisMethod = Math.max(0, endNestedInst - startNestedInst);
                    long nestedCyclesForThisMethod = Math.max(0, endNestedCycles - startNestedCycles);

                    // Subtract DB Tax and Nested Service Tax to get Pure Logic
                    long logicInst = Math.max(0, instructions - taxInstForThisMethod - nestedInstForThisMethod);
                    long logicCycles = Math.max(0, cycles - taxCyclesForThisMethod - nestedCyclesForThisMethod);
                    double cpi = logicInst > 0 ? (double) logicCycles / logicInst : 0.0;
                    double taxCpi = taxInstForThisMethod > 0
                            ? (double) taxCyclesForThisMethod / taxInstForThisMethod
                            : 0.0;

                    if (!atTopLevel) {
                        long[] currentNestedTax = nestedServiceTaxAccumulator.get();
                        currentNestedTax[0] += logicInst;
                        currentNestedTax[1] += logicCycles;
                    }

                    recordMeasurement(serviceClass, methodName, "ok", logicInst, logicCycles, cpi,
                            startThreadId, endThreadId);

                    recordMeasurement(serviceClass, methodName + "_DB", "ok", taxInstForThisMethod,
                            taxCyclesForThisMethod, taxCpi, startThreadId, endThreadId);

                    logger.info(String.format(
                            "service=%s method=%s instructions=%d cycles=%d cpi=%s startThreadId=%d endThreadId=%d",
                            serviceClass, methodName, logicInst, logicCycles, cpi, startThreadId, endThreadId));
                }
            }
        }
    }
}
