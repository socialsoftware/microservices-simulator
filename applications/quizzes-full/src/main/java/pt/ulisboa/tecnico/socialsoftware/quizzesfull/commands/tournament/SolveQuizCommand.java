package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class SolveQuizCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer userAggregateId;
    private final Integer quizAnswerAggregateId;
    private final Long quizAnswerVersion;

    public SolveQuizCommand(UnitOfWork unitOfWork, String serviceName,
                             Integer tournamentAggregateId, Integer userAggregateId,
                             Integer quizAnswerAggregateId, Long quizAnswerVersion) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.userAggregateId = userAggregateId;
        this.quizAnswerAggregateId = quizAnswerAggregateId;
        this.quizAnswerVersion = quizAnswerVersion;
    }

    public Integer getTournamentAggregateId() { return tournamentAggregateId; }
    public Integer getUserAggregateId() { return userAggregateId; }
    public Integer getQuizAnswerAggregateId() { return quizAnswerAggregateId; }
    public Long getQuizAnswerVersion() { return quizAnswerVersion; }
}
