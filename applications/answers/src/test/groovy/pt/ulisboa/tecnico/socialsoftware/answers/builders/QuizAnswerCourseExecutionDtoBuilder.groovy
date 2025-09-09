package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.QuizAnswerCourseExecutionDto
import java.time.LocalDateTime

class QuizAnswerCourseExecutionDtoBuilder extends SpockTest {
    private QuizAnswerCourseExecutionDto quizanswercourseexecutionDto

    static QuizAnswerCourseExecutionDtoBuilder aQuizAnswerCourseExecutionDto() {
        return new QuizAnswerCourseExecutionDtoBuilder()
    }

    QuizAnswerCourseExecutionDtoBuilder() {
        this.quizanswercourseexecutionDto = new QuizAnswerCourseExecutionDto()
        // Set default values
        this.quizanswercourseexecutionDto.setCourseExecutionAggregateId(1)
        this.quizanswercourseexecutionDto.setCourseExecutionName("Default courseExecutionName")
        this.quizanswercourseexecutionDto.setCourseExecutionAcronym("Default courseExecutionAcronym")
        this.quizanswercourseexecutionDto.setCourseExecutionAcademicTerm("Default courseExecutionAcademicTerm")
    }

    QuizAnswerCourseExecutionDtoBuilder withCourseExecutionAggregateId(Integer courseExecutionAggregateId) {
        this.quizanswercourseexecutionDto.setCourseExecutionAggregateId(courseExecutionAggregateId)
        return this
    }

    QuizAnswerCourseExecutionDtoBuilder withCourseExecutionName(String courseExecutionName) {
        this.quizanswercourseexecutionDto.setCourseExecutionName(courseExecutionName)
        return this
    }

    QuizAnswerCourseExecutionDtoBuilder withCourseExecutionAcronym(String courseExecutionAcronym) {
        this.quizanswercourseexecutionDto.setCourseExecutionAcronym(courseExecutionAcronym)
        return this
    }

    QuizAnswerCourseExecutionDtoBuilder withCourseExecutionAcademicTerm(String courseExecutionAcademicTerm) {
        this.quizanswercourseexecutionDto.setCourseExecutionAcademicTerm(courseExecutionAcademicTerm)
        return this
    }

    QuizAnswerCourseExecutionDto build() {
        return this.quizanswercourseexecutionDto
    }
}
