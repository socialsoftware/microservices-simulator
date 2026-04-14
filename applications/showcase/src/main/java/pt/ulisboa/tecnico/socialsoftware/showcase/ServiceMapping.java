package pt.ulisboa.tecnico.socialsoftware.showcase;

public enum ServiceMapping {
    BOOKING("booking"),
    ROOM("room"),
    USER("user");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
