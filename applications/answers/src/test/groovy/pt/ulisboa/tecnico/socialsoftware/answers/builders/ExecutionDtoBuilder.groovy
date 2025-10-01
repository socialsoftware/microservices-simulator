package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.ExecutionDto
import java.time.LocalDateTime

class ExecutionDtoBuilder extends SpockTest {
    private ExecutionDto executionDto

    static ExecutionDtoBuilder aExecutionDto() {
        return new ExecutionDtoBuilder()
    }

    ExecutionDtoBuilder() {
        this.executionDto = new ExecutionDto()
        // Set default values
        this.executionDto.setName("Default name")
        this.executionDto.setAcronym("Default acronym")
        this.executionDto.setAcademicTerm("Default academicTerm")
        this.executionDto.setStartDate(LocalDateTime.now())
        this.executionDto.setEndDate(LocalDateTime.now())
    }

    ExecutionDtoBuilder withName(String name) {
        this.executionDto.setName(name)
        return this
    }

    ExecutionDtoBuilder withAcronym(String acronym) {
        this.executionDto.setAcronym(acronym)
        return this
    }

    ExecutionDtoBuilder withAcademicTerm(String academicTerm) {
        this.executionDto.setAcademicTerm(academicTerm)
        return this
    }

    ExecutionDtoBuilder withStartDate(LocalDateTime startDate) {
        this.executionDto.setStartDate(startDate)
        return this
    }

    ExecutionDtoBuilder withEndDate(LocalDateTime endDate) {
        this.executionDto.setEndDate(endDate)
        return this
    }

    ExecutionDtoBuilder withCourse(Object course) {
        this.executionDto.setCourse(course)
        return this
    }

    ExecutionDtoBuilder withStudents(Object students) {
        this.executionDto.setStudents(students)
        return this
    }

    ExecutionDto build() {
        return this.executionDto
    }
}
