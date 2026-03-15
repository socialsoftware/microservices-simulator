package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

import java.util.List;
import java.util.Map;

/**
 * A discriminated union representing one extractable expression from a Groovy AST node.
 * Used to build replayable "recipes" for saga constructor arguments and test setup variables.
 */
public sealed interface InputExpression {

    record ConstructorCall(String typeName, List<InputExpression> args,
                           Map<String, InputExpression> namedArgs) implements InputExpression {}

    record MethodCall(String scope, String methodName,
                      List<InputExpression> args) implements InputExpression {}

    record VariableRef(String name) implements InputExpression {}

    record AggregateIdRef(String variableName) implements InputExpression {}

    record Literal(Object value) implements InputExpression {}

    record ConstantRef(String name) implements InputExpression {}

    record ListExpr(List<InputExpression> elements) implements InputExpression {}

    record GetterCall(String scope, String property) implements InputExpression {}
}
