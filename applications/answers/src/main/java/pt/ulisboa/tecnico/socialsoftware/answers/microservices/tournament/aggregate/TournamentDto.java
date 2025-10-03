package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TournamentDto implements Serializable {
	private Integer aggregateId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Integer numberOfQuestions;
	private boolean cancelled;
	private TournamentCreator tournamentCreator;
	private Set<TournamentParticipant> tournamentParticipants;
	private TournamentExecution tournamentExecution;
	private Set<TournamentTopic> tournamentTopics;
	private TournamentQuiz tournamentQuiz;
	private Integer version;
	private AggregateState state;

	public TournamentDto() {
	}

	public TournamentDto(Tournament tournament) {
		this.aggregateId = tournament.getAggregateId();
		this.startTime = tournament.getStartTime();
		this.endTime = tournament.getEndTime();
		this.numberOfQuestions = tournament.getNumberOfQuestions();
		this.cancelled = tournament.isCancelled();
		this.tournamentCreator = tournament.getTournamentCreator();
		this.tournamentParticipants = tournament.getTournamentParticipants();
		this.tournamentExecution = tournament.getTournamentExecution();
		this.tournamentTopics = tournament.getTournamentTopics();
		this.tournamentQuiz = tournament.getTournamentQuiz();
		this.version = tournament.getVersion();
		this.state = tournament.getState();
	}

	public Integer getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(Integer aggregateId) {
		this.aggregateId = aggregateId;
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

	public boolean isCancelled() {
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
	}

	public Set<TournamentParticipant> getTournamentParticipants() {
		return tournamentParticipants;
	}

	public void setTournamentParticipants(Set<TournamentParticipant> tournamentParticipants) {
		this.tournamentParticipants = tournamentParticipants;
	}

	public TournamentExecution getTournamentExecution() {
		return tournamentExecution;
	}

	public void setTournamentExecution(TournamentExecution tournamentExecution) {
		this.tournamentExecution = tournamentExecution;
	}

	public Set<TournamentTopic> getTournamentTopics() {
		return tournamentTopics;
	}

	public void setTournamentTopics(Set<TournamentTopic> tournamentTopics) {
		this.tournamentTopics = tournamentTopics;
	}

	public TournamentQuiz getTournamentQuiz() {
		return tournamentQuiz;
	}

	public void setTournamentQuiz(TournamentQuiz tournamentQuiz) {
		this.tournamentQuiz = tournamentQuiz;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public AggregateState getState() {
		return state;
	}

	public void setState(AggregateState state) {
		this.state = state;
	}
}