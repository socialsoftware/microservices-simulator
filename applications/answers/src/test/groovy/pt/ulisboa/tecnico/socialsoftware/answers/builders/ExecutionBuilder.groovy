package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.Execution
import java.time.LocalDateTime

class ExecutionBuilder extends SpockTest {
    private Execution execution

    static ExecutionBuilder aExecution() {
        return new ExecutionBuilder()
    }

    ExecutionBuilder() {
        this.execution = new Execution()
        // Set default values
        this.execution.setId(1L)
        this.execution.setVersion(1)
        this.execution.setName("Default name")
        this.execution.setAcronym("Default acronym")
        this.execution.setAcademicTerm("Default academicTerm")
        this.execution.setStartDate(LocalDateTime.now())
        this.execution.setEndDate(LocalDateTime.now())
    }

    ExecutionBuilder withName(String name) {
        this.execution.setName(name)
        return this
    }

    ExecutionBuilder withAcronym(String acronym) {
        this.execution.setAcronym(acronym)
        return this
    }

    ExecutionBuilder withAcademicTerm(String academicTerm) {
        this.execution.setAcademicTerm(academicTerm)
        return this
    }

    ExecutionBuilder withStartDate(LocalDateTime startDate) {
        this.execution.setStartDate(startDate)
        return this
    }

    ExecutionBuilder withEndDate(LocalDateTime endDate) {
        this.execution.setEndDate(endDate)
        return this
    }

    ExecutionBuilder withCourse(Object course) {
        this.execution.setCourse(course)
        return this
    }

    ExecutionBuilder withStudents(Object students) {
        this.execution.setStudents(students)
        return this
    }

    Execution build() {
        return this.execution
    }
}
