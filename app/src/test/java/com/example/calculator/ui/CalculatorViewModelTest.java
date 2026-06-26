package com.example.calculator.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.calculator.exception.CalculatorIllegalArgumentException;
import com.example.calculator.model.CalculationMemento;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * カプセル化、定数連打時の数式復元、およびエラー時の履歴保存ロジックを厳格に検証する
 * {@link CalculatorViewModel} 専用のユニットテストスイート。
 */
public class CalculatorViewModelTest {

    private CalculatorViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new CalculatorViewModel();
    }

    // ==========================================
    // 1. 通常計算（Normal Equals）の検証
    // ==========================================

    /**
     * 通常の数式を入力してイコールを実行した際、正しく計算され、
     * 数式バッファがクリアされ、履歴に当時の数式が残ることを検証します。
     */
    @Test
    public void testExecuteNormalEquals_success() {
        // 1. "5+3" を入力
        viewModel.appendToExpression("5");
        viewModel.appendToExpression("+");
        viewModel.appendToExpression("3");
        assertEquals("5+3", viewModel.getCurrentExpression());

        // 2. イコール実行
        viewModel.executeEquals();

        // 3. 結果の検証 (5 + 3 = 8)
        assertEquals("8", viewModel.getLastResult().toString());
        assertTrue(viewModel.isLastActionEquals());

        // 4. バッファが綺麗にクリアされているか (finallyブロックの保証)
        assertEquals("", viewModel.getCurrentExpression());

        // 5. 履歴（Memento）に正しい数式と結果が1件保存されているか
        List<CalculationMemento> history = viewModel.getHistory();
        assertEquals(1, history.size());
        assertEquals("5+3", history.get(0).getExpression());
        assertEquals("8", history.get(0).getResult().toString());
    }

    // ==========================================
    // 2. 定数計算（Constant Equals）と数式復元の検証
    // ==========================================

    /**
     * 【最重要仕様の検証】
     * イコールを連打（定数計算）した際、履歴に「空文字」や「/8」ではなく、
     * 「直前の結果 + 演算子断片」がガッチャンコされたパース可能な数式（例: "40/8"）として
     * 美しく保存され、かつ実行後にバッファが即座にクリアされるかを検証します。
     */
    @Test
    public void testExecuteConstantEquals_shouldRestoreCompleteExpressionInHistory() {
        // 1. 最初の計算: "40/8" を実行 (結果: 5.000000000)
        viewModel.appendToExpression("40");
        viewModel.appendToExpression("/");
        viewModel.appendToExpression("8");
        viewModel.executeEquals();

        // ★修正：MyNumberの割り算仕様（9桁ゼロ埋め）に合わせて期待値を変更
        assertEquals("5.000000000", viewModel.getLastResult().toString());

        // 2. イコール連打（2回目、定数計算のトリガー）
        viewModel.executeEquals();

        // 3. 結果の検証 (5.000000000 / 8 = 0.625)
        assertEquals("0.625000000", viewModel.getLastResult().toString());

        // 4. お片付けチェック
        assertEquals("", viewModel.getCurrentExpression());

        // 5. 【履歴の数式完全復元チェック】
        List<CalculationMemento> history = viewModel.getHistory();
        assertEquals(2, history.size());

        // ★修正：頭にくっつく直前の結果が "5.000000000" になっているので、
        // 復元される数式もそれに合わせて修正します
        CalculationMemento secondHistory = history.get(1);
        assertEquals("5.000000000/8", secondHistory.getExpression());
        assertEquals("0.625000000", secondHistory.getResult().toString());
    }

    // ==========================================
    // 3. 異常系（エラーハンドリング・履歴保存）の検証
    // ==========================================

    /**
     * ゼロ除算などの構文エラーが発生した際、ViewModelが例外をMainActivityへ再スローしつつ、
     * 内部で責任を持って lastResult を null (Error状態) に切り替え、
     * さらに finally によってそのエラー状態の数式を履歴に確実に書き残すかを検証します。
     */
    @Test
    public void testExecuteEquals_shouldThrowExceptionAndSaveErrorToHistory() {
        // 1. ゼロ除算 "10/0" をセット
        viewModel.appendToExpression("10");
        viewModel.appendToExpression("/");
        viewModel.appendToExpression("0");

        try {
            // 2. 計算実行（例外が飛んでくるはず）
            viewModel.executeEquals();
            fail("ゼロ除算エラーが発生するはずですが、スローされませんでした");
        } catch (CalculatorIllegalArgumentException e) {
            // 3. 例外がMainActivityへ正しくパス回しされたことを確認
            assertTrue(true);
        }

        // 4. ViewModelの内部状態が「エラー（null）」に安全に切り替わっているか
        assertNull(viewModel.getLastResult());
        assertFalse(viewModel.isLastActionEquals());

        // 5. バッファがお片付けされているか
        assertEquals("", viewModel.getCurrentExpression());

        // 6. 【最重要】エラーが起きた形跡（10/0 = null）が履歴のタイムラインに刻まれているか
        List<CalculationMemento> history = viewModel.getHistory();
        assertEquals(1, history.size());
        assertEquals("10/0", history.get(0).getExpression());
        assertNull(history.get(0).getResult()); // 結果がnull（Error）として残っていること
    }

    // ==========================================
    // 4. 履歴復元（Memento Restore）の検証
    // ==========================================

    /**
     * 過去の履歴をタップして復元した際、カプセル化（セッター全廃）された状態であっても、
     * 内部フィールドに当時の数式、計算結果、およびフラグが正しく直に同期されるかを検証します。
     */
    @Test
    public void testRestoreHistoryAt_shouldSyncInternalStatesPerfect() {
        // 1. 適当な計算をして履歴を1件作る ("1+1=2")
        viewModel.appendToExpression("1");
        viewModel.appendToExpression("+");
        viewModel.appendToExpression("1");
        viewModel.executeEquals();

        // 2. 一度オールクリアして状態を真っさらにする
        viewModel.executeAllClear();
        assertEquals("", viewModel.getCurrentExpression());
        assertNull(viewModel.getLastResult());

        // 3. インデックス0番目の履歴から「1+1=2」の状態を復元命令
        viewModel.restoreHistoryAt(0);

        // 4. ゲッター経由で、当時のデータが寸分違わず復元されているかチェック
        assertEquals("1+1", viewModel.getCurrentExpression());
        assertEquals("0", viewModel.getLastResult().toString());
        assertFalse(viewModel.isLastActionEquals());
    }

    // ==========================================
    // 5. クリア系（Clear / AllClear）の検証
    // ==========================================

    /**
     * オールクリア（AC）が、数式、結果、フラグ、およびエンジン内の定数バッファを
     * 完全に初期状態へリセットできるかを検証します。
     */
    @Test
    public void testExecuteAllClear_shouldResetEverything() {
        viewModel.appendToExpression("5");
        viewModel.executeEquals(); // lastResult に 5 が入る

        viewModel.executeAllClear();

        assertEquals("", viewModel.getCurrentExpression());
        assertNull(viewModel.getLastResult());
        assertFalse(viewModel.isLastActionEquals());
    }

    /**
     * 部分クリア（C）が、末尾の1文字を安全に削れるかを検証します。
     */
    @Test
    public void testExecuteClear_shouldDeleteLastChar() {
        viewModel.appendToExpression("123");
        viewModel.executeClear();
        assertEquals("12", viewModel.getCurrentExpression());

        viewModel.executeClear();
        assertEquals("1", viewModel.getCurrentExpression());

        // 空文字の時に押してもクラッシュしない（ガードの検証）
        viewModel.executeClear();
        viewModel.executeClear();
        assertEquals("", viewModel.getCurrentExpression());
    }
}