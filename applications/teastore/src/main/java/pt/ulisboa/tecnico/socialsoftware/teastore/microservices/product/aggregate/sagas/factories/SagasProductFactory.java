package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductFactory;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.sagas.SagaProduct;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.sagas.dtos.SagaProductDto;

@Service
@Profile("sagas")
public class SagasProductFactory implements ProductFactory {
    @Override
    public Product createProduct(Integer aggregateId, ProductDto productDto) {
        return new SagaProduct(aggregateId, productDto);
    }

    @Override
    public Product createProductFromExisting(Product existingProduct) {
        return new SagaProduct((SagaProduct) existingProduct);
    }

    @Override
    public ProductDto createProductDto(Product product) {
        return new SagaProductDto(product);
    }
}