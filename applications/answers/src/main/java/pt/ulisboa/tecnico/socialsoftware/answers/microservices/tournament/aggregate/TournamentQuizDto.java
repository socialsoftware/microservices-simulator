package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TournamentQuizDto implements Serializable {
	private Integer quizAggregateId;
	private Integer quizVersion;
	private Tournament tournament;

	public TournamentQuizDto() {
	}

	public TournamentQuizDto(TournamentQuiz tournamentquiz) {
		this.quizAggregateId = tournamentquiz.getQuizAggregateId();
		this.quizVersion = tournamentquiz.getQuizVersion();
		this.tournament = tournamentquiz.getTournament();
	}

	public Integer getQuizAggregateId() {
		return quizAggregateId;
	}

	public void setQuizAggregateId(Integer quizAggregateId) {
		this.quizAggregateId = quizAggregateId;
	}

	public Integer getQuizVersion() {
		return quizVersion;
	}

	public void setQuizVersion(Integer quizVersion) {
		this.quizVersion = quizVersion;
	}

	public Tournament getTournament() {
		return tournament;
	}

	public void setTournament(Tournament tournament) {
		this.tournament = tournament;
	}

}