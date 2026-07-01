package com.example.calculator.model;

import static org.junit.Assert.assertEquals;

import com.example.calculator.exception.CalculatorIllegalArgumentException;

import org.junit.Test;

/**
 * 独自の不変数値クラス {@link MyNumber} のロジック、境界値、および標準精度（9桁）への共通丸め処理を検証するテストスイート。
 */
public class MyNumberTest {

    // ==========================================
    // 1. String Conversion (toString) Tests
    // ==========================================

    /**
     * スケールが0のとき、小数点がつかない純粋な整数文字列が返ることを検証します。
     */
    @Test
    public void testToString_withInteger() {
        MyNumber n = new MyNumber(123, 0);
        assertEquals("123", n.toString());
    }

    /**
     * 指定されたスケール（小数点以下の桁数）の位置に正しくドットが挿入されるかを検証します。
     */
    @Test
    public void testToString_withNormalDecimal() {
        MyNumber n = new MyNumber(123, 2); // 1.23
        assertEquals("1.23", n.toString());
    }

    /**
     * 整数値の桁数がスケール未満のとき、先頭が正しく「0.0...」とゼロ埋めされるかを検証します。
     */
    @Test
    public void testToString_withZeroPadding() {
        MyNumber n = new MyNumber(5, 3); // 0.005
        assertEquals("0.005", n.toString());
    }

    /**
     * 負の数のとき、先頭のマイナス符号と小数点の位置が崩れずに変換できるかを検証します。
     */
    @Test
    public void testToString_withNegativeNumber() {
        MyNumber n = new MyNumber(-45, 1); // -4.5
        assertEquals("-4.5", n.toString());
    }

    // ==========================================
    // 2. Addition & Subtraction Tests
    // ==========================================

    /**
     * スケールが異なる数値同士を足した際、内部で自動的に桁合わせが行われるかを検証します。
     */
    @Test
    public void testAdd_withDifferentScales() {
        MyNumber n1 = new MyNumber(1, 1);  // 0.1
        MyNumber n2 = new MyNumber(2, 2);  // 0.02
        MyNumber result = n1.add(n2);      // 0.12
        assertEquals("0.12", result.toString());
    }

    /**
     * 初期入力などで標準精度（9桁）を超える数値が渡されて足し算をした場合、
     * 計算結果が自動的に9桁に切り捨て丸めされるかを検証します。
     * ★修正点: 計算結果が0に丸め込まれるため、新仕様に従いscaleは0、表示は"0"になります。
     */
    @Test
    public void testAdd_shouldRoundDownWhenResultExceedsDefaultPrecision() {
        // 0.00000000005 (scale: 11) + 0.00000000005 (scale: 11) = 0.00000000010 (scale: 11)
        // これが標準精度の9桁に丸め込まれると、小数点以下10桁目以降が切り捨てられて「完全に0」になる
        MyNumber n1 = new MyNumber(5, 11);
        MyNumber n2 = new MyNumber(5, 11);
        MyNumber result = n1.add(n2);

        assertEquals(0, result.getScale());
        assertEquals("0", result.toString());
    }

    /**
     * 引き算において、標準精度（9桁）を超えるスケールが流れてきた場合、
     * 計算の出口で自動的に9桁に切り捨て丸めされるかを検証します。
     * ★修正点: 計算結果が0に丸め込まれるため、新仕様に従いscaleは0、表示は"0"になります。
     */
    @Test
    public void testSubtract_shouldRoundDownWhenResultExceedsDefaultPrecision() {
        // 0.00000000009 (scale: 11) - 0.00000000001 (scale: 11) = 0.00000000008 (scale: 11)
        // 9桁に丸め込まれることで、小数点以下11桁目の「8」が切り落とされて「完全に0」になる
        MyNumber n1 = new MyNumber(9, 11);
        MyNumber n2 = new MyNumber(1, 11);
        MyNumber result = n1.subtract(n2);

        assertEquals(0, result.getScale());
        assertEquals("0", result.toString());
    }

    /**
     * 計算結果がlong型の表現限界（19桁）を超えた際、安全にエラーがスローされるかを検証します。
     */
    @Test(expected = CalculatorIllegalArgumentException.class)
    public void testAdd_shouldThrowExceptionOnOverflow() {
        MyNumber n1 = new MyNumber(Long.MAX_VALUE - 5, 0);
        MyNumber n2 = new MyNumber(10, 0);
        n1.add(n2);
    }

    // ==========================================
    // 3. Multiplication & Division Tests
    // ==========================================

    /**
     * 通常の掛け算において、計算結果のスケールが双方のスケールの合計値として正しく累積されるかを検証します。
     */
    @Test
    public void testMultiply_scaleAccumulation() {
        MyNumber n1 = new MyNumber(2, 1);  // 0.2
        MyNumber n2 = new MyNumber(3, 1);  // 0.3
        MyNumber result = n1.multiply(n2); // 0.06 (scale: 1 + 1 = 2)
        assertEquals("0.06", result.toString());
    }

    /**
     * 掛け算の累積によって合計スケールが標準精度（9桁）を超えた場合、
     * 内部で自動的に9桁に丸め込まれ、無限のスケール肥大化が完全に防止されるかを検証します。
     * ★修正点: 計算結果が0に丸め込まれるため、新仕様に従いscaleは0、表示は"0"になります。
     */
    @Test
    public void testMultiply_shouldRoundDownWhenScaleExceedsDefaultPrecision() {
        // 0.00001 (scale: 5) * 0.00001 (scale: 5) = 0.0000000001 (本来はscale: 10)
        // 共通メソッドにより、10桁目の「1」が切り捨てられて「完全に0」になる
        MyNumber n1 = new MyNumber(1, 5);
        MyNumber n2 = new MyNumber(1, 5);
        MyNumber result = n1.multiply(n2);

        assertEquals(0, result.getScale());
        assertEquals("0", result.toString());
    }

    /**
     * 割り切れる割り算において、標準精度（9桁）までゼロ埋めされて出力されるかを検証します。
     */
    @Test
    public void testDivide_exactDivision() {
        MyNumber n1 = new MyNumber(1, 0);  // 1
        MyNumber n2 = new MyNumber(2, 0);  // 2
        MyNumber result = n1.divide(n2);   // 0.5
        assertEquals("0.500000000", result.toString());
    }

    /**
     * 1/3のような割り切れない循環小数の際、標準精度（9桁）の境界で安全に計算が打ち切られ、
     * 定数実行（連打）をしてもエラーにならずにスケール9を維持し続けるかを検証します。
     */
    @Test
    public void testDivide_repeatingDecimalWithPrecision() {
        MyNumber n1 = new MyNumber(1, 0);  // 1
        MyNumber n2 = new MyNumber(3, 0);  // 3
        MyNumber result = n1.divide(n2);   // 0.333333333

        assertEquals(MyNumber.DEFAULT_PRECISION, result.getScale());
        assertEquals("0.333333333", result.toString());

        // さらにもう一回割る（連打のシミュレーション。スケールが9を超えないかチェック）
        MyNumber nextResult = result.divide(n2);
        assertEquals(MyNumber.DEFAULT_PRECISION, nextResult.getScale());
    }

    /**
     * 0で割り算を実行した際、アプリがクラッシュせずに特定のビジネス例外がスローされるかを検証します。
     */
    @Test(expected = CalculatorIllegalArgumentException.class)
    public void testDivide_shouldThrowExceptionOnDivisionByZero() {
        MyNumber n1 = new MyNumber(5, 0);
        MyNumber n2 = new MyNumber(0, 0);
        n1.divide(n2);
    }

    // ==========================================
    // 4. Power (pow) Tests
    // ==========================================

    /**
     * 整数を指数とした累乗計算が正確に行われるかを検証します。
     */
    @Test
    public void testPow_withPositiveIntegerExponent() {
        MyNumber base = new MyNumber(2, 0);     // 2
        MyNumber exp = new MyNumber(3, 0);      // 3
        assertEquals("8", base.pow(exp).toString()); // 2^3 = 8
    }

    /**
     * どんな数値であっても、0乗した結果は確実に「1」になる数学的仕様を検証します。
     */
    @Test
    public void testPow_withZeroExponent() {
        MyNumber base = new MyNumber(5, 1);     // 0.5
        MyNumber exp = new MyNumber(0, 0);      // 0
        assertEquals("1", base.pow(exp).toString());
    }

    /**
     * 指数に小数が指定された場合、エラーが検知されるかを検証します。
     */
    @Test(expected = CalculatorIllegalArgumentException.class)
    public void testPow_shouldThrowExceptionOnDecimalExponent() {
        MyNumber base = new MyNumber(2, 0);
        MyNumber exp = new MyNumber(15, 1); // 1.5乗
        base.pow(exp);
    }

    /**
     * ユーザーが巨大すぎる指数を入力した際、安全装置がCPUフリーズを防ぐかを検証します。
     */
    @Test(expected = CalculatorIllegalArgumentException.class)
    public void testPow_shouldThrowExceptionOnHugeExponent() {
        MyNumber base = new MyNumber(2, 0);
        MyNumber exp = new MyNumber(99999, 0);
        base.pow(exp);
    }

    // ==========================================
    // 5. Unary Operator & Parsing Tests
    // ==========================================

    /**
     * 単項マイナス（符号反転）を実行した際、正負が正しく逆転するかを検証します。
     */
    @Test
    public void testUnaryMinus_negation() {
        MyNumber n = new MyNumber(10, 1); // 1.0
        assertEquals("-1.0", n.unaryMinus().toString());
    }

    /**
     * double型の実数から、内部の整数値と小数点位置が正確にパースされるかを検証します。
     */
    @Test
    public void testParseToMyNumber_withDecimalString() {
        MyNumber parsed = MyNumber.parseToMyNumber(12.34);
        assertEquals("12.34", parsed.toString());
    }

    /**
     * 実質的に整数である実数が渡された際、末尾の不要な「.0」が綺麗にトリミングされるかを検証します。
     */
    @Test
    public void testParseToMyNumber_withIntegerEquivalentDouble() {
        MyNumber parsed = MyNumber.parseToMyNumber(5.0);
        assertEquals("5", parsed.toString());
    }

    // ==========================================
    // 6. Zero Value Specification Tests (★新規追加)
    // ==========================================

    /**
     * 値が0の場合、インスタンス生成時やパース時にscaleが強制的に0になり、
     * "0" という純粋な文字列が返るという「0の特別仕様」を厳格に検証します。
     */
    @Test
    public void testZero_shouldAlwaysHaveZeroScale() {
        // ① コンストラクタで敢えて大きなscaleを指定しても、内部で0に補正されるか
        MyNumber zeroWithScale = new MyNumber(0, 5);
        assertEquals(0, zeroWithScale.getScale());
        assertEquals("0", zeroWithScale.toString());

        // ② パースメソッドから 0.0 を読み込んだ際も、scaleが0として最適化されるか
        MyNumber parsedZero = MyNumber.parseToMyNumber(0.0);
        assertEquals(0, parsedZero.getScale());
        assertEquals("0", parsedZero.toString());

        // ③ 負のゼロ (-0.0) が流れ込んできた場合も、正の "0" (scale:0) に正規化されるか
        MyNumber negativeZero = MyNumber.parseToMyNumber(-0.0);
        assertEquals(0, negativeZero.getScale());
        assertEquals("0", negativeZero.toString());
    }

    // ==========================================
    // 7. Hardware & Type Limit Edge Cases (★新規追加)
    // ==========================================

    /**
     * 有効桁数15桁を超える巨大な整数を実数としてパースした際、
     * エラーを投げずに「double型の精度限界によるサイレントな丸め誤差」を
     * 容認したままパースを完了するシステム仕様（仕様書⑭のケース2）を検証します。
     * double 型は、64ビットのメモリを以下の3つの役割に明確に分担させて数字を表現しています。
     * 符号部（1ビット）：プラスかマイナスか
     * 指数部（11ビット）：小数点の位置（10の何乗にするか。これで308乗などの巨大なスケールを生み出す）
     * 仮数部（52ビット）：数字の並びそのもの（ここが有効桁数 15〜17桁 の正体）
     * 元のデータ: [101101101...（合計57ビット）...1110]
     *                                           ↓↓↓
     * 格納バケツ: [101101101...（合計53ビットまで）] [切り捨て・丸め]
     */
    @Test
    public void testParseToMyNumber_silentRoundingOfHugeDigits() {
        // ユーザーが画面で「123456789012345678」（18桁）を入力したと想定
        double input = 123456789012345678.0;

        // パースを呼び出す（内部でdoubleの仮数部限界により末尾が '80' に丸め込まれる）
        MyNumber parsed = MyNumber.parseToMyNumber(input);

        // エラーにならずに通過し、丸め込まれた近似値の状態で保持されていることを確認
        assertEquals("123456789012345680", parsed.toString());
    }

    /**
     * Long型のマイナス限界値（Long.MIN_VALUE）を実数としてパースした際、
     * double型としては正確に保持されるものの、DecimalFormatによる10進数文字列化の過程で
     * 床（Longの下限値）を下に突き抜けてアンダーフローとなり、
     * 安全にビジネス例外がスローされる挙動（仕様書⑭のケース1）を検証します。
     */
    @Test
    public void testParseToMyNumber_shouldThrowExceptionOnLongMinValueUnderflow() {
        // -9223372036854775808 は -2^63 のため double にキャストした瞬間は無傷
        double longMinValueAsDouble = (double) Long.MIN_VALUE;

        try {
            // パース実行（内部で "-9223372036854776000" に丸められ下限を突破する）
            MyNumber.parseToMyNumber(longMinValueAsDouble);
            org.junit.Assert.fail("Longの限界床を突き抜けたため、例外が発生しなければなりません");
        } catch (CalculatorIllegalArgumentException e) {
            // 期待通りの安全装置（ビジネス例外）が作動したことを確認
            assertEquals("入力された数値が大きすぎます（桁あふれ）", e.getMessage());
        }
    }

    /**
     * 内部計算などの結果、奇跡的にLong型のマイナス最小値に到達した場合、
     * toString()の内部に仕込まれた文字列ハック（Math.absを使わない符号反転回避）により、
     * クラッシュせずに安全に表示文字列を出力できるかを検証します。
     */
    @Test
    public void testToString_safeWithLongMinValue() {
        // 内部状態として Long.MIN_VALUE を持つインスタンスを擬似的に用意
        MyNumber n = new MyNumber(Long.MIN_VALUE, 0);

        // Math.abs(Long.MIN_VALUE) のオーバーフローに嵌ることなく、正確に表示できるか
        assertEquals("-9223372036854775808", n.toString());
    }

    /**
     * 画面にLong型のマイナス最小値が表示されている状態から「符号反転ボタン (+/-)」を押した場合、
     * 対になるプラスの表現限界（Long.MAX_VALUE）を「1」だけオーバーフローするため、
     * 計算段階で安全にブロックされる挙動（仕様書番号⑳）を検証します。
     */
    @Test(expected = CalculatorIllegalArgumentException.class)
    public void testUnaryMinus_shouldThrowExceptionOnLongMinValueNegation() {
        // 画面が「-9223372036854775808」の状態
        MyNumber n = new MyNumber(Long.MIN_VALUE, 0);

        // 符号反転ボタンを押す（内部計算で +9223372036854775808 になり、MAX_VALUE...807 を超える）
        n.unaryMinus();
    }
}