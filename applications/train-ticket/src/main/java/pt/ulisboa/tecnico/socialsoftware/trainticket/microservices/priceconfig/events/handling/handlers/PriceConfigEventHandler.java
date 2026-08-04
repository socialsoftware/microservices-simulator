package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.eventProcessing.PriceConfigEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigRepository;

public abstract class PriceConfigEventHandler extends EventHandler {
    private PriceConfigRepository priceconfigRepository;
    protected PriceConfigEventProcessing priceconfigEventProcessing;

    public PriceConfigEventHandler(PriceConfigRepository priceconfigRepository, PriceConfigEventProcessing priceconfigEventProcessing) {
        this.priceconfigRepository = priceconfigRepository;
        this.priceconfigEventProcessing = priceconfigEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return priceconfigRepository.findAll().stream().map(PriceConfig::getAggregateId).collect(Collectors.toSet());
    }

}
