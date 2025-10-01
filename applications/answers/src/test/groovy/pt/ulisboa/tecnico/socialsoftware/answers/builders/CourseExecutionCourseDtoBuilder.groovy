package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.CourseExecutionCourseDto
import java.time.LocalDateTime

class CourseExecutionCourseDtoBuilder extends SpockTest {
    private CourseExecutionCourseDto courseexecutioncourseDto

    static CourseExecutionCourseDtoBuilder aCourseExecutionCourseDto() {
        return new CourseExecutionCourseDtoBuilder()
    }

    CourseExecutionCourseDtoBuilder() {
        this.courseexecutioncourseDto = new CourseExecutionCourseDto()
        // Set default values
        this.courseexecutioncourseDto.setCourseAggregateId(1)
        this.courseexecutioncourseDto.setCourseName("Default courseName")
        this.courseexecutioncourseDto.setCourseAcronym("Default courseAcronym")
        this.courseexecutioncourseDto.setCourseType("Default courseType")
    }

    CourseExecutionCourseDtoBuilder withCourseAggregateId(Integer courseAggregateId) {
        this.courseexecutioncourseDto.setCourseAggregateId(courseAggregateId)
        return this
    }

    CourseExecutionCourseDtoBuilder withCourseName(String courseName) {
        this.courseexecutioncourseDto.setCourseName(courseName)
        return this
    }

    CourseExecutionCourseDtoBuilder withCourseAcronym(String courseAcronym) {
        this.courseexecutioncourseDto.setCourseAcronym(courseAcronym)
        return this
    }

    CourseExecutionCourseDtoBuilder withCourseType(String courseType) {
        this.courseexecutioncourseDto.setCourseType(courseType)
        return this
    }

    CourseExecutionCourseDto build() {
        return this.courseexecutioncourseDto
    }
}
