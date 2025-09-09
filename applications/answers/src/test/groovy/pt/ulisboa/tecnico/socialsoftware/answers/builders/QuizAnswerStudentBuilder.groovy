package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.QuizAnswerStudent
import java.time.LocalDateTime

class QuizAnswerStudentBuilder extends SpockTest {
    private QuizAnswerStudent quizanswerstudent

    static QuizAnswerStudentBuilder aQuizAnswerStudent() {
        return new QuizAnswerStudentBuilder()
    }

    QuizAnswerStudentBuilder() {
        this.quizanswerstudent = new QuizAnswerStudent()
        // Set default values
        this.quizanswerstudent.setId(1L)
        this.quizanswerstudent.setVersion(1)
        this.quizanswerstudent.setStudentAggregateId(1)
        this.quizanswerstudent.setStudentName("Default studentName")
        this.quizanswerstudent.setStudentUsername("Default studentUsername")
        this.quizanswerstudent.setStudentEmail("Default studentEmail")
    }

    QuizAnswerStudentBuilder withStudentAggregateId(Integer studentAggregateId) {
        this.quizanswerstudent.setStudentAggregateId(studentAggregateId)
        return this
    }

    QuizAnswerStudentBuilder withStudentName(String studentName) {
        this.quizanswerstudent.setStudentName(studentName)
        return this
    }

    QuizAnswerStudentBuilder withStudentUsername(String studentUsername) {
        this.quizanswerstudent.setStudentUsername(studentUsername)
        return this
    }

    QuizAnswerStudentBuilder withStudentEmail(String studentEmail) {
        this.quizanswerstudent.setStudentEmail(studentEmail)
        return this
    }

    QuizAnswerStudent build() {
        return this.quizanswerstudent
    }
}
