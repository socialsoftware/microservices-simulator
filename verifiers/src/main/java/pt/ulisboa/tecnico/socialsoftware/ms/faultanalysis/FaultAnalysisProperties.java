package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "simulator.analysis")
public class FaultAnalysisProperties {
    private final String targetApplicationsDir;
    private final String targetApplicationBasePackage;

    public FaultAnalysisProperties(@DefaultValue("/applcations") String targetApplicationsDir,
                                   String targetApplicationBasePackage) {
        this.targetApplicationsDir = targetApplicationsDir;
        this.targetApplicationBasePackage = targetApplicationBasePackage;
    }

    public String getTargetApplicationsDir() {
        return targetApplicationsDir;
    }

    public String getTargetApplicationBasePackage() {
        return targetApplicationBasePackage;
    }
}
