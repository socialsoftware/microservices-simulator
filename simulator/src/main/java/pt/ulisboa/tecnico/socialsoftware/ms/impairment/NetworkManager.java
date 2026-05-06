package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.command.AbortCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.command.CommitCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.command.PrepareCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.command.AbortSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.command.CommitSagaCommand;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletionException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class NetworkManager {
    // TODO - Change paths
    @Value("${simulator.network-impairment.enabled:false}")
    private boolean impairmentEnabled;
    @Value("${simulator.network-impairment.configuration-file:#{null}}")
    private String CONFIG_FILE;
    @Value("${simulator.network-impairment.report-file:#{null}}")
    private String REPORT_FILE;

    private Map<String, String> microserviceToNode = new HashMap<>();
    private Map<String, DelayConfig> delayConfigs = new HashMap<>();
    private Random random = new Random();
    private BufferedWriter writer;
    private static final Logger logger = LoggerFactory.getLogger(NetworkManager.class);

    public NetworkManager() {
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

    // ******************
    // * --- Set Up --- *
    // ******************

    // --- Private Helpers ---

    private void terminateWithError(String msg) {
        logger.error(msg);
        appendToReport(msg);
        reset();
    }

    private void load() {
        if (!impairmentEnabled) {
            logger.info("Impairment not enabled");
            return;
        }

        initReport();

        // Checks if the configuration file exists and loads it if possible
        if (CONFIG_FILE == null || REPORT_FILE == null) {
            terminateWithError("Configuration-file and report-file must be specified in application.yaml");
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

    // --- Public Methods ---

    public synchronized void injectConfiguration(String json) {
        // Receives a configuration and overrides the current one - used for testing
        reset();
        this.impairmentEnabled = true;
        initReport();
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
        this.impairmentEnabled = false;
    }

    // ******************************
    // * --- Network Management --- *
    // ******************************

    // --- Private Helpers ---

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

    private int generateFault(String n) {
        // TODO - Implement this to randomly generate faults at runtime
        return 0;
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

    private void delay(String funcName, String commandName, int delayValue, boolean isBefore) {
        Span delaySpan = TraceManager.getInstance().startDelaySpan(funcName, commandName, delayValue, isBefore);
        try {
            Thread.sleep(delayValue);
        } catch (InterruptedException e) {
            // Thread.currentThread().interrupt();
            throw new CompletionException(e);
        } finally {
            TraceManager.getInstance().endDelaySpan(delaySpan);
        }
    }

    private Boolean isTransactionalCommand(Command command) {
        if (command == null) {
            return true;
        }

        Class<? extends Command> type = command.getClass();
        if (type == CommitSagaCommand.class || type == AbortSagaCommand.class || type == PrepareCausalCommand.class
                || type == CommitCausalCommand.class || type == AbortCausalCommand.class) {
            return true;
        }

        return false;
    }

    private Command unwrapCommand(Command command) {
        if (command.getClass() == SagaCommand.class) {
            return ((SagaCommand) command).getPayload();
        }
        return command;
    }

    // --- Main Method ---

    @Around("execution(public * pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway.send(pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command)) && args(command) && target(gateway)")
    public Object wrapSend(ProceedingJoinPoint joinPoint, Command command, CommandGateway gateway) throws Throwable {
        if (isTransactionalCommand(command)) {
            return joinPoint.proceed();
        }

        command = unwrapCommand(command);
        String commandName = command.getClass().getSimpleName();
        String funcName = command.getUnitOfWork() != null ? command.getUnitOfWork().getFunctionalityName() : "unknown";

        TraceManager.getInstance().startCommandSpan(funcName, commandName);
        try {
            if (!impairmentEnabled) {
                return joinPoint.proceed();
            }

            String sourceService = resolveMicroserviceName(gateway);
            String targetService = command.getServiceName();
            return executeWithImpairment(joinPoint, command, sourceService, targetService, commandName, funcName);
        } finally {
            TraceManager.getInstance().endCommandSpan(funcName, commandName);
        }
    }

    private Object executeWithImpairment(ProceedingJoinPoint joinPoint, Command command, String sourceService,
            String targetService, String commandName, String funcName) throws Throwable {
        int faultValue = generateFault(commandName);
        int delayBeforeValue = generateDelay(sourceService, targetService);
        int delayAfterValue = generateDelay(sourceService, targetService);

        boolean isImpaired = (faultValue + delayBeforeValue + delayAfterValue > 0);
        if (isImpaired) {
            TraceManager.getInstance().setSpanAttribute(funcName, "isImpaired", true);
        }

        logStep(funcName, commandName, sourceService, targetService, faultValue, delayBeforeValue, delayAfterValue);

        if (faultValue == 1) {
            logger.error("EXCEPTION THROWN during {}", funcName);
            throw new SimulatorException("Fault on " + commandName);
        }

        delay(funcName, commandName, delayBeforeValue, true);
        Object result = joinPoint.proceed();
        delay(funcName, commandName, delayAfterValue, false);

        return result;
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
            appendToReport("### NETWORK MANAGER REPORT STARTED: " + new Date() + " ###");
        } catch (IOException e) {
            logger.error("Error initializing network report: " + e.getMessage());
        }
    }

    private void logStep(String funcName, String commandName, String sourceService, String targetService,
            int faultValue, int delayBeforeValue, int delayAfterValue) {

        String ln = String.format(
                "Impairing %s\n" + "  >> on command %s (%s->%s): Fault [%d] | Before [%dms] | After [%dms]",
                funcName, commandName, sourceService, targetService, faultValue, delayBeforeValue, delayAfterValue);

        logger.info(ln);
        appendToReport(ln);
    }

    private void appendToReport(String content) {
        if (writer != null) {
            try {
                writer.write(content);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                logger.error("Error writing to network report: " + e.getMessage());
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
            logger.error("Error reading capacity report: " + e.getMessage());
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
            logger.error("Error cleaning capacity report: " + e.getMessage());
        }
    }
}