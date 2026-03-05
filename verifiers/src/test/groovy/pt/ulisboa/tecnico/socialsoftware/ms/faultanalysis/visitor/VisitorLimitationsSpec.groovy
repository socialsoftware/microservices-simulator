package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.AnalysisTestSupport
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.CommandHandlerVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.ServiceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.WorkflowFunctionalityVisitor
import spock.lang.PendingFeature

/**
 * Documents known structural limitations of the three-pass static analysis visitors.
 *
 * Each test asserts the *expected correct* behaviour for an alternative-but-valid Java coding
 * style that the current visitor implementation does not handle. The @PendingFeature annotation
 * marks each test as a known limitation: the test will be skipped (counted as "ignored") as
 * long as the limitation exists, and will become an error if the visitor is fixed without
 * removing the annotation.
 */
class VisitorLimitationsSpec extends AnalysisTestSupport {

    static final String VARIANTS_BASE =
            "src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/faultanalysis/testapp_variants"

    protected variantPath(String relativePath) {
        java.nio.file.Paths.get("${VARIANTS_BASE}/${relativePath}")
    }

    // -------------------------------------------------------------------------
    // S1 — ServiceVisitor: registerChanged in a private helper method
    // -------------------------------------------------------------------------

    @PendingFeature
    def "S1 ServiceVisitor should classify method as WRITE when registerChanged is in a private helper"() {
        given:
        def context = new ApplicationAnalysisContext()
        def visitor = new ServiceVisitor()

        when:
        visitor.visit(parseFile(variantPath("service/CourseServiceWithHelper.java")), context)

        then: "createCourse is WRITE because its private helper calls registerChanged"
        def service = context.services.find { it.name == "CourseServiceWithHelper" }
        service != null
        service.getAccessPolicy("createCourse") == AccessPolicy.WRITE
    }

    // -------------------------------------------------------------------------
    // C1 — CommandHandlerVisitor: this. prefix on service calls
    // -------------------------------------------------------------------------

    @PendingFeature
    def "C1 CommandHandlerVisitor should detect service calls written as this.field.method()"() {
        given: "context pre-populated with Pass 1 results"
        def context = new ApplicationAnalysisContext()
        def serviceVisitor = new ServiceVisitor()
        serviceVisitor.visit(parseFile(testAppPath("service/CourseService.java")), context)

        when: "handler that uses this.courseService.method() is visited"
        def handlerVisitor = new CommandHandlerVisitor()
        handlerVisitor.visit(parseFile(variantPath("commandhandler/CourseCommandHandlerThisPrefix.java")), context)

        then: "both commands are mapped"
        def handler = context.commandHandlers.find { it.name == "CourseCommandHandlerThisPrefix" }
        handler != null
        handler.commandDispatch.size() == 2
        handler.commandDispatch["GetCourseByIdCommand"].accessPolicy == AccessPolicy.READ
        handler.commandDispatch["CreateCourseCommand"].accessPolicy == AccessPolicy.WRITE
    }

    // -------------------------------------------------------------------------
    // C2 — CommandHandlerVisitor: getAggregateTypeName() returns a constant
    // -------------------------------------------------------------------------

    @PendingFeature
    def "C2 CommandHandlerVisitor should extract aggregate type name when returned via a constant"() {
        given: "context pre-populated with Pass 1 results"
        def context = new ApplicationAnalysisContext()
        def serviceVisitor = new ServiceVisitor()
        serviceVisitor.visit(parseFile(testAppPath("service/CourseService.java")), context)

        when: "handler that returns AGGREGATE_TYPE constant is visited"
        def handlerVisitor = new CommandHandlerVisitor()
        handlerVisitor.visit(parseFile(variantPath("commandhandler/CourseCommandHandlerConstantAggregate.java")), context)

        then: "aggregateTypeName is resolved to the constant's value"
        def handler = context.commandHandlers.find { it.name == "CourseCommandHandlerConstantAggregate" }
        handler != null
        handler.aggregateTypeName == "Course"
    }

    // -------------------------------------------------------------------------
    // C3 — CommandHandlerVisitor: wrong service call wins when handler calls two services
    // -------------------------------------------------------------------------

    @PendingFeature
    def "C3 CommandHandlerVisitor should map command to WRITE even when handler calls a READ service first"() {
        given: "context pre-populated with Pass 1 results for both services"
        def context = new ApplicationAnalysisContext()
        def serviceVisitor = new ServiceVisitor()
        serviceVisitor.visit(parseFile(testAppPath("service/CourseService.java")), context)
        serviceVisitor.visit(parseFile(testAppPath("service/CourseExecutionService.java")), context)

        when: "handler that does a validation READ call before the actual WRITE call is visited"
        def handlerVisitor = new CommandHandlerVisitor()
        handlerVisitor.visit(parseFile(variantPath("commandhandler/CourseCommandHandlerValidationFirst.java")), context)

        then: "CreateCourseCommand is mapped as WRITE, not READ"
        def handler = context.commandHandlers.find { it.name == "CourseCommandHandlerValidationFirst" }
        handler != null
        handler.commandDispatch["CreateCourseCommand"].accessPolicy == AccessPolicy.WRITE
    }

    // -------------------------------------------------------------------------
    // W1 — WorkflowFunctionalityVisitor: step action is a method reference
    // -------------------------------------------------------------------------

    @PendingFeature
    def "W1 WorkflowFunctionalityVisitor should detect command footprints when step uses method reference"() {
        given: "context pre-populated with Pass 1 and Pass 2 results"
        def context = new ApplicationAnalysisContext()
        def serviceVisitor = new ServiceVisitor()
        serviceVisitor.visit(parseFile(testAppPath("service/CourseService.java")), context)
        serviceVisitor.visit(parseFile(testAppPath("service/CourseExecutionService.java")), context)
        def handlerVisitor = new CommandHandlerVisitor()
        handlerVisitor.visit(parseFile(testAppPath("commandhandler/CourseCommandHandler.java")), context)
        handlerVisitor.visit(parseFile(testAppPath("commandhandler/CourseExecutionCommandHandler.java")), context)

        when: "saga that passes step action as method reference is visited"
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        workflowVisitor.visit(parseFile(variantPath("saga/CreateCourseExecutionSagaMethodRef.java")), context)

        then: "getCourseStep has one footprint resolved from the referenced method"
        def saga = context.sagas.find { it.name == "CreateCourseExecutionSagaMethodRef" }
        saga != null
        saga.steps.size() == 1

        def getCourseStep = saga.steps.find { it.name == "CreateCourseExecutionSagaMethodRef::getCourseStep" }
        getCourseStep != null
        getCourseStep.stepFootprints.size() == 1
        getCourseStep.stepFootprints[0].aggregateName == "Course"
        getCourseStep.stepFootprints[0].accessPolicy == AccessPolicy.READ
    }

    // -------------------------------------------------------------------------
    // W2 — WorkflowFunctionalityVisitor: step name comes from a constant
    // -------------------------------------------------------------------------

    @PendingFeature
    def "W2 WorkflowFunctionalityVisitor should register step when its name comes from a constant"() {
        given: "context pre-populated with Pass 1 and Pass 2 results"
        def context = new ApplicationAnalysisContext()
        def serviceVisitor = new ServiceVisitor()
        serviceVisitor.visit(parseFile(testAppPath("service/CourseService.java")), context)
        serviceVisitor.visit(parseFile(testAppPath("service/CourseExecutionService.java")), context)
        def handlerVisitor = new CommandHandlerVisitor()
        handlerVisitor.visit(parseFile(testAppPath("commandhandler/CourseCommandHandler.java")), context)
        handlerVisitor.visit(parseFile(testAppPath("commandhandler/CourseExecutionCommandHandler.java")), context)

        when: "saga that uses a constant for the step name is visited"
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        workflowVisitor.visit(parseFile(variantPath("saga/CreateCourseExecutionSagaConstantName.java")), context)

        then: "the step is registered with name resolved from the constant"
        def saga = context.sagas.find { it.name == "CreateCourseExecutionSagaConstantName" }
        saga != null
        saga.steps.size() == 1

        def getCourseStep = saga.steps.find { it.name == "CreateCourseExecutionSagaConstantName::getCourseStep" }
        getCourseStep != null
        getCourseStep.stepFootprints.size() == 1
        getCourseStep.stepFootprints[0].aggregateName == "Course"
        getCourseStep.stepFootprints[0].accessPolicy == AccessPolicy.READ
    }
}
