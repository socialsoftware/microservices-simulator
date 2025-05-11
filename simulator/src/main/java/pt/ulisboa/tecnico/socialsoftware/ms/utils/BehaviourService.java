package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourHandler;
import org.springframework.stereotype.Service;

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


}