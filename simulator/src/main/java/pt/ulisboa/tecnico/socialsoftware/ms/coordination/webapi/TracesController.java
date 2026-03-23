package pt.ulisboa.tecnico.socialsoftware.ms.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.ms.tracing.TraceService;

@RestController
public class TracesController {
    @Autowired
    private TraceService traceService;

    @GetMapping("/traces/createRoot")
    public ResponseEntity<String> createRoot() {
        try {
            System.out.println("Creating master root span");
            traceService.createMasterRoot();
            String traceId = traceService.getMasterRootTraceId();
            String spanId = traceService.getMasterRootSpanId();
            return ResponseEntity.ok(traceId + ":" + spanId);
        } catch (Exception e) {
            System.err.println("Error creating master root span: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/traces/start")
    public ResponseEntity<String> start(
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String spanId) {
        try {
            if (traceId != null && spanId != null) {
                System.out.println("Root span started with remote parent traceId=" + traceId);
                traceService.startRootSpan(traceId, spanId);
            } else {
                System.out.println("Root span started");
                traceService.startRootSpan();
            }
            String responseTraceId = traceService.getRootTraceId();
            String responseSpanId = traceService.getRootSpanId();
            return ResponseEntity.ok(responseTraceId + ":" + responseSpanId);
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
            traceService.endMasterRootSpan();
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
