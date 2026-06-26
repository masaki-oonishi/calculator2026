package com.example.calculator.model;

public class CalculationMemento {
    private final String expression;
    private final MyNumber result;

    public CalculationMemento(String expression, MyNumber result){
        this.expression = expression;
        this.result = result;
    }

    public String getExpression() { return expression; }
    public MyNumber getResult() { return result; }
}
