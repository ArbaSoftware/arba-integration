package nl.arba.integration.execution.expressions;

public class InvalidExpressionException extends Exception {
    private String expression;
    public InvalidExpressionException(String expression) {
        super("Invalid expression '" + expression + "'");
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }
}
