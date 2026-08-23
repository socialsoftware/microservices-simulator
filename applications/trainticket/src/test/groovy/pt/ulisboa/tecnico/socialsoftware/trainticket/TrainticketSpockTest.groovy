package pt.ulisboa.tecnico.socialsoftware.trainticket

import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService

import java.math.BigDecimal
import java.time.LocalTime

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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStationDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.functionalities.RouteFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.service.RouteService
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.functionalities.ContactsFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.service.ContactsService
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.functionalities.TripFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.service.TripService
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.functionalities.PriceConfigFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.service.PriceConfigService

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

    public static final String ROUTE_START_STATION_NAME = "Shanghai"
    public static final String ROUTE_MIDDLE_STATION_NAME = "Nanjing"
    public static final String ROUTE_END_STATION_NAME = "Beijing"
    public static final String ROUTE_OTHER_STATION_NAME = "Suzhou"
    public static final Integer ROUTE_STATION_AGGREGATE_ID_ONE = 101
    public static final Integer ROUTE_STATION_AGGREGATE_ID_TWO = 102
    public static final Integer ROUTE_STATION_AGGREGATE_ID_THREE = 103
    public static final Integer ROUTE_SEQUENCE_ZERO = 0
    public static final Integer ROUTE_SEQUENCE_ONE = 1
    public static final Integer ROUTE_SEQUENCE_TWO = 2
    public static final Integer ROUTE_SEQUENCE_THREE = 3
    public static final Integer ROUTE_DISTANCE_ZERO = 0
    public static final Integer ROUTE_DISTANCE_ONE = 1
    public static final Integer ROUTE_DISTANCE_MIDDLE = 150
    public static final Integer ROUTE_DISTANCE_END = 350

    public static final Integer CONTACTS_USER_AGGREGATE_ID = 201
    public static final Integer CONTACTS_USER_AGGREGATE_ID_TWO = 202
    public static final String CONTACTS_NAME = "Zhang San"
    public static final String CONTACTS_NAME_TWO = "Li Si"
    public static final DocumentType CONTACTS_DOCUMENT_TYPE = DocumentType.ID_CARD
    public static final DocumentType CONTACTS_DOCUMENT_TYPE_TWO = DocumentType.PASSPORT
    public static final DocumentType CONTACTS_DOCUMENT_TYPE_NONE = DocumentType.NONE
    public static final String CONTACTS_DOCUMENT_NUMBER = "ID-700800900"
    public static final String CONTACTS_DOCUMENT_NUMBER_TWO = "PP-G12345678"
    public static final String CONTACTS_DOCUMENT_NUMBER_BLANK = "   "
    public static final String CONTACTS_PHONE_NUMBER = "+86-21-5555-0100"
    public static final String CONTACTS_PHONE_NUMBER_TWO = "+86-10-6666-0200"

    public static final String TRIP_NUMBER = "G1234"
    public static final String TRIP_NUMBER_TWO = "D5678"
    public static final Integer TRIP_ROUTE_AGGREGATE_ID = 301
    public static final Integer TRIP_ROUTE_AGGREGATE_ID_TWO = 302
    public static final Integer TRIP_TRAIN_TYPE_AGGREGATE_ID = 401
    public static final Integer TRIP_TRAIN_TYPE_AGGREGATE_ID_TWO = 402
    public static final LocalTime TRIP_START_TIME = LocalTime.of(8, 0)
    public static final LocalTime TRIP_END_TIME = LocalTime.of(14, 30)
    public static final LocalTime TRIP_END_TIME_TWO = LocalTime.of(16, 45)
    public static final LocalTime TRIP_END_TIME_ON_POINT = TRIP_START_TIME.plusNanos(1)
    public static final LocalTime TRIP_END_TIME_EQUAL_TO_START = TRIP_START_TIME
    public static final LocalTime TRIP_END_TIME_BEFORE_START = TRIP_START_TIME.minusNanos(1)

    public static final Integer PRICE_CONFIG_ROUTE_AGGREGATE_ID = 501
    public static final Integer PRICE_CONFIG_ROUTE_AGGREGATE_ID_TWO = 502
    public static final Integer PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID = 601
    public static final Integer PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID_TWO = 602
    public static final BigDecimal PRICE_CONFIG_BASIC_RATE = new BigDecimal("0.7500")
    public static final BigDecimal PRICE_CONFIG_BASIC_RATE_TWO = new BigDecimal("0.9000")
    public static final BigDecimal PRICE_CONFIG_FIRST_CLASS_RATE = new BigDecimal("1.2500")
    public static final BigDecimal PRICE_CONFIG_FIRST_CLASS_RATE_TWO = new BigDecimal("1.5000")
    public static final BigDecimal PRICE_CONFIG_RATE_ON_POINT = new BigDecimal("0.0001")
    public static final BigDecimal PRICE_CONFIG_RATE_OFF_POINT = BigDecimal.ZERO
    public static final BigDecimal PRICE_CONFIG_RATE_NEGATIVE = new BigDecimal("-0.5000")


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
    @Autowired(required = false)
    protected RouteService routeService
    @Autowired(required = false)
    protected RouteFunctionalities routeFunctionalities
    @Autowired(required = false)
    protected ContactsService contactsService
    @Autowired(required = false)
    protected ContactsFunctionalities contactsFunctionalities
    @Autowired(required = false)
    protected TripService tripService
    @Autowired(required = false)
    protected TripFunctionalities tripFunctionalities
    @Autowired(required = false)
    protected PriceConfigService priceConfigService
    @Autowired(required = false)
    protected PriceConfigFunctionalities priceConfigFunctionalities

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

    // The station ids a route stations on must resolve to real Station aggregates: session 2.4.c
    // reroutes this helper onto CreateRoute, whose data-assembly step fetches each one.
    Set<RouteStationDto> routeStationsOf(List<List> stations) {
        return stations.withIndex().collect { entry, index ->
            new RouteStationDto(index, entry[0] as Integer, entry[1] as String, entry[2] as Integer)
        }.toSet()
    }

    Set<RouteStationDto> twoStationRoute(String startStationName = ROUTE_START_STATION_NAME,
                                         String endStationName = ROUTE_END_STATION_NAME) {
        return routeStationsOf([
                [createStation(startStationName, STATION_STAY_TIME), startStationName, ROUTE_DISTANCE_ZERO],
                [createStation(endStationName, STATION_STAY_TIME), endStationName, ROUTE_DISTANCE_END]
        ])
    }

    Integer createRoute(String startStationName = ROUTE_START_STATION_NAME,
                        String endStationName = ROUTE_END_STATION_NAME,
                        Set<RouteStationDto> routeStations = null) {
        def stations = routeStations != null ? routeStations : twoStationRoute(startStationName, endStationName)
        return routeFunctionalities.createRoute(new RouteDto(startStationName, endStationName, stations)).aggregateId
    }

    Integer createContacts(Integer userAggregateId = CONTACTS_USER_AGGREGATE_ID,
                           String name = CONTACTS_NAME,
                           DocumentType documentType = CONTACTS_DOCUMENT_TYPE,
                           String documentNumber = CONTACTS_DOCUMENT_NUMBER,
                           String phoneNumber = CONTACTS_PHONE_NUMBER) {
        def contactsDto = new ContactsDto(userAggregateId, name, documentType, documentNumber, phoneNumber)
        return contactsFunctionalities.createContacts(contactsDto).aggregateId
    }

    Integer createTrip(String tripNumber = TRIP_NUMBER,
                       Integer routeAggregateId = createRoute(),
                       Integer trainTypeAggregateId = createTrainType(),
                       LocalTime startTime = TRIP_START_TIME,
                       LocalTime endTime = TRIP_END_TIME) {
        def tripDto = new TripDto(tripNumber, routeAggregateId, trainTypeAggregateId, startTime, endTime)
        return tripFunctionalities.createTrip(tripDto).aggregateId
    }

    Integer createPriceConfig(Integer routeAggregateId = PRICE_CONFIG_ROUTE_AGGREGATE_ID,
                              Integer trainTypeAggregateId = PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                              BigDecimal basicPriceRate = PRICE_CONFIG_BASIC_RATE,
                              BigDecimal firstClassPriceRate = PRICE_CONFIG_FIRST_CLASS_RATE) {
        def priceConfigDto = new PriceConfigDto(routeAggregateId, trainTypeAggregateId,
                basicPriceRate, firstClassPriceRate)
        return priceConfigFunctionalities.createPriceConfig(priceConfigDto).aggregateId
    }
}
