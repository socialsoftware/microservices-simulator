package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaQuestion;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Option;
import java.util.Set;
import java.util.List;

@Service
@Profile("sagas")
public class SagasQuestionFactory implements QuestionFactory {
    @Override
    public Question createQuestion(Integer aggregateId, QuestionCourse course, QuestionDto questionDto, Set<QuestionTopic> topics, List<Option> options) {
        return new SagaQuestion(aggregateId, course, questionDto, topics, options);
    }

    @Override
    public Question createQuestionFromExisting(Question existingQuestion) {
        return new SagaQuestion((SagaQuestion) existingQuestion);
    }

    @Override
    public QuestionDto createQuestionDto(Question question) {
        return new SagaQuestionDto(question);
    }
}