package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.functionalities.PaymentFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.webapi.requestDtos.CreatePaymentRequestDto;

@RestController
public class PaymentController {
    @Autowired
    private PaymentFunctionalities paymentFunctionalities;

    @PostMapping("/payments/create")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDto createPayment(@RequestBody CreatePaymentRequestDto createRequest) {
        return paymentFunctionalities.createPayment(createRequest);
    }

    @GetMapping("/payments/{paymentAggregateId}")
    public PaymentDto getPaymentById(@PathVariable Integer paymentAggregateId) {
        return paymentFunctionalities.getPaymentById(paymentAggregateId);
    }

    @PutMapping("/payments")
    public PaymentDto updatePayment(@RequestBody PaymentDto paymentDto) {
        return paymentFunctionalities.updatePayment(paymentDto);
    }

    @DeleteMapping("/payments/{paymentAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable Integer paymentAggregateId) {
        paymentFunctionalities.deletePayment(paymentAggregateId);
    }

    @GetMapping("/payments")
    public List<PaymentDto> getAllPayments() {
        return paymentFunctionalities.getAllPayments();
    }
}
