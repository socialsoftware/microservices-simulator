package pt.ulisboa.tecnico.socialsoftware.businessrules;

public enum ServiceMapping {
    PRODUCT("product");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
