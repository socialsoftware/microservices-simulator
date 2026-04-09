package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ServiceBuildingBlock extends BuildingBlock {
    /** Keyed by FQN signature, e.g. "createItem(com.example...ItemDto,pt.ulisboa...UnitOfWork)". */
    private final Map<String, AccessPolicy> methodAccessPolicies = new HashMap<>();

    public ServiceBuildingBlock(Path file, String packageName, String fqn) {
        super(file, packageName, fqn);
    }

    /** Register a method by its FQN signature. */
    public void addMethod(String signature, AccessPolicy policy) {
        methodAccessPolicies.put(signature, policy);
    }

    public Map<String, AccessPolicy> getMethodAccessPolicies() {
        return methodAccessPolicies;
    }

    /**
     * Looks up the access policy for a given query.
     * If the query contains '(', it is treated as a full FQN signature (exact lookup).
     * Otherwise it is treated as a bare method name: returns the policy of the first matching
     * entry whose key starts with query + "(". Suitable for non-overloaded lookups and
     * backward compatibility with existing tests.
     * Returns READ if no match is found.
     */
    public AccessPolicy getAccessPolicy(String query) {
        if (query.contains("(")) {
            return methodAccessPolicies.getOrDefault(query, AccessPolicy.READ);
        }
        return methodAccessPolicies.entrySet().stream()
                .filter(e -> e.getKey().startsWith(query + "("))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(AccessPolicy.READ);
    }
}
