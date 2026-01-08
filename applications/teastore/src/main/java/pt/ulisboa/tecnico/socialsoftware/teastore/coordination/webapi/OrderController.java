package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.functionalities.OrderFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.*;

@RestController
public class OrderController {
    @Autowired
    private OrderFunctionalities orderFunctionalities;

    @PostMapping("/orders/create")
    public OrderDto createOrder(@RequestBody OrderDto orderDto) throws Exception {
        return orderFunctionalities.createOrder(orderDto);
    }

    @GetMapping("/orders/{orderAggregateId}")
    public OrderDto findByOrderId(@PathVariable Integer orderAggregateId) {
        return orderFunctionalities.findByOrderId(orderAggregateId);
    }

    @GetMapping("/orders/user/{userAggregateId}")
    public List<OrderDto> findByUserAggregateId(@PathVariable Integer userAggregateId) {
        return orderFunctionalities.findByUserAggregateId(userAggregateId);
    }

    @PostMapping("/orders/{orderAggregateId}/cancel")
    public void cancelOrder(@PathVariable Integer orderAggregateId) throws Exception {
        orderFunctionalities.cancelOrder(orderAggregateId);
    }
}
