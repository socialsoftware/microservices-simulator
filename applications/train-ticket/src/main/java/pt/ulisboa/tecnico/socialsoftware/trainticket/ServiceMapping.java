package pt.ulisboa.tecnico.socialsoftware.trainticket;

public enum ServiceMapping {
    STATION("station"),
    ;

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
