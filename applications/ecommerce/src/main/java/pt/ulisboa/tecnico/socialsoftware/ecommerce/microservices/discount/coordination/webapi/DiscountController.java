package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.functionalities.DiscountFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.webapi.requestDtos.CreateDiscountRequestDto;

@RestController
public class DiscountController {
    @Autowired
    private DiscountFunctionalities discountFunctionalities;

    @PostMapping("/discounts/create")
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountDto createDiscount(@RequestBody CreateDiscountRequestDto createRequest) {
        return discountFunctionalities.createDiscount(createRequest);
    }

    @GetMapping("/discounts/{discountAggregateId}")
    public DiscountDto getDiscountById(@PathVariable Integer discountAggregateId) {
        return discountFunctionalities.getDiscountById(discountAggregateId);
    }

    @PutMapping("/discounts")
    public DiscountDto updateDiscount(@RequestBody DiscountDto discountDto) {
        return discountFunctionalities.updateDiscount(discountDto);
    }

    @DeleteMapping("/discounts/{discountAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDiscount(@PathVariable Integer discountAggregateId) {
        discountFunctionalities.deleteDiscount(discountAggregateId);
    }

    @GetMapping("/discounts")
    public List<DiscountDto> getAllDiscounts() {
        return discountFunctionalities.getAllDiscounts();
    }
}
