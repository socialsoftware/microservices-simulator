package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;

@RestController
public class ImpairmentController {
    private static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").getAbsolutePath());
    @Autowired
    private ImpairmentService impairmentService;

    // ! TODO - Remove -> Remove on admin controller
    @PostMapping("/behaviour/load")
    public String load(@RequestParam String dir) {
        System.out.println("Behaviour load started");
        impairmentService.LoadDir(mavenBaseDir, dir);
        System.out.println("Provided dir: " + dir);
        return "OK";
    }

    @GetMapping("/behaviour/reset")
    public String reset() {
        impairmentService.reset();
        return "OK";
    }

    @GetMapping(value = "/behaviour/clean")
    public String clean() {
        System.out.println("Report clean started");
        impairmentService.cleanReportFile();
        return "OK";
    }

    // *TESTING METHODS*

    @GetMapping(value = "/behaviour/report")
    public String getReport() {
        return impairmentService.getReport();
    }

    @PostMapping(value = "/behaviour/inject")
    public String injectPlacement(@RequestBody JsonNode json) {
        impairmentService.injectPlacement(json.toString());
        return "OK";
    }
}
