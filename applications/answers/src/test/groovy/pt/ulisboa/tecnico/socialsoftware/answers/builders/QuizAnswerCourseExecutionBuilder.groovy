package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.QuizAnswerCourseExecution
import java.time.LocalDateTime

class QuizAnswerCourseExecutionBuilder extends SpockTest {
    private QuizAnswerCourseExecution quizanswercourseexecution

    static QuizAnswerCourseExecutionBuilder aQuizAnswerCourseExecution() {
        return new QuizAnswerCourseExecutionBuilder()
    }

    QuizAnswerCourseExecutionBuilder() {
        this.quizanswercourseexecution = new QuizAnswerCourseExecution()
        // Set default values
        this.quizanswercourseexecution.setId(1L)
        this.quizanswercourseexecution.setVersion(1)
        this.quizanswercourseexecution.setCourseExecutionAggregateId(1)
        this.quizanswercourseexecution.setCourseExecutionName("Default courseExecutionName")
        this.quizanswercourseexecution.setCourseExecutionAcronym("Default courseExecutionAcronym")
        this.quizanswercourseexecution.setCourseExecutionAcademicTerm("Default courseExecutionAcademicTerm")
    }

    QuizAnswerCourseExecutionBuilder withCourseExecutionAggregateId(Integer courseExecutionAggregateId) {
        this.quizanswercourseexecution.setCourseExecutionAggregateId(courseExecutionAggregateId)
        return this
    }

    QuizAnswerCourseExecutionBuilder withCourseExecutionName(String courseExecutionName) {
        this.quizanswercourseexecution.setCourseExecutionName(courseExecutionName)
        return this
    }

    QuizAnswerCourseExecutionBuilder withCourseExecutionAcronym(String courseExecutionAcronym) {
        this.quizanswercourseexecution.setCourseExecutionAcronym(courseExecutionAcronym)
        return this
    }

    QuizAnswerCourseExecutionBuilder withCourseExecutionAcademicTerm(String courseExecutionAcademicTerm) {
        this.quizanswercourseexecution.setCourseExecutionAcademicTerm(courseExecutionAcademicTerm)
        return this
    }

    QuizAnswerCourseExecution build() {
        return this.quizanswercourseexecution
    }
}
