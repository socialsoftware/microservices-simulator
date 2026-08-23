package pt.ulisboa.tecnico.socialsoftware.trainticket

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.retry.RetryRegistry
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandService
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.stream.CommandResponseAggregator
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.stream.StreamCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceService
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommandHandler
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.CentralizedVersionService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.IVersionService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionServiceClient

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationCustomRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.factories.SagasStationFactory
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.repositories.StationCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.functionalities.StationFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.messaging.StationCommandHandler
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.service.StationService
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeCustomRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.factories.SagasTrainTypeFactory
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.repositories.TrainTypeCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.functionalities.TrainTypeFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.messaging.TrainTypeCommandHandler
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.service.TrainTypeService
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserCustomRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.sagas.factories.SagasUserFactory
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.sagas.repositories.UserCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.messaging.UserCommandHandler
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteCustomRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.factories.SagasRouteFactory
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.repositories.RouteCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.functionalities.RouteFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.messaging.RouteCommandHandler
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.service.RouteService
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsCustomRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.factories.SagasContactsFactory
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.repositories.ContactsCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.functionalities.ContactsFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.messaging.ContactsCommandHandler
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.service.ContactsService
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.factories.SagasTripFactory
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.repositories.TripCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripCustomRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.functionalities.TripFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.messaging.TripCommandHandler
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.service.TripService
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.factories.SagasPriceConfigFactory
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.repositories.PriceConfigCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigCustomRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigRepository
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.functionalities.PriceConfigFunctionalities
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.messaging.PriceConfigCommandHandler
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.service.PriceConfigService

// Domain imports (factories, custom repositories, services, functionalities, command handlers,
// event processing/handling) are added here as aggregates are implemented in Phase 2.

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfigurationSagas {
    @Bean
    AggregateIdGeneratorService aggregateIdGeneratorService() {
        return new AggregateIdGeneratorService()
    }

    @Bean
    IVersionService versionService(LocalCommandGateway commandGateway) {
        return new VersionServiceClient(commandGateway)
    }

    @Bean
    CentralizedVersionService centralizedVersionService() {
        return new CentralizedVersionService()
    }

    @Bean
    EventApplicationService eventApplicationService() {
        return new EventApplicationService()
    }

    @Bean
    EventService eventService() {
        return new EventService()
    }

    @Bean
    SagaUnitOfWorkService unitOfWorkService() {
        return new SagaUnitOfWorkService()
    }

    @Bean
    ImpairmentService ImpairmentService() {
        return new ImpairmentService()
    }

    @Bean
    RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults()
    }

    @Bean
    MessagingObjectMapperProvider messagingObjectMapperProvider() {
        return new MessagingObjectMapperProvider(new ObjectMapper().findAndRegisterModules())
    }

    @Bean
    LocalCommandService localCommandService(ApplicationContext applicationContext, MessagingObjectMapperProvider mapperProvider) {
        return new LocalCommandService(applicationContext, mapperProvider)
    }

    @Bean
    LocalCommandGateway commandGateway(ApplicationContext applicationContext, RetryRegistry registry, LocalCommandService localCommandService, MessagingObjectMapperProvider mapperProvider) {
        return new LocalCommandGateway(applicationContext, registry, localCommandService, mapperProvider)
    }

    @Bean
    StreamBridge streamBridge() {
        def mock = Mockito.mock(StreamBridge.class)
        Mockito.when(mock.send(Mockito.anyString(), Mockito.any())).thenReturn(true)
        return mock
    }

    @Bean
    CommandResponseAggregator commandResponseAggregator() {
        return new CommandResponseAggregator()
    }

    @Bean
    StreamCommandGateway streamCommandGateway(LocalCommandGateway commandGateway) {
        def mock = Mockito.mock(StreamCommandGateway.class)

        // Delegate basic send(command) to the in-memory CommandGateway
        Mockito.when(mock.send(Mockito.any())).thenAnswer(inv -> commandGateway.send(inv.getArgument(0)))

        return mock
    }

    @Bean
    TraceService TraceService() {
        return new TraceService()
    }

    // Command Handlers
    @Bean
    SagaCommandHandler sagaCommandHandler() {
        return new SagaCommandHandler()
    }

    @Bean
    VersionCommandHandler versionCommandHandler() {
        return new VersionCommandHandler()
    }

    // Domain beans are added below as aggregates are implemented in Phase 2.

    @Bean
    SagasStationFactory sagasStationFactory() {
        return new SagasStationFactory()
    }

    @Bean
    StationCustomRepositorySagas stationCustomRepositorySagas() {
        return new StationCustomRepositorySagas()
    }

    @Bean
    StationService stationService(SagaUnitOfWorkService unitOfWorkService,
                                  StationRepository stationRepository,
                                  StationCustomRepository stationCustomRepository) {
        return new StationService(unitOfWorkService, stationRepository, stationCustomRepository)
    }

    @Bean
    StationCommandHandler stationCommandHandler() {
        return new StationCommandHandler()
    }

    @Bean
    StationFunctionalities stationFunctionalities() {
        return new StationFunctionalities()
    }

    @Bean
    SagasTrainTypeFactory sagasTrainTypeFactory() {
        return new SagasTrainTypeFactory()
    }

    @Bean
    TrainTypeCustomRepositorySagas trainTypeCustomRepositorySagas() {
        return new TrainTypeCustomRepositorySagas()
    }

    @Bean
    TrainTypeService trainTypeService(SagaUnitOfWorkService unitOfWorkService,
                                      TrainTypeRepository trainTypeRepository,
                                      TrainTypeCustomRepository trainTypeCustomRepository) {
        return new TrainTypeService(unitOfWorkService, trainTypeRepository, trainTypeCustomRepository)
    }

    @Bean
    TrainTypeCommandHandler trainTypeCommandHandler() {
        return new TrainTypeCommandHandler()
    }

    @Bean
    TrainTypeFunctionalities trainTypeFunctionalities() {
        return new TrainTypeFunctionalities()
    }

    @Bean
    SagasUserFactory sagasUserFactory() {
        return new SagasUserFactory()
    }

    @Bean
    UserCustomRepositorySagas userCustomRepositorySagas() {
        return new UserCustomRepositorySagas()
    }

    @Bean
    UserService userService(SagaUnitOfWorkService unitOfWorkService,
                            UserRepository userRepository,
                            UserCustomRepository userCustomRepository) {
        return new UserService(unitOfWorkService, userRepository, userCustomRepository)
    }

    @Bean
    UserCommandHandler userCommandHandler() {
        return new UserCommandHandler()
    }

    @Bean
    UserFunctionalities userFunctionalities() {
        return new UserFunctionalities()
    }

    @Bean
    SagasRouteFactory sagasRouteFactory() {
        return new SagasRouteFactory()
    }

    @Bean
    RouteCustomRepositorySagas routeCustomRepositorySagas() {
        return new RouteCustomRepositorySagas()
    }

    @Bean
    RouteService routeService(SagaUnitOfWorkService unitOfWorkService,
                              RouteRepository routeRepository,
                              RouteCustomRepository routeCustomRepository) {
        return new RouteService(unitOfWorkService, routeRepository, routeCustomRepository)
    }

    @Bean
    RouteCommandHandler routeCommandHandler() {
        return new RouteCommandHandler()
    }

    @Bean
    RouteFunctionalities routeFunctionalities() {
        return new RouteFunctionalities()
    }

    @Bean
    SagasContactsFactory sagasContactsFactory() {
        return new SagasContactsFactory()
    }

    @Bean
    ContactsCustomRepositorySagas contactsCustomRepositorySagas() {
        return new ContactsCustomRepositorySagas()
    }

    @Bean
    ContactsService contactsService(SagaUnitOfWorkService unitOfWorkService,
                                    ContactsRepository contactsRepository,
                                    ContactsCustomRepository contactsCustomRepository) {
        return new ContactsService(unitOfWorkService, contactsRepository, contactsCustomRepository)
    }

    @Bean
    ContactsCommandHandler contactsCommandHandler() {
        return new ContactsCommandHandler()
    }

    @Bean
    ContactsFunctionalities contactsFunctionalities() {
        return new ContactsFunctionalities()
    }

    @Bean
    SagasTripFactory sagasTripFactory() {
        return new SagasTripFactory()
    }

    @Bean
    TripCustomRepositorySagas tripCustomRepositorySagas() {
        return new TripCustomRepositorySagas()
    }

    @Bean
    TripService tripService(SagaUnitOfWorkService unitOfWorkService,
                            TripRepository tripRepository,
                            TripCustomRepository tripCustomRepository) {
        return new TripService(unitOfWorkService, tripRepository, tripCustomRepository)
    }

    @Bean
    TripCommandHandler tripCommandHandler() {
        return new TripCommandHandler()
    }

    @Bean
    TripFunctionalities tripFunctionalities() {
        return new TripFunctionalities()
    }

    @Bean
    SagasPriceConfigFactory sagasPriceConfigFactory() {
        return new SagasPriceConfigFactory()
    }

    @Bean
    PriceConfigCustomRepositorySagas priceConfigCustomRepositorySagas() {
        return new PriceConfigCustomRepositorySagas()
    }

    @Bean
    PriceConfigService priceConfigService(SagaUnitOfWorkService unitOfWorkService,
                                          PriceConfigRepository priceConfigRepository,
                                          PriceConfigCustomRepository priceConfigCustomRepository) {
        return new PriceConfigService(unitOfWorkService, priceConfigRepository, priceConfigCustomRepository)
    }

    @Bean
    PriceConfigCommandHandler priceConfigCommandHandler() {
        return new PriceConfigCommandHandler()
    }

    @Bean
    PriceConfigFunctionalities priceConfigFunctionalities() {
        return new PriceConfigFunctionalities()
    }

}
