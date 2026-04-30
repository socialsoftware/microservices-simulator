package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImpairmentService {
    private static String directory;

    public void LoadDir(String dir, String testNameFile) {
        directory = dir + "/src/test/resources/" + testNameFile + "/";
        ImpairmentHandler.getInstance();
        ImpairmentHandler.setDirectory(directory);
        NetworkManager.getInstance().reset();
        NetworkManager.setDirectory(directory);
        NetworkManager.getInstance().load();
    }

    public void reset() {
        ImpairmentHandler.getInstance().reset();
    }

    public void cleanUpCounter() {
        ImpairmentHandler.getInstance().cleanUpCounter();
    }

    public void cleanReportFile() {
        ImpairmentHandler.getInstance().cleanReportFile();
        
    }

    public void cleanDirectory() {
        ImpairmentHandler.setDirectory("");
    }

    public int getRetryValue(String funcName) {
        return ImpairmentHandler.getInstance().getRetryValue(funcName);
    }

    public void generateTestBehaviour(String fileName) {
        if (directory == null) {
            return;
        }
        Path filePath = Paths.get(directory, fileName);
        if (!Files.exists(filePath)) {
            return;
        }
        new ImpairmentGenerator(directory, filePath);
    }

    public void flush() {
        TraceManager.getInstance().forceFlush();
    }

    // *TESTING METHODS*

    public String getReport() {
        return ImpairmentHandler.getInstance().getReport();
    }

    public void injectPlacement(JsonNode json) {
        NetworkManager.getInstance().loadConfig(json);
    }
}