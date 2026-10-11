package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.traintype

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainTicketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainType

@DataJpaTest
@Transactional
@Import(TrainTypeServiceTest.LocalBeanConfiguration)
class TrainTypeServiceTest extends TrainTicketSpockTest {

    def "createTrainType: persists the train type and reads it back through a fresh UnitOfWork"() {
        when:
        def trainTypeAggregateId = createTrainType()
        flushAndClear()
        def trainType = loadForCheck(trainTypeAggregateId, TrainType)

        then:
        trainType.aggregateId == trainTypeAggregateId
        trainType.name == TRAIN_TYPE_NAME
        trainType.economyClassSeats == TRAIN_TYPE_ECONOMY_CLASS_SEATS
        trainType.firstClassSeats == TRAIN_TYPE_FIRST_CLASS_SEATS
        trainType.averageSpeed == TRAIN_TYPE_AVERAGE_SPEED
        trainType.version != null
    }

    def "createTrainType: zero seats in both classes are accepted"() {
        when:
        def trainTypeAggregateId = createTrainType(TRAIN_TYPE_NAME, 0, 0, TRAIN_TYPE_AVERAGE_SPEED)
        flushAndClear()
        def trainType = loadForCheck(trainTypeAggregateId, TrainType)

        then:
        trainType.economyClassSeats == 0
        trainType.firstClassSeats == 0
    }

    def "createTrainType: TRAIN_TYPE_SEATS_NON_NEGATIVE rejects economy #economyClassSeats and first class #firstClassSeats"() {
        when:
        createTrainType(TRAIN_TYPE_NAME, economyClassSeats, firstClassSeats, TRAIN_TYPE_AVERAGE_SPEED)

        then:
        def ex = thrown(TrainTicketException)
        ex.errorMessage == TrainTicketErrorMessage.TRAIN_TYPE_SEATS_NON_NEGATIVE

        where:
        economyClassSeats              | firstClassSeats
        -1                             | TRAIN_TYPE_FIRST_CLASS_SEATS
        null                           | TRAIN_TYPE_FIRST_CLASS_SEATS
        TRAIN_TYPE_ECONOMY_CLASS_SEATS | -1
        TRAIN_TYPE_ECONOMY_CLASS_SEATS | null
    }

    def "createTrainType: TRAIN_TYPE_SPEED_POSITIVE rejects an average speed of #averageSpeed"() {
        when:
        createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS, TRAIN_TYPE_FIRST_CLASS_SEATS, averageSpeed)

        then:
        def ex = thrown(TrainTicketException)
        ex.errorMessage == TrainTicketErrorMessage.TRAIN_TYPE_SPEED_POSITIVE

        where:
        averageSpeed << [0, -1, null]
    }

    def "createTrainType: TRAIN_TYPE_NAME_UNIQUE rejects a second active train type with the same name"() {
        given:
        createTrainType(TRAIN_TYPE_NAME)

        when:
        createTrainType(TRAIN_TYPE_NAME, 10, 5, 100)

        then:
        def ex = thrown(TrainTicketException)
        ex.errorMessage == TrainTicketErrorMessage.TRAIN_TYPE_NAME_UNIQUE
    }

    def "createTrainType: train types with different names coexist"() {
        when:
        def first = createTrainType(TRAIN_TYPE_NAME)
        def second = createTrainType(TRAIN_TYPE_NAME_2)

        then:
        first != second
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
