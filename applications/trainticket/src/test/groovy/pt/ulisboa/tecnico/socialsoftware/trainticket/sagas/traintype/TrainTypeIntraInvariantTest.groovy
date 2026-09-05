package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.traintype

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.SagaTrainType

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.TRAIN_TYPE_HAS_SEATS
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.TRAIN_TYPE_SEATS_NON_NEGATIVE
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.TRAIN_TYPE_SPEED_POSITIVE

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class TrainTypeIntraInvariantTest extends TrainticketSpockTest {

    private static SagaTrainType trainTypeWith(Integer economyClassSeats, Integer firstClassSeats, Integer averageSpeed) {
        return new SagaTrainType(1, new TrainTypeDto(TRAIN_TYPE_NAME, economyClassSeats, firstClassSeats, averageSpeed))
    }

    def "create train type"() {
        // Spec: plan.md §2 TrainType - field list (name, economyClassSeats, firstClassSeats, averageSpeed)
        when:
        def trainType = trainTypeWith(TRAIN_TYPE_ECONOMY_CLASS_SEATS, TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)
        trainType.verifyInvariants()

        then:
        trainType.aggregateId == 1
        trainType.name == TRAIN_TYPE_NAME
        trainType.economyClassSeats == TRAIN_TYPE_ECONOMY_CLASS_SEATS
        trainType.firstClassSeats == TRAIN_TYPE_FIRST_CLASS_SEATS
        trainType.averageSpeed == TRAIN_TYPE_AVERAGE_SPEED
        trainType.state == Aggregate.AggregateState.ACTIVE
        trainType.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "train type: TRAIN_TYPE_SEATS_NON_NEGATIVE violation - economy class seats are negative"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_SEATS_NON_NEGATIVE
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_ECONOMY_CLASS_SEATS, TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)
        trainType.setEconomyClassSeats(-5)

        when:
        trainType.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == TRAIN_TYPE_SEATS_NON_NEGATIVE
    }

    def "train type: TRAIN_TYPE_SEATS_NON_NEGATIVE violation - first class seats are negative"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_SEATS_NON_NEGATIVE
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_ECONOMY_CLASS_SEATS, TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)
        trainType.setFirstClassSeats(-5)

        when:
        trainType.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == TRAIN_TYPE_SEATS_NON_NEGATIVE
    }

    def "train type: TRAIN_TYPE_SEATS_NON_NEGATIVE on-point - economy class seats are zero"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_SEATS_NON_NEGATIVE, BVA on-point
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_SEATS_ZERO, TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        trainType.verifyInvariants()

        then:
        notThrown(TrainticketException)
        trainType.economyClassSeats == TRAIN_TYPE_SEATS_ZERO
    }

    def "train type: TRAIN_TYPE_SEATS_NON_NEGATIVE off-point - economy class seats are minus one"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_SEATS_NON_NEGATIVE, BVA off-point
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_SEATS_NEGATIVE, TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        trainType.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == TRAIN_TYPE_SEATS_NON_NEGATIVE
    }

    def "train type: TRAIN_TYPE_SEATS_NON_NEGATIVE on-point - first class seats are zero"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_SEATS_NON_NEGATIVE, BVA on-point
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_ECONOMY_CLASS_SEATS, TRAIN_TYPE_SEATS_ZERO, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        trainType.verifyInvariants()

        then:
        notThrown(TrainticketException)
        trainType.firstClassSeats == TRAIN_TYPE_SEATS_ZERO
    }

    def "train type: TRAIN_TYPE_SEATS_NON_NEGATIVE off-point - first class seats are minus one"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_SEATS_NON_NEGATIVE, BVA off-point
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_ECONOMY_CLASS_SEATS, TRAIN_TYPE_SEATS_NEGATIVE, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        trainType.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == TRAIN_TYPE_SEATS_NON_NEGATIVE
    }

    def "train type: TRAIN_TYPE_HAS_SEATS violation - both seat counts are zero"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_HAS_SEATS
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_ECONOMY_CLASS_SEATS, TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)
        trainType.setEconomyClassSeats(TRAIN_TYPE_SEATS_ZERO)
        trainType.setFirstClassSeats(TRAIN_TYPE_SEATS_ZERO)

        when:
        trainType.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == TRAIN_TYPE_HAS_SEATS
    }

    def "train type: TRAIN_TYPE_HAS_SEATS on-point - the seat counts sum to one"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_HAS_SEATS, BVA on-point
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_SEATS_ONE, TRAIN_TYPE_SEATS_ZERO, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        trainType.verifyInvariants()

        then:
        notThrown(TrainticketException)
        trainType.economyClassSeats + trainType.firstClassSeats == TRAIN_TYPE_SEATS_ONE
    }

    def "train type: TRAIN_TYPE_HAS_SEATS off-point - the seat counts sum to zero"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_HAS_SEATS, BVA off-point
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_SEATS_ZERO, TRAIN_TYPE_SEATS_ZERO, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        trainType.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == TRAIN_TYPE_HAS_SEATS
    }

    def "train type: TRAIN_TYPE_SPEED_POSITIVE violation - average speed is negative"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_SPEED_POSITIVE
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_ECONOMY_CLASS_SEATS, TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)
        trainType.setAverageSpeed(TRAIN_TYPE_AVERAGE_SPEED_NEGATIVE)

        when:
        trainType.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == TRAIN_TYPE_SPEED_POSITIVE
    }

    def "train type: TRAIN_TYPE_SPEED_POSITIVE on-point - average speed is one"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_SPEED_POSITIVE, BVA on-point
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_ECONOMY_CLASS_SEATS, TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED_ONE)

        when:
        trainType.verifyInvariants()

        then:
        notThrown(TrainticketException)
        trainType.averageSpeed == TRAIN_TYPE_AVERAGE_SPEED_ONE
    }

    def "train type: TRAIN_TYPE_SPEED_POSITIVE off-point - average speed is zero"() {
        // Spec: plan.md §3.1 - TRAIN_TYPE_SPEED_POSITIVE, BVA off-point
        given:
        def trainType = trainTypeWith(TRAIN_TYPE_ECONOMY_CLASS_SEATS, TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED_ZERO)

        when:
        trainType.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == TRAIN_TYPE_SPEED_POSITIVE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
