package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;

@Entity
public abstract class Tournament extends Aggregate {
    @Id
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentCreator tournamentCreator;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "tournament")
    private Set<TournamentParticipant> tournamentParticipants = new HashSet<>();
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentExecution tournamentExecution;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "tournament")
    private Set<TournamentTopic> tournamentTopics = new HashSet<>();
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentQuiz tournamentQuiz; 

    public Tournament() {
    }

    public Tournament(Integer aggregateId, TournamentDto tournamentDto, TournamentCreator tournamentCreator, TournamentExecution tournamentExecution, TournamentQuiz tournamentQuiz) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setStartTime(tournamentDto.getStartTime());
        setEndTime(tournamentDto.getEndTime());
        setNumberOfQuestions(tournamentDto.getNumberOfQuestions());
        setCancelled(tournamentDto.getCancelled());
        setTournamentCreator(tournamentCreator);
        setTournamentExecution(tournamentExecution);
        setTournamentQuiz(tournamentQuiz);
    }

    public Tournament(Tournament other) {
        super(other);
        setStartTime(other.getStartTime());
        setEndTime(other.getEndTime());
        setNumberOfQuestions(other.getNumberOfQuestions());
        setCancelled(other.getCancelled());
        setTournamentCreator(new TournamentCreator(other.getTournamentCreator()));
        setTournamentParticipants(other.getTournamentParticipants().stream().map(TournamentParticipant::new).collect(Collectors.toSet()));
        setTournamentExecution(new TournamentExecution(other.getTournamentExecution()));
        setTournamentTopics(other.getTournamentTopics().stream().map(TournamentTopic::new).collect(Collectors.toSet()));
        setTournamentQuiz(new TournamentQuiz(other.getTournamentQuiz()));
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

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public TournamentCreator getTournamentCreator() {
        return tournamentCreator;
    }

    public void setTournamentCreator(TournamentCreator tournamentCreator) {
        this.tournamentCreator = tournamentCreator;
        if (this.tournamentCreator != null) {
            this.tournamentCreator.setTournament(this);
        }
    }

    public Set<TournamentParticipant> getTournamentParticipants() {
        return tournamentParticipants;
    }

    public void setTournamentParticipants(Set<TournamentParticipant> tournamentParticipants) {
        this.tournamentParticipants = tournamentParticipants;
        if (this.tournamentParticipants != null) {
            this.tournamentParticipants.forEach(tournamentParticipant -> tournamentParticipant.setTournament(this));
        }
    }

    public TournamentExecution getTournamentExecution() {
        return tournamentExecution;
    }

    public void setTournamentExecution(TournamentExecution tournamentExecution) {
        this.tournamentExecution = tournamentExecution;
        if (this.tournamentExecution != null) {
            this.tournamentExecution.setTournament(this);
        }
    }

    public Set<TournamentTopic> getTournamentTopics() {
        return tournamentTopics;
    }

    public void setTournamentTopics(Set<TournamentTopic> tournamentTopics) {
        this.tournamentTopics = tournamentTopics;
        if (this.tournamentTopics != null) {
            this.tournamentTopics.forEach(tournamentTopic -> tournamentTopic.setTournament(this));
        }
    }

    public TournamentQuiz getTournamentQuiz() {
        return tournamentQuiz;
    }

    public void setTournamentQuiz(TournamentQuiz tournamentQuiz) {
        this.tournamentQuiz = tournamentQuiz;
        if (this.tournamentQuiz != null) {
            this.tournamentQuiz.setTournament(this);
        }
    }

	public void addParticipant(TournamentParticipant participant) {
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

	public TournamentParticipant findParticipant(Integer userAggregateId) {
		return this.tournamentParticipants.stream()
		                   .filter(p -> p.getParticipantAggregateId().equals(userAggregateId))
		                   .findFirst()
		                   .orElse(null);
	}

	public Boolean removeParticipant(TournamentParticipant participant) {
		Tournament prev = (Tournament) getPrev();
		               if (prev != null) {
		                   if ((prev.getStartTime() != null && DateHandler.now().isAfter(prev.getStartTime())) || prev.isCancelled()) {
		                       throw new ProjectException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
		                   }
		               }
		               return this.tournamentParticipants.remove(participant);
	}

	public TournamentTopic findTopic(Integer topicAggregateId) {
		return getTournamentTopics().stream()
		                   .filter(t -> topicAggregateId.equals(t.getTopicAggregateId()))
		                   .findFirst()
		                   .orElse(null);
	}

	public void removeTopic(TournamentTopic tournamentTopic) {
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