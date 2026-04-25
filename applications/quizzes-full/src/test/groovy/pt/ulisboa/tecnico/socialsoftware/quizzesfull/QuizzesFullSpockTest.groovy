package pt.ulisboa.tecnico.socialsoftware.quizzesfull

import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.functionalities.CourseFunctionalities

class QuizzesFullSpockTest extends SpockTest {

    public static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)

    public static final String COURSE_NAME_1 = "Software Engineering"
    public static final String COURSE_NAME_2 = "Distributed Systems"
    public static final String COURSE_TYPE_TECNICO = "TECNICO"
    public static final String COURSE_TYPE_EXTERNAL = "EXTERNAL"

    @Autowired
    public ImpairmentService impairmentService
    @Autowired(required = false)
    protected SagaUnitOfWorkService unitOfWorkService

    @Autowired(required = false)
    protected CourseFunctionalities courseFunctionalities

    def loadBehaviorScripts() {
        def mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)
        def scriptDir = "groovy/" + this.class.simpleName
        impairmentService.LoadDir(mavenBaseDir, scriptDir)
    }

    SagaState sagaStateOf(Integer aggregateId) {
        def uow = unitOfWorkService.createUnitOfWork("TEST")
        def agg = (SagaAggregate) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, uow)
        return agg.getSagaState()
    }

    CourseDto createCourse(String name, String type) {
        return courseFunctionalities.createCourse(name, type)
    }
}
