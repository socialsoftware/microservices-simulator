package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import org.springframework.stereotype.Service;

@Service
public class ExecutionParametersService {
    private static String directory;

    public void LoadDir(String dir) {
        directory = dir + "/src/test/resources/";
        ReadStepsFile.getInstance();
        ReadStepsFile.setDirectory(directory);
    }

    
}