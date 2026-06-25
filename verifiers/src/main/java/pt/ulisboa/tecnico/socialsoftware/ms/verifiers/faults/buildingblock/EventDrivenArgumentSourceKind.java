package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

public enum EventDrivenArgumentSourceKind {
    EVENT_SUBSCRIBER_AGGREGATE_ID,
    EVENT_FIELD,
    EVENT_PAYLOAD,
    EVENT_PROCESSING_PARAMETER,
    INJECTABLE_FIELD,
    RUNTIME_CALL,
    SOURCE_PLACEHOLDER,
    EVENT_EXPRESSION
}
