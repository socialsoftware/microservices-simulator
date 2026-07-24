package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.ReportService;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletionException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "simulator.impairment.network-delays", name = "enabled", havingValue = "true")
public class NetworkManager {
    @Value("${simulator.impairment.network-delays.configuration-file:#{null}}")
    private String CONFIG_FILE;

    private Map<String, String> microserviceToNode = new HashMap<>();
    private Map<String, DelayConfig> delayConfigs = new HashMap<>();
    private Random random = new Random();
    private final ReportService reportService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public NetworkManager(@Qualifier("ImpairmentReportService") ReportService reportService) {
        this.reportService = reportService;
    }

    @PostConstruct
    public void init() {
        load();
    }

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

    // ****************************
    // * --- Public Interface --- *
    // ****************************

    public Object executeWithImpairment(CommandGateway gateway, ProceedingJoinPoint joinPoint, Command command,
            String commandName, String funcName, String executionId) throws Throwable {

        String sourceService = resolveMicroserviceName(gateway);
        String targetService = command.getServiceName();

        int delayBeforeValue = generateDelay(sourceService, targetService);
        int delayAfterValue = generateDelay(sourceService, targetService);

        boolean isImpaired = (delayBeforeValue + delayAfterValue > 0);
        if (isImpaired) {
            TraceManager.getInstance().setSpanAttribute(executionId, "isImpaired", true);
        }

        logStep(funcName, commandName, sourceService, targetService, delayBeforeValue,
                delayAfterValue);

        delay(executionId, commandName, delayBeforeValue, true);
        Object result = joinPoint.proceed();
        delay(executionId, commandName, delayAfterValue, false);

        return result;
    }

    public synchronized void injectConfiguration(String json) {
        // Receives a configuration and overrides the current one - used for testing
        reset();
        report("Injecting new Configuration");
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(json);
            parseConfig(root);
            logger.info("Network configuration loaded from JSON string");
        } catch (IOException | NumberFormatException | SimulatorException e) {
            terminateWithError("Error injecting network configuration: " + e.getMessage());
        }
    }

    public void reset() {
        this.microserviceToNode.clear();
        this.delayConfigs.clear();
        this.reportService.cleanReport();
    }

    // ******************
    // * --- Set Up --- *
    // ******************

    private void load() {
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
        } catch (IOException e) {
            terminateWithError("Error loading network configuration: " + e.getMessage());
        }

        report("Configuration fully loaded!");
    }

    private void parseConfig(JsonNode root) {
        if (!(root.has("Placement") && root.has("Delays"))) {
            terminateWithError("ConfigurationError: Must have fields 'Placement' and 'Delays'");
            return;
        }

        JsonNode placement = root.get("Placement");
        if (!placement.has("nodes")) {
            terminateWithError("ConfigurationError: 'Placement' must have field 'nodes");
            return;
        }

        // Load nodes from Placement field
        JsonNode nodes = placement.get("nodes");
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
        JsonNode delays = root.get("Delays");
        loadDelayConfig(delays, "intraservice");
        loadDelayConfig(delays, "intranode");
        loadDelayConfig(delays, "internode");
    }

    private void loadDelayConfig(JsonNode delaysNode, String type) {
        if (!delaysNode.has(type))
            return;

        JsonNode config = delaysNode.get(type);
        if (config.has("uni")) {
            JsonNode params = config.get("uni");
            delayConfigs.put(type, new DelayConfig("uni", params.get(0).asDouble(), params.get(1).asDouble()));
        } else if (config.has("exp")) {
            JsonNode params = config.get("exp");
            delayConfigs.put(type, new DelayConfig("exp", params.get(0).asDouble(), params.get(1).asDouble()));
        }
    }

    // ***************************************
    // * --- Network Management & Delays --- *
    // ***************************************

    private int generateDelay(String sourceService, String targetService) {
        String delayType;

        if (sourceService == null || targetService == null) {
            // No delay if a service isnt identified
            return 0;
        }

        String sourceNode = microserviceToNode.get(sourceService.toLowerCase());
        String targetNode = microserviceToNode.get(targetService.toLowerCase());

        if (sourceNode == null || targetNode == null) {
            // No delay if a service placement isnt specified
            return 0;
        }

        if (sourceService.equalsIgnoreCase(targetService)) {
            // Same service communication
            delayType = "intraservice";
        } else if (sourceNode.equals(targetNode)) {
            // Same node communication
            delayType = "intranode";
        } else {
            // Different node communication
            delayType = "internode";
        }

        DelayConfig config = delayConfigs.get(delayType);
        if (config == null) {
            // Fallback: dont inject delay
            return 0;
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

    private String resolveMicroserviceName(CommandGateway gateway) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains(".microservices.")) {
                String[] parts = className.split("\\.");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals("microservices")) {
                        return parts[i + 1];
                    }
                }
            }
        }

        return "unknown";
    }

    private void delay(String executionId, String commandName, int delayValue, boolean isBefore) {
        Span delaySpan = TraceManager.getInstance().startDelaySpan(executionId, commandName, delayValue, isBefore);
        try {
            Thread.sleep(delayValue);
        } catch (InterruptedException e) {
            // Thread.currentThread().interrupt();
            throw new CompletionException(e);
        } finally {
            TraceManager.getInstance().endDelaySpan(delaySpan);
        }
    }

    // **************************
    // * --- Report Logging --- *
    // **************************

    private void report(String msg) {
        reportService.report(String.format("[NetworkManager]: %s", msg));
    }

    private void terminateWithError(String msg) {
        msg = String.format("[NetworkManager]: %s", msg);
        reportService.report(msg);
        logger.error(msg);
        reset();
    }

    private void logStep(String funcName, String commandName, String sourceService, String targetService,
            int delayBeforeValue, int delayAfterValue) {

        String ln = String.format(
                "Impairing %s\n" + "  >> on command %s (%s->%s): Before [%dms] | After [%dms]",
                funcName, commandName, sourceService, targetService, delayBeforeValue, delayAfterValue);

        logger.info(ln);
        report(ln);
    }
}