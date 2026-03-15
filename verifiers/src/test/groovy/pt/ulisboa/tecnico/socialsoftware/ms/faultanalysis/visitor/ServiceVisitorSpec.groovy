package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.AnalysisTestSupport
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.ServiceVisitor

class ServiceVisitorSpec extends AnalysisTestSupport {

    def "ServiceVisitor ignores @Service classes without UnitOfWorkService constructor parameter"() {
        given:
        def context = new ApplicationAnalysisContext()
        def visitor = new ServiceVisitor()

        when: "visiting a @Service class whose constructor does not take UnitOfWorkService"
        visitor.visit(parseFile(testAppPath("service/CourseFactory.java")), context)

        then: "no service is registered"
        context.services.isEmpty()
    }

    def "ServiceVisitor only picks up services with UnitOfWorkService constructor when scanning a mixed package"() {
        given:
        def context = new ApplicationAnalysisContext()
        def visitor = new ServiceVisitor()

        when: "visiting all three files in the service package"
        visitor.visit(parseFile(testAppPath("service/CourseService.java")), context)
        visitor.visit(parseFile(testAppPath("service/CourseExecutionService.java")), context)
        visitor.visit(parseFile(testAppPath("service/CourseFactory.java")), context)

        then: "only the two real services are registered — factory is excluded"
        context.services.size() == 2
        context.services.every { it.name != "CourseFactory" }
    }

    def "ServiceVisitor classifies READ and WRITE methods for both services"() {
        given:
        def context = new ApplicationAnalysisContext()
        def visitor = new ServiceVisitor()

        when:
        visitor.visit(parseFile(testAppPath("service/CourseService.java")), context)
        visitor.visit(parseFile(testAppPath("service/CourseExecutionService.java")), context)

        then: "two services are registered"
        context.services.size() == 2

        and: "CourseService has correct access policies"
        def courseService = context.services.find { it.name == "CourseService" }
        courseService != null
        courseService.getAccessPolicy("getCourseById") == AccessPolicy.READ
        courseService.getAccessPolicy("createCourse") == AccessPolicy.WRITE

        and: "CourseExecutionService has correct access policies"
        def courseExecService = context.services.find { it.name == "CourseExecutionService" }
        courseExecService != null
        courseExecService.getAccessPolicy("getCourseExecutionById") == AccessPolicy.READ
        courseExecService.getAccessPolicy("createCourseExecution") == AccessPolicy.WRITE
    }
}
