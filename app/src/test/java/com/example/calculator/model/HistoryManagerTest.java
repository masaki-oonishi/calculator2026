package com.example.calculator.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class HistoryManagerTest {

    private HistoryManager historyManager;

    @Before
    public void setUp() {
        historyManager = new HistoryManager();
    }

    @Test
    public void testAddHistory_UpToMaxCount() {
        // 1. 最大件数（10件）を超える「11件」の履歴を追加する
        for (int i = 1; i <= 11; i++) {
            MyNumber result = MyNumber.parseToMyNumber(i);
            CalculationMemento memento = new CalculationMemento("1+" + i, result);
            historyManager.addHistory(memento);
        }

        // 2. 履歴リストのサイズが「10」を超えていないか検証
        assertEquals(10, historyManager.getAllHistory().size());

        // 3. 【最重要】一番最初に入れた "1+1" が消えて、
        // 2番目に入れた "1+2" がリストの先頭（一番古いデータ）になっているか検証
        assertEquals("1+2", historyManager.getAllHistory().get(0).getExpression());

        // 4. 最後に入れた "1+11" がリストの末尾（最新データ）になっているか検証
        assertEquals("1+11", historyManager.getAllHistory().get(9).getExpression());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAllHistory_IsUnmodifiable() {
        CalculationMemento memento = new CalculationMemento("1+1", MyNumber.parseToMyNumber(2));
        historyManager.addHistory(memento);

        // 外部から取得したリストに対して直接要素を追加しようとしたとき、
        // unmodifiableList によって正しく例外（ガード）が発生するかを検証
        historyManager.getAllHistory().add(memento);
    }
}