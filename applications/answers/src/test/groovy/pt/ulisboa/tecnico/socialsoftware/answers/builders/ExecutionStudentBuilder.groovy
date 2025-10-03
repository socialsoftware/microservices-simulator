package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.ExecutionStudent
import java.time.LocalDateTime

class ExecutionStudentBuilder extends SpockTest {
    private ExecutionStudent executionstudent

    static ExecutionStudentBuilder aExecutionStudent() {
        return new ExecutionStudentBuilder()
    }

    ExecutionStudentBuilder() {
        this.executionstudent = new ExecutionStudent()
        // Set default values
        this.executionstudent.setId(1L)
        this.executionstudent.setVersion(1)
        this.executionstudent.setStudentAggregateId(1)
        this.executionstudent.setStudentName("Default studentName")
        this.executionstudent.setStudentUsername("Default studentUsername")
        this.executionstudent.setStudentEmail("Default studentEmail")
        this.executionstudent.setEnrollmentDate(LocalDateTime.now())
    }

    ExecutionStudentBuilder withStudentAggregateId(Integer studentAggregateId) {
        this.executionstudent.setStudentAggregateId(studentAggregateId)
        return this
    }

    ExecutionStudentBuilder withStudentName(String studentName) {
        this.executionstudent.setStudentName(studentName)
        return this
    }

    ExecutionStudentBuilder withStudentUsername(String studentUsername) {
        this.executionstudent.setStudentUsername(studentUsername)
        return this
    }

    ExecutionStudentBuilder withStudentEmail(String studentEmail) {
        this.executionstudent.setStudentEmail(studentEmail)
        return this
    }

    ExecutionStudentBuilder withEnrollmentDate(LocalDateTime enrollmentDate) {
        this.executionstudent.setEnrollmentDate(enrollmentDate)
        return this
    }

    ExecutionStudent build() {
        return this.executionstudent
    }
}
