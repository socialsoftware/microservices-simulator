package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionTopic;

import java.util.Set;

public class CreateQuestionCommand extends Command {
    private final String title;
    private final String content;
    private final QuestionCourse questionCourse;
    private final Set<Option> options;
    private final Set<QuestionTopic> topics;

    public CreateQuestionCommand(UnitOfWork unitOfWork, String serviceName,
                                 String title, String content,
                                 QuestionCourse questionCourse,
                                 Set<Option> options, Set<QuestionTopic> topics) {
        super(unitOfWork, serviceName, null);
        this.title = title;
        this.content = content;
        this.questionCourse = questionCourse;
        this.options = options;
        this.topics = topics;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public QuestionCourse getQuestionCourse() { return questionCourse; }
    public Set<Option> getOptions() { return options; }
    public Set<QuestionTopic> getTopics() { return topics; }
}
