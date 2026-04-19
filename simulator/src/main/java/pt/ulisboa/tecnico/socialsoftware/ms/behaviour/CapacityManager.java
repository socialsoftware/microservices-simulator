package pt.ulisboa.tecnico.socialsoftware.ms.behaviour;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class CapacityManager {
    private static CapacityManager instance;
    private static String directory;
    private static final String CONFIG_FILE = "simulator_config.json";
    private static final String REPORT_FILE = "CapacityReport.txt";
    private final Map<String, Semaphore> msCapacities = new ConcurrentHashMap<>();
    private final Map<String, Integer> requirements = new ConcurrentHashMap<>();
    private final Map<String, String> stepToMicroservice = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    private final Map<String, List<String>> waitingRequests = new ConcurrentHashMap<>();
    private final Map<String, List<String>> activeRequests = new ConcurrentHashMap<>();
    private BufferedWriter writer;

    public static synchronized CapacityManager getInstance() {
        if (instance == null) {
            instance = new CapacityManager();
        }
        return instance;
    }

    public static void setDirectory(String dir) {
        directory = dir;
    }

    public synchronized void load() {
        // Checks if the configuration file exists and loads it if possible
        if (loaded || directory == null || directory.isEmpty()) {
            return;
        }

        initReport();

        Path filePath = Paths.get(directory, CONFIG_FILE);
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
            loaded = true;
            System.out.println("Capacity configuration loaded from " + filePath);
        } catch (IOException | NumberFormatException | SimulatorException e) {
            String errorMsg = "Error loading capacity configuration: " + e.getMessage();
            System.err.println(errorMsg);
            appendToReport("### " + errorMsg + " ###");
            reset();
        }
    }

    public synchronized void loadConfig(String json) {
        // Receives and a configuration and overrides the current one - used for testing
        reset();
        initReport();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(json);
            parseConfig(root);
            loaded = true;
            System.out.println("Capacity configuration loaded from JSON string");
        } catch (IOException | NumberFormatException | SimulatorException e) {
            String errorMsg = "Error loading capacity configuration from JSON: " + e.getMessage();
            System.err.println(errorMsg);
            appendToReport("### CONFIGURATION ERROR: " + errorMsg + " ###");
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

                    // Load steps for this microservice
                    if (ms.has("steps")) {
                        JsonNode steps = ms.get("steps");
                        for (JsonNode step : steps) {
                            String stepName = step.get("name").asText();
                            int requirement = step.get("requirement").asInt();
                            if (requirement < 0) {
                                throw new SimulatorException(
                                        "Invalid requirement for step '" + stepName + "': " + requirement);
                            }
                            if (requirement > capacity) {
                                throw new SimulatorException(String.format(
                                        "Invalid requirement: Step '%s' requires %d, but microservice '%s' capacity is %d",
                                        stepName, requirement, msName, capacity));
                            }
                            requirements.put(stepName, requirement);
                            stepToMicroservice.put(stepName, msName);
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

    public void acquire(String stepName, String requestId) throws InterruptedException {
        if (!loaded) {
            return;
        }

        String msName = stepToMicroservice.get(stepName);
        if (msName == null) {
            return;
        }

        Semaphore semaphore = msCapacities.get(msName);
        if (semaphore == null) {
            throw new SimulatorException("Microservice " + msName + " does not exist in capacity configuration");
        }

        Integer requirement = requirements.get(stepName);
        if (requirement != null && requirement > 0) {
            waitingRequests.get(msName).add(requestId);
            logState(msName, "WAITING", stepName, requestId);

            try {
                semaphore.acquire(requirement);
            } finally {
                waitingRequests.get(msName).remove(requestId);
            }

            activeRequests.get(msName).add(requestId);
            logState(msName, "ACQUIRED", stepName, requestId);
        }
    }

    public void release(String stepName, String requestId) {
        if (!loaded) {
            return;
        }

        String msName = stepToMicroservice.get(stepName);
        if (msName != null) {
            Integer requirement = requirements.get(stepName);
            Semaphore semaphore = msCapacities.get(msName);
            if (semaphore != null && requirement != null && requirement > 0) {
                if (activeRequests.get(msName).remove(requestId)) {
                    // Only release if it was actually active
                    semaphore.release(requirement);
                    logState(msName, "RELEASED", stepName, requestId);
                }
            }
        }
    }

    private void initReport() {
        if (directory != null && !directory.isEmpty()) {
            try {
                if (writer != null) {
                    writer.close();
                }
                Path reportPath = Paths.get(directory, REPORT_FILE);
                writer = new BufferedWriter(new FileWriter(reportPath.toFile(), false));
                appendToReport("### CAPACITY MANAGER REPORT STARTED: " + new Date() + " ###");
            } catch (IOException e) {
                System.err.println("Error initializing capacity report: " + e.getMessage());
            }
        }
    }

    private synchronized void logState(String msName, String action, String stepName, String requestId) {
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
                msName, stepName, action, requestId, active_snapshot, waiting_snapshot, available);
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

    public synchronized void reset() {
        msCapacities.clear();
        requirements.clear();
        stepToMicroservice.clear();
        loaded = false;

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

    // ? TODO - Remove??
    public Map<String, Integer> getAvailableCapacities() {
        Map<String, Integer> status = new ConcurrentHashMap<>();
        msCapacities.forEach((ms, sem) -> status.put(ms, sem.availablePermits()));
        return status;
    }

    public String getReport() {
        if (directory == null || directory.isEmpty()) {
            return "";
        }
        Path reportPath = Paths.get(directory, REPORT_FILE);
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
        if (directory == null || directory.isEmpty()) {
            return;
        }
        Path reportPath = Paths.get(directory, REPORT_FILE);
        try {
            Files.deleteIfExists(reportPath);
        } catch (IOException e) {
            System.err.println("Error deleting capacity report: " + e.getMessage());
        }
    }
}
