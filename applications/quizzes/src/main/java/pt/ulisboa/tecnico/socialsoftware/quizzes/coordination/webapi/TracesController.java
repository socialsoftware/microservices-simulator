package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.TraceService;

@RestController
public class TracesController {
    @Autowired
    private TraceService traceService;

   @GetMapping("/traces/start")
    public String start() {
        System.out.println("Root span started");
        traceService.startRootSpan();
        return "OK";
    }
    
    @GetMapping(value = "/traces/end")
    public String stop() {
        System.out.println("Stop root span");
        traceService.endRootSpan();
        return "OK";
    }

    @GetMapping(value = "/traces/flush")
    public String flush() {
        System.out.println("Flush root span");
        traceService.spanFlush();
        return "OK";
    }
}
