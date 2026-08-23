package pt.ulisboa.tecnico.socialsoftware.trainticket;

public enum ServiceMapping {
    STATION("station"),
    TRAIN_TYPE("trainType"),
    USER("user"),
    ROUTE("route"),
    CONTACTS("contacts"),
    ;

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
