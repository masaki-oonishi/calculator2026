package com.example.calculator.ui;

import androidx.lifecycle.ViewModel;

import com.example.calculator.engine.Calculator;
import com.example.calculator.exception.CalculatorIllegalArgumentException;
import com.example.calculator.model.CalculationMemento;
import com.example.calculator.model.HistoryManager;
import com.example.calculator.model.MyNumber;

import java.util.List;

public class CalculatorViewModel extends ViewModel {
    private final Calculator calculator = new Calculator();
    private final StringBuilder currentExpression = new StringBuilder();
    private final HistoryManager historyManager = new HistoryManager();

    private MyNumber lastResult = null;
    private boolean isLastActionEquals = false;

    // --- ゲッター / セッター ---
    public String getCurrentExpression() { return currentExpression.toString(); }
    public MyNumber getLastResult() { return lastResult; }
    public void setLastActionEquals(boolean lastActionEquals) { isLastActionEquals = lastActionEquals; };
    public boolean isLastActionEquals() { return isLastActionEquals; }
    public void appendToExpression(String str) { currentExpression.append(str); }
    public void clearExpression() { currentExpression.setLength(0); }
    public List<CalculationMemento> getHistory(){
        return historyManager.getAllHistory();
    }

    // 外部からの「命令（Tell）」を受け付けるパブリックメソッド
    public void executeEquals(){
        if(currentExpression.length() == 0){
            executeConstantEquals();
        }
        else{
            executeNormalEquals();
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

    public MyNumber evaluateConstant(MyNumber previousResult) {
        return calculator.evaluateConstant(previousResult);
    }

    public void restoreHistoryAt(int index){
        List<CalculationMemento> historyList = historyManager.getAllHistory();
        if(index < 0 || index >= historyList.size()) return;

        CalculationMemento memento = historyList.get(index);

        currentExpression.setLength(0);
        currentExpression.append(memento.getExpression());

        this.lastResult = new MyNumber(0,0);
        this.isLastActionEquals = false;
    }

    private void executeConstantEquals(){
        if(lastResult == null) return;

        String previousResultStr = lastResult.toString();

        try {
            this.lastResult = evaluateConstant(lastResult);
            currentExpression.append(previousResultStr).append(calculator.getLastConstantExpressionSnippet());
            this.isLastActionEquals = true;

        } catch(CalculatorIllegalArgumentException e){
            this.lastResult = null;
            this.isLastActionEquals = false;
            throw e;

        } finally {
            saveCurrentState();
            currentExpression.setLength(0);
        }
    }

    private void executeNormalEquals(){
        try {
            this.lastResult = calculator.evaluate(currentExpression.toString());
            this.isLastActionEquals = true;

        } catch (CalculatorIllegalArgumentException e){
            this.lastResult = null;
            this.isLastActionEquals = false;
            throw e;

        }
        finally {
            saveCurrentState();
            this.currentExpression.setLength(0);
        }
    }

    //Mementoパターン関連のメソッド
    private void saveCurrentState(){
        CalculationMemento memento = new CalculationMemento(
                this.currentExpression.toString(),
                this.lastResult
        );
        historyManager.addHistory(memento);
    }
}