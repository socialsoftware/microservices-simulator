package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.functionalities.ProductFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.*;

@RestController
public class ProductController {
    @Autowired
    private ProductFunctionalities productFunctionalities;

    @PostMapping("/products/create")
    public ProductDto createProduct(@RequestBody ProductDto productDto) throws Exception {
        return productFunctionalities.createProduct(productDto);
    }

    @GetMapping("/products/{productAggregateId}")
    public ProductDto findByProductId(@PathVariable Integer productAggregateId) {
        return productFunctionalities.findByProductId(productAggregateId);
    }

    @GetMapping("/products/category/{categoryName}")
    public List<ProductDto> findByCategory(@PathVariable String categoryName) {
        return productFunctionalities.findByCategory(categoryName);
    }

    @DeleteMapping("/products/{productAggregateId}/delete")
    public void deleteProduct(@PathVariable Integer productAggregateId) throws Exception {
        productFunctionalities.deleteProduct(productAggregateId);
    }
}
