package com.example.calculator.ui;

import androidx.lifecycle.ViewModel;

import com.example.calculator.engine.AstCalculationStrategy;
import com.example.calculator.engine.CalculationStrategy;
import com.example.calculator.engine.Calculator;
import com.example.calculator.engine.EngineType;
import com.example.calculator.engine.RpnCalculationStrategy;
import com.example.calculator.model.CalculationMemento;
import com.example.calculator.model.HistoryManager;
import com.example.calculator.model.MyNumber;

import java.util.ArrayList;
import java.util.List;

public class CalculatorViewModel extends ViewModel {
    private final Calculator calculator = new Calculator();
    private final StringBuilder currentExpression = new StringBuilder();

    private MyNumber lastResult = null;
    private boolean isLastActionEquals = false;

    private EngineType currentEngineType = EngineType.AST;

    private final HistoryManager historyManager = new HistoryManager();
    public String getCurrentExpression() { return currentExpression.toString(); }
    public void appendToExpression(String str) { currentExpression.append(str); }
    public void clearExpression() { currentExpression.setLength(0); }

    public MyNumber getLastResult() { return lastResult; }
    public void setLastResult(MyNumber result) { this.lastResult = result; }

    public boolean isLastActionEquals() { return isLastActionEquals; }
    public void setLastActionEquals(boolean isEquals) { this.isLastActionEquals = isEquals; }

    public void setCalculationStrategy(CalculationStrategy strategy, EngineType type) {
        this.currentEngineType = type;
        calculator.setCalculationStrategy(strategy);
    }

    public EngineType getCurrentEngineType() { return currentEngineType; }

    public MyNumber evaluate(String expression) {
        return calculator.evaluate(expression);
    }

    public MyNumber evaluateConstant(MyNumber previousResult) {
        return calculator.evaluateConstant(previousResult);
    }

    public void clearConstant() {
        calculator.clearConstant();
    }

    public void executeEquals(){
        if(currentExpression.length() == 0){
            if(lastResult != null){
                lastResult = evaluateConstant(lastResult);
            }
            return;
        }

        this.lastResult = evaluate(currentExpression.toString());

        saveCurrentState();

        this.currentExpression.setLength(0);
    }

    //Mementoパターン関連のメソッド
    public void saveCurrentState(){
        CalculationMemento memento = new CalculationMemento(
                this.currentExpression.toString(),
                this.lastResult,
                this.currentEngineType
        );
        historyManager.addHistory(memento);
    }

    public List<CalculationMemento> getHistory(){
        return historyManager.getAllHistory();
    }

    public void restoreHistoryAt(int index){
        CalculationMemento memento = historyManager.getHistoryAt(index);
        if(memento == null) return;

        this.currentExpression.setLength(0);
        this.currentExpression.append(memento.getExpression());
        this.lastResult = memento.getResult();
        this.isLastActionEquals = true;

        if(memento.getEngineType() == EngineType.AST){
            setCalculationStrategy(new AstCalculationStrategy(), EngineType.AST);
        }
        else{
            setCalculationStrategy(new RpnCalculationStrategy(), EngineType.RPN);
        }
    }

    public void executeAllClear(){
        this.currentExpression.setLength(0);
        this.lastResult = null;
        this.isLastActionEquals = false;
        this.calculator.clearConstant();
    }

    public void executeClear(){
        int length = currentExpression.length();
        if(!(length == 0)){
            currentExpression.deleteCharAt(length - 1);
        }
    }
}