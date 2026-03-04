package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.OrderCustomer;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.OrderItem;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.OrderProduct;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.enums.OrderStatus;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.enums.PaymentMethod;

public class OrderValidationAnnotations {

    public static class CustomerValidation {
        @NotNull
        private OrderCustomer customer;
        
        public OrderCustomer getCustomer() {
            return customer;
        }
        
        public void setCustomer(OrderCustomer customer) {
            this.customer = customer;
        }
    }

    public static class ProductsValidation {
        @NotNull
    @NotEmpty
        private Set<OrderProduct> products;
        
        public Set<OrderProduct> getProducts() {
            return products;
        }
        
        public void setProducts(Set<OrderProduct> products) {
            this.products = products;
        }
    }

    public static class ItemsValidation {
        @NotNull
    @NotEmpty
        private Set<OrderItem> items;
        
        public Set<OrderItem> getItems() {
            return items;
        }
        
        public void setItems(Set<OrderItem> items) {
            this.items = items;
        }
    }

    public static class TotalAmountValidation {
        @NotNull
        private Double totalAmount;
        
        public Double getTotalAmount() {
            return totalAmount;
        }
        
        public void setTotalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
        }
    }

    public static class OrderDateValidation {
        @NotNull
        private LocalDateTime orderDate;
        
        public LocalDateTime getOrderDate() {
            return orderDate;
        }
        
        public void setOrderDate(LocalDateTime orderDate) {
            this.orderDate = orderDate;
        }
    }

    public static class StatusValidation {
        @NotNull
        private OrderStatus status;
        
        public OrderStatus getStatus() {
            return status;
        }
        
        public void setStatus(OrderStatus status) {
            this.status = status;
        }
    }

    public static class PaymentMethodValidation {
        @NotNull
        private PaymentMethod paymentMethod;
        
        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }
        
        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
    }

}