package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis

import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ScenarioGenerator

class ScenarioGeneratorSpec extends AnalysisTestSupport {

    def "ScenarioGenerator full pipeline produces correct ApplicationAnalysisContext"() {
        given: "a ScenarioGenerator pointed at the test application"
        def generator = new ScenarioGenerator(testAppProperties())

        when: "the full analysis pipeline runs"
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

        and: "Pass 3 — 3 sagas with 5 steps and correct footprints"
        ctx.sagas.size() == 3
        ctx.steps.size() == 5

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

        and: "Pass 3.5 — saga creation site found in CourseFunctionalities"
        ctx.sagaCreationSites.size() == 1
        ctx.sagaCreationSites[0].className() == "CourseFunctionalities"
        ctx.sagaCreationSites[0].methodName() == "createCourse"
        ctx.sagaCreationSites[0].sagaClassName() == "CreateCourseSaga"

        and: "Pass 4 — input seeds extracted from Groovy test fixtures"
        ctx.inputSeeds.size() >= 1
        ctx.inputSeeds.any { it.sagaClassName == "CreateCourseSaga" }
    }
}
