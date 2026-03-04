package pt.ulisboa.tecnico.socialsoftware.crossrefs;

public enum ServiceMapping {
    COURSE("course"),
    ENROLLMENT("enrollment"),
    TEACHER("teacher");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
