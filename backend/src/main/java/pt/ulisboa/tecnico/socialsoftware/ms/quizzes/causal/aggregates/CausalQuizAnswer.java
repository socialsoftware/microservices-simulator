package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.AnswerCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.AnswerStudent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.AnsweredQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;

@Entity
public class CausalQuizAnswer extends QuizAnswer implements CausalAggregate {
    public CausalQuizAnswer() {
        super();
    }

    public CausalQuizAnswer(Integer aggregateId, AnswerCourseExecution answerCourseExecution, AnswerStudent answerStudent, AnsweredQuiz answeredQuiz) {
        super(aggregateId, answerCourseExecution, answerStudent, answeredQuiz);
    }

    public CausalQuizAnswer(CausalQuizAnswer other) {
        super(other);
    }
}
