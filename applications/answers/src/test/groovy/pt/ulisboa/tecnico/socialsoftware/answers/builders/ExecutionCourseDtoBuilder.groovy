package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.ExecutionCourseDto
import java.time.LocalDateTime

class ExecutionCourseDtoBuilder extends SpockTest {
    private ExecutionCourseDto executioncourseDto

    static ExecutionCourseDtoBuilder aExecutionCourseDto() {
        return new ExecutionCourseDtoBuilder()
    }

    ExecutionCourseDtoBuilder() {
        this.executioncourseDto = new ExecutionCourseDto()
        // Set default values
        this.executioncourseDto.setId(1L)
        this.executioncourseDto.setCourseAggregateId(1)
        this.executioncourseDto.setName("Default name")
        this.executioncourseDto.setCourseAcronym("Default courseAcronym")
        this.executioncourseDto.setCourseType("Default courseType")
    }

    ExecutionCourseDtoBuilder withId(Long id) {
        this.executioncourseDto.setId(id)
        return this
    }

    ExecutionCourseDtoBuilder withCourseAggregateId(Integer courseAggregateId) {
        this.executioncourseDto.setCourseAggregateId(courseAggregateId)
        return this
    }

    ExecutionCourseDtoBuilder withName(String name) {
        this.executioncourseDto.setName(name)
        return this
    }

    ExecutionCourseDtoBuilder withCourseAcronym(String courseAcronym) {
        this.executioncourseDto.setCourseAcronym(courseAcronym)
        return this
    }

    ExecutionCourseDtoBuilder withCourseType(String courseType) {
        this.executioncourseDto.setCourseType(courseType)
        return this
    }

    ExecutionCourseDto build() {
        return this.executioncourseDto
    }
}
