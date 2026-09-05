package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

public enum OrderStatus {
    NOTPAID,
    PAID,
    COLLECTED,
    USED,
    CANCELLED
}
