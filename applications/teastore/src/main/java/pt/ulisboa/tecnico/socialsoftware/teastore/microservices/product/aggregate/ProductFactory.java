package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;

public interface ProductFactory {
    Product createProduct(Integer aggregateId, ProductCategory productCategory, ProductDto productDto);
    Product createProductFromExisting(Product existingProduct);
    ProductDto createProductDto(Product product);
}
