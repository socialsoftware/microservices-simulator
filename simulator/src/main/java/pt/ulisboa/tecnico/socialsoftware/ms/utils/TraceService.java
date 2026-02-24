package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import org.springframework.stereotype.Service;

@Service
public class TraceService {
    private static String directory;

    public void startRootSpan() {
        TraceManager.getInstance().startRootSpan();
    }

    public void endRootSpan() {
        TraceManager.getInstance().endRootSpan();
    }

    public void spanFlush() {
        TraceManager.getInstance().forceFlush();
    }
}