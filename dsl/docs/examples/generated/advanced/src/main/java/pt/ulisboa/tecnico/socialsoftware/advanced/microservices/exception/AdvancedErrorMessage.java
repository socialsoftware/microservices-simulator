package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception;

public final class AdvancedErrorMessage {
    private AdvancedErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String ORDER_NOT_FOUND = "Order with id %d does not exist";

    public static final String CUSTOMER_NOT_FOUND = "Customer with id %d does not exist";

    public static final String INVOICE_NOT_FOUND = "Invoice with id %d does not exist";

    public static final String INSUFFICIENT_STOCK = "Insufficient stock for product %s";

    public static final String INVALID_ORDER_STATUS = "Cannot transition order to status %s";

    public static final String CUSTOMER_MISSING_NAME = "Customer requires a name.";

    public static final String CUSTOMER_MISSING_EMAIL = "Customer requires a email.";

    public static final String ORDER_MISSING_PRODUCTS = "Order requires a products.";

    public static final String PRODUCT_MISSING_NAME = "Product requires a name.";

}