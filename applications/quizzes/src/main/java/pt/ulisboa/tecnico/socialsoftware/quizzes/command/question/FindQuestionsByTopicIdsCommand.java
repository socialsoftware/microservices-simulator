package pt.ulisboa.tecnico.socialsoftware.quizzes.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

import java.util.List;

public class FindQuestionsByTopicIdsCommand extends Command {
    private List<Integer> topicIds;

    public FindQuestionsByTopicIdsCommand(UnitOfWork unitOfWork, String serviceName, List<Integer> topicIds) {
        super(unitOfWork, serviceName, null);
        this.topicIds = topicIds;
    }

    public List<Integer> getTopicIds() {
        return topicIds;
    }
}
