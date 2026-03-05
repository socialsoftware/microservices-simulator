package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis

import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ScenarioGenerator

class ScenarioGeneratorSpec extends AnalysisTestSupport {

    def "ScenarioGenerator full pipeline produces correct ApplicationAnalysisContext"() {
        given: "a ScenarioGenerator pointed at the test application"
        def props = new FaultAnalysisProperties(TEST_APP_BASE, "pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp")
        def generator = new ScenarioGenerator(props)

        when: "the three-pass analysis runs"
        generator.init()
        def ctx = generator.applicationAnalysisContext

        then: "Pass 1 — 2 services with correct method policies"
        ctx.services.size() == 2

        def courseService = ctx.services.find { it.name == "CourseService" }
        courseService != null
        courseService.getAccessPolicy("getCourseById") == AccessPolicy.READ
        courseService.getAccessPolicy("createCourse") == AccessPolicy.WRITE

        def courseExecService = ctx.services.find { it.name == "CourseExecutionService" }
        courseExecService != null
        courseExecService.getAccessPolicy("getCourseExecutionById") == AccessPolicy.READ
        courseExecService.getAccessPolicy("createCourseExecution") == AccessPolicy.WRITE

        and: "Pass 2 — 2 command handlers with correct aggregate types and dispatch"
        ctx.commandHandlers.size() == 2

        def courseHandler = ctx.commandHandlers.find { it.name == "CourseCommandHandler" }
        courseHandler != null
        courseHandler.aggregateTypeName == "Course"
        courseHandler.commandDispatch["GetCourseByIdCommand"].accessPolicy == AccessPolicy.READ
        courseHandler.commandDispatch["CreateCourseCommand"].accessPolicy == AccessPolicy.WRITE

        def courseExecHandler = ctx.commandHandlers.find { it.name == "CourseExecutionCommandHandler" }
        courseExecHandler != null
        courseExecHandler.aggregateTypeName == "CourseExecution"
        courseExecHandler.commandDispatch["GetCourseExecutionByIdCommand"].accessPolicy == AccessPolicy.READ
        courseExecHandler.commandDispatch["CreateCourseExecutionCommand"].accessPolicy == AccessPolicy.WRITE

        and: "Pass 3 — 2 sagas with 4 steps and correct footprints"
        ctx.sagas.size() == 2
        ctx.steps.size() == 4

        def createExecSaga = ctx.sagas.find { it.name == "CreateCourseExecutionSaga" }
        createExecSaga != null
        createExecSaga.steps.find { it.name == "CreateCourseExecutionSaga::getCourseStep" }
                .stepFootprints[0].aggregateName == "Course"
        createExecSaga.steps.find { it.name == "CreateCourseExecutionSaga::getCourseStep" }
                .stepFootprints[0].accessPolicy == AccessPolicy.READ
        createExecSaga.steps.find { it.name == "CreateCourseExecutionSaga::createCourseExecutionStep" }
                .stepFootprints[0].aggregateName == "CourseExecution"
        createExecSaga.steps.find { it.name == "CreateCourseExecutionSaga::createCourseExecutionStep" }
                .stepFootprints[0].accessPolicy == AccessPolicy.WRITE

        def updateCourseSaga = ctx.sagas.find { it.name == "UpdateCourseSaga" }
        updateCourseSaga != null
        updateCourseSaga.steps.find { it.name == "UpdateCourseSaga::getCourseExecutionStep" }
                .stepFootprints[0].aggregateName == "CourseExecution"
        updateCourseSaga.steps.find { it.name == "UpdateCourseSaga::getCourseExecutionStep" }
                .stepFootprints[0].accessPolicy == AccessPolicy.READ
        updateCourseSaga.steps.find { it.name == "UpdateCourseSaga::updateCourseStep" }
                .stepFootprints[0].aggregateName == "Course"
        updateCourseSaga.steps.find { it.name == "UpdateCourseSaga::updateCourseStep" }
                .stepFootprints[0].accessPolicy == AccessPolicy.WRITE
    }
}
