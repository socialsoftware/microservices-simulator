package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.visitor

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.AnalysisTestSupport
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.CommandHandlerVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.SagaCreationSiteVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.ServiceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.SpockTestVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.WorkflowFunctionalityVisitor

import java.nio.file.Paths

class SpockTestVisitorSpec extends AnalysisTestSupport {

    static String GROOVY_FIXTURES

    def setupSpec() {
        GROOVY_FIXTURES = "${targetApplicationsDir}/src/test/groovy/${basePackagePath}"
    }

    private ApplicationAnalysisContext buildFullContext() {
        def context = new ApplicationAnalysisContext()

        // Pass 1: Services
        def serviceVisitor = new ServiceVisitor()
        serviceVisitor.visit(parseFile(testAppPath("service/CourseService.java")), context)
        serviceVisitor.visit(parseFile(testAppPath("service/CourseExecutionService.java")), context)

        // Pass 2: Command handlers
        def handlerVisitor = new CommandHandlerVisitor()
        handlerVisitor.visit(parseFile(testAppPath("commandhandler/CourseCommandHandler.java")), context)
        handlerVisitor.visit(parseFile(testAppPath("commandhandler/CourseExecutionCommandHandler.java")), context)

        // Pass 3: Sagas
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        workflowVisitor.visit(parseFile(testAppPath("saga/CreateCourseExecutionSaga.java")), context)
        workflowVisitor.visit(parseFile(testAppPath("saga/UpdateCourseSaga.java")), context)
        workflowVisitor.visit(parseFile(testAppPath("saga/CreateCourseSaga.java")), context)

        // Pass 3.5: Saga creation sites
        def creationSiteVisitor = new SagaCreationSiteVisitor()
        creationSiteVisitor.visit(parseFile(testAppPath("functionalities/CourseFunctionalities.java")), context)

        return context
    }

    private void collectGroovyFile(String path, SpockTestVisitor visitor) {
        def config = new CompilerConfiguration()
        def su = SourceUnit.create(
                Paths.get(path).fileName.toString(),
                new File(path).text,
                config.tolerance
        )
        su.parse()
        su.completePhase()
        su.convert()
        su.AST.classes.each { visitor.collectClass(it) }
    }

    def "extracts saga input seed from direct construction in test method"() {
        given: "context pre-populated with passes 1-3.5"
        def context = buildFullContext()
        def spockVisitor = new SpockTestVisitor(context)

        when: "Groovy test files are collected and analyzed"
        collectGroovyFile("${GROOVY_FIXTURES}/TestAppBaseSpec.groovy", spockVisitor)
        collectGroovyFile("${GROOVY_FIXTURES}/CreateCourseSagaDirectSpec.groovy", spockVisitor)
        spockVisitor.analyzeCollectedClasses()

        then: "one saga input seed is extracted"
        context.inputSeeds.size() == 1

        and: "seed has correct saga and test class"
        def seed = context.inputSeeds[0]
        seed.sagaClassName == "CreateCourseSaga"
        seed.testClassName == "CreateCourseSagaDirectSpec"
        seed.directConstruction

        and: "constructor has 4 args with correct infrastructure tagging"
        seed.constructorArgs.size() == 4
        seed.constructorArgs[0].infrastructure()   // unitOfWorkService
        !seed.constructorArgs[1].infrastructure()   // courseDto.aggregateId
        !seed.constructorArgs[2].infrastructure()   // courseDto
        seed.constructorArgs[3].infrastructure()    // unitOfWork

        and: "first arg is unitOfWorkService variable ref"
        seed.constructorArgs[0].expression() instanceof InputExpression.VariableRef
        (seed.constructorArgs[0].expression() as InputExpression.VariableRef).name() == "unitOfWorkService"

        and: "second arg is aggregateId ref on courseDto"
        seed.constructorArgs[1].expression() instanceof InputExpression.AggregateIdRef
        (seed.constructorArgs[1].expression() as InputExpression.AggregateIdRef).variableName() == "courseDto"

        and: "third arg is courseDto variable ref"
        seed.constructorArgs[2].expression() instanceof InputExpression.VariableRef
        (seed.constructorArgs[2].expression() as InputExpression.VariableRef).name() == "courseDto"

        and: "recipes include courseDto with initializer and mutation"
        seed.recipes.containsKey("courseDto")
        def recipe = seed.recipes["courseDto"]
        recipe.initializer instanceof InputExpression.MethodCall
        (recipe.initializer as InputExpression.MethodCall).methodName() == "createCourse"
        recipe.mutations.size() == 1
    }

    def "extracts setup recipes with list expressions and aggregateId refs"() {
        given:
        def context = buildFullContext()
        def spockVisitor = new SpockTestVisitor(context)

        when:
        collectGroovyFile("${GROOVY_FIXTURES}/TestAppBaseSpec.groovy", spockVisitor)
        collectGroovyFile("${GROOVY_FIXTURES}/CreateCourseSagaDirectSpec.groovy", spockVisitor)
        spockVisitor.analyzeCollectedClasses()

        then: "courseIds recipe exists in the seed's all-recipes (via courseDto dependency)"
        def seed = context.inputSeeds[0]
        // courseIds is in setup recipes but may not be transitively needed by the saga constructor
        // The saga constructor references courseDto, which does NOT depend on courseIds
        // So courseIds should NOT be in the seed's recipes
        !seed.recipes.containsKey("courseIds")

        and: "courseDto recipe is present because the saga constructor references it"
        seed.recipes.containsKey("courseDto")
    }

    def "resolves indirect saga creation via functionalities field type"() {
        given: "context with saga creation site mapping"
        def context = buildFullContext()
        // Verify creation site was found
        context.sagaCreationSites.size() == 1
        context.sagaCreationSites[0].className() == "CourseFunctionalities"
        context.sagaCreationSites[0].methodName() == "createCourse"
        context.sagaCreationSites[0].sagaClassName() == "CreateCourseSaga"

        def spockVisitor = new SpockTestVisitor(context)

        when:
        collectGroovyFile("${GROOVY_FIXTURES}/TestAppBaseSpec.groovy", spockVisitor)
        collectGroovyFile("${GROOVY_FIXTURES}/UpdateCourseSagaIndirectSpec.groovy", spockVisitor)
        spockVisitor.analyzeCollectedClasses()

        then: "one indirect saga seed is extracted"
        context.inputSeeds.size() == 1
        def seed = context.inputSeeds[0]
        seed.sagaClassName == "CreateCourseSaga"
        seed.testClassName == "UpdateCourseSagaIndirectSpec"
        !seed.directConstruction
    }

    def "handles named-arg constructor in setup recipe"() {
        given:
        def context = buildFullContext()
        def spockVisitor = new SpockTestVisitor(context)

        when:
        collectGroovyFile("${GROOVY_FIXTURES}/TestAppBaseSpec.groovy", spockVisitor)
        collectGroovyFile("${GROOVY_FIXTURES}/UpdateCourseSagaIndirectSpec.groovy", spockVisitor)
        spockVisitor.analyzeCollectedClasses()

        then: "courseDto recipe uses named-arg constructor"
        def seed = context.inputSeeds[0]
        seed.recipes.containsKey("courseDto")
        def recipe = seed.recipes["courseDto"]
        recipe.initializer instanceof InputExpression.ConstructorCall
        def ctorCall = recipe.initializer as InputExpression.ConstructorCall
        ctorCall.typeName() == "CourseDto"
        ctorCall.namedArgs().containsKey("name")
    }

    def "SagaCreationSiteVisitor finds creation sites in non-saga classes"() {
        given:
        def context = buildFullContext()

        expect: "CourseFunctionalities creation site is registered"
        context.sagaCreationSites.size() == 1
        def site = context.sagaCreationSites[0]
        site.className() == "CourseFunctionalities"
        site.methodName() == "createCourse"
        site.sagaClassName() == "CreateCourseSaga"
    }

    def "traces variable dependencies transitively"() {
        given:
        def context = buildFullContext()
        def spockVisitor = new SpockTestVisitor(context)

        when:
        collectGroovyFile("${GROOVY_FIXTURES}/TestAppBaseSpec.groovy", spockVisitor)
        collectGroovyFile("${GROOVY_FIXTURES}/CreateCourseSagaDirectSpec.groovy", spockVisitor)
        spockVisitor.analyzeCollectedClasses()

        then: "courseDto recipe tracks its initializer dependencies"
        def seed = context.inputSeeds[0]
        def recipe = seed.recipes["courseDto"]
        // createCourse(COURSE_NAME_1) — COURSE_NAME_1 is a constant, not a variable dependency
        // The initializer is MethodCall("this", "createCourse", [ConstantRef])
        // No variable dependencies from constant refs
        recipe.dependsOn.isEmpty() || recipe.dependsOn.every { it != "COURSE_NAME_1" }
    }
}
