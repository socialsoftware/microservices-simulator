package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class UpdateQuestionCommand extends Command {
    private final Integer quizAggregateId;
    private final Integer questionAggregateId;
    private final String title;
    private final String content;
    private final Integer aggregateVersion;

    public UpdateQuestionCommand(UnitOfWork unitOfWork,
            String serviceName,
            Integer quizAggregateId,
            Integer questionAggregateId,
            String title,
            String content,
            Integer aggregateVersion) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.questionAggregateId = questionAggregateId;
        this.title = title;
        this.content = content;
        this.aggregateVersion = aggregateVersion;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Integer getAggregateVersion() {
        return aggregateVersion;
    }
}
