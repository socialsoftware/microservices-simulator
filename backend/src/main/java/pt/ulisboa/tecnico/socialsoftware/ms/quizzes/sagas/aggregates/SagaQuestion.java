package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import java.util.List;

@Entity
public class SagaQuestion extends Question implements SagaAggregate {
    @OneToOne
    private SagaState sagaState;
    
    public SagaQuestion() {
        super();
    }

    public SagaQuestion(Integer aggregateId, QuestionCourse questionCourse, QuestionDto questionDto, List<QuestionTopic> questionTopics) {
        super(aggregateId, questionCourse, questionDto, questionTopics);
    }

    public SagaQuestion(SagaQuestion other) {
        super(other);
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
    
}
