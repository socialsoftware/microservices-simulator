package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class UpdateParticipantAnswerCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer studentAggregateId;
    private final Integer quizAnswerAggregateId;
    private final Integer questionAggregateId;
    private final boolean correct;
    private final Long eventVersion;

    public UpdateParticipantAnswerCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
            Integer studentAggregateId, Integer quizAnswerAggregateId, Integer questionAggregateId, boolean correct,
            Long eventVersion) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.studentAggregateId = studentAggregateId;
        this.quizAnswerAggregateId = quizAnswerAggregateId;
        this.questionAggregateId = questionAggregateId;
        this.correct = correct;
        this.eventVersion = eventVersion;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public Integer getQuizAnswerAggregateId() {
        return quizAnswerAggregateId;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public boolean isCorrect() {
        return correct;
    }

    public Long getEventVersion() {
        return eventVersion;
    }
}
