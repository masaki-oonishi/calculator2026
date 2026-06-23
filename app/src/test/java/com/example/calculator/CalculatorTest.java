package com.example.calculator;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.example.calculator.engine.Calculator;
import com.example.calculator.model.MyNumber;

public class CalculatorTest {

    private Calculator calculator;

    @Before
    public void setUp() {
        calculator = new Calculator();
    }

    // 【正常系ヘルパー】
    private void assertCalculation(String title, String expression, String expected) {
        System.out.println("----- " + title + " -----");
        System.out.println("入力数式 : " + expression);
        try {
            MyNumber result = calculator.evaluate(expression);
            System.out.println("計算結果 : " + result.toString() + " (期待値: " + expected + ")");
            assertEquals(expected, result.toString());
        } catch (Exception e) {
            System.out.println("エラー発生 : " + e.getMessage());
            fail("予期せぬエラーでテストが失敗しました: " + e.getMessage());
        }
        System.out.println();
    }

    // 【異常系ヘルパー】
    private void assertException(String title, String expression, Class<? extends Exception> expectedException) {
        System.out.println("----- " + title + " (異常系) -----");
        System.out.println("入力数式 : " + expression);
        try {
            calculator.evaluate(expression);
            fail("例外が発生しませんでした。期待される例外: " + expectedException.getSimpleName());
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                System.out.println("成功（期待通りのエラー）: " + e.getMessage());
            } else {
                fail("予期せぬ例外が発生しました。期待: " + expectedException.getSimpleName()
                        + ", 実際: " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
            }
        }
        System.out.println();
    }

    // ★新設：【定数実行ヘルパー】前回の結果を引き継いで連打を検証し、次ステップのために結果を返す
    private MyNumber assertConstant(String stepTitle, MyNumber previousResult, String expected) {
        System.out.println(" ↳ [＝連打] " + stepTitle);
        try {
            MyNumber result = calculator.evaluateConstant(previousResult);
            System.out.println("    計算結果 : " + result.toString() + " (期待値: " + expected + ")");
            assertEquals(expected, result.toString());
            return result;
        } catch (Exception e) {
            fail("定数実行中にエラーが発生しました: " + e.getMessage());
            return null;
        }
    }

    // =================================================================
    //  既存のテストケース
    // =================================================================

    @Test
    public void testNormalCasesWithParentheses() {
        System.out.println("======================================");
        System.out.println("   正常系：カッコ付きテスト開始   ");
        System.out.println("======================================\n");

        assertCalculation("基本カッコ", "(12 + 3) * 5", "75");
        assertCalculation("後ろ側のカッコ", "12 + (3 * 5)", "27");
        assertCalculation("カッコのネスト（二重）", "((2 + 3) * 4) - 5", "15");
        assertCalculation("複雑なネスト", "3 + (4 * (5 - 2))", "15");
        assertCalculation("独立した複数のカッコ", "(2 + 3) * (4 + 6)", "50");
        assertCalculation("小数のカッコ計算", "(1.5 + 2.5) * 3.0", "12.0");
    }

    @Test
    public void testNormalCasesWithNegativeNumbers() {
        System.out.println("======================================");
        System.out.println("   正常系：負の数テスト開始   ");
        System.out.println("======================================\n");

        assertCalculation("先頭がマイナス", "(-3) + 5", "2");
        assertCalculation("カッコ内のマイナス", "2 * (-3 + 5)", "4");
        assertCalculation("演算子の直後にカッコ付きマイナス", "5 * (-3)", "-15");
    }

    @Test
    public void testErrorCases() {
        System.out.println("======================================");
        System.out.println("   異常系：エラーハンドリングテスト開始   ");
        System.out.println("======================================\n");

        assertException("ゼロ除算", "5 / 0", IllegalArgumentException.class);
        assertException("演算子の過剰（数値不足）", "12 * + 3", IllegalArgumentException.class);
    }

    @Test
    public void testEdgeCasesNormal() {
        System.out.println("======================================");
        System.out.println("   正常系：エッジケーステスト開始   ");
        System.out.println("======================================\n");

        assertCalculation("数値のみのカッコ", "(5)", "5");
        assertCalculation("多重カッコに囲まれた数値", "((5))", "5");
        assertCalculation("小数の連続掛け算", "0.1 * 0.1 * 0.1", "0.001");
        assertCalculation("負の数×負の数（カッコあり）", "-5 * (-3)", "15");
        assertCalculation("小数の2乗", "1.5 ^ 2", "2.25");
        assertCalculation("累乗の後ろにカッコ付きマイナス", "2 ^ (-3)", "0.125000000");
    }

    @Test
    public void testEdgeCasesAbnormal() {
        System.out.println("======================================");
        System.out.println("   異常系：エッジケース（桁あふれ等）開始   ");
        System.out.println("======================================\n");

        assertException("中身が空のカッコ", "()", IllegalArgumentException.class);
        assertException("対応するカッコがない", "((5 + 3)", IllegalArgumentException.class);
        assertException("未対応の単項プラス", "5 * +3", IllegalArgumentException.class);
        assertException("カッコのない演算子直後のマイナス（負の数同士）", "-5 * -3", IllegalArgumentException.class);
        assertException("カッコのない演算子直後のマイナス（掛け算）", "5 * -3", IllegalArgumentException.class);
        assertException("カッコのない演算子直後のマイナス（累乗）", "2 ^ -3", IllegalArgumentException.class);
        assertException("指数が小数の累乗（未対応）", "2 ^ 1.5", IllegalArgumentException.class);
        assertException("累乗のオーバーフロー", "999999999 ^ 999999999", IllegalArgumentException.class);
        assertException("掛け算によるオーバーフロー", "5000000000 * 5000000000", IllegalArgumentException.class);
        assertException("桁合わせ（足し算）によるオーバーフロー", "9000000000000000 + 0.000001", IllegalArgumentException.class);
    }

    // =================================================================
    //  定数実行（＝ボタン連打）のテストケース
    // =================================================================

    @Test
    public void testConstantCalculation() {
        System.out.println("======================================");
        System.out.println("   正常系：定数実行（＝連打）テスト開始   ");
        System.out.println("======================================\n");

        // シナリオ1: 最もシンプルなフラットな計算
        System.out.println("----- シナリオ1: 3 + 5 の連打 (+5が記憶されるべき) -----");
        MyNumber r1 = calculator.evaluate("3 + 5");
        assertEquals("8", r1.toString());
        r1 = assertConstant("連打1回目", r1, "13");
        r1 = assertConstant("連打2回目", r1, "18");
        System.out.println();

        // シナリオ2: カッコが含まれる計算
        System.out.println("----- シナリオ2: (2 + 3) * 4 の連打 (*4が記憶されるべき) -----");
        MyNumber r2 = calculator.evaluate("(2 + 3) * 4");
        assertEquals("20", r2.toString());
        r2 = assertConstant("連打1回目", r2, "80");
        r2 = assertConstant("連打2回目", r2, "320");
        System.out.println();

        // シナリオ3: 【重要】優先度が混在する計算
        // ASTのルート（+ 20）を記憶するため、23 の次は 43, 63 となるのが正解！
        System.out.println("----- シナリオ3: 3 + 5 * 4 の連打 (+20が記憶されるべき) -----");
        MyNumber r3 = calculator.evaluate("3 + 5 * 4");
        assertEquals("23", r3.toString());
        r3 = assertConstant("連打1回目", r3, "43");
        r3 = assertConstant("連打2回目", r3, "63");
        System.out.println();

        // シナリオ4: カッコと累乗が混在する計算
        // これもASTのルートは「+」です。右側は 3 ^ 2 = 9 になるため、「+ 9」が記憶されるのが正解！
        // (1 + 9 = 10) ➡ 10 + 9 = 19 ➡ 19 + 9 = 28
        System.out.println("----- シナリオ4: 1 + 3 ^ (1 + 1) の連打 (+9が記憶されるべき) -----");
        MyNumber r4 = calculator.evaluate("1 + 3 ^ (1 + 1)");
        assertEquals("10", r4.toString());
        r4 = assertConstant("連打1回目", r4, "19");
        r4 = assertConstant("連打2回目", r4, "28");
        System.out.println();
    }
}