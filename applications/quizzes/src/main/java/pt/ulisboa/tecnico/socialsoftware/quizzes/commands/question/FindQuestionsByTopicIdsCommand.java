package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.question;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

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
