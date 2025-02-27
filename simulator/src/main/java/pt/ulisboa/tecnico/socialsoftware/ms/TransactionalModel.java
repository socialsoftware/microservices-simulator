package pt.ulisboa.tecnico.socialsoftware.ms;

public enum TransactionalModel {
    SAGAS("sagas"),
    TCC("tcc");

    private String value;

    TransactionalModel(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}