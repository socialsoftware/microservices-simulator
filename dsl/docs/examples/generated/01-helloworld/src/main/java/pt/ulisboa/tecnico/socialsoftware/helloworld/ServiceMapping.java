package pt.ulisboa.tecnico.socialsoftware.helloworld;

public enum ServiceMapping {
    TASK("task");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
