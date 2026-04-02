package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.tracing.TraceManager;

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
    }

    public void cleanUpCounter() {
        ImpairmentHandler.getInstance().cleanUpCounter();
    }

    public void cleanReportFile() {
        ImpairmentHandler.getInstance().cleanReportFile();
        
    }

    public void cleanDirectory() {
        ImpairmentHandler.getInstance().setDirectory("");
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

}