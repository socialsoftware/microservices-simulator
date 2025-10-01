package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Embeddable
public class TournamentCreator {
    private Long id;
    private Integer creatorAggregateId;
    private String creatorName;
    private String creatorUsername;
    private Integer creatorVersion;
    private AggregateState creatorState;
    private Object tournament; 

    public TournamentCreator(Long id, Integer creatorAggregateId, String creatorName, String creatorUsername, Integer creatorVersion, AggregateState creatorState, Object tournament) {
        this.id = id;
        this.creatorAggregateId = creatorAggregateId;
        this.creatorName = creatorName;
        this.creatorUsername = creatorUsername;
        this.creatorVersion = creatorVersion;
        this.creatorState = creatorState;
        this.tournament = tournament;
    }

    public TournamentCreator(TournamentCreator other) {
        // Copy constructor
    }
	public TournamentCreator(Integer userId) {
		setCreatorAggregateId(userId);
		            setCreatorName('Default Name');
		            setCreatorUsername('Default Username');
		            setCreatorVersion(1);
	}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCreatorAggregateId() {
        return creatorAggregateId;
    }

    public void setCreatorAggregateId(Integer creatorAggregateId) {
        this.creatorAggregateId = creatorAggregateId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public Integer getCreatorVersion() {
        return creatorVersion;
    }

    public void setCreatorVersion(Integer creatorVersion) {
        this.creatorVersion = creatorVersion;
    }

    public AggregateState getCreatorState() {
        return creatorState;
    }

    public void setCreatorState(AggregateState creatorState) {
        this.creatorState = creatorState;
    }

    public Object getTournament() {
        return tournament;
    }

    public void setTournament(Object tournament) {
        this.tournament = tournament;
    }
	public String buildDto() {
		return 'User: ' + getCreatorAggregateId();
	}

}