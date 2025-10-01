package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.CourseExecutionStudent
import java.time.LocalDateTime

class CourseExecutionStudentBuilder extends SpockTest {
    private CourseExecutionStudent courseexecutionstudent

    static CourseExecutionStudentBuilder aCourseExecutionStudent() {
        return new CourseExecutionStudentBuilder()
    }

    CourseExecutionStudentBuilder() {
        this.courseexecutionstudent = new CourseExecutionStudent()
        // Set default values
        this.courseexecutionstudent.setId(1L)
        this.courseexecutionstudent.setVersion(1)
        this.courseexecutionstudent.setStudentAggregateId(1)
        this.courseexecutionstudent.setStudentName("Default studentName")
        this.courseexecutionstudent.setStudentUsername("Default studentUsername")
        this.courseexecutionstudent.setStudentEmail("Default studentEmail")
        this.courseexecutionstudent.setEnrollmentDate(LocalDateTime.now())
    }

    CourseExecutionStudentBuilder withStudentAggregateId(Integer studentAggregateId) {
        this.courseexecutionstudent.setStudentAggregateId(studentAggregateId)
        return this
    }

    CourseExecutionStudentBuilder withStudentName(String studentName) {
        this.courseexecutionstudent.setStudentName(studentName)
        return this
    }

    CourseExecutionStudentBuilder withStudentUsername(String studentUsername) {
        this.courseexecutionstudent.setStudentUsername(studentUsername)
        return this
    }

    CourseExecutionStudentBuilder withStudentEmail(String studentEmail) {
        this.courseexecutionstudent.setStudentEmail(studentEmail)
        return this
    }

    CourseExecutionStudentBuilder withEnrollmentDate(LocalDateTime enrollmentDate) {
        this.courseexecutionstudent.setEnrollmentDate(enrollmentDate)
        return this
    }

    CourseExecutionStudent build() {
        return this.courseexecutionstudent
    }
}
