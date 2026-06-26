package com.example.calculator.engine;

import com.example.calculator.model.MyNumber;

public class Calculator {
    private CalculationStrategy strategy;

    public Calculator(){
//        this.strategy = new RpnCalculationStrategy();
        this.strategy = new AstCalculationStrategy();
    }

    public void setCalculationStrategy(CalculationStrategy newStrategy) {
        this.strategy = newStrategy;
        this.clearConstant();
    }

    public MyNumber evaluate(String expression){
        return strategy.evaluate(expression);
    }

    public MyNumber evaluateConstant(MyNumber previousResult){
        return strategy.evaluateConstant(previousResult);
    }

    public void clearConstant(){
        strategy.clearConstant();
    }

    public String getLastConstantExpressionSnippet() { return strategy.getLastConstantExpressionSnippet(); }
}
