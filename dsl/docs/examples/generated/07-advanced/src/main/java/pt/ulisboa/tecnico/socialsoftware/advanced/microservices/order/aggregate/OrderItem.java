package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;

@Entity
public class OrderItem {
    @Id
    @GeneratedValue
    private Long id;
    private String key;
    private String productName;
    private Integer quantity;
    private Double unitPrice;
    @OneToOne
    private Order order;

    public OrderItem() {

    }

    public OrderItem(OrderItemDto orderItemDto) {
        setKey(orderItemDto.getKey());
        setProductName(orderItemDto.getProductName());
        setQuantity(orderItemDto.getQuantity());
        setUnitPrice(orderItemDto.getUnitPrice());
    }


    public OrderItem(OrderItem other) {
        setProductName(other.getProductName());
        setQuantity(other.getQuantity());
        setUnitPrice(other.getUnitPrice());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }




    public OrderItemDto buildDto() {
        OrderItemDto dto = new OrderItemDto();
        dto.setKey(getKey());
        dto.setProductName(getProductName());
        dto.setQuantity(getQuantity());
        dto.setUnitPrice(getUnitPrice());
        return dto;
    }
}