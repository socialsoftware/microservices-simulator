package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImpairmentService {
    private static String directory;

    @Autowired(required = false)
    private NetworkManager networkManager;

    // ! TODO - Remove
    public void LoadDir(String dir, String testNameFile) {
        directory = dir + "/src/test/resources/" + testNameFile + "/";
        ImpairmentHandler.getInstance();
        ImpairmentHandler.setDirectory(directory);
    }

    // ! TODO - Remove
    public void cleanDirectory() {
        ImpairmentHandler.setDirectory("");
    }

    // ! TODO - Remove
    public void cleanUpCounter() {
        ImpairmentHandler.getInstance().cleanUpCounter();
    }

    public void reset() {
        if (networkManager != null) {
            networkManager.reset();
        }
    }

    public void cleanReportFile() {
        if (networkManager != null) {
            networkManager.cleanReportFile();
        }

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
        if (networkManager != null) {
            return networkManager.getReport();
        }
        return "";
    }

    public void injectPlacement(String json) {
        if (networkManager != null) {
            networkManager.injectConfiguration(json);
        }
    }
}