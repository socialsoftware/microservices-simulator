package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.advanced.coordination.functionalities.OrderFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.coordination.webapi.requestDtos.CreateOrderRequestDto;

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

    @PostMapping("/orders/{orderId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderProductDto addOrderProduct(@PathVariable Integer orderId, @RequestParam Integer productAggregateId, @RequestBody OrderProductDto productDto) {
        return orderFunctionalities.addOrderProduct(orderId, productAggregateId, productDto);
    }

    @PostMapping("/orders/{orderId}/products/batch")
    public List<OrderProductDto> addOrderProducts(@PathVariable Integer orderId, @RequestBody List<OrderProductDto> productDtos) {
        return orderFunctionalities.addOrderProducts(orderId, productDtos);
    }

    @GetMapping("/orders/{orderId}/products/{productAggregateId}")
    public OrderProductDto getOrderProduct(@PathVariable Integer orderId, @PathVariable Integer productAggregateId) {
        return orderFunctionalities.getOrderProduct(orderId, productAggregateId);
    }

    @PutMapping("/orders/{orderId}/products/{productAggregateId}")
    public OrderProductDto updateOrderProduct(@PathVariable Integer orderId, @PathVariable Integer productAggregateId, @RequestBody OrderProductDto productDto) {
        return orderFunctionalities.updateOrderProduct(orderId, productAggregateId, productDto);
    }

    @DeleteMapping("/orders/{orderId}/products/{productAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeOrderProduct(@PathVariable Integer orderId, @PathVariable Integer productAggregateId) {
        orderFunctionalities.removeOrderProduct(orderId, productAggregateId);
    }
}
