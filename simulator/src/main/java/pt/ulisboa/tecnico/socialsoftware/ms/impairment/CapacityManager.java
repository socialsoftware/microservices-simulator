package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.ReportService;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(prefix = "simulator.capacity-management", name = "enabled", havingValue = "true")
public class CapacityManager {
    @Autowired
    @Qualifier("CapacityReportService")
    private ReportService reportService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // TODO - Change paths
    @Value("${simulator.capacity-management.configuration-file:#{null}}")
    private String CONFIG_FILE;

    private final Map<String, Semaphore> msCapacities = new ConcurrentHashMap<>();
    private final Map<String, Integer> requirements = new ConcurrentHashMap<>();
    private final Map<String, List<String>> waitingRequests = new ConcurrentHashMap<>();
    private final Map<String, List<String>> activeRequests = new ConcurrentHashMap<>();
    // Scale factor to convert float capacities/requirements to integer permits
    private static final int SCALE = 1000;

    @PostConstruct
    public void init() {
        load();
    }

    // ****************************
    // * --- Public Interface --- *
    // ****************************

    public synchronized void injectConfiguration(String json) {
        // Receives a configuration and overrides the current one - used for testing
        reset();
        reportService.report("Injecting new Configuration");
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(json);
            parseConfig(root);
            logger.info("Capacity configuration loaded from JSON string");
        } catch (IOException | NumberFormatException | SimulatorException e) {
            terminateWithError("Error injecting capacity configuration: " + e.getMessage());
        }
    }

    public synchronized void reset() {
        msCapacities.clear();
        requirements.clear();
        waitingRequests.clear();
        activeRequests.clear();
        reportService.cleanReport();
    }

    public String getReport() {
        return reportService.getReport();
    }

    public void cleanReportFile() {
        reportService.cleanReport();
    }

    // ******************
    // * --- Set Up --- *
    // ******************

    // --- Private Helpers ---

    private synchronized void load() {
        // Checks if the configuration file exists and loads it if possible
        if (CONFIG_FILE == null) {
            terminateWithError("Configuration-file must be specified in application.yaml");
            return;
        }

        Path filePath = Paths.get(CONFIG_FILE);
        File jsonFile = filePath.toFile();
        if (!jsonFile.exists()) {
            terminateWithError("Configuration file not found at " + jsonFile.getAbsolutePath());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(jsonFile);
            parseConfig(root);
        } catch (IOException | NumberFormatException | SimulatorException e) {
            terminateWithError("Error loading capacity configuration: " + e.getMessage());
        }

        reportService.report("Configuration fully loaded!");
    }

    private void parseConfig(JsonNode root) throws IOException {
        if (!(root.has("Capacities"))) {
            terminateWithError("ConfigurationError: Must have field 'Capacities'");
            return;
        }

        JsonNode capacities = root.get("Capacities");
        if (!capacities.has("microservices")) {
            terminateWithError("ConfigurationError: 'Capacities' must have field 'microservices'");
            return;
        }

        Map<String, Integer> microserviceCapacities = new HashMap<>();

        // Load microservice capacities
        JsonNode microservices = capacities.get("microservices");
        for (JsonNode ms : microservices) {
            if (!ms.has("name") || !ms.has("capacity") || !ms.has("services")) {
                terminateWithError(
                        "ConfigurationError: 'microservices' must have fields 'name', 'capacity' and 'services'");
                return;
            }

            // Use lowercase for internal mapping consistency
            String msName = ms.get("name").asText().toLowerCase();

            double capacity = ms.get("capacity").asDouble();
            if (capacity < 0) {
                terminateWithError("Invalid capacity for microservice '" + msName + "': " + capacity);
                return;
            }

            int scaledCapacity = (int) Math.round(capacity * SCALE);
            msCapacities.put(msName, new Semaphore(scaledCapacity, true));
            microserviceCapacities.put(msName, scaledCapacity);
            waitingRequests.put(msName, Collections.synchronizedList(new ArrayList<>()));
            activeRequests.put(msName, Collections.synchronizedList(new ArrayList<>()));

            JsonNode services = ms.get("services");
            if (!services.isArray()) {
                terminateWithError("ConfigurationError: 'services' must be an array");
                return;
            }

            for (JsonNode service : services) {
                if (!service.has("name") || !service.has("requirement")) {
                    terminateWithError("ConfigurationError: services must include fields 'name' and 'requirement'");
                    return;
                }

                String operationName = service.get("name").asText().toLowerCase();
                String operationKey = msName + "." + operationName;
                double requirement = service.get("requirement").asDouble();

                if (requirement < 0) {
                    terminateWithError("ConfigurationError: Invalid requirement for service '" + operationName + "': "
                            + requirement);
                    return;
                }
                int scaledRequirement = (int) Math.round(requirement * SCALE);
                if (scaledRequirement > scaledCapacity) {
                    terminateWithError(String.format(
                            "ConfigurationError: Invalid requirement: Service method '%s' requires %.3f, but microservice '%s' capacity is %.3f",
                            operationName, requirement, msName, capacity));
                    return;
                }

                requirements.put(operationKey, scaledRequirement);
            }
        }

        // Validate node capacity from Placement field
        if (!root.has("Placement"))
            return;

        JsonNode placement = root.get("Placement");
        JsonNode nodes = placement.get("nodes");
        if (nodes == null || !nodes.isArray()) {
            reportService.report("ConfigurationError: 'Placement' must have field 'nodes' of array type");
            return;
        }

        for (JsonNode node : nodes) {
            if (!node.has("capacity") || !node.has("name")) {
                reportService.report("ConfigurationError: malformated node on 'Placement'");
                continue;
            }

            String nodeName = node.get("name").asText();
            double nodeLimitFloat = node.get("capacity").asDouble();
            int nodeLimit = (int) Math.round(nodeLimitFloat * SCALE);
            int currentMSCapacitySum = 0;

            JsonNode mss = node.get("microservices");
            if (mss == null || !mss.isArray()) {
                reportService
                        .report("ConfigurationError: 'Placement' 'node' must have field 'microservices' of array type");
                continue;
            }

            for (JsonNode ms : mss) {
                String msName = ms.asText().toLowerCase();
                Integer cap = microserviceCapacities.get(msName);

                if (cap != null) {
                    currentMSCapacitySum += cap;
                }
            }

            if (currentMSCapacitySum > nodeLimit) {
                String errorMsg = String.format(
                        "ConfigurationError: Node '%s' capacity exceeded! Limit: %.3f, Sum of MS capacities: %.3f",
                        nodeName, nodeLimit / (double) SCALE, currentMSCapacitySum / (double) SCALE);
                terminateWithError(errorMsg);
            }
        }
    }

    // *******************************
    // * --- Capacity Management --- *
    // *******************************

    private void acquire(String microserviceName, String methodName, String requestId)
            throws InterruptedException {
        if (microserviceName == null)
            return;

        Semaphore semaphore = msCapacities.get(microserviceName);
        if (semaphore == null) {
            return;
        }

        String operationKey = microserviceName + "." + methodName.toLowerCase();
        Integer requirement = requirements.get(operationKey);
        if (requirement != null && requirement > 0) {
            waitingRequests.get(microserviceName).add(requestId);
            logState(microserviceName, "WAITING", operationKey, requestId);

            try {
                semaphore.acquire(requirement);
            } finally {
                waitingRequests.get(microserviceName).remove(requestId);
            }

            activeRequests.get(microserviceName).add(requestId);
            logState(microserviceName, "ACQUIRED", operationKey, requestId);
        }
    }

    private void release(String microserviceName, String methodName, String requestId) {
        if (microserviceName == null)
            return;

        String operationKey = microserviceName + "." + methodName.toLowerCase();
        Integer requirement = requirements.get(operationKey);
        Semaphore semaphore = msCapacities.get(microserviceName);
        if (semaphore != null && requirement != null && requirement > 0) {
            if (activeRequests.get(microserviceName).remove(requestId)) {
                // Only release if it was actually active
                semaphore.release(requirement);
                logState(microserviceName, "RELEASED", operationKey, requestId);
            }
        }
    }

    private String resolveMicroserviceName(Class<?> serviceClass) {
        String packageName = serviceClass.getPackageName();
        String[] packageTokens = packageName.split("\\.");
        for (int i = 0; i < packageTokens.length - 1; i++) {
            if ("microservices".equals(packageTokens[i])) {
                return packageTokens[i + 1].toLowerCase();
            }
        }
        return null;
    }

    // --- Main Method ---

    @Around("execution(public * pt.ulisboa.tecnico.socialsoftware.quizzes.microservices..service.*.*(..))")
    public Object applyMicroserviceCapacity(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().getName().toLowerCase();
        String microserviceName = resolveMicroserviceName(joinPoint.getTarget().getClass());
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        try {
            acquire(microserviceName, methodName, requestId);
            return joinPoint.proceed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting for capacity for service " + microserviceName + "." + methodName, e);
        } finally {
            release(microserviceName, methodName, requestId);
        }
    }

    // **************************
    // * --- Report Logging --- *
    // **************************

    private void terminateWithError(String msg) {
        reset();
        logger.error(msg);
        reportService.report(msg);
    }

    private synchronized void logState(String msName, String action, String operationName, String requestId) {
        List<String> active_snapshot;
        List<String> waiting_snapshot;
        synchronized (activeRequests.get(msName)) {
            active_snapshot = new ArrayList<>(activeRequests.get(msName));
        }
        synchronized (waitingRequests.get(msName)) {
            waiting_snapshot = new ArrayList<>(waitingRequests.get(msName));
        }

        int availablePermits = msCapacities.get(msName).availablePermits();
        double available = availablePermits / (double) SCALE;
        String logMsg = String.format("[%s][%s] %s: %s | Active: %s | Waiting: %s | Available: %.3f",
                msName, operationName, action, requestId, active_snapshot, waiting_snapshot, available);
        reportService.report(logMsg);
    }
}