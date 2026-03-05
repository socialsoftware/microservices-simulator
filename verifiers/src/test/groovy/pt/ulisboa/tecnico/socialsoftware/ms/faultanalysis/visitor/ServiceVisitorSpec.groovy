package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.AnalysisTestSupport
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.ServiceVisitor

class ServiceVisitorSpec extends AnalysisTestSupport {

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
