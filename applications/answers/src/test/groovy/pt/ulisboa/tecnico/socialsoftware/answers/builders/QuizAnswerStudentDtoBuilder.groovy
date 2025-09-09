package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.QuizAnswerStudentDto
import java.time.LocalDateTime

class QuizAnswerStudentDtoBuilder extends SpockTest {
    private QuizAnswerStudentDto quizanswerstudentDto

    static QuizAnswerStudentDtoBuilder aQuizAnswerStudentDto() {
        return new QuizAnswerStudentDtoBuilder()
    }

    QuizAnswerStudentDtoBuilder() {
        this.quizanswerstudentDto = new QuizAnswerStudentDto()
        // Set default values
        this.quizanswerstudentDto.setStudentAggregateId(1)
        this.quizanswerstudentDto.setStudentName("Default studentName")
        this.quizanswerstudentDto.setStudentUsername("Default studentUsername")
        this.quizanswerstudentDto.setStudentEmail("Default studentEmail")
    }

    QuizAnswerStudentDtoBuilder withStudentAggregateId(Integer studentAggregateId) {
        this.quizanswerstudentDto.setStudentAggregateId(studentAggregateId)
        return this
    }

    QuizAnswerStudentDtoBuilder withStudentName(String studentName) {
        this.quizanswerstudentDto.setStudentName(studentName)
        return this
    }

    QuizAnswerStudentDtoBuilder withStudentUsername(String studentUsername) {
        this.quizanswerstudentDto.setStudentUsername(studentUsername)
        return this
    }

    QuizAnswerStudentDtoBuilder withStudentEmail(String studentEmail) {
        this.quizanswerstudentDto.setStudentEmail(studentEmail)
        return this
    }

    QuizAnswerStudentDto build() {
        return this.quizanswerstudentDto
    }
}
