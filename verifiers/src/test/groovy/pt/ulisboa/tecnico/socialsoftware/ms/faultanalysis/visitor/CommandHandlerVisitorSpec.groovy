package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.AnalysisTestSupport
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.CommandHandlerVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.ServiceVisitor

class CommandHandlerVisitorSpec extends AnalysisTestSupport {

    def "CommandHandlerVisitor maps commands to dispatch info for both handlers"() {
        given: "context pre-populated with Pass 1 results"
        def context = new ApplicationAnalysisContext()
        def serviceVisitor = new ServiceVisitor()
        serviceVisitor.visit(parseFile(testAppPath("service/CourseService.java")), context)
        serviceVisitor.visit(parseFile(testAppPath("service/CourseExecutionService.java")), context)

        when: "command handler files are visited"
        def handlerVisitor = new CommandHandlerVisitor()
        handlerVisitor.visit(parseFile(testAppPath("commandhandler/CourseCommandHandler.java")), context)
        handlerVisitor.visit(parseFile(testAppPath("commandhandler/CourseExecutionCommandHandler.java")), context)

        then: "two command handlers are registered"
        context.commandHandlers.size() == 2

        and: "CourseCommandHandler has correct aggregate type and dispatch"
        def courseHandler = context.commandHandlers.find { it.name == "CourseCommandHandler" }
        courseHandler != null
        courseHandler.aggregateTypeName == "Course"
        courseHandler.commandDispatch.containsKey("GetCourseByIdCommand")
        courseHandler.commandDispatch["GetCourseByIdCommand"].accessPolicy == AccessPolicy.READ
        courseHandler.commandDispatch.containsKey("CreateCourseCommand")
        courseHandler.commandDispatch["CreateCourseCommand"].accessPolicy == AccessPolicy.WRITE

        and: "CourseExecutionCommandHandler has correct aggregate type and dispatch"
        def courseExecHandler = context.commandHandlers.find { it.name == "CourseExecutionCommandHandler" }
        courseExecHandler != null
        courseExecHandler.aggregateTypeName == "CourseExecution"
        courseExecHandler.commandDispatch.containsKey("GetCourseExecutionByIdCommand")
        courseExecHandler.commandDispatch["GetCourseExecutionByIdCommand"].accessPolicy == AccessPolicy.READ
        courseExecHandler.commandDispatch.containsKey("CreateCourseExecutionCommand")
        courseExecHandler.commandDispatch["CreateCourseExecutionCommand"].accessPolicy == AccessPolicy.WRITE
    }
}
