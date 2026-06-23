package com.example.calculator.model;

import android.service.credentials.BeginGetCredentialOption;

import androidx.annotation.NonNull;

import com.example.calculator.exception.CalculatorIllegalArgumentException;

import java.math.BigInteger;
import java.text.DecimalFormat;

public class MyNumber {
    // ==========================================
    // クラス定数（設定値・マジックナンバーの排除）
    // ==========================================
    /** 割り算における標準の精度（小数点以下の調整桁数） */
    public static final int DEFAULT_PRECISION = 9;

    /** CPUハングアップ（フリーズ）を防ぐための、累乗計算における指数の絶対上限値 */
    private static final int MAX_POWER_EXPONENT = 9999;

    /** long型が物理的に表現できる最大の桁数（これ以上のスケールはlongに収まらない） */
    private static final int MAX_SCALE_LIMIT = 19;
    private final long value;
    private final int scale;

    public MyNumber(long value, int scale){
        this.value = value; //整数値
        this.scale = scale; //小数点以下の桁数（スケール）
    }

    public long getValue() { return value; }
    public int getScale() { return scale; }

    @NonNull
    @Override
    public String toString(){
        if (scale == 0) return String.valueOf(value);

        // Javaの Math.abs(long) は、最小値（Long.MIN_VALUE）を入れたときだけ、
        // プラスに反転できずにマイナスのまま返す
        // （対になるプラスの最大値より、マイナスの最小値の方が1だけ絶対値が大きいため、オーバーフローが発生）
        // StringBuilder s = new StringBuilder(String.valueOf(Math.abs(value)));
        String valueStr = String.valueOf(value);
        if (value < 0) {
            valueStr = valueStr.substring(1); // 先頭の "-" を削る
        }
        StringBuilder s = new StringBuilder(valueStr);
        while(s.length() <= scale){
            s.insert(0, "0");
        }
        int dotPosition = s.length() - scale;
        String result = s.substring(0, dotPosition) + "." + s.substring(dotPosition);
        return (value < 0) ? "-" + result : result;
    }

    public MyNumber add(MyNumber other){
        int maxScale = Math.max(this.scale, other.scale);

        try{
              // Math.powは内部がdouble型なので誤差が生じる、
              // そのため、整数で計算できるBigIntegerを使う
            BigInteger v1 = BigInteger.valueOf(this.value)
                    .multiply(BigInteger.TEN.pow(maxScale - this.scale));
            BigInteger v2 = BigInteger.valueOf(other.value)
                    .multiply(BigInteger.TEN.pow(maxScale - other.scale));

            BigInteger sum = v1.add(v2);
            return matchPrecisionAndCreate(sum, maxScale);
        }
        catch(ArithmeticException e){
            throw new CalculatorIllegalArgumentException("計算結果が大きすぎて処理できません（オーバーフロー）");
        }
    }

    public MyNumber subtract(MyNumber other) {
        int maxScale = Math.max(this.scale, other.scale);
        try {
            BigInteger v1 = BigInteger.valueOf(this.value)
                    .multiply(BigInteger.TEN.pow(maxScale - this.scale));
            BigInteger v2 = BigInteger.valueOf(other.value)
                    .multiply(BigInteger.TEN.pow(maxScale - other.scale));

            BigInteger diff = v1.subtract(v2);
            return matchPrecisionAndCreate(diff, maxScale);
        }
        catch (ArithmeticException e) {
            throw new CalculatorIllegalArgumentException("引き算の処理中に桁あふれが発生しました");
        }
    }

    public MyNumber multiply(MyNumber other){
        try{
            BigInteger v1 = BigInteger.valueOf(this.value);
            BigInteger v2 = BigInteger.valueOf(other.value);

            BigInteger result = v1.multiply(v2);
            int newScale = this.scale + other.scale;
            return matchPrecisionAndCreate(result, newScale);
        }
        catch(ArithmeticException e){
            throw new CalculatorIllegalArgumentException("計算結果が大きすぎて処理できません（オーバーフロー）");
        }

    }

    public MyNumber divide(MyNumber other){
        if (other.value == 0) {
            throw new CalculatorIllegalArgumentException("0で割ることはできません");
        }

        try{
            BigInteger bigThis = BigInteger.valueOf(this.value);
            BigInteger bigOther = BigInteger.valueOf(other.value);
            BigInteger bigMultiplier = BigInteger.TEN.pow(DEFAULT_PRECISION);

            BigInteger numerator = bigThis.multiply(bigMultiplier);
            BigInteger result = numerator.divide(bigOther);
            int newScale = this.scale + DEFAULT_PRECISION - other.scale;


            return matchPrecisionAndCreate(result, newScale);
        }
        catch(ArithmeticException e){
            throw new CalculatorIllegalArgumentException("割り算の精度調整中に桁あふれが発生しました");
        }
    }

    public MyNumber pow(MyNumber exponent){
        long exp;
        if(exponent.getScale() == 0){
            exp = exponent.getValue();
        }
        else{
            if (exponent.getScale() >= MAX_SCALE_LIMIT) {
                throw new CalculatorIllegalArgumentException("累乗の指数が精密すぎて処理できません");
            }
            BigInteger divisorBig = BigInteger.TEN.pow(exponent.getScale());
            BigInteger expValBig = BigInteger.valueOf(exponent.getValue());

            // remainderは余りを求める
            if(expValBig.remainder(divisorBig).compareTo(BigInteger.ZERO) == 0){
                exp = safeLongValue(expValBig.divide(divisorBig));
            } else {
                throw new CalculatorIllegalArgumentException("累乗の指数に小数は指定できません（整数のみ対応）");
            }
        }

        if(exp == 0) return new MyNumber(1, 0);

        // 安全装置：CPUハングアップを防止
        if(exp > MAX_POWER_EXPONENT || exp < -MAX_POWER_EXPONENT){
            throw new CalculatorIllegalArgumentException("累乗の指数が大きすぎます");
        }

        boolean isNegative = exp < 0;
        long absExp = (int) Math.abs(exp); // safeLongValueを通っているため安全にキャスト可能

        MyNumber base = this;
        MyNumber result = new MyNumber(1, 0);

        try{
            for(long i=0; i < absExp; i++){
                result = result.multiply(base);
            }
        }
        catch(CalculatorIllegalArgumentException e){
            throw new CalculatorIllegalArgumentException("累乗計算中に桁あふれ（オーバーフロー）が発生しました");
        }

        if(isNegative){
            MyNumber one = new MyNumber(1, 0);
            return one.divide(result);
        }
        return result;
    }

    public MyNumber unaryMinus(){
        try {
            long negatedValue = Math.negateExact(this.getValue());
            return new MyNumber(negatedValue, this.getScale());
        } catch (ArithmeticException e) {
            throw new CalculatorIllegalArgumentException("符号反転中に桁あふれが発生しました");
        }
    }

    private long safeLongValue(BigInteger bi){
        BigInteger minLong = BigInteger.valueOf(Long.MIN_VALUE);
        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);

        if(bi.compareTo(minLong) < 0 || bi.compareTo(maxLong) > 0){
            throw new ArithmeticException("Long integer overflow");
        }

        return bi.longValue();
    }

    private MyNumber matchPrecisionAndCreate(BigInteger result, int scale){
        if(scale > DEFAULT_PRECISION){
            int excessScale = scale - DEFAULT_PRECISION;
            result = result.divide(BigInteger.TEN.pow(excessScale));
            scale = DEFAULT_PRECISION;
        }

        return new MyNumber(safeLongValue(result), scale);
    }

    public static MyNumber parseToMyNumber(double d) {
        // 指数表記(E-6など)を禁止し、無駄な末尾の0をつけないフォーマッター
        // '#'を18個並べることで、double型の有効桁数（約15〜17桁）を完全にカバー
        DecimalFormat df = new DecimalFormat("#.##################");
        String s = df.format(d);

        long value;
        int scale;
        try {
            if (s.contains(".")) {
                scale = s.length() - s.indexOf(".") - 1;
                value = Long.parseLong(s.replace(".", ""));
            } else {
                scale = 0;
                value = Long.parseLong(s);
            }
            return new MyNumber(value, scale);
        } catch(NumberFormatException e){
            throw new CalculatorIllegalArgumentException("入力された数値が大きすぎます（桁あふれ）");
        }
    }
}
