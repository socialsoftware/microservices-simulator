package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.TraceManager;

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