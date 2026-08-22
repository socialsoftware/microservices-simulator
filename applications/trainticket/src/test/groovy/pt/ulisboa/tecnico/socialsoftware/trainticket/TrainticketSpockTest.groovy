package pt.ulisboa.tecnico.socialsoftware.trainticket

import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService

// Domain imports (DTOs, functionalities, services) are added here as aggregates are implemented in Phase 2.

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.functionalities.StationFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.service.StationService

class TrainticketSpockTest extends SpockTest {

    public static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)

    // Domain constants are added here as aggregates are implemented in Phase 2.

    public static final Integer NONEXISTENT_AGGREGATE_ID = 9999

    public static final String STATION_NAME = "Shanghai"
    public static final String STATION_NAME_TWO = "Beijing"
    public static final Integer STATION_STAY_TIME = 10
    public static final Integer STATION_STAY_TIME_TWO = 25
    public static final Integer STATION_STAY_TIME_ZERO = 0
    public static final Integer STATION_STAY_TIME_NEGATIVE = -1

    public static final String TRAIN_TYPE_NAME = "GaoTieOne"
    public static final String TRAIN_TYPE_NAME_TWO = "DongCheTwo"
    public static final Integer TRAIN_TYPE_ECONOMY_CLASS_SEATS = 100
    public static final Integer TRAIN_TYPE_FIRST_CLASS_SEATS = 50
    public static final Integer TRAIN_TYPE_AVERAGE_SPEED = 300
    public static final Integer TRAIN_TYPE_SEATS_ZERO = 0
    public static final Integer TRAIN_TYPE_SEATS_ONE = 1
    public static final Integer TRAIN_TYPE_SEATS_NEGATIVE = -1
    public static final Integer TRAIN_TYPE_AVERAGE_SPEED_ONE = 1
    public static final Integer TRAIN_TYPE_AVERAGE_SPEED_ZERO = 0
    public static final Integer TRAIN_TYPE_AVERAGE_SPEED_NEGATIVE = -1


    @Autowired
    public ImpairmentService impairmentService
    @Autowired(required = false)
    protected SagaUnitOfWorkService unitOfWorkService
    @Autowired
    protected AggregateIdGeneratorService aggregateIdGeneratorService

    // Domain @Autowired fields are added here as aggregates are implemented in Phase 2.

    @Autowired(required = false)
    protected StationService stationService
    @Autowired(required = false)
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

    // Domain create* helpers are added below as aggregates are implemented in Phase 2.

    Integer createStation(String name = STATION_NAME, Integer stayTime = STATION_STAY_TIME) {
        def stationDto = new StationDto()
        stationDto.setName(name)
        stationDto.setStayTime(stayTime)
        return stationFunctionalities.createStation(stationDto).aggregateId
    }
}
