package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
public class CapacityController {
    @Autowired
    private CapacityService capacityService;

    @GetMapping("/capacity/reset")
    public String reset() {
        System.out.println("Capacity reset started");
        capacityService.reset();
        return "OK";
    }

    @GetMapping("/capacity/report/clean")
    public String clean() {
        System.out.println("Capacity report cleaned");
        capacityService.cleanReportFile();
        return "OK";
    }

    // *TESTING METHODS*

    @GetMapping(value = "/capacity/report/read")
    public String getReport() {
        return capacityService.getReport();
    }

    @PostMapping(value = "/capacity/inject")
    public String updatePlacement(@RequestBody JsonNode json) {
        capacityService.injectCapacities(json.toString());
        return "OK";
    }
}
