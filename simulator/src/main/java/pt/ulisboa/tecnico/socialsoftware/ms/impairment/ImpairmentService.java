package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImpairmentService {
    private String directory;

    @Autowired
    private ImpairmentHandler impairmentHandler;

    // ! TODO - Deprecated
    public void LoadDir(String dir, String testNameFile) {
        directory = dir + "/src/test/resources/" + testNameFile + "/";
        impairmentHandler.setDirectory(directory);
    }

    // ! TODO - Deprecated
    public void cleanDirectory() {
        impairmentHandler.setDirectory("");
    }

    // ! TODO - Deprecated
    public void cleanUpCounter() {
        impairmentHandler.cleanUpCounter();
    }

    public void reset() {
        impairmentHandler.reset();
    }

    public void cleanReportFile() {
        impairmentHandler.cleanReportFile();
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

    // *TESTING METHODS*

    public String getReport() {
        return impairmentHandler.getReport();
    }

    public void injectPlacement(String json) {
        impairmentHandler.injectDelayConfiguration(json);
    }
}