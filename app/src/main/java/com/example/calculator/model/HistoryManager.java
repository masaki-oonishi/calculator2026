package com.example.calculator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryManager {
    private final List<CalculationMemento> historyList = new ArrayList<>();

    public void addHistory(CalculationMemento memento){
        historyList.add(memento);
    }

    public CalculationMemento getHistoryAt(int index){
        if(index < 0 || index >= historyList.size()){
            return null;
        }
        return historyList.get(index);
    }

    public List<CalculationMemento> getAllHistory(){
        return Collections.unmodifiableList(historyList);
    }

    public void clearAll(){
        historyList.clear();
    }
}
