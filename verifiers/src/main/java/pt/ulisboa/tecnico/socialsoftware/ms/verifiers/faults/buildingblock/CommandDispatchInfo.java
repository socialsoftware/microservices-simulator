package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

/**
 * Command dispatch mapping: which service method handles a given command type.
 * The service method is identified by its full FQN signature so that overloaded
 * methods are distinguished correctly.
 */
public record CommandDispatchInfo(
        ServiceBuildingBlock service,
        String serviceMethodSignature,
        String aggregateName) {

    /** Human-readable method name, derived from the signature by stripping parameter types. */
    public String serviceMethodName() {
        int parenIdx = serviceMethodSignature.indexOf('(');
        return parenIdx < 0 ? serviceMethodSignature : serviceMethodSignature.substring(0, parenIdx);
    }

    /** FQN of the service class handling this command. */
    public String serviceClassName() {
        return service.getFqn();
    }

    /** READ or WRITE, resolved via the FQN signature for correct overload handling. */
    public AccessPolicy accessPolicy() {
        return service.getAccessPolicy(serviceMethodSignature);
    }
}
