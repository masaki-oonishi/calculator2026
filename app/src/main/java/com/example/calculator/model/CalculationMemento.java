package com.example.calculator.model;

import com.example.calculator.engine.EngineType;

public class CalculationMemento {
    private final String expression;
    private final MyNumber result;
    private final EngineType engineType;

    public CalculationMemento(String expression, MyNumber result, EngineType engineType){
        this.expression = expression;
        this.result = result;
        this.engineType = engineType;
    }

    public String getExpression() { return expression; }
    public MyNumber getResult() { return result; }
    public EngineType getEngineType() { return engineType; }
}
