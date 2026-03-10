package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class CapacityManager {
    private static CapacityManager instance;
    private static String directory;
    private static final String CONFIG_FILE = "capacities.json";
    private static final String REPORT_FILE = "CapacityReport.txt";
    private final Map<String, Semaphore> msCapacities = new ConcurrentHashMap<>();
    private final Map<String, Integer> endpointRequirements = new ConcurrentHashMap<>();
    private final Map<String, String> endpointToMicroservice = new ConcurrentHashMap<>();
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
        if (loaded || directory == null || directory.isEmpty()) {
            return;
        }

        Path filePath = Paths.get(directory, CONFIG_FILE);
        File jsonFile = filePath.toFile();
        if (!jsonFile.exists()) {
            System.err.println("[CapacityManager] CRITICAL: File not found at " + jsonFile.getAbsolutePath());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(jsonFile);
            System.out.println("[CapacityManager] Loading configuration from: " + jsonFile.getAbsolutePath());

            if (root.has("microservices")) {
                JsonNode microservices = root.get("microservices");
                for (JsonNode ms : microservices) {
                    String msName = ms.get("name").asText();
                    int capacity = ms.get("capacity").asInt();
                    msCapacities.put(msName, new Semaphore(capacity, true));

                    waitingRequests.put(msName, Collections.synchronizedList(new ArrayList<>()));
                    activeRequests.put(msName, Collections.synchronizedList(new ArrayList<>()));
                }
            }

            if (root.has("endpoints")) {
                JsonNode endpoints = root.get("endpoints");
                for (JsonNode ep : endpoints) {
                    String epName = ep.get("name").asText();
                    String msName = ep.get("microservice").asText();
                    int requirement = ep.get("requirement").asInt();
                    endpointRequirements.put(epName, requirement);
                    endpointToMicroservice.put(epName, msName);
                }
            }

            loaded = true;
            System.out.println("Capacity configuration loaded from " + filePath);

            // Init report file
            Path reportPath = Paths.get(directory, REPORT_FILE);
            writer = new BufferedWriter(new FileWriter(reportPath.toFile(), false));
            writer.write("### CAPACITY MANAGER REPORT STARTED: " + new Date() + " ###\n");
            writer.flush();
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading capacity configuration: " + e.getMessage());
        }
    }

    public void acquire(String functionalityName, String requestId) throws InterruptedException {
        if (!loaded) {
            return;
        }

        String msName = endpointToMicroservice.get(functionalityName);
        if (msName == null) {
            return;
        }

        Semaphore semaphore = msCapacities.get(msName);
        if (semaphore == null) {
            throw new SimulatorException("Microservice " + msName + " does not exist in capacity configuration");
        }

        Integer requirement = endpointRequirements.get(functionalityName);
        if (requirement != null && requirement > 0) {
            waitingRequests.get(msName).add(requestId);
            logState(msName, "WAITING", requestId);

            try {
                semaphore.acquire(requirement);
            } finally {
                waitingRequests.get(msName).remove(requestId);
            }

            activeRequests.get(msName).add(requestId);
            logState(msName, "ACQUIRED", requestId);
        }
    }

    public void release(String functionalityName, String requestId) {
        if (!loaded) {
            return;
        }

        String msName = endpointToMicroservice.get(functionalityName);
        if (msName != null) {
            Integer requirement = endpointRequirements.get(functionalityName);
            Semaphore semaphore = msCapacities.get(msName);
            if (semaphore != null && requirement != null && requirement > 0) {
                semaphore.release(requirement);
                activeRequests.get(msName).remove(requestId);
                logState(msName, "RELEASED", requestId);
            }
        }
    }

    private synchronized void logState(String msName, String action, String logId) {
        List<String> active_snapshot;
        List<String> waiting_snapshot;
        synchronized (activeRequests.get(msName)) {
            active_snapshot = new ArrayList<>(activeRequests.get(msName));
        }
        synchronized (waitingRequests.get(msName)) {
            waiting_snapshot = new ArrayList<>(waitingRequests.get(msName));
        }

        int available = msCapacities.get(msName).availablePermits();
        String logMsg = String.format("[%s] %s: %s | Active: %s | Waiting: %s | Available: %d",
                msName, action, logId, active_snapshot, waiting_snapshot, available);
        // System.out.println("[CapacityManager] " + logMsg);

        if (writer != null) {
            try {
                writer.write(logMsg + "\n");
                writer.flush();
            } catch (IOException e) {
                System.err.println("Error writing to capacity report: " + e.getMessage());
            }
        }
    }

    public synchronized void reset() {
        msCapacities.clear();
        endpointRequirements.clear();
        endpointToMicroservice.clear();
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

    public Map<String, Integer> getAvailableCapacities() {
        Map<String, Integer> status = new ConcurrentHashMap<>();
        msCapacities.forEach((ms, sem) -> status.put(ms, sem.availablePermits()));
        return status;
    }
}
