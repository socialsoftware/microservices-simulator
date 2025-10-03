package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.ExecutionCourse
import java.time.LocalDateTime

class ExecutionCourseBuilder extends SpockTest {
    private ExecutionCourse executioncourse

    static ExecutionCourseBuilder aExecutionCourse() {
        return new ExecutionCourseBuilder()
    }

    ExecutionCourseBuilder() {
        this.executioncourse = new ExecutionCourse()
        // Set default values
        this.executioncourse.setId(1L)
        this.executioncourse.setVersion(1)
        this.executioncourse.setCourseAggregateId(1)
        this.executioncourse.setName("Default name")
        this.executioncourse.setCourseAcronym("Default courseAcronym")
        this.executioncourse.setCourseType("Default courseType")
    }

    ExecutionCourseBuilder withId(Long id) {
        this.executioncourse.setId(id)
        return this
    }

    ExecutionCourseBuilder withCourseAggregateId(Integer courseAggregateId) {
        this.executioncourse.setCourseAggregateId(courseAggregateId)
        return this
    }

    ExecutionCourseBuilder withName(String name) {
        this.executioncourse.setName(name)
        return this
    }

    ExecutionCourseBuilder withCourseAcronym(String courseAcronym) {
        this.executioncourse.setCourseAcronym(courseAcronym)
        return this
    }

    ExecutionCourseBuilder withCourseType(String courseType) {
        this.executioncourse.setCourseType(courseType)
        return this
    }

    ExecutionCourse build() {
        return this.executioncourse
    }
}
