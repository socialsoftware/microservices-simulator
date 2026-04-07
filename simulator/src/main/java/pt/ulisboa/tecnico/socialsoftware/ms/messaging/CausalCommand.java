package pt.ulisboa.tecnico.socialsoftware.ms.messaging;

public class CausalCommand extends Command {

    private Command payload;

    protected CausalCommand() {}

    public CausalCommand(Command payload) {
        super(payload.getUnitOfWork(), payload.getServiceName(), payload.getRootAggregateId());
        this.payload = payload;
    }

    public Command getPayload() {
        return payload;
    }
}
