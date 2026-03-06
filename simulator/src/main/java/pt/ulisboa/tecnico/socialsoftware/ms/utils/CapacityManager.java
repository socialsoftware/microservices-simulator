package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class CapacityManager {
    // TODO - How to test this!?
    private static CapacityManager instance;
    private static String directory;
    private final Map<String, Semaphore> msCapacities = new ConcurrentHashMap<>();
    private final Map<String, Integer> endpointRequirements = new ConcurrentHashMap<>();
    private final Map<String, String> endpointToMicroservice = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

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
        // ! TODO - Change this to correct format
        if (loaded || directory == null || directory.isEmpty()) {
            return;
        }
        Path filePath = Paths.get(directory, "capacities.json");
        File jsonFile = filePath.toFile();
        if (!jsonFile.exists()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(jsonFile);

            if (root.has("microservices")) {
                JsonNode microservices = root.get("microservices");
                for (JsonNode ms : microservices) {
                    String msName = ms.get("name").asText();
                    int capacity = ms.get("capacity").asInt();
                    msCapacities.put(msName, new Semaphore(capacity, true));
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
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading capacity configuration: " + e.getMessage());
        }
    }

    public void acquire(String functionalityName) throws InterruptedException {
        // ? TODO - Are we using endpoits or functionalities? Are they the same?
        if (!loaded) {
            return;
        }

        String msName = endpointToMicroservice.get(functionalityName);
        if (msName == null) {
            // If not configured, we assume it doesn't require capacity or isn't monitored
            // ? TODO - raise error?
            return;
        }

        Semaphore semaphore = msCapacities.get(msName);
        if (semaphore == null) {
            throw new SimulatorException("Microservice " + msName + " does not exist in capacity configuration");
        }

        Integer requirement = endpointRequirements.get(functionalityName);
        if (requirement != null && requirement > 0) {
            semaphore.acquire(requirement);
        }
    }

    public void release(String functionalityName) {
        if (!loaded) {
            return;
        }

        String msName = endpointToMicroservice.get(functionalityName);
        if (msName != null) {
            Integer requirement = endpointRequirements.get(functionalityName);
            Semaphore semaphore = msCapacities.get(msName);
            if (semaphore != null && requirement != null && requirement > 0) {
                semaphore.release(requirement);
            }
        }
    }

    public synchronized void reset() {
        // ? TODO - needed?
        msCapacities.clear();
        endpointRequirements.clear();
        endpointToMicroservice.clear();
        loaded = false;
    }

    public Map<String, Integer> getAvailableCapacities() {
        Map<String, Integer> status = new ConcurrentHashMap<>();
        msCapacities.forEach((ms, sem) -> status.put(ms, sem.availablePermits()));
        return status;
    }
}
