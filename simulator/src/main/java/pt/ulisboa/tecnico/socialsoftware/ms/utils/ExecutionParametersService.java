package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.ReadStepsFile;
import org.springframework.stereotype.Service;

@Service
public class ExecutionParametersService {
    private static String directory;

    public void LoadDir(String dir, String testNameFile) {
        directory = dir + "/src/test/resources/" + testNameFile + "/";
        ReadStepsFile.getInstance();
        ReadStepsFile.setDirectory(directory);
    }

    public void cleanUpCounter() {
        ReadStepsFile.getInstance().cleanUpCounter();
    }

    public void cleanReportFile() {
        ReadStepsFile.getInstance().cleanReportFile();
        
    }


}