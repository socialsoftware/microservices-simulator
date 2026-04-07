package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.functionalities.OrderFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;

@RestController
public class OrderController {
    @Autowired
    private OrderFunctionalities orderFunctionalities;

    @PostMapping("/orders/create")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto createOrder(@RequestBody CreateOrderRequestDto createRequest) {
        return orderFunctionalities.createOrder(createRequest);
    }

    @GetMapping("/orders/{orderAggregateId}")
    public OrderDto getOrderById(@PathVariable Integer orderAggregateId) {
        return orderFunctionalities.getOrderById(orderAggregateId);
    }

    @PutMapping("/orders")
    public OrderDto updateOrder(@RequestBody OrderDto orderDto) {
        return orderFunctionalities.updateOrder(orderDto);
    }

    @DeleteMapping("/orders/{orderAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Integer orderAggregateId) {
        orderFunctionalities.deleteOrder(orderAggregateId);
    }

    @GetMapping("/orders")
    public List<OrderDto> getAllOrders() {
        return orderFunctionalities.getAllOrders();
    }
}
