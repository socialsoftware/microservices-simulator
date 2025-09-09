package pt.ulisboa.tecnico.socialsoftware.ms;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
* Service for managing microservice behavior simulation
*/
@Service
public class BehaviourService {

private final Map<String, String> behaviorMap = new ConcurrentHashMap<>();
        private boolean globalBehavior = false;

        public String startBehaviour(String behaviourSpec) {
        try {
        // Parse and store behavior specification
        // TODO: Implement behavior parsing logic
        globalBehavior = true;
        return "Behaviour started successfully: " + behaviourSpec;
        } catch (Exception e) {
        throw new RuntimeException("Failed to start behaviour: " + e.getMessage(), e);
        }
        }

        public void stopBehaviour() {
        try {
        globalBehavior = false;
        behaviorMap.clear();
        } catch (Exception e) {
        throw new RuntimeException("Failed to stop behaviour: " + e.getMessage(), e);
        }
        }

        public String getStatus() {
        return globalBehavior ? "ACTIVE" : "INACTIVE";
        }

        public void addBehaviour(String serviceName, String methodName, String behavior) {
        String key = serviceName + "." + methodName;
        behaviorMap.put(key, behavior);
        }

        public void clearBehaviour() {
        behaviorMap.clear();
        globalBehavior = false;
        }

        public String getBehaviour(String serviceName, String methodName) {
        String key = serviceName + "." + methodName;
        return behaviorMap.get(key);
        }
        }