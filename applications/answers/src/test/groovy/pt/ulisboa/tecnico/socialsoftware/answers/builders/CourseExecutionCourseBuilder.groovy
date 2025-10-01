package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.CourseExecutionCourse
import java.time.LocalDateTime

class CourseExecutionCourseBuilder extends SpockTest {
    private CourseExecutionCourse courseexecutioncourse

    static CourseExecutionCourseBuilder aCourseExecutionCourse() {
        return new CourseExecutionCourseBuilder()
    }

    CourseExecutionCourseBuilder() {
        this.courseexecutioncourse = new CourseExecutionCourse()
        // Set default values
        this.courseexecutioncourse.setId(1L)
        this.courseexecutioncourse.setVersion(1)
        this.courseexecutioncourse.setCourseAggregateId(1)
        this.courseexecutioncourse.setCourseName("Default courseName")
        this.courseexecutioncourse.setCourseAcronym("Default courseAcronym")
        this.courseexecutioncourse.setCourseType("Default courseType")
    }

    CourseExecutionCourseBuilder withCourseAggregateId(Integer courseAggregateId) {
        this.courseexecutioncourse.setCourseAggregateId(courseAggregateId)
        return this
    }

    CourseExecutionCourseBuilder withCourseName(String courseName) {
        this.courseexecutioncourse.setCourseName(courseName)
        return this
    }

    CourseExecutionCourseBuilder withCourseAcronym(String courseAcronym) {
        this.courseexecutioncourse.setCourseAcronym(courseAcronym)
        return this
    }

    CourseExecutionCourseBuilder withCourseType(String courseType) {
        this.courseexecutioncourse.setCourseType(courseType)
        return this
    }

    CourseExecutionCourse build() {
        return this.courseexecutioncourse
    }
}
