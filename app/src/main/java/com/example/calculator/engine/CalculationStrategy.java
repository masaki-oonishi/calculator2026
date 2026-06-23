package com.example.calculator.engine;

import com.example.calculator.model.MyNumber;

public interface CalculationStrategy {
    /**
     * 数式を解析して計算結果を返却。
     */
    MyNumber evaluate(String expression);

    /**
     *  前回の計算結果を引継ぎ、記憶された定数計算を連打
     */
    MyNumber evaluateConstant(MyNumber previousResult);

    /**
     * 必要に応じて、記憶された定数実行データをクリア（ACボタン用など）。
     */
    void clearConstant();
}
