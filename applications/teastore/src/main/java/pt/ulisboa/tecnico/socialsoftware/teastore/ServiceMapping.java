package pt.ulisboa.tecnico.socialsoftware.teastore;

public enum ServiceMapping {
    CART("cart"),
    CATEGORY("category"),
    ORDER("order"),
    PRODUCT("product"),
    USER("user");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
