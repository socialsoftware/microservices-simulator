package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception;

public final class EcommerceErrorMessage {
    private EcommerceErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String INVOICE_MISSING_INVOICENUMBER = "Invoice requires a invoiceNumber.";

    public static final String INVOICE_MISSING_ISSUEDAT = "Invoice requires a issuedAt.";

    public static final String ORDER_MISSING_PLACEDAT = "Order requires a placedAt.";

    public static final String PAYMENT_MISSING_AUTHORIZATIONCODE = "Payment requires a authorizationCode.";

    public static final String PAYMENT_MISSING_PAYMENTMETHOD = "Payment requires a paymentMethod.";

    public static final String PRODUCT_MISSING_SKU = "Product requires a sku.";

    public static final String PRODUCT_MISSING_NAME = "Product requires a name.";

    public static final String PRODUCT_MISSING_DESCRIPTION = "Product requires a description.";

    public static final String SHIPPING_MISSING_ADDRESS = "Shipping requires a address.";

    public static final String SHIPPING_MISSING_CARRIER = "Shipping requires a carrier.";

    public static final String SHIPPING_MISSING_TRACKINGNUMBER = "Shipping requires a trackingNumber.";

    public static final String USER_MISSING_USERNAME = "User requires a username.";

    public static final String USER_MISSING_EMAIL = "User requires a email.";

    public static final String USER_MISSING_PASSWORDHASH = "User requires a passwordHash.";

    public static final String USER_MISSING_SHIPPINGADDRESS = "User requires a shippingAddress.";

}