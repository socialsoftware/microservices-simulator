package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TournamentParticipantQuizAnswerDto implements Serializable {
	private Integer quizAnswerAggregateId;
	private Integer quizAnswerVersion;
	private boolean answered;
	private Integer numberOfAnswered;
	private Integer numberOfCorrect;
	private Object tournamentParticipant;

	public TournamentParticipantQuizAnswerDto() {
	}

	public TournamentParticipantQuizAnswerDto(TournamentParticipantQuizAnswer tournamentparticipantquizanswer) {
		this.quizAnswerAggregateId = tournamentparticipantquizanswer.getQuizAnswerAggregateId();
		this.quizAnswerVersion = tournamentparticipantquizanswer.getQuizAnswerVersion();
		this.answered = tournamentparticipantquizanswer.isAnswered();
		this.numberOfAnswered = tournamentparticipantquizanswer.getNumberOfAnswered();
		this.numberOfCorrect = tournamentparticipantquizanswer.getNumberOfCorrect();
		this.tournamentParticipant = tournamentparticipantquizanswer.getTournamentParticipant();
	}

	public Integer getQuizAnswerAggregateId() {
		return quizAnswerAggregateId;
	}

	public void setQuizAnswerAggregateId(Integer quizAnswerAggregateId) {
		this.quizAnswerAggregateId = quizAnswerAggregateId;
	}

	public Integer getQuizAnswerVersion() {
		return quizAnswerVersion;
	}

	public void setQuizAnswerVersion(Integer quizAnswerVersion) {
		this.quizAnswerVersion = quizAnswerVersion;
	}

	public boolean isAnswered() {
		return answered;
	}

	public void setAnswered(Boolean answered) {
		this.answered = answered;
	}

	public Integer getNumberOfAnswered() {
		return numberOfAnswered;
	}

	public void setNumberOfAnswered(Integer numberOfAnswered) {
		this.numberOfAnswered = numberOfAnswered;
	}

	public Integer getNumberOfCorrect() {
		return numberOfCorrect;
	}

	public void setNumberOfCorrect(Integer numberOfCorrect) {
		this.numberOfCorrect = numberOfCorrect;
	}

	public Object getTournamentParticipant() {
		return tournamentParticipant;
	}

	public void setTournamentParticipant(Object tournamentParticipant) {
		this.tournamentParticipant = tournamentParticipant;
	}

}