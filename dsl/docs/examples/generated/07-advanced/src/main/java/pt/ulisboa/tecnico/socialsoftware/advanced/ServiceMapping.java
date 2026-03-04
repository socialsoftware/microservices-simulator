package pt.ulisboa.tecnico.socialsoftware.advanced;

public enum ServiceMapping {
    CUSTOMER("customer"),
    INVOICE("invoice"),
    ORDER("order"),
    PRODUCT("product");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
