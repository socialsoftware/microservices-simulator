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
        this.executionDto.setAcronym("Default acronym")
        this.executionDto.setAcademicTerm("Default academicTerm")
        this.executionDto.setEndDate(LocalDateTime.now())
    }

    ExecutionDtoBuilder withAcronym(String acronym) {
        this.executionDto.setAcronym(acronym)
        return this
    }

    ExecutionDtoBuilder withAcademicTerm(String academicTerm) {
        this.executionDto.setAcademicTerm(academicTerm)
        return this
    }

    ExecutionDtoBuilder withEndDate(LocalDateTime endDate) {
        this.executionDto.setEndDate(endDate)
        return this
    }

    ExecutionDtoBuilder withExecutionCourse(ExecutionCourse executionCourse) {
        this.executionDto.setExecutionCourse(executionCourse)
        return this
    }

    ExecutionDtoBuilder withStudents(Set<ExecutionStudent> students) {
        this.executionDto.setStudents(students)
        return this
    }

    ExecutionDto build() {
        return this.executionDto
    }
}
