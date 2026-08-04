package pt.ulisboa.tecnico.socialsoftware.trainticket;

public enum ServiceMapping {
    CONTACTS("contacts"),
    ORDER("order"),
    PAYMENT("payment"),
    PRICE_CONFIG("priceConfig"),
    ROUTE("route"),
    STATION("station"),
    TRAIN("train"),
    TRIP("trip"),
    USER("user");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
