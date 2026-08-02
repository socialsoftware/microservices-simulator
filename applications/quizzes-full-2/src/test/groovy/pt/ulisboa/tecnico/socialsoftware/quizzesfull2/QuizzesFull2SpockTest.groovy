package pt.ulisboa.tecnico.socialsoftware.quizzesfull2

import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService

// Domain imports (DTOs, functionalities, services) are added here as aggregates are implemented in Phase 2.
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.sagas.SagaCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.coordination.functionalities.CourseFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.service.CourseService

class QuizzesFull2SpockTest extends SpockTest {

    public static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)

    // Domain constants are added here as aggregates are implemented in Phase 2.
    public static final Integer COURSE_AGGREGATE_ID = 1
    public static final String COURSE_NAME = "Software Engineering"
    public static final CourseType COURSE_TYPE = CourseType.TECNICO

    @Autowired
    public ImpairmentService impairmentService
    @Autowired(required = false)
    protected SagaUnitOfWorkService unitOfWorkService
    @Autowired(required = false)
    protected AggregateIdGeneratorService aggregateIdGeneratorService

    // Domain @Autowired fields are added here as aggregates are implemented in Phase 2.
    @Autowired(required = false)
    protected CourseService courseService
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

    protected <T> T loadForCheck(Integer aggregateId, Class<T> type) {
        def uow = unitOfWorkService.createUnitOfWork("check")
        return type.cast(unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, uow))
    }

    // Domain create* helpers are added below as aggregates are implemented in Phase 2.

    Integer createCourse(String name = COURSE_NAME, CourseType type = COURSE_TYPE) {
        def course = new SagaCourse(aggregateIdGeneratorService.getNewAggregateId(), name, type)
        unitOfWorkService.registerChanged(course, unitOfWorkService.createUnitOfWork("fixture"))
        return course.getAggregateId()
    }
}
