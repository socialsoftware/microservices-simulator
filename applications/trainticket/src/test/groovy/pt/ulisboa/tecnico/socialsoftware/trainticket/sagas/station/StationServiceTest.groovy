package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.station

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class StationServiceTest extends TrainticketSpockTest {

    def "getStationById: reads back the persisted station through a fresh UnitOfWork"() {
        // Spec: plan.md §1 Station - GetStationById
        given:
        def stationAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)

        when:
        def result = stationService.getStationById(stationAggregateId,
                unitOfWorkService.createUnitOfWork("getStationById"))

        then:
        result.aggregateId == stationAggregateId
        result.name == STATION_NAME
        result.stayTime == STATION_STAY_TIME
        result.isActive()
    }

    def "getStationById: unknown aggregate id"() {
        // Spec: plan.md §1 Station - GetStationById; Path A (aggregateLoadAndRegisterRead)
        when:
        stationService.getStationById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getStationById"))

        then:
        thrown(SimulatorException)
    }

    def "getStations: returns every persisted station"() {
        // Spec: plan.md §1 Station - GetStations
        given:
        def firstAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)
        def secondAggregateId = createStation(STATION_NAME_TWO, STATION_STAY_TIME_TWO)

        when:
        def result = stationService.getStations(unitOfWorkService.createUnitOfWork("getStations"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        def second = result.find { it.aggregateId == secondAggregateId }
        first.name == STATION_NAME
        first.stayTime == STATION_STAY_TIME
        second.name == STATION_NAME_TWO
        second.stayTime == STATION_STAY_TIME_TWO
    }

    def "getStations: returns an empty list when no station exists"() {
        // Spec: plan.md §1 Station - GetStations
        when:
        def result = stationService.getStations(unitOfWorkService.createUnitOfWork("getStations"))

        then:
        result.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
