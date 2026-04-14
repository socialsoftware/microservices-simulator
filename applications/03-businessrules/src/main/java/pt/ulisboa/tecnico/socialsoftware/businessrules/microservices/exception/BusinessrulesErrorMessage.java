package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.exception;

public final class BusinessrulesErrorMessage {
    private BusinessrulesErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String PRODUCT_MISSING_NAME = "Product requires a name.";

    public static final String PRODUCT_MISSING_SKU = "Product requires a sku.";

}