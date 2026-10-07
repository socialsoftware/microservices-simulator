package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.station

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainTicketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station

@DataJpaTest
@Transactional
@Import(StationServiceTest.LocalBeanConfiguration)
class StationServiceTest extends TrainTicketSpockTest {

    def "createStation: persists the station and reads it back through a fresh UnitOfWork"() {
        when:
        def stationAggregateId = createStation()
        flushAndClear()
        def station = loadForCheck(stationAggregateId, Station)

        then:
        station.aggregateId == stationAggregateId
        station.name == STATION_NAME
        station.stayTime == STATION_STAY_TIME
        station.version != null
    }

    def "createStation: a stay time of zero is accepted"() {
        when:
        def stationAggregateId = createStation(STATION_NAME, 0)
        flushAndClear()

        then:
        loadForCheck(stationAggregateId, Station).stayTime == 0
    }

    def "createStation: STATION_STAY_TIME_NON_NEGATIVE rejects a stay time of #stayTime"() {
        when:
        createStation(STATION_NAME, stayTime)

        then:
        def ex = thrown(TrainTicketException)
        ex.errorMessage == TrainTicketErrorMessage.STATION_STAY_TIME_NON_NEGATIVE

        where:
        stayTime << [-1, null]
    }

    def "createStation: STATION_NAME_UNIQUE rejects a second active station with the same name"() {
        given:
        createStation(STATION_NAME, STATION_STAY_TIME)

        when:
        createStation(STATION_NAME, 5)

        then:
        def ex = thrown(TrainTicketException)
        ex.errorMessage == TrainTicketErrorMessage.STATION_NAME_UNIQUE
    }

    def "createStation: stations with different names coexist"() {
        when:
        def first = createStation(STATION_NAME)
        def second = createStation(STATION_NAME_2)

        then:
        first != second
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
