package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import org.springframework.stereotype.Service;

@Service
public class ExecutionParametersService {
    private static String directory;

    public void LoadDir(String dir) {
        directory = dir + "/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/resources";
        System.out.println("Directory set to: " + directory);
        ReadStepsFile.getInstance().setDirectory(directory);
    }

    
}