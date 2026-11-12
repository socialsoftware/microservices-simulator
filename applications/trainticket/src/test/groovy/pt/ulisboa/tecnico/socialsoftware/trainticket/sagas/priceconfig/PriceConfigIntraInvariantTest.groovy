package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.priceconfig

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.SagaPriceConfig

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.PRICE_RATES_POSITIVE

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class PriceConfigIntraInvariantTest extends TrainticketSpockTest {

    private static SagaPriceConfig priceConfigWith(BigDecimal basicPriceRate, BigDecimal firstClassPriceRate) {
        return new SagaPriceConfig(1, new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID,
                PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID, basicPriceRate, firstClassPriceRate))
    }

    def "create price config"() {
        // Spec: plan.md §7 PriceConfig - field list (routeAggregateId, trainTypeAggregateId, basicPriceRate, firstClassPriceRate)
        when:
        def priceConfig = priceConfigWith(PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_FIRST_CLASS_RATE)
        priceConfig.verifyInvariants()

        then:
        priceConfig.aggregateId == 1
        priceConfig.routeAggregateId == PRICE_CONFIG_ROUTE_AGGREGATE_ID
        priceConfig.trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID
        priceConfig.basicPriceRate == PRICE_CONFIG_BASIC_RATE
        priceConfig.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE
        priceConfig.state == Aggregate.AggregateState.ACTIVE
        priceConfig.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "price config: PRICE_RATES_POSITIVE violation - the basic rate is negative"() {
        // Spec: plan.md §3.1 - PRICE_RATES_POSITIVE
        given:
        def priceConfig = priceConfigWith(PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_FIRST_CLASS_RATE)
        priceConfig.setBasicPriceRate(PRICE_CONFIG_RATE_NEGATIVE)

        when:
        priceConfig.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == PRICE_RATES_POSITIVE
    }

    def "price config: PRICE_RATES_POSITIVE violation - the first class rate is negative"() {
        // Spec: plan.md §3.1 - PRICE_RATES_POSITIVE
        given:
        def priceConfig = priceConfigWith(PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_FIRST_CLASS_RATE)
        priceConfig.setFirstClassPriceRate(PRICE_CONFIG_RATE_NEGATIVE)

        when:
        priceConfig.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == PRICE_RATES_POSITIVE
    }

    def "price config: PRICE_RATES_POSITIVE on-point - the basic rate is the smallest positive amount"() {
        // Spec: plan.md §3.1 - PRICE_RATES_POSITIVE, last satisfying value
        given:
        def priceConfig = priceConfigWith(PRICE_CONFIG_RATE_ON_POINT, PRICE_CONFIG_FIRST_CLASS_RATE)

        when:
        priceConfig.verifyInvariants()

        then:
        notThrown(TrainticketException)
        priceConfig.basicPriceRate == PRICE_CONFIG_RATE_ON_POINT
    }

    def "price config: PRICE_RATES_POSITIVE off-point - the basic rate is zero"() {
        // Spec: plan.md §3.1 - PRICE_RATES_POSITIVE, first violating value
        given:
        def priceConfig = priceConfigWith(PRICE_CONFIG_RATE_OFF_POINT, PRICE_CONFIG_FIRST_CLASS_RATE)

        when:
        priceConfig.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == PRICE_RATES_POSITIVE
    }

    def "price config: PRICE_RATES_POSITIVE on-point - the first class rate is the smallest positive amount"() {
        // Spec: plan.md §3.1 - PRICE_RATES_POSITIVE, last satisfying value
        given:
        def priceConfig = priceConfigWith(PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_RATE_ON_POINT)

        when:
        priceConfig.verifyInvariants()

        then:
        notThrown(TrainticketException)
        priceConfig.firstClassPriceRate == PRICE_CONFIG_RATE_ON_POINT
    }

    def "price config: PRICE_RATES_POSITIVE off-point - the first class rate is zero"() {
        // Spec: plan.md §3.1 - PRICE_RATES_POSITIVE, first violating value
        given:
        def priceConfig = priceConfigWith(PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_RATE_OFF_POINT)

        when:
        priceConfig.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == PRICE_RATES_POSITIVE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
