package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.TraceService;

@RestController
public class TracesController {
    @Autowired
    private TraceService traceService;

    @GetMapping("/traces/start")
    public ResponseEntity<String> start() {
        try {
            System.out.println("Root span started");
            traceService.startRootSpan();
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            System.err.println("Error starting root span: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping(value = "/traces/end")
    public ResponseEntity<String> stop() {
        try {
            System.out.println("Stop root span");
            traceService.endRootSpan();
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            System.err.println("Error stopping root span: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping(value = "/traces/flush")
    public ResponseEntity<String> flush() {
        try {
            System.out.println("Flush root span");
            traceService.spanFlush();
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            System.err.println("Error flushing spans: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
