package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.aggregate;

import pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos.ProductDto;

public interface ProductFactory {
    Product createProduct(Integer aggregateId, ProductDto productDto);
    Product createProductFromExisting(Product existingProduct);
    ProductDto createProductDto(Product product);
}
