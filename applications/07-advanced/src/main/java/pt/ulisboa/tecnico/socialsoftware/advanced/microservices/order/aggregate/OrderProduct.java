package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.ProductDto;

@Entity
public class OrderProduct {
    @Id
    @GeneratedValue
    private Long id;
    private String productName;
    private Double productPrice;
    private Integer productAggregateId;
    private Integer productVersion;
    private AggregateState productState;
    @ManyToOne
    private Order order;

    public OrderProduct() {

    }

    public OrderProduct(ProductDto productDto) {
        setProductAggregateId(productDto.getAggregateId());
        setProductVersion(productDto.getVersion());
        setProductState(productDto.getState());
    }

    public OrderProduct(OrderProductDto orderProductDto) {
        setProductName(orderProductDto.getName());
        setProductPrice(orderProductDto.getPrice());
        setProductAggregateId(orderProductDto.getAggregateId());
        setProductVersion(orderProductDto.getVersion());
        setProductState(orderProductDto.getState() != null ? AggregateState.valueOf(orderProductDto.getState()) : null);
    }

    public OrderProduct(OrderProduct other) {
        setProductName(other.getProductName());
        setProductPrice(other.getProductPrice());
        setProductAggregateId(other.getProductAggregateId());
        setProductVersion(other.getProductVersion());
        setProductState(other.getProductState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(Double productPrice) {
        this.productPrice = productPrice;
    }

    public Integer getProductAggregateId() {
        return productAggregateId;
    }

    public void setProductAggregateId(Integer productAggregateId) {
        this.productAggregateId = productAggregateId;
    }

    public Integer getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(Integer productVersion) {
        this.productVersion = productVersion;
    }

    public AggregateState getProductState() {
        return productState;
    }

    public void setProductState(AggregateState productState) {
        this.productState = productState;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }




    public OrderProductDto buildDto() {
        OrderProductDto dto = new OrderProductDto();
        dto.setName(getProductName());
        dto.setPrice(getProductPrice());
        dto.setAggregateId(getProductAggregateId());
        dto.setVersion(getProductVersion());
        dto.setState(getProductState() != null ? getProductState().name() : null);
        return dto;
    }
}