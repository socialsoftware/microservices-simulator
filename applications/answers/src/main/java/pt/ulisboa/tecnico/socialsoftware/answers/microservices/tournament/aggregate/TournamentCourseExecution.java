package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Embeddable
public class TournamentCourseExecution {
    private Long id;
    private Integer courseExecutionAggregateId;
    private Integer courseExecutionCourseId;
    private String courseExecutionAcronym;
    private String courseExecutionStatus;
    private Integer courseExecutionVersion;
    private Object tournament; 

    public TournamentCourseExecution(Long id, Integer courseExecutionAggregateId, Integer courseExecutionCourseId, String courseExecutionAcronym, String courseExecutionStatus, Integer courseExecutionVersion, Object tournament) {
        this.id = id;
        this.courseExecutionAggregateId = courseExecutionAggregateId;
        this.courseExecutionCourseId = courseExecutionCourseId;
        this.courseExecutionAcronym = courseExecutionAcronym;
        this.courseExecutionStatus = courseExecutionStatus;
        this.courseExecutionVersion = courseExecutionVersion;
        this.tournament = tournament;
    }

    public TournamentCourseExecution(TournamentCourseExecution other) {
        // Copy constructor
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }

    public void setCourseExecutionAggregateId(Integer courseExecutionAggregateId) {
        this.courseExecutionAggregateId = courseExecutionAggregateId;
    }

    public Integer getCourseExecutionCourseId() {
        return courseExecutionCourseId;
    }

    public void setCourseExecutionCourseId(Integer courseExecutionCourseId) {
        this.courseExecutionCourseId = courseExecutionCourseId;
    }

    public String getCourseExecutionAcronym() {
        return courseExecutionAcronym;
    }

    public void setCourseExecutionAcronym(String courseExecutionAcronym) {
        this.courseExecutionAcronym = courseExecutionAcronym;
    }

    public String getCourseExecutionStatus() {
        return courseExecutionStatus;
    }

    public void setCourseExecutionStatus(String courseExecutionStatus) {
        this.courseExecutionStatus = courseExecutionStatus;
    }

    public Integer getCourseExecutionVersion() {
        return courseExecutionVersion;
    }

    public void setCourseExecutionVersion(Integer courseExecutionVersion) {
        this.courseExecutionVersion = courseExecutionVersion;
    }

    public Object getTournament() {
        return tournament;
    }

    public void setTournament(Object tournament) {
        this.tournament = tournament;
    }


}