package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.functionalities.ShippingFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.webapi.requestDtos.CreateShippingRequestDto;

@RestController
public class ShippingController {
    @Autowired
    private ShippingFunctionalities shippingFunctionalities;

    @PostMapping("/shippings/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ShippingDto createShipping(@RequestBody CreateShippingRequestDto createRequest) {
        return shippingFunctionalities.createShipping(createRequest);
    }

    @GetMapping("/shippings/{shippingAggregateId}")
    public ShippingDto getShippingById(@PathVariable Integer shippingAggregateId) {
        return shippingFunctionalities.getShippingById(shippingAggregateId);
    }

    @PutMapping("/shippings")
    public ShippingDto updateShipping(@RequestBody ShippingDto shippingDto) {
        return shippingFunctionalities.updateShipping(shippingDto);
    }

    @DeleteMapping("/shippings/{shippingAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShipping(@PathVariable Integer shippingAggregateId) {
        shippingFunctionalities.deleteShipping(shippingAggregateId);
    }

    @GetMapping("/shippings")
    public List<ShippingDto> getAllShippings() {
        return shippingFunctionalities.getAllShippings();
    }
}
