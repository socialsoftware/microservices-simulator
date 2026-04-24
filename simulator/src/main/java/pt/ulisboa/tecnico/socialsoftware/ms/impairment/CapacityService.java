package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CapacityService {
    @Autowired(required = false)
    private CapacityManager manager;

    public void reset() {
        if (manager != null) {
            manager.reset();
        }
    }

    public void cleanReportFile() {
        if (manager != null) {
            manager.cleanReportFile();
        }
    }

    public String getReport() {
        return manager != null ? manager.getReport() : "Capacity management is disabled.";
    }

    public void injectCapacities(String json) {
        if (manager != null) {
            manager.injectConfiguration(json);
        }
    }
}
