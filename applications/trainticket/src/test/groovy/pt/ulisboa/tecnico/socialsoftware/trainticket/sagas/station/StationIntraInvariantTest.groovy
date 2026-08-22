package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.station

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.SagaStation

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.STATION_STAY_TIME_NON_NEGATIVE

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class StationIntraInvariantTest extends TrainticketSpockTest {

    def "create station"() {
        // Spec: plan.md §1 Station - field list (name, stayTime)
        when:
        def station = new SagaStation(1, new StationDto(STATION_NAME, STATION_STAY_TIME))
        station.verifyInvariants()

        then:
        station.aggregateId == 1
        station.name == STATION_NAME
        station.stayTime == STATION_STAY_TIME
        station.state == Aggregate.AggregateState.ACTIVE
        station.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "station: STATION_STAY_TIME_NON_NEGATIVE violation"() {
        // Spec: plan.md §3.1 - STATION_STAY_TIME_NON_NEGATIVE
        given:
        def station = new SagaStation(1, new StationDto(STATION_NAME, STATION_STAY_TIME))
        station.setStayTime(-5)

        when:
        station.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == STATION_STAY_TIME_NON_NEGATIVE
    }

    def "station: STATION_STAY_TIME_NON_NEGATIVE on-point - stayTime is zero"() {
        // Spec: plan.md §3.1 - STATION_STAY_TIME_NON_NEGATIVE, BVA on-point
        given:
        def station = new SagaStation(1, new StationDto(STATION_NAME, STATION_STAY_TIME_ZERO))

        when:
        station.verifyInvariants()

        then:
        notThrown(TrainticketException)
        station.stayTime == STATION_STAY_TIME_ZERO
    }

    def "station: STATION_STAY_TIME_NON_NEGATIVE off-point - stayTime is minus one"() {
        // Spec: plan.md §3.1 - STATION_STAY_TIME_NON_NEGATIVE, BVA off-point
        given:
        def station = new SagaStation(1, new StationDto(STATION_NAME, STATION_STAY_TIME_NEGATIVE))

        when:
        station.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == STATION_STAY_TIME_NON_NEGATIVE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
