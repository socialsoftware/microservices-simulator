package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

/**
 * One argument position in a saga constructor call.
 *
 * @param position       zero-based index in the argument list
 * @param expression     the extracted expression tree
 * @param infrastructure true for unitOfWorkService, unitOfWork*, commandGateway
 */
public record SagaConstructorArg(int position, InputExpression expression, boolean infrastructure) {
}
