package pt.ulisboa.tecnico.socialsoftware.businessrules.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.businessrules.coordination.functionalities.ProductFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.businessrules.coordination.webapi.requestDtos.CreateProductRequestDto;

@RestController
public class ProductController {
    @Autowired
    private ProductFunctionalities productFunctionalities;

    @PostMapping("/products/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@RequestBody CreateProductRequestDto createRequest) {
        return productFunctionalities.createProduct(createRequest);
    }

    @GetMapping("/products/{productAggregateId}")
    public ProductDto getProductById(@PathVariable Integer productAggregateId) {
        return productFunctionalities.getProductById(productAggregateId);
    }

    @PutMapping("/products")
    public ProductDto updateProduct(@RequestBody ProductDto productDto) {
        return productFunctionalities.updateProduct(productDto);
    }

    @DeleteMapping("/products/{productAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Integer productAggregateId) {
        productFunctionalities.deleteProduct(productAggregateId);
    }

    @GetMapping("/products")
    public List<ProductDto> getAllProducts() {
        return productFunctionalities.getAllProducts();
    }
}
