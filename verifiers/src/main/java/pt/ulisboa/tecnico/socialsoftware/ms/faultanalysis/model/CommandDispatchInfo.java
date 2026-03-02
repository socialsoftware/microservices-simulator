package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

public record CommandDispatchInfo(
        String serviceClassName,
        String serviceMethodName,
        AccessPolicy accessPolicy,
        String aggregateName) {
}
