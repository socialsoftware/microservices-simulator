package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.AnalysisTestSupport
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.CommandHandlerVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.ServiceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.WorkflowFunctionalityVisitor

class WorkflowFunctionalityVisitorSpec extends AnalysisTestSupport {

    def "WorkflowFunctionalityVisitor extracts saga steps with correct footprints"() {
        given: "context pre-populated with Pass 1 and Pass 2 results"
        def context = new ApplicationAnalysisContext()
        def serviceVisitor = new ServiceVisitor()
        serviceVisitor.visit(parseFile(testAppPath("service/CourseService.java")), context)
        serviceVisitor.visit(parseFile(testAppPath("service/CourseExecutionService.java")), context)
        def handlerVisitor = new CommandHandlerVisitor()
        handlerVisitor.visit(parseFile(testAppPath("commandhandler/CourseCommandHandler.java")), context)
        handlerVisitor.visit(parseFile(testAppPath("commandhandler/CourseExecutionCommandHandler.java")), context)

        when: "saga files are visited"
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        workflowVisitor.visit(parseFile(testAppPath("saga/CreateCourseExecutionSaga.java")), context)
        workflowVisitor.visit(parseFile(testAppPath("saga/UpdateCourseSaga.java")), context)

        then: "two sagas and four steps are registered"
        context.sagas.size() == 2
        context.steps.size() == 4

        and: "CreateCourseExecutionSaga has correct steps"
        def createExecSaga = context.sagas.find { it.name == "CreateCourseExecutionSaga" }
        createExecSaga != null
        createExecSaga.steps.size() == 2

        def getCourseStep = createExecSaga.steps.find { it.name == "CreateCourseExecutionSaga::getCourseStep" }
        getCourseStep != null
        getCourseStep.stepFootprints.size() == 1
        getCourseStep.stepFootprints[0].aggregateName == "Course"
        getCourseStep.stepFootprints[0].accessPolicy == AccessPolicy.READ

        def createExecStep = createExecSaga.steps.find { it.name == "CreateCourseExecutionSaga::createCourseExecutionStep" }
        createExecStep != null
        createExecStep.stepFootprints.size() == 1
        createExecStep.stepFootprints[0].aggregateName == "CourseExecution"
        createExecStep.stepFootprints[0].accessPolicy == AccessPolicy.WRITE

        and: "UpdateCourseSaga has correct steps"
        def updateCourseSaga = context.sagas.find { it.name == "UpdateCourseSaga" }
        updateCourseSaga != null
        updateCourseSaga.steps.size() == 2

        def getCourseExecStep = updateCourseSaga.steps.find { it.name == "UpdateCourseSaga::getCourseExecutionStep" }
        getCourseExecStep != null
        getCourseExecStep.stepFootprints.size() == 1
        getCourseExecStep.stepFootprints[0].aggregateName == "CourseExecution"
        getCourseExecStep.stepFootprints[0].accessPolicy == AccessPolicy.READ

        def updateCourseStep = updateCourseSaga.steps.find { it.name == "UpdateCourseSaga::updateCourseStep" }
        updateCourseStep != null
        updateCourseStep.stepFootprints.size() == 1
        updateCourseStep.stepFootprints[0].aggregateName == "Course"
        updateCourseStep.stepFootprints[0].accessPolicy == AccessPolicy.WRITE
    }
}
