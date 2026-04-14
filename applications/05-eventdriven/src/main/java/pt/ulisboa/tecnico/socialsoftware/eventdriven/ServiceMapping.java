package pt.ulisboa.tecnico.socialsoftware.eventdriven;

public enum ServiceMapping {
    AUTHOR("author"),
    POST("post");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
