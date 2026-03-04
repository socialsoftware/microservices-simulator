package pt.ulisboa.tecnico.socialsoftware.tutorial;

public enum ServiceMapping {
    BOOK("book"),
    LOAN("loan"),
    MEMBER("member");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
