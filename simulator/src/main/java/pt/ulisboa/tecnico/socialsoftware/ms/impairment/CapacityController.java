package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.util.Map;

@RestController
public class CapacityController {
    private static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").getAbsolutePath());

    @Autowired
    private CapacityService capacityService;

    @PostMapping("/capacity/load")
    public String load(@RequestParam String dir) {
        System.out.println("Capacity load started");
        capacityService.loadDir(mavenBaseDir, dir);
        System.out.println("Provided dir: " + dir);
        return "OK";
    }

    @GetMapping("/capacity/reset")
    public String reset() {
        System.out.println("Capacity reset started");
        capacityService.reset();
        return "OK";
    }

    @GetMapping("/capacity/clean")
    public String clean() {
        System.out.println("Capacity report cleaned");
        capacityService.cleanReportFile();
        return "OK";
    }

    // *TESTING METHODS*

    @GetMapping(value = "/capacity/report")
    public String getReport() {
        return capacityService.getReport();
    }

    @PostMapping(value = "/capacity/inject")
    public String updatePlacement(@RequestBody JsonNode json) {
        capacityService.injectCapacities(json.toString());
        return "OK";
    }

    @GetMapping("/capacity/status")
    public Map<String, Integer> status() {
        return capacityService.getAvailableCapacities();
    }
}
