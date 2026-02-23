package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.validation;


import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.OrderCustomer;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.OrderProduct;

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

}