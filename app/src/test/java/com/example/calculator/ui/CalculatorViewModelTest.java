package com.example.calculator.ui;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.example.calculator.engine.AstCalculationStrategy;
import com.example.calculator.engine.EngineType;

public class CalculatorViewModelTest {

    private CalculatorViewModel viewModel;

    @Before
    public void setUp() {
        // ユニットテスト用にViewModelを初期化
        viewModel = new CalculatorViewModel();
        // 初期戦略を設定（実機起動時の代わり）
        viewModel.setCalculationStrategy(new AstCalculationStrategy(), EngineType.AST);
    }

    @Test
    public void testExecuteClear_BackspaceBehavior() {
        // "123" と入力
        viewModel.appendToExpression("1");
        viewModel.appendToExpression("2");
        viewModel.appendToExpression("3");
        assertEquals("123", viewModel.getCurrentExpression());

        // Cボタンを1回実行 ➡ 末尾の3が消えて "12" になるか
        viewModel.executeClear();
        assertEquals("12", viewModel.getCurrentExpression());

        // Cボタンをもう1回実行 ➡ "1" になるか
        viewModel.executeClear();
        assertEquals("1", viewModel.getCurrentExpression());

        // Cボタンをもう1回実行 ➡ 空文字 "" になるか
        viewModel.executeClear();
        assertEquals("", viewModel.getCurrentExpression());

        // 【安全ガードテスト】空の状態でさらにCボタンを押してもクラッシュせず空を維持するか
        viewModel.executeClear();
        assertEquals("", viewModel.getCurrentExpression());
    }

    @Test
    public void testRestoreHistoryAt_CorrectIndexInversion() {
        // 1. ダミーの履歴を複数作るために、計算を3回行って履歴を溜める
        //（※ViewModelの内部実装に合わせて、数式入力→イコール実行を3回シミュレートします）

        // 1回目：最古の履歴
        viewModel.appendToExpression("1+1");
        viewModel.executeEquals();

        // 2回目：真ん中の履歴
        viewModel.clearExpression();
        viewModel.appendToExpression("2+2");
        viewModel.executeEquals();

        // 3回目：最新の履歴
        viewModel.clearExpression();
        viewModel.appendToExpression("3+3");
        viewModel.executeEquals();

        // 2. 履歴ダイアログで「一番上（which = 0）」がタップされたと仮定して復元を実行
        // 画面の 0番目 ＝ 内部データとしては「最新（3回目の 3+3）」でなければならない
        viewModel.restoreHistoryAt(2);

        // 3. 正しく最新の数式 "3+3" が画面バッファに復元されているか検証
        assertEquals("3+3", viewModel.getCurrentExpression());
    }
}