package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class NetworkManager {
    private static final String PLACEMENT_FILE = "placement.json";
    private static NetworkManager instance;
    private static String directory;
    private Random random = new Random();
    private boolean loaded = false;

    private Map<String, String> microserviceToNode = new HashMap<>();
    private Map<String, DelayConfig> delayConfigs = new HashMap<>();

    private static class DelayConfig {
        String type; // "uni" or "exp"
        double param1; // a or mu
        double param2; // b or sigma

        DelayConfig(String type, double p1, double p2) {
            this.type = type;
            this.param1 = p1;
            this.param2 = p2;
        }
    }

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public static void setDirectory(String dir) {
        directory = dir;
    }

    public void load() {
        if (loaded || directory == null) {
            return;
        }

        Path filePath = Paths.get(directory, PLACEMENT_FILE);
        File file = filePath.toFile();
        if (!file.exists()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(file);

            // Load nodes
            JsonNode nodes = root.get("nodes");
            if (nodes != null && nodes.isArray()) {
                for (JsonNode node : nodes) {
                    String nodeName = node.get("name").asText();
                    JsonNode mss = node.get("microservices");
                    if (mss != null && mss.isArray()) {
                        for (JsonNode ms : mss) {
                            microserviceToNode.put(ms.asText().toLowerCase(), nodeName);
                        }
                    }
                }
            }

            // Load delays
            JsonNode delays = root.get("delays");
            if (delays != null) {
                loadDelayConfig(delays, "intraservice");
                loadDelayConfig(delays, "intranode");
                loadDelayConfig(delays, "internode");
            }

            loaded = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDelayConfig(JsonNode delaysNode, String type) {
        JsonNode config = delaysNode.get(type);
        if (config != null) {
            if (config.has("uni")) {
                JsonNode params = config.get("uni");
                delayConfigs.put(type, new DelayConfig("uni", params.get(0).asDouble(), params.get(1).asDouble()));
            } else if (config.has("exp")) {
                JsonNode params = config.get("exp");
                delayConfigs.put(type, new DelayConfig("exp", params.get(0).asDouble(), params.get(1).asDouble()));
            }
        }
    }

    public int generateDelay(String sourceService, String targetService) {
        System.out.println("FLOW: " + sourceService + " -> " + targetService);
        String delayType;
        if (sourceService != null && targetService != null && sourceService.equalsIgnoreCase(targetService)) {
            // Same service communication
            delayType = "intraservice";
        } else {
            String sourceNode = microserviceToNode.get(sourceService != null ? sourceService.toLowerCase() : "");
            String targetNode = microserviceToNode.get(targetService != null ? targetService.toLowerCase() : "");

            if (sourceNode != null && targetNode != null && sourceNode.equals(targetNode)) {
                // Same node communication
                delayType = "intranode";
            } else {
                // Different node communication
                delayType = "internode";
            }
        }

        DelayConfig config = delayConfigs.get(delayType);
        if (config == null) {
            // Fallback: use a default Log-Normal
            return (int) Math.exp(3.0 + 0.5 * random.nextGaussian());
        }

        if ("uni".equals(config.type)) {
            // UNIFORM DISTRIBUTION
            double value = config.param1 + (config.param2 - config.param1) * random.nextDouble();
            return (int) value;
        } else {
            // LOG NORMAL DISTRIBUTION
            double value = Math.exp(config.param1 + config.param2 * random.nextGaussian());
            return (int) value;
        }
    }

    public int generateFault() {
        // TODO - Implement this to randomly generate faults at runtime
        return 0;
    }

    public void reset() {
        this.loaded = false;
        this.microserviceToNode.clear();
        this.delayConfigs.clear();
    }
}
