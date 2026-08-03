package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate;

public class UserDto {
    private Integer aggregateId;
    private Long version;
    private String name;
    private String username;
    private Role role;
    private Boolean active;

    public UserDto() {
    }

    public UserDto(Integer aggregateId, Long version, String name, String username, Role role, Boolean active) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.name = name;
        this.username = username;
        this.role = role;
        this.active = active;
    }

    public UserDto(User user) {
        this.aggregateId = user.getAggregateId();
        this.version = user.getVersion();
        this.name = user.getName();
        this.username = user.getUsername();
        this.role = user.getRole();
        this.active = user.isActive();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
