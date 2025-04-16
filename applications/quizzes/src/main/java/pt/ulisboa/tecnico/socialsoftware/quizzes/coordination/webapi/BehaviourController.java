package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.ExecutionParametersService;

@RestController
public class BehaviourController {
    private static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").getAbsolutePath());

    @Autowired
    private ExecutionParametersService executionParametersService;

    @GetMapping(value = "/behaviour/load")
    public String load() {
        System.out.println("Behaviour load started");
        //executionParametersService.LoadDir(mavenBaseDir);
        System.out.println("Maven base dir: " + mavenBaseDir);
        return "OK";
    }
}
