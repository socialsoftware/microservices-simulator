package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

@Entity
public class Tournament extends Aggregate {
    @Id
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    private Object tournamentCreator;
    private Object tournamentParticipants;
    private Object tournamentCourseExecution;
    private Object tournamentTopics;
    private Object tournamentQuiz; 

    public Tournament(LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions, Boolean cancelled, Object tournamentCreator, Object tournamentParticipants, Object tournamentCourseExecution, Object tournamentTopics, Object tournamentQuiz) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfQuestions = numberOfQuestions;
        this.cancelled = cancelled;
        this.tournamentCreator = tournamentCreator;
        this.tournamentParticipants = tournamentParticipants;
        this.tournamentCourseExecution = tournamentCourseExecution;
        this.tournamentTopics = tournamentTopics;
        this.tournamentQuiz = tournamentQuiz;
    }

    public Tournament(Tournament other) {
        // Copy constructor
    }


    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Object getTournamentCreator() {
        return tournamentCreator;
    }

    public void setTournamentCreator(Object tournamentCreator) {
        this.tournamentCreator = tournamentCreator;
    }

    public Object getTournamentParticipants() {
        return tournamentParticipants;
    }

    public void setTournamentParticipants(Object tournamentParticipants) {
        this.tournamentParticipants = tournamentParticipants;
    }

    public Object getTournamentCourseExecution() {
        return tournamentCourseExecution;
    }

    public void setTournamentCourseExecution(Object tournamentCourseExecution) {
        this.tournamentCourseExecution = tournamentCourseExecution;
    }

    public Object getTournamentTopics() {
        return tournamentTopics;
    }

    public void setTournamentTopics(Object tournamentTopics) {
        this.tournamentTopics = tournamentTopics;
    }

    public Object getTournamentQuiz() {
        return tournamentQuiz;
    }

    public void setTournamentQuiz(Object tournamentQuiz) {
        this.tournamentQuiz = tournamentQuiz;
    }
	public void addParticipant(Object participant) {
		Tournament prev = (Tournament) getPrev();
		               if (DateHandler.now().isAfter(prev.getStartTime())) {
		                   throw new ProjectException(CANNOT_ADD_PARTICIPANT, getAggregateId());
		               }
		               if (prev != null && prev.isCancelled()) {
		                   throw new ProjectException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
		               }
		               this.tournamentParticipants.add(participant);
		               participant.setTournament(this);
	}

	public Object findParticipant(Integer userAggregateId) {
		return this.tournamentParticipants.stream()
		                   .filter(p -> p.getParticipantAggregateId().equals(userAggregateId))
		                   .findFirst()
		                   .orElse(null);
	}

	public Boolean removeParticipant(Object participant) {
		Tournament prev = (Tournament) getPrev();
		               if (prev != null) {
		                   if ((prev.getStartTime() != null && DateHandler.now().isAfter(prev.getStartTime())) || prev.isCancelled()) {
		                       throw new ProjectException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
		                   }
		               }
		               return this.tournamentParticipants.remove(participant);
	}

	public Object findTopic(Integer topicAggregateId) {
		return getTournamentTopics().stream()
		                   .filter(t -> topicAggregateId.equals(t.getTopicAggregateId()))
		                   .findFirst()
		                   .orElse(null);
	}

	public void removeTopic(Object tournamentTopic) {
		this.tournamentTopics.remove(tournamentTopic);
	}

	public void cancel() {
		this.cancelled = true;
	}

	public void remove() {
		if (getTournamentParticipants().size() > 0) {
		                   throw new ProjectException(CANNOT_DELETE_TOURNAMENT, getAggregateId());
		               }
		               super.remove();
	}

	public void setVersion(Integer version) {
		if (this.tournamentQuiz.getQuizVersion() == null) {
		                   this.tournamentQuiz.setQuizVersion(version);
		               }
		               super.setVersion(version);
	}

}