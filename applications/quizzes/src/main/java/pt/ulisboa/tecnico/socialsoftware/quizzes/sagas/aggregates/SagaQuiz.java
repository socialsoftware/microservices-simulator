package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizType;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
@Entity
public class SagaQuiz extends Quiz implements SagaAggregate {
    private SagaState sagaState;
    
    public SagaQuiz() {
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuiz(Integer aggregateId, QuizCourseExecution quizCourseExecution, Set<QuizQuestion> quizQuestions, QuizDto quizDto, QuizType quizType) {
        super(aggregateId, quizCourseExecution, quizQuestions, quizDto, quizType);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuiz(SagaQuiz other) {
        super(other);
        this.sagaState = other.getSagaState();
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
