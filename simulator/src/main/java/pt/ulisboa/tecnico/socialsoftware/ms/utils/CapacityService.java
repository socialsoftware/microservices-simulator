package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CapacityService {
    private static String directory;

    public void loadDir(String dir, String testNameFile) {
        directory = dir + "/src/test/resources/" + testNameFile + "/";
        CapacityManager.setDirectory(directory);
        CapacityManager.getInstance().load();
    }

    public void reset() {
        CapacityManager.getInstance().reset();
    }

    public Map<String, Integer> getAvailableCapacities() {
        return CapacityManager.getInstance().getAvailableCapacities();
    }
}
