package pt.ulisboa.tecnico.socialsoftware.typesenums;

public enum ServiceMapping {
    CONTACT("contact");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
