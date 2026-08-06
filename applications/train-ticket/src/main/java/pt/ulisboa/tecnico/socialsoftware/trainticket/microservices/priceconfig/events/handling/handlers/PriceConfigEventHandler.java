package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.eventProcessing.PriceConfigEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigRepository;

public abstract class PriceConfigEventHandler extends EventHandler {
    protected PriceConfigEventProcessing priceconfigEventProcessing;

    public PriceConfigEventHandler(PriceConfigRepository priceconfigRepository, PriceConfigEventProcessing priceconfigEventProcessing) {
        super(priceconfigRepository);
        this.priceconfigEventProcessing = priceconfigEventProcessing;
    }

}
