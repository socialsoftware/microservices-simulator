package pt.ulisboa.tecnico.socialsoftware.blcm.execution.domain;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.blcm.tournament.domain.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.blcm.user.dto.UserDto;

import java.util.Set;

@Embeddable
public class ExecutionStudent {
    private Integer userAggregateId;
    private Integer userVersion;
    private String name;
    private String username;
    private boolean active;
    private Aggregate.AggregateState state;

    public ExecutionStudent() {
    }

    public ExecutionStudent(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserVersion(userDto.getVersion());
        setName(userDto.getName());
        setUsername(userDto.getUsername());
        setActive(userDto.isActive());
        setState(Aggregate.AggregateState.valueOf(userDto.getState()));
    }

    public ExecutionStudent(ExecutionStudent other) {
        setUserAggregateId(other.getUserAggregateId());
        setUserVersion(other.getUserVersion());
        setName(other.getName());
        setUsername(other.getUsername());
        setActive(other.isActive());
        setState(other.getState());
    }

    public void anonymize() {
        setName("ANONYMOUS");
        setUsername("ANONYMOUS");
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String userName) {
        this.username = userName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Aggregate.AggregateState getState() {
        return state;
    }

    public void setState(Aggregate.AggregateState state) {
        this.state = state;
    }

    public UserDto buildDto() {
        UserDto userDto = new UserDto();
        userDto.setAggregateId(getUserAggregateId());
        userDto.setVersion(getUserVersion());
        userDto.setName(getName());
        userDto.setUsername(getUsername());
        return userDto;
    }

    public static void syncStudentVersions(Set<ExecutionStudent> prevStudents, Set<ExecutionStudent> v1Students, Set<ExecutionStudent> v2Students) {
        for(ExecutionStudent s1 : v1Students) {
            for(ExecutionStudent s2 : v2Students) {
                if(s1.getUserAggregateId().equals(s2.getUserAggregateId())) {
                    if(s1.getUserVersion() > s2.getUserVersion()) {
                        s2.setUserVersion(s1.getUserVersion());
                        s2.setName(s1.getName());
                        s2.setUsername(s1.getUsername());
                    }

                    if(s2.getUserVersion() > s1.getUserVersion()) {
                        s1.setUserVersion(s2.getUserVersion());
                        s1.setName(s2.getName());
                        s1.setUsername(s2.getUsername());
                    }
                }
            }

            // no need to check again because the prev does not contain any newer version than v1 an v2
            for(ExecutionStudent prevStudent : prevStudents) {
                if(s1.getUserAggregateId().equals(prevStudent.getUserAggregateId())) {
                    if(s1.getUserVersion() > prevStudent.getUserVersion()) {
                        prevStudent.setUserVersion(s1.getUserVersion());
                        prevStudent.setName(s1.getName());
                        prevStudent.setUsername(s1.getUsername());
                    }

                    if(prevStudent.getUserVersion() > s1.getUserVersion()) {
                        s1.setUserVersion(prevStudent.getUserVersion());
                        s1.setName(prevStudent.getName());
                        s1.setUsername(prevStudent.getUsername());
                    }
                }
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getUserAggregateId();
        hash = 31 * hash + (getUserVersion() == null ? 0 : getUserVersion().hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TournamentParticipant)) {
            return false;
        }
        TournamentParticipant tournamentParticipant = (TournamentParticipant) obj;
        return getUserAggregateId() != null && getUserAggregateId().equals(tournamentParticipant.getParticipantAggregateId()) &&
                getUserVersion() != null && getUserVersion().equals(tournamentParticipant.getParticipantVersion());
    }
}
