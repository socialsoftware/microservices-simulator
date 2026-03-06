package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.CapacityService;

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

    @GetMapping("/capacity/status")
    public Map<String, Integer> status() {
        return capacityService.getAvailableCapacities();
    }
}
