package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception;

public final class TeastoreErrorMessage {
    private TeastoreErrorMessage() {}

    public static final String CART_MISSING_USERID = "Cart requires a userId.";

    public static final String CART_MISSING_CHECKEDOUT = "Cart requires a checkedOut.";

    public static final String CART_MISSING_TOTALPRICE = "Cart requires a totalPrice.";

    public static final String CATEGORY_MISSING_NAME = "Category requires a name.";

    public static final String CATEGORY_MISSING_DESCRIPTION = "Category requires a description.";

    public static final String ORDER_MISSING_TIME = "Order requires a time.";

    public static final String ORDER_MISSING_TOTALPRICEINCENTS = "Order requires a totalPriceInCents.";

    public static final String ORDER_MISSING_ADDRESSNAME = "Order requires a addressName.";

    public static final String ORDER_MISSING_ADDRESS1 = "Order requires a address1.";

    public static final String ORDER_MISSING_ADDRESS2 = "Order requires a address2.";

    public static final String ORDER_MISSING_CREDITCARDCOMPANY = "Order requires a creditCardCompany.";

    public static final String ORDER_MISSING_CREDITCARDNUMBER = "Order requires a creditCardNumber.";

    public static final String ORDER_MISSING_CREDITCARDEXPIRYDATE = "Order requires a creditCardExpiryDate.";

    public static final String PRODUCT_MISSING_NAME = "Product requires a name.";

    public static final String PRODUCT_MISSING_DESCRIPTION = "Product requires a description.";

    public static final String PRODUCT_MISSING_LISTPRICEINCENTS = "Product requires a listPriceInCents.";

    public static final String USER_MISSING_USERNAME = "User requires a userName.";

    public static final String USER_MISSING_PASSWORD = "User requires a password.";

    public static final String USER_MISSING_REALNAME = "User requires a realName.";

    public static final String USER_MISSING_EMAIL = "User requires a email.";

}