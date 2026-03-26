package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService;

import java.io.File;

@RestController
public class BehaviourController {
    private static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").getAbsolutePath());

    @Autowired
    private BehaviourService behaviourService;

    @PostMapping("/behaviour/load")
    public String load(@RequestParam String dir) {
        System.out.println("Behaviour load started");
        behaviourService.LoadDir(mavenBaseDir, dir);
        System.out.println("Provided dir: " + dir);
        return "OK";
    }

    @GetMapping("/behaviour/reset")
    public String reset() {
        System.out.println("Behaviour reset started");
        behaviourService.reset();
        return "OK";
    }

    @GetMapping(value = "/behaviour/clean")
    public String clean() {
        System.out.println("Report clean started");
        behaviourService.cleanReportFile();
        return "OK";
    }

    // *TESTING METHODS*

    @GetMapping(value = "/behaviour/report")
    public String getReport() {
        return behaviourService.getReport();
    }

    @PostMapping(value = "/behaviour/inject")
    public String injectPlacement(@RequestBody JsonNode json) {
        behaviourService.injectPlacement(json);
        return "OK";
    }
}
