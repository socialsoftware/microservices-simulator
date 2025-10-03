package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.ExecutionStudentDto
import java.time.LocalDateTime

class ExecutionStudentDtoBuilder extends SpockTest {
    private ExecutionStudentDto executionstudentDto

    static ExecutionStudentDtoBuilder aExecutionStudentDto() {
        return new ExecutionStudentDtoBuilder()
    }

    ExecutionStudentDtoBuilder() {
        this.executionstudentDto = new ExecutionStudentDto()
        // Set default values
        this.executionstudentDto.setStudentAggregateId(1)
        this.executionstudentDto.setStudentName("Default studentName")
        this.executionstudentDto.setStudentUsername("Default studentUsername")
        this.executionstudentDto.setStudentEmail("Default studentEmail")
        this.executionstudentDto.setEnrollmentDate(LocalDateTime.now())
    }

    ExecutionStudentDtoBuilder withStudentAggregateId(Integer studentAggregateId) {
        this.executionstudentDto.setStudentAggregateId(studentAggregateId)
        return this
    }

    ExecutionStudentDtoBuilder withStudentName(String studentName) {
        this.executionstudentDto.setStudentName(studentName)
        return this
    }

    ExecutionStudentDtoBuilder withStudentUsername(String studentUsername) {
        this.executionstudentDto.setStudentUsername(studentUsername)
        return this
    }

    ExecutionStudentDtoBuilder withStudentEmail(String studentEmail) {
        this.executionstudentDto.setStudentEmail(studentEmail)
        return this
    }

    ExecutionStudentDtoBuilder withEnrollmentDate(LocalDateTime enrollmentDate) {
        this.executionstudentDto.setEnrollmentDate(enrollmentDate)
        return this
    }

    ExecutionStudentDto build() {
        return this.executionstudentDto
    }
}
