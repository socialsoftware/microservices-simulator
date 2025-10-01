package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.CourseExecutionStudentDto
import java.time.LocalDateTime

class CourseExecutionStudentDtoBuilder extends SpockTest {
    private CourseExecutionStudentDto courseexecutionstudentDto

    static CourseExecutionStudentDtoBuilder aCourseExecutionStudentDto() {
        return new CourseExecutionStudentDtoBuilder()
    }

    CourseExecutionStudentDtoBuilder() {
        this.courseexecutionstudentDto = new CourseExecutionStudentDto()
        // Set default values
        this.courseexecutionstudentDto.setStudentAggregateId(1)
        this.courseexecutionstudentDto.setStudentName("Default studentName")
        this.courseexecutionstudentDto.setStudentUsername("Default studentUsername")
        this.courseexecutionstudentDto.setStudentEmail("Default studentEmail")
        this.courseexecutionstudentDto.setEnrollmentDate(LocalDateTime.now())
    }

    CourseExecutionStudentDtoBuilder withStudentAggregateId(Integer studentAggregateId) {
        this.courseexecutionstudentDto.setStudentAggregateId(studentAggregateId)
        return this
    }

    CourseExecutionStudentDtoBuilder withStudentName(String studentName) {
        this.courseexecutionstudentDto.setStudentName(studentName)
        return this
    }

    CourseExecutionStudentDtoBuilder withStudentUsername(String studentUsername) {
        this.courseexecutionstudentDto.setStudentUsername(studentUsername)
        return this
    }

    CourseExecutionStudentDtoBuilder withStudentEmail(String studentEmail) {
        this.courseexecutionstudentDto.setStudentEmail(studentEmail)
        return this
    }

    CourseExecutionStudentDtoBuilder withEnrollmentDate(LocalDateTime enrollmentDate) {
        this.courseexecutionstudentDto.setEnrollmentDate(enrollmentDate)
        return this
    }

    CourseExecutionStudentDto build() {
        return this.courseexecutionstudentDto
    }
}
