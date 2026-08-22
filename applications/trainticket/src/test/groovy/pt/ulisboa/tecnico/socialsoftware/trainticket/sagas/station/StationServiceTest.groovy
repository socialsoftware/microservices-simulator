package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.station

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.DUPLICATE_STATION_NAME

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

    def "createStation: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §1 Station - CreateStation
        given:
        def stationDto = new StationDto(STATION_NAME, STATION_STAY_TIME)

        when:
        def result = stationService.createStation(stationDto,
                unitOfWorkService.createUnitOfWork("createStation"))

        then: 'read back through a second, fresh UnitOfWork'
        result.aggregateId != null
        def readBack = stationService.getStationById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == STATION_NAME
        readBack.stayTime == STATION_STAY_TIME
        readBack.isActive()
    }

    def "createStation: DUPLICATE_STATION_NAME violation"() {
        // Spec: plan.md §1 Station - rule UNIQUE_STATION_NAME (P3, own table)
        given:
        createStation(STATION_NAME, STATION_STAY_TIME)

        when:
        stationService.createStation(new StationDto(STATION_NAME, STATION_STAY_TIME_TWO),
                unitOfWorkService.createUnitOfWork("createStation"))

        then:
        def ex = thrown(TrainticketException)
        ex.message == DUPLICATE_STATION_NAME
    }

    def "createStation: a name freed by a soft-deleted station is reusable"() {
        // Spec: plan.md §1 Station - rule UNIQUE_STATION_NAME constrains active stations only
        given:
        def deletedAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)
        stationService.deleteStation(deletedAggregateId,
                unitOfWorkService.createUnitOfWork("deleteStation"))

        when:
        def result = stationService.createStation(new StationDto(STATION_NAME, STATION_STAY_TIME_TWO),
                unitOfWorkService.createUnitOfWork("createStation"))

        then:
        result.aggregateId != deletedAggregateId
        def readBack = stationService.getStationById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == STATION_NAME
        readBack.stayTime == STATION_STAY_TIME_TWO
    }

    def "updateStation: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §1 Station - UpdateStation
        given:
        def stationAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)

        when:
        stationService.updateStation(stationAggregateId,
                new StationDto(STATION_NAME_TWO, STATION_STAY_TIME_TWO),
                unitOfWorkService.createUnitOfWork("updateStation"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = stationService.getStationById(stationAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == STATION_NAME_TWO
        readBack.stayTime == STATION_STAY_TIME_TWO
    }

    def "updateStation: keeping its own name is not a duplicate"() {
        // Spec: plan.md §1 Station - rule UNIQUE_STATION_NAME excludes the station under change
        given:
        def stationAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)

        when:
        stationService.updateStation(stationAggregateId,
                new StationDto(STATION_NAME, STATION_STAY_TIME_TWO),
                unitOfWorkService.createUnitOfWork("updateStation"))

        then:
        notThrown(TrainticketException)
        def readBack = stationService.getStationById(stationAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == STATION_NAME
        readBack.stayTime == STATION_STAY_TIME_TWO
    }

    def "updateStation: DUPLICATE_STATION_NAME violation"() {
        // Spec: plan.md §1 Station - rule UNIQUE_STATION_NAME (P3, own table, also on update)
        given:
        createStation(STATION_NAME, STATION_STAY_TIME)
        def stationAggregateId = createStation(STATION_NAME_TWO, STATION_STAY_TIME_TWO)

        when:
        stationService.updateStation(stationAggregateId,
                new StationDto(STATION_NAME, STATION_STAY_TIME_TWO),
                unitOfWorkService.createUnitOfWork("updateStation"))

        then:
        def ex = thrown(TrainticketException)
        ex.message == DUPLICATE_STATION_NAME
    }

    def "updateStation: unknown aggregate id"() {
        // Spec: plan.md §1 Station - UpdateStation; Path A (aggregateLoadAndRegisterRead)
        when:
        stationService.updateStation(NONEXISTENT_AGGREGATE_ID,
                new StationDto(STATION_NAME, STATION_STAY_TIME),
                unitOfWorkService.createUnitOfWork("updateStation"))

        then:
        thrown(SimulatorException)
    }

    def "deleteStation: the station stops being an active station"() {
        // Spec: plan.md §1 Station - DeleteStation (soft delete)
        given:
        def stationAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)
        def survivorAggregateId = createStation(STATION_NAME_TWO, STATION_STAY_TIME_TWO)

        when:
        stationService.deleteStation(stationAggregateId,
                unitOfWorkService.createUnitOfWork("deleteStation"))

        then: 'read back through a second, fresh UnitOfWork'
        def remaining = stationService.getStations(unitOfWorkService.createUnitOfWork("check"))
        remaining.collect { it.aggregateId } == [survivorAggregateId]
    }

    def "deleteStation: the deleted station is no longer loadable by id"() {
        // Spec: plan.md §1 Station - DeleteStation (soft delete); Path A
        given:
        def stationAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)

        when:
        stationService.deleteStation(stationAggregateId,
                unitOfWorkService.createUnitOfWork("deleteStation"))

        and:
        stationService.getStationById(stationAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "deleteStation: unknown aggregate id"() {
        // Spec: plan.md §1 Station - DeleteStation; Path A (aggregateLoadAndRegisterRead)
        when:
        stationService.deleteStation(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("deleteStation"))

        then:
        thrown(SimulatorException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
