package pt.ulisboa.tecnico.socialsoftware.ms.behaviour;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import pt.ulisboa.tecnico.socialsoftware.ms.tracing.TraceManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class BehaviourService {
    private static String directory;

    public void LoadDir(String dir, String testNameFile) {
        directory = dir + "/src/test/resources/" + testNameFile + "/";
        BehaviourHandler.getInstance();
        BehaviourHandler.setDirectory(directory);
        NetworkManager.getInstance().reset();
        NetworkManager.setDirectory(directory);
        NetworkManager.getInstance().load();
    }

    public void reset() {
        BehaviourHandler.getInstance().reset();
    }

    public void cleanUpCounter() {
        BehaviourHandler.getInstance().cleanUpCounter();
    }

    public void cleanReportFile() {
        BehaviourHandler.getInstance().cleanReportFile();

    }

    public void cleanDirectory() {
        BehaviourHandler.getInstance().setDirectory("");
    }

    public int getRetryValue(String funcName) {
        return BehaviourHandler.getInstance().getRetryValue(funcName);
    }

    public void generateTestBehaviour(String fileName) {
        if (directory == null) {
            return;
        }
        Path filePath = Paths.get(directory, fileName);
        if (!Files.exists(filePath)) {
            return;
        }
        new BehaviourGenerator(directory, filePath);
    }

    public void flush() {
        TraceManager.getInstance().forceFlush();
    }

    // *TESTING METHODS*

    public String getReport() {
        return BehaviourHandler.getInstance().getReport();
    }

    public void injectPlacement(JsonNode json) {
        NetworkManager.getInstance().loadConfig(json);
    }
}