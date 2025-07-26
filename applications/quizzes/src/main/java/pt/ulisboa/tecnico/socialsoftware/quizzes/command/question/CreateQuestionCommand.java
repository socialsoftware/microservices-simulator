package pt.ulisboa.tecnico.socialsoftware.quizzes.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

import java.util.List;

public class CreateQuestionCommand extends Command {
    private QuestionCourse course;
    private QuestionDto questionDto;
    private List<TopicDto> topics;

    public CreateQuestionCommand(UnitOfWork unitOfWork, String serviceName, QuestionCourse course, QuestionDto questionDto, List<TopicDto> topics) {
        super(unitOfWork, serviceName, null);
        this.course = course;
        this.questionDto = questionDto;
        this.topics = topics;
    }

    public QuestionCourse getCourse() { return course; }
    public QuestionDto getQuestionDto() { return questionDto; }
    public List<TopicDto> getTopics() { return topics; }
}
