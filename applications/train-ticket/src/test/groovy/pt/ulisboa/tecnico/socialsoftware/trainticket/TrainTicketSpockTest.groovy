package pt.ulisboa.tecnico.socialsoftware.trainticket

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.functionalities.StationFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.service.StationService

// Domain imports (DTOs, functionalities, services) are added here as aggregates are implemented in Phase 2.

class TrainTicketSpockTest extends SpockTest {

    // Domain constants are added here as aggregates are implemented in Phase 2.

    // Station
    public static final String STATION_NAME = "Shang Hai"
    public static final String STATION_NAME_2 = "Nan Jing"
    public static final Integer STATION_STAY_TIME = 10

    @Autowired
    public ImpairmentService impairmentService
    @Autowired(required = false)
    protected SagaUnitOfWorkService unitOfWorkService
    @PersistenceContext
    protected EntityManager entityManager

    // Domain @Autowired fields are added here as aggregates are implemented in Phase 2.
    @Autowired
    protected StationService stationService
    @Autowired
    protected StationFunctionalities stationFunctionalities

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

    // Forces the next read to go through Hibernate's instantiation path. Under @DataJpaTest every
    // UnitOfWork in a test shares one persistence context, so a "fresh UnitOfWork" read-back returns
    // the managed write instance and never exercises the load path - final fields set reflectively
    // on load, @Convert converters and lazy associations all go unproven without this.
    protected void flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    // Domain create* helpers are added below as aggregates are implemented in Phase 2.

    Integer createStation(String name = STATION_NAME, Integer stayTime = STATION_STAY_TIME) {
        def stationDto = new StationDto()
        stationDto.setName(name)
        stationDto.setStayTime(stayTime)
        return stationFunctionalities.createStation(stationDto).aggregateId
    }
}
