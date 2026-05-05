package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.opentelemetry.api.trace.Span;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;

/* 
    ! TODO - 3 Main Problems:
    
    ! 1. A test is failling because of the way we handle retries. When a retry is specified, the functionality is executed again, 
    ! but the behaviour is not re-injected, which means that the same behaviour is applied to all retries. This honeslty should be the case
    ! but its not the behaviour expected by the test.

    ! 2. No missmatches are checked when loading the behaviour from the CSV file.

    ! 3. execute() and executeSteps() in ExecutionPlan() were originally assynchronous and synchronous, respectively. Right now runStep() 
    ! is a hybrid of both, which is a bit confusing. We should decide on one approach and stick to it.
 */

public class ImpairmentHandler {
    private static ImpairmentHandler instance;
    private static String directory;
    private static Map<String, Integer> funcCounter = new HashMap<>();
    private static Map<String, Integer> funcRetry = new HashMap<>();
    private static final String REPORT_FILE = "BehaviourReport.txt";
    private static final Logger logger = LoggerFactory.getLogger(ImpairmentHandler.class);
    private Map<WorkflowFunctionality, Map<String, List<Integer>>> behaviourCache = Collections
            .synchronizedMap(new WeakHashMap<>());

    // ****************************
    // * --- Public Interface --- *
    // ****************************

    public static synchronized ImpairmentHandler getInstance() {
        if (instance == null) {
            instance = new ImpairmentHandler();
        }
        return instance;
    }

    public void cleanUpCounter() {
        funcCounter.clear();
        appendToReport("Test finished\n");
    }

    public int getRetryValue(String funcName) {
        System.out.println(
                "\u001B[33mRetry value for " + funcName + ": " + funcRetry.getOrDefault(funcName, 0) + "\u001B[0m");
        return funcRetry.getOrDefault(funcName, 0);
    }

    public static void setDirectory(String dir) {
        directory = dir;
    }

    // ********************************
    // * --- Behaviour Management --- *
    // ********************************

    private Map<String, List<Integer>> fetchBehaviour(WorkflowFunctionality functionality, FlowStep step) {
        String funcName = functionality.getClass().getSimpleName();

        Map<String, List<Integer>> loadedBehaviour = behaviourCache.get(functionality);
        boolean behaviourIsNew = (loadedBehaviour == null);

        if (behaviourIsNew) {
            loadedBehaviour = loadFunctionalityBehaviour(funcName);
            behaviourCache.put(functionality, loadedBehaviour);
            System.out.println("Cache size: " + behaviourCache.size());
        }

        return loadedBehaviour;
    }

    private Map<String, List<Integer>> loadFunctionalityBehaviour(String funcName) {
        // TODO - check for duplicates and non existing steps
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        if (directory == null) {
            return map;
        }
        Path filePath = Paths.get(directory, funcName + ".csv");
        System.out.println("HERE filepath: " + filePath.toString());
        if (!Files.exists(filePath)) {
            return map;
        }
        int functionalityCounter = getFuncionalityCounter(funcName);
        System.out.println("HERE counter: " + functionalityCounter);
        try {
            List<String[]> block = parseCSVForBlock(filePath, funcName, functionalityCounter);
            System.out.println("HERE block: " + block.toString());
            if (!block.isEmpty()) {
                for (String[] row : block) {
                    String key = row[0];
                    List<Integer> values = Arrays.asList(
                            Integer.parseInt(row[1]),
                            Integer.parseInt(row[2]),
                            Integer.parseInt(row[3]));
                    System.out.println("HERE values: " + values);
                    map.put(key, values);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private static synchronized int getFuncionalityCounter(String functionality) {
        return funcCounter.compute(functionality, (k, v) -> (v == null) ? 1 : v + 1);
    }

    private static List<String[]> parseCSVForBlock(Path filePath, String funcName, int targetBlock) throws IOException {
        List<String[]> currentBlock = new ArrayList<>();
        int blockNumber = 0;

        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.contains("retry"))
                    funcRetry.put(funcName, Integer.parseInt(line.split(",")[1]));

                if (line.equalsIgnoreCase("run")) {
                    blockNumber++;

                } else {
                    if (blockNumber == targetBlock && !line.isEmpty()) {
                        currentBlock.add(line.split(","));
                    }

                    if (blockNumber > targetBlock) {
                        break;
                    }
                }
            }
        }

        return currentBlock;
    }

    // *******************************
    // * --- Behaviour Injection --- *
    // *******************************

    // --- Private Helpers ---

    private void delay(String funcName, String stepName, int delayValue, boolean isBefore) {
        if (delayValue <= 0) {
            return;
        }

        Span delaySpan = TraceManager.getInstance().startDelaySpan(funcName, stepName, delayValue, isBefore);
        try {
            Thread.sleep(delayValue);
        } catch (InterruptedException e) {
            // Thread.currentThread().interrupt();
            throw new CompletionException(e);
        } finally {
            TraceManager.getInstance().endDelaySpan(delaySpan);
        }
    }

    // --- Main Methods ---

    public void injectFault(WorkflowFunctionality functionality, FlowStep step)
            throws SimulatorException {
        Map<String, List<Integer>> behaviour = fetchBehaviour(functionality, step);
        String funcName = functionality.getClass().getSimpleName();
        String stepName = step.getName();

        if (behaviour.containsKey(stepName)) {
            TraceManager.getInstance().setSpanAttribute(funcName, "hasBehaviour", true);
            int faultValue = behaviour.get(stepName).get(0);

            Boolean faultSpecified = (faultValue == 1);
            if (faultSpecified) {
                logStep(funcName, stepName, behaviour.get(stepName));
                logger.info("EXCEPTION THROWN: {} with version {}", funcName, funcRetry.getOrDefault(funcName, 0));
                throw new SimulatorException("Fault on " + stepName);
            }
        } else {
            TraceManager.getInstance().setSpanAttribute(funcName, "hasBehaviour", false);
        }
    }

    public int injectDelayBefore(WorkflowFunctionality functionality, String spanName, FlowStep step) {
        Map<String, List<Integer>> behaviour = fetchBehaviour(functionality, step);
        String stepName = step.getName();

        if (behaviour.containsKey(stepName)) {
            TraceManager.getInstance().setSpanAttribute(spanName, "hasBehaviour", true);
            int delayBeforeValue = behaviour.get(stepName).get(1);
            delay(spanName, stepName, delayBeforeValue, true);
            return delayBeforeValue;
        }

        TraceManager.getInstance().setSpanAttribute(spanName, "hasBehaviour", false);
        return 0;
    }

    public int injectDelayAfter(WorkflowFunctionality functionality, String spanName, FlowStep step) {
        Map<String, List<Integer>> behaviour = fetchBehaviour(functionality, step);
        String stepName = step.getName();

        if (behaviour.containsKey(stepName)) {
            int delayAfterValue = behaviour.get(stepName).get(2);
            delay(spanName, stepName, delayAfterValue, false);
            logStep(spanName, stepName, behaviour.get(stepName));
            return delayAfterValue;
        }

        return 0;
    }

    // **************************
    // * --- Report Logging --- *
    // **************************

    // --- Private Helpers ---

    private void logStep(String funcName, String stepName, List<Integer> stepBehaviour) {
        StringBuilder report = new StringBuilder();
        StringBuilder colorReport = new StringBuilder();

        report.append("Functionality: ").append(funcName).append("\n");
        colorReport.append("Functionality: ").append(funcName).append("\n");

        report.append("Step: ").append(stepName).append("\n");
        colorReport.append("Step: ").append(stepName).append("\n");

        report.append("Behaviour: ").append(stepBehaviour).append("\n");
        colorReport.append("Behaviour: ").append(stepBehaviour).append("\n");

        logger.info(colorReport.toString());
        appendToReport(report.toString());
    }

    private void appendToReport(String content) {

        if (directory == null) {
            return;
        }
        if (content == null || content.isEmpty()) {
            return;
        }

        Path filePath = Paths.get(directory, REPORT_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write(content);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write to file: " + filePath);
            e.printStackTrace();
        }
    }

    // --- Public Methods ---

    public String getReport() {
        if (directory == null) {
            return "";
        }
        Path filePath = Paths.get(directory, REPORT_FILE);
        if (!Files.exists(filePath)) {
            return "";
        }
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void cleanReportFile() {
        if (directory == null) {
            System.out.println("Directory not set. Please set the directory first.");
            return;
        }
        Path filePath = Paths.get(directory, REPORT_FILE);
        try {
            Files.writeString(filePath, "", StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to clean file: " + filePath);
            e.printStackTrace();
        }
    }
}