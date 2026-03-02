package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceBuildingBlock extends BuildingBlock {
    private final Map<String, AccessPolicy> methodAccessPolicies = new LinkedHashMap<>();

    public ServiceBuildingBlock(Path file, String packageName, String name) {
        super(file, packageName, name);
    }

    public void addMethod(String methodName, AccessPolicy policy) {
        methodAccessPolicies.put(methodName, policy);
    }

    public Map<String, AccessPolicy> getMethodAccessPolicies() {
        return methodAccessPolicies;
    }

    public AccessPolicy getAccessPolicy(String methodName) {
        return methodAccessPolicies.getOrDefault(methodName, AccessPolicy.READ);
    }
}
