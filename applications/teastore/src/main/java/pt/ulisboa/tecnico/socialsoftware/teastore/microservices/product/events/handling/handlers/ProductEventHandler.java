package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.eventProcessing.ProductEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductRepository;

public abstract class ProductEventHandler extends EventHandler {
    private ProductRepository productRepository;
    protected ProductEventProcessing productEventProcessing;

    public ProductEventHandler(ProductRepository productRepository, ProductEventProcessing productEventProcessing) {
        this.productRepository = productRepository;
        this.productEventProcessing = productEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return productRepository.findAll().stream().map(Product::getAggregateId).collect(Collectors.toSet());
    }

}
