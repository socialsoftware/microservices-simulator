package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.command.AbortCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.command.CommitCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.command.PrepareCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.command.AbortSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.command.CommitSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

/* 
    ! TODO - Main Problems:
    
    ! 1. A test is failling because of the way we handle retries. When a retry is specified, the functionality is executed again, 
    ! but the behaviour is not re-injected, which means that the same behaviour is applied to all retries. This honeslty should be the case
    ! but its not the behaviour expected by the test.

    ! 2. No missmatches are checked when loading the behaviour from the CSV file.

    ! 3. This class is responsible for command tracing
 */

@Aspect
@Component
public class ImpairmentHandler {
    @Autowired(required = false)
    private NetworkManager networkManager;

    @Autowired
    private ImpairmentReportService reportService;

    @Value("${simulator.impairment.network-delays.enabled:false}")
    private boolean networkDelaysEnabled;

    // Legacy code for functionality behaviour management (deprecated)
    private String directory;
    private Map<String, Integer> funcCounter = new HashMap<>();
    private Map<String, Integer> funcRetry = new HashMap<>();
    private Map<WorkflowFunctionality, Map<String, List<Integer>>> behaviourCache = Collections
            .synchronizedMap(new WeakHashMap<>());

    public ImpairmentHandler() {
    }

    // ****************************
    // * --- Public Interface --- *
    // ****************************

    public void reset() {
        if (networkDelaysEnabled) {
            networkManager.reset();
        }
        // Reset other components such as faults if needed here
    }

    public void injectDelayConfiguration(String json) {
        if (networkDelaysEnabled) {
            networkManager.injectConfiguration(json);
        }
    }

    public String getReport() {
        return reportService.getReport();
    }

    public void cleanReportFile() {
        reportService.cleanReportFile();
    }

    // *******************************
    // * --- Behaviour Injection --- *
    // *******************************

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
            // TODO - Implement fault injection
            /*
             * if (faultValue == 1) {
             * reportService.logInfo("EXCEPTION THROWN during " + funcName);
             * throw new SimulatorException("Fault on " + commandName);
             * }
             */

            if (networkDelaysEnabled) {
                return networkManager.executeWithImpairment(gateway, joinPoint, command, commandName, funcName);
            } else {
                return joinPoint.proceed();
            }

        } finally {
            TraceManager.getInstance().endCommandSpan(funcName, commandName);
        }
    }

    // ************************************
    // * --- Legacy Code (Deprecated) --- *
    // ************************************

    // --- Public Interface ---

    public void cleanUpCounter() {
        funcCounter.clear();
        reportService.report("Test finished\n");
    }

    public int getRetryValue(String funcName) {
        System.out.println(
                "\u001B[33mRetry value for " + funcName + ": " + funcRetry.getOrDefault(funcName, 0) + "\u001B[0m");
        return funcRetry.getOrDefault(funcName, 0);
    }

    public void setDirectory(String dir) {
        directory = dir;
    }

    // --- Behaviour Management ---

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

    private synchronized int getFuncionalityCounter(String functionality) {
        return funcCounter.compute(functionality, (k, v) -> (v == null) ? 1 : v + 1);
    }

    private List<String[]> parseCSVForBlock(Path filePath, String funcName, int targetBlock) throws IOException {
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
}