package pt.ulisboa.tecnico.socialsoftware.ecommerce;

public enum ServiceMapping {
    CART("cart"),
    INVOICE("invoice"),
    ORDER("order"),
    PAYMENT("payment"),
    PRODUCT("product"),
    SHIPPING("shipping"),
    USER("user");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
