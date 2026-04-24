package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(prefix = "simulator.capacity-management", name = "enabled", havingValue = "true")
public class CapacityManager {
    // TODO - Change paths
    @Value("${simulator.capacity-management.configuration-file:#{null}}")
    private String CONFIG_FILE;
    @Value("${simulator.capacity-management.report-file:#{null}}")
    private String REPORT_FILE;

    private final Map<String, Semaphore> msCapacities = new ConcurrentHashMap<>();
    private final Map<String, Integer> requirements = new ConcurrentHashMap<>();
    private final Map<String, List<String>> waitingRequests = new ConcurrentHashMap<>();
    private final Map<String, List<String>> activeRequests = new ConcurrentHashMap<>();
    private BufferedWriter writer;

    public CapacityManager() {
    }

    @PostConstruct
    public void init() {
        load();
    }

    // ******************
    // * --- Set Up --- *
    // ******************

    // --- Private Helpers ---

    private synchronized void load() {
        // Checks if the configuration file exists and loads it if possible
        if (CONFIG_FILE == null || REPORT_FILE == null) {
            System.err.println("CapacityManager configuration-file and report-file must be specified in application.yaml");
            return;
        }

        initReport();

        Path filePath = Paths.get(CONFIG_FILE);
        File jsonFile = filePath.toFile();
        if (!jsonFile.exists()) {
            String msg = "[CapacityManager] CRITICAL: File not found at " + jsonFile.getAbsolutePath();
            System.err.println(msg);
            appendToReport(msg);
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(jsonFile);
            parseConfig(root);
            System.out.println("Capacity configuration loaded from " + filePath);
        } catch (IOException | NumberFormatException | SimulatorException e) {
            String errorMsg = "Error loading capacity configuration: " + e.getMessage();
            System.err.println(errorMsg);
            appendToReport("### " + errorMsg + " ###");
            reset();
        }
    }

    private void parseConfig(JsonNode root) throws IOException {
        Map<String, Integer> microserviceCapacities = new HashMap<>();

        // Load microservice capacities
        if (root.has("Capacities")) {
            JsonNode capacities = root.get("Capacities");
            if (capacities.has("microservices")) {
                JsonNode microservices = capacities.get("microservices");
                for (JsonNode ms : microservices) {
                    // Use lowercase for internal mapping consistency
                    String msName = ms.get("name").asText().toLowerCase();
                    int capacity = ms.get("capacity").asInt();
                    if (capacity < 0) {
                        throw new SimulatorException("Invalid capacity for microservice '" + msName + "': " + capacity);
                    }
                    msCapacities.put(msName, new Semaphore(capacity, true));
                    microserviceCapacities.put(msName, capacity);

                    waitingRequests.put(msName, Collections.synchronizedList(new ArrayList<>()));
                    activeRequests.put(msName, Collections.synchronizedList(new ArrayList<>()));

                    JsonNode services = ms.get("services");
                    if (services != null) {
                        if (!services.isArray()) {
                            throw new SimulatorException(
                                    "Invalid services definition for microservice '" + msName + "'");
                        }

                        for (JsonNode service : services) {
                            if (!service.has("name") || !service.has("requirement")) {
                                throw new SimulatorException(
                                        "Invalid Service method entry for microservice '" + msName + "'");
                            }

                            String operationName = service.get("name").asText().toLowerCase();
                            String operationKey = msName + "." + operationName;
                            int requirement = service.get("requirement").asInt();

                            if (requirement < 0) {
                                throw new SimulatorException(
                                        "Invalid requirement for service method '" + operationName + "': "
                                                + requirement);
                            }
                            if (requirement > capacity) {
                                throw new SimulatorException(String.format(
                                        "Invalid requirement: Service method '%s' requires %d, but microservice '%s' capacity is %d",
                                        operationName, requirement, msName, capacity));
                            }

                            requirements.put(operationKey, requirement);
                        }
                    }
                }
            }
        }

        // Validate node capacity from Placement field
        if (root.has("Placement")) {
            JsonNode placement = root.get("Placement");
            JsonNode nodes = placement.get("nodes");
            if (nodes != null && nodes.isArray()) {
                for (JsonNode node : nodes) {
                    if (node.has("capacity")) {
                        String nodeName = node.get("name").asText();
                        int nodeLimit = node.get("capacity").asInt();
                        int currentMSCapacitySum = 0;

                        JsonNode mss = node.get("microservices");
                        if (mss != null && mss.isArray()) {
                            for (JsonNode ms : mss) {
                                String msName = ms.asText().toLowerCase();
                                Integer cap = microserviceCapacities.get(msName);

                                if (cap != null) {
                                    currentMSCapacitySum += cap;
                                }
                            }
                        }

                        if (currentMSCapacitySum > nodeLimit) {
                            String errorMsg = String.format(
                                    "[CapacityManager] VALIDATION ERROR: Node '%s' capacity exceeded! " +
                                            "Limit: %d, Sum of MS capacities: %d",
                                    nodeName, nodeLimit, currentMSCapacitySum);
                            throw new SimulatorException(errorMsg);
                        }
                    }
                }
            }
        }
    }

    // --- Public Methods ---

    public synchronized void injectConfiguration(String json) {
        // Receives a configuration and overrides the current one - used for testing
        reset();
        initReport();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(json);
            parseConfig(root);
            System.out.println("Capacity configuration loaded from JSON string");
        } catch (IOException | NumberFormatException | SimulatorException e) {
            String errorMsg = "Error loading capacity configuration from JSON: " + e.getMessage();
            System.err.println(errorMsg);
            appendToReport("### CONFIGURATION ERROR: " + errorMsg + " ###");
            reset();
        }
    }

    public synchronized void reset() {
        msCapacities.clear();
        requirements.clear();
        waitingRequests.clear();
        activeRequests.clear();

        if (writer != null) {
            try {
                writer.close();
                writer = null;
            } catch (IOException e) {
                System.err.println("Error closing capacity report: " + e.getMessage());
            }
        }
    }

    // *******************************
    // * --- Capacity Management --- *
    // *******************************

    // --- Private Helpers ---

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

    // ---Private Helpers ---

    private void initReport() {
        if (REPORT_FILE == null)
            return;

        try {
            if (writer != null) {
                writer.close();
            }
            Path reportPath = Paths.get(REPORT_FILE);
            writer = new BufferedWriter(new FileWriter(reportPath.toFile(), false));
            appendToReport("### CAPACITY MANAGER REPORT STARTED: " + new Date() + " ###");
        } catch (IOException e) {
            System.err.println("Error initializing capacity report: " + e.getMessage());
        }
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

        int available = msCapacities.get(msName).availablePermits();
        String logMsg = String.format("[%s][%s] %s: %s | Active: %s | Waiting: %s | Available: %d",
                msName, operationName, action, requestId, active_snapshot, waiting_snapshot, available);
        appendToReport(logMsg);
    }

    private void appendToReport(String content) {
        if (writer != null) {
            try {
                writer.write(content);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("Error writing to capacity report: " + e.getMessage());
            }
        }
    }

    // --- Public Methods ---

    public String getReport() {
        if (REPORT_FILE == null)
            return "";

        Path reportPath = Paths.get(REPORT_FILE);
        if (!Files.exists(reportPath)) {
            return "";
        }
        try {
            return Files.readString(reportPath);
        } catch (IOException e) {
            System.err.println("Error reading capacity report: " + e.getMessage());
            return "";
        }
    }

    public void cleanReportFile() {
        if (REPORT_FILE == null)
            return;

        Path reportPath = Paths.get(REPORT_FILE);
        try {
            Files.writeString(reportPath, "", StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error cleaning capacity report: " + e.getMessage());
        }
    }
}
