package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService;

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

    
    @GetMapping(value = "/behaviour/clean")
    public String clean() {
        System.out.println("Report clean started");
        behaviourService.cleanReportFile();
        return "OK";
    }
}
