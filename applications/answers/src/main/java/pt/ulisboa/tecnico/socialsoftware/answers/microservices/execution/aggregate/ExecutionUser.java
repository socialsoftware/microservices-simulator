package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;

@Entity
public class ExecutionUser {
    @Id
    @GeneratedValue
    private Long id;
    private String userName;
    private String userUsername;
    private boolean userActive;
    private Integer userAggregateId;
    private Integer userVersion;
    private AggregateState userState;
    @OneToOne
    private Execution execution;

    public ExecutionUser() {

    }

    public ExecutionUser(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserVersion(userDto.getVersion());
        setUserState(userDto.getState());
        setUserName(userDto.getName());
        setUserUsername(userDto.getUsername());
        setUserActive(userDto.getActive());
    }

    public ExecutionUser(ExecutionUser other) {
        setUserUsername(other.getUserUsername());
        setUserActive(other.getUserActive());
        setUserAggregateId(other.getUserAggregateId());
        setUserVersion(other.getUserVersion());
        setUserState(other.getUserState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public boolean getUserActive() {
        return userActive;
    }

    public void setUserActive(boolean userActive) {
        this.userActive = userActive;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Integer userVersion) {
        this.userVersion = userVersion;
    }

    public AggregateState getUserState() {
        return userState;
    }

    public void setUserState(AggregateState userState) {
        this.userState = userState;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }



    public ExecutionUserDto buildDto() {
        ExecutionUserDto dto = new ExecutionUserDto();
        dto.setName(getUserName());
        dto.setUsername(getUserUsername());
        dto.setActive(getUserActive());
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setState(getUserState());
        return dto;
    }
}