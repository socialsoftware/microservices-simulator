package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CapacityService {
    private static String directory;

    public void loadDir(String dir, String testNameFile) {
        directory = dir + "/src/test/resources/" + testNameFile + "/";
        CapacityManager.getInstance().reset();
        CapacityManager.setDirectory(directory);
        CapacityManager.getInstance().load();
    }

    public void reset() {
        CapacityManager.getInstance().reset();
    }

    public Map<String, Integer> getAvailableCapacities() {
        return CapacityManager.getInstance().getAvailableCapacities();
    }

    public String getReport() {
        return CapacityManager.getInstance().getReport();
    }

    public void cleanReportFile() {
        CapacityManager.getInstance().cleanReportFile();
    }

    public void injectCapacities(String json) {
        CapacityManager.getInstance().loadConfig(json);
    }
}
