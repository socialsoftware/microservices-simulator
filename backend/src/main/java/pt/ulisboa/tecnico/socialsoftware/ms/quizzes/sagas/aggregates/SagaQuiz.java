package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizType;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.Set;

@Entity
public class SagaQuiz extends Quiz implements SagaAggregate {
    @OneToOne
    private SagaState sagaState;
    
    public SagaQuiz() {
    }

    public SagaQuiz(Integer aggregateId, QuizCourseExecution quizCourseExecution, Set<QuizQuestion> quizQuestions, QuizDto quizDto, QuizType quizType) {
        super(aggregateId, quizCourseExecution, quizQuestions, quizDto, quizType);
    }

    public SagaQuiz(SagaQuiz other) {
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
