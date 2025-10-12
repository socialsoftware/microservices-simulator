package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;

@Entity
public abstract class Tournament extends Aggregate {
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
            this.tournamentParticipants.forEach(item -> item.setTournament(this));
        }
    }

    public void addTournamentParticipant(TournamentParticipant tournamentParticipant) {
        if (this.tournamentParticipants == null) {
            this.tournamentParticipants = new HashSet<>();
        }
        this.tournamentParticipants.add(tournamentParticipant);
        if (tournamentParticipant != null) {
            tournamentParticipant.setTournament(this);
        }
    }

    public void removeTournamentParticipant(Long id) {
        if (this.tournamentParticipants != null) {
            this.tournamentParticipants.removeIf(item -> 
                item.getId() != null && item.getId().equals(id));
        }
    }

    public boolean containsTournamentParticipant(Long id) {
        if (this.tournamentParticipants == null) {
            return false;
        }
        return this.tournamentParticipants.stream().anyMatch(item -> 
            item.getId() != null && item.getId().equals(id));
    }

    public TournamentParticipant findTournamentParticipantById(Long id) {
        if (this.tournamentParticipants == null) {
            return null;
        }
        return this.tournamentParticipants.stream()
            .filter(item -> item.getId() != null && item.getId().equals(id))
            .findFirst()
            .orElse(null);
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
            this.tournamentTopics.forEach(item -> item.setTournament(this));
        }
    }

    public void addTournamentTopic(TournamentTopic tournamentTopic) {
        if (this.tournamentTopics == null) {
            this.tournamentTopics = new HashSet<>();
        }
        this.tournamentTopics.add(tournamentTopic);
        if (tournamentTopic != null) {
            tournamentTopic.setTournament(this);
        }
    }

    public void removeTournamentTopic(Long id) {
        if (this.tournamentTopics != null) {
            this.tournamentTopics.removeIf(item -> 
                item.getId() != null && item.getId().equals(id));
        }
    }

    public boolean containsTournamentTopic(Long id) {
        if (this.tournamentTopics == null) {
            return false;
        }
        return this.tournamentTopics.stream().anyMatch(item -> 
            item.getId() != null && item.getId().equals(id));
    }

    public TournamentTopic findTournamentTopicById(Long id) {
        if (this.tournamentTopics == null) {
            return null;
        }
        return this.tournamentTopics.stream()
            .filter(item -> item.getId() != null && item.getId().equals(id))
            .findFirst()
            .orElse(null);
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



    // ============================================================================
    // INVARIANTS
    // ============================================================================

    public boolean invariantStartTimeBeforeEndTime() {
        return this.startTime.isBefore(this.endTime);
    }

    public boolean invariantUniqueParticipant() {
        return this.tournamentParticipants.stream().map(item -> item.get${capitalize(participantAggregateId)}()).distinct().count() == this.tournamentParticipants.size();
    }

    public boolean invariantParticipantsEnrolledBeforeStartTime() {
        return forall p : tournamentParticipants | p.this.participantEnrollTime != null;
    }

    public boolean invariantAnswerBeforeStart() {
        return forall p : tournamentParticipants | p.this.tournamentParticipantQuizAnswer != null;
    }

    public boolean invariantDeleteWhenNoParticipants() {
        return this.tournamentParticipants != null;
    }

    public boolean invariantCreatorParticipantConsistency() {
        return tournamentParticipants.noneMatch(p -> p.this.participantAggregateId == tournamentCreator.creatorAggregateId);
    }

    public boolean invariantCreatorIsNotAnonymous() {
        return tournamentCreator.creatorName != 'ANONYMOUS' &&
               tournamentCreator.creatorUsername != 'ANONYMOUS';
    }
    @Override
    public void verifyInvariants() {
        if (!(invariantStartTimeBeforeEndTime()
               && invariantUniqueParticipant()
               && invariantParticipantsEnrolledBeforeStartTime()
               && invariantAnswerBeforeStart()
               && invariantDeleteWhenNoParticipants()
               && invariantCreatorParticipantConsistency()
               && invariantCreatorIsNotAnonymous())) {
            throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
        }
    }
}