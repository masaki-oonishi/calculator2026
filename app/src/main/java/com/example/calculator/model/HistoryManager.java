package com.example.calculator.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class HistoryManager {
    private final int MAX_HISTORY_COUNT = 10;
    private final Deque<CalculationMemento> history = new ArrayDeque<>();

    public void addHistory(CalculationMemento memento){
        history.addLast(memento);
        if(history.size() > MAX_HISTORY_COUNT){
            history.removeFirst();
        }
    }

    public List<CalculationMemento> getAllHistory(){
        return List.copyOf(history);
    }
}
