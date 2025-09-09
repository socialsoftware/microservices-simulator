package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.TraceManager;

@Service
public class BehaviourService {
    private static String directory;

    public void LoadDir(String dir, String testNameFile) {
        directory = dir + "/src/test/resources/" + testNameFile + "/";
        BehaviourHandler.getInstance();
        BehaviourHandler.setDirectory(directory);
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

}