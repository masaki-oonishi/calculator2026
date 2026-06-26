package com.example.calculator.engine;

import androidx.annotation.NonNull;

import com.example.calculator.model.MyNumber;

enum MyOperator{
    // 開きカッコを優先度0（一番低い）として定義
    // これにより、通常の演算子が来ても、開きカッコを勝手にポップして計算してしまうのを防げる
    PAREN_OPEN(0, 1, "("),
    ADD(1, 2, "+"){ @Override MyNumber apply(MyNumber a, MyNumber b) { return a.add(b); } },
    SUBTRACT(1, 2, "-"){ @Override MyNumber apply(MyNumber a, MyNumber b) { return a.subtract(b); } },
    MULTIPLY(2, 2, "*"){ @Override MyNumber apply(MyNumber a, MyNumber b) { return a.multiply(b); } },
    DIVIDE( 2, 2, "/"){ @Override MyNumber apply(MyNumber a, MyNumber b) { return a.divide(b); } },
    POW(4, 2, "^"){ @Override MyNumber apply(MyNumber a, MyNumber b) { return a.pow(b); }},
    UNARY_MINUS(3, 1, "-") { @Override MyNumber apply(MyNumber a, MyNumber b) { return b.unaryMinus(); }};

    //　計算の優先順位を定義
    final int priority;
    // 数値を１つしか使わない演算子を実装する場合に必要
    final int numOperands;

    final String symbol;
    MyOperator(int priority, int numOperands, String symbol){
        this.priority = priority;
        this.numOperands = numOperands;
        this.symbol = symbol;
    }

    //パッケージprivate
    MyNumber apply(MyNumber a, MyNumber b) { return null; }

    @NonNull
    @Override
    public String toString() {
        return this.symbol;
    }

    static MyOperator fromSymbol(String symbol){
        switch(symbol){
            case "+": return ADD;
            case "-": return SUBTRACT;
            case "*": return MULTIPLY;
            case "/": return DIVIDE;
            case "^": return POW;
            default: throw new IllegalArgumentException("未対応の演算子:" + symbol);
        }
    }
}