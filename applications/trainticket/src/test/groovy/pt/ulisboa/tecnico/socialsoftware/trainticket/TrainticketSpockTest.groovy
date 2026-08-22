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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.functionalities.TrainTypeFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.service.TrainTypeService
import pt.ulisboa.tecnico.socialsoftware.trainticket.enums.DocumentType
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.Gender
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.service.UserService

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

    public static final String USER_NAME = "zhangsan"
    public static final String USER_NAME_TWO = "lisi"
    public static final String USER_PASSWORD = "pass-zhangsan"
    public static final String USER_PASSWORD_TWO = "pass-lisi"
    public static final Gender USER_GENDER = Gender.FEMALE
    public static final Gender USER_GENDER_TWO = Gender.MALE
    public static final DocumentType USER_DOCUMENT_TYPE = DocumentType.ID_CARD
    public static final DocumentType USER_DOCUMENT_TYPE_TWO = DocumentType.PASSPORT
    public static final DocumentType USER_DOCUMENT_TYPE_NONE = DocumentType.NONE
    public static final String USER_DOCUMENT_NUMBER = "ID-100200300"
    public static final String USER_DOCUMENT_NUMBER_TWO = "PP-400500600"
    public static final String USER_DOCUMENT_NUMBER_BLANK = "   "
    public static final String USER_EMAIL = "zhangsan@trainticket.test"
    public static final String USER_EMAIL_TWO = "lisi@trainticket.test"


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
    @Autowired(required = false)
    protected TrainTypeService trainTypeService
    @Autowired(required = false)
    protected TrainTypeFunctionalities trainTypeFunctionalities
    @Autowired(required = false)
    protected UserService userService
    @Autowired(required = false)
    protected UserFunctionalities userFunctionalities

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

    Integer createTrainType(String name = TRAIN_TYPE_NAME,
                            Integer economyClassSeats = TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                            Integer firstClassSeats = TRAIN_TYPE_FIRST_CLASS_SEATS,
                            Integer averageSpeed = TRAIN_TYPE_AVERAGE_SPEED) {
        def trainTypeDto = new TrainTypeDto(name, economyClassSeats, firstClassSeats, averageSpeed)
        return trainTypeFunctionalities.createTrainType(trainTypeDto).aggregateId
    }

    Integer createUser(String userName = USER_NAME,
                       String password = USER_PASSWORD,
                       Gender gender = USER_GENDER,
                       DocumentType documentType = USER_DOCUMENT_TYPE,
                       String documentNumber = USER_DOCUMENT_NUMBER,
                       String email = USER_EMAIL) {
        def userDto = new UserDto(userName, password, gender, documentType, documentNumber, email)
        return userFunctionalities.createUser(userDto).aggregateId
    }
}
