package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class CreateQuizAnswerCommand extends Command {
    private final Integer quizAggregateId;
    private final Long quizVersion;
    private final Integer userAggregateId;
    private final Long userVersion;
    private final String userName;
    private final String userUsername;
    private final Integer executionAggregateId;
    private final Long executionVersion;

    public CreateQuizAnswerCommand(UnitOfWork unitOfWork, String serviceName,
                                   Integer quizAggregateId, Long quizVersion,
                                   Integer userAggregateId, Long userVersion,
                                   String userName, String userUsername,
                                   Integer executionAggregateId, Long executionVersion) {
        super(unitOfWork, serviceName, null);
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
        this.userAggregateId = userAggregateId;
        this.userVersion = userVersion;
        this.userName = userName;
        this.userUsername = userUsername;
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
    }

    public Integer getQuizAggregateId() { return quizAggregateId; }
    public Long getQuizVersion() { return quizVersion; }
    public Integer getUserAggregateId() { return userAggregateId; }
    public Long getUserVersion() { return userVersion; }
    public String getUserName() { return userName; }
    public String getUserUsername() { return userUsername; }
    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public Long getExecutionVersion() { return executionVersion; }
}
