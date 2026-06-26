package com.example.calculator.engine;

import androidx.annotation.NonNull;

import com.example.calculator.exception.CalculatorIllegalArgumentException;
import com.example.calculator.model.MyNumber;

import net.objecthunter.exp4j.tokenizer.NumberToken;
import net.objecthunter.exp4j.tokenizer.OperatorToken;
import net.objecthunter.exp4j.tokenizer.Token;
import net.objecthunter.exp4j.tokenizer.Tokenizer;

import java.util.Collections;

public class AstCalculationStrategy implements CalculationStrategy {
    private MyOperator lastBinaryNodeTemplate = null;
    private MyNumber lastConstantValue = null;
    private interface Expression {
        MyNumber eval();
    }

    private static class NumberNode implements Expression {
        private final MyNumber value;
        private NumberNode(MyNumber value) { this.value = value; };
        @Override
        public MyNumber eval(){ return this.value; };

        @NonNull
        public String toString(){ return value.toString(); };
    }

    private static class UnaryMinusNode implements Expression {
        private final Expression child;
        private UnaryMinusNode(Expression child) { this.child = child; };

        @Override
        public MyNumber eval() { return this.child.eval().unaryMinus(); };
    }

    private static class BinaryNode implements Expression {
        private final Expression left;
        private final Expression right;
        private final MyOperator operator;

        private BinaryNode(Expression left, Expression right, MyOperator operator){
            this.left = left;
            this.right = right;
            this.operator = operator;
        }

        @Override
        public MyNumber eval(){
            return operator.apply(left.eval(), right.eval());
        }

        @Override
        public String toString(){
            return operator.toString();
        }
    }

    private static class AstParser {
        private final Tokenizer tokenizer;
        private Token token = null;
        private AstParser(String expression){
            this.tokenizer = new Tokenizer(
                expression,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptySet()
            );
        }

        private void nextToken(){
            token = (tokenizer.hasNext()) ? tokenizer.nextToken() : null;
        }

        private boolean eatOperator(String expectedSymbol){
            if(token instanceof OperatorToken){
                OperatorToken opToken = (OperatorToken) token;
                String actualSymbol = opToken.getOperator().getSymbol();

                if(actualSymbol.equals(expectedSymbol)){
                    nextToken();
                    return true;
                }
            }
            return false;
        }

        private Expression parseFactor(){
            Expression x;
            //　（）なしで単項プラスとマイナスの処理をやりたいなら
//            if(eatOperator("-")){
//                x = new UnaryMinusNode(parseFactor());
//            }
//            else if(eatOperator("+")){
//                x = parseFactor();
//            }
            if(token.getType() == Token.TOKEN_PARENTHESES_OPEN){
                nextToken();
                x = parseExpression();
                if (token != null && token.getType() == Token.TOKEN_PARENTHESES_CLOSE) {
                    nextToken();
                } else {
                    throw new CalculatorIllegalArgumentException("対応する閉じカッコがありません");
                }
            }
            else if(token instanceof NumberToken){
                double value = ((NumberToken) token).getValue();
                MyNumber myNumber = MyNumber.parseToMyNumber(value);
                x = new NumberNode(myNumber);
                nextToken();
            }
            else{
                throw new CalculatorIllegalArgumentException("構文エラー");
            }
            return x;
        }

        private Expression parsePower(){
            Expression x = parseFactor();

            if(eatOperator("^")){
                x = new BinaryNode(x, parsePower(), MyOperator.POW);
            }

            return x;
        }

        private Expression parseTerm(){
            Expression x = parsePower();
            for(;;){
                if(eatOperator("*")) x = new BinaryNode(x, parsePower(), MyOperator.MULTIPLY);
                else if(eatOperator("/")) x = new BinaryNode(x, parsePower(), MyOperator.DIVIDE);
                else return x;
            }
        }

        private Expression parseExpression(){
            Expression x;

            // 単項マイナスは「式の一番最初」か「カッコの直後」だけ許可する
            if (eatOperator("-")) {
                x = new UnaryMinusNode(parseTerm());
            }
            else if (eatOperator("+")) {
                throw new CalculatorIllegalArgumentException("単項プラスは未対応です");
            }
            else{
                x = parseTerm();
            }

//            Expression x = parseTerm();

            for(;;){
                if(eatOperator("+")) x = new BinaryNode(x, parseTerm(), MyOperator.ADD);
                else if(eatOperator("-")) x = new BinaryNode(x, parseTerm(), MyOperator.SUBTRACT);
                else return x;
            }
        }

        public Expression parse(){
            nextToken();
            Expression root = parseExpression();
            return root;
        }
    }

    @Override
    public MyNumber evaluate(String expression){
        AstParser parser = new AstParser(expression);
        Expression rootNode = parser.parse();

        // BinaryNode以外（数式が「5」だけの場合など）は、通常通り評価して返す
        if(!(rootNode instanceof BinaryNode)) {
            return rootNode.eval();
        }

        BinaryNode binaryRoot = (BinaryNode) rootNode;

        this.lastBinaryNodeTemplate = binaryRoot.operator;

        MyNumber leftVal = binaryRoot.left.eval();
        MyNumber rightVal = binaryRoot.right.eval();

        this.lastConstantValue = rightVal;

        return binaryRoot.operator.apply(leftVal, rightVal);
    }

    @Override
    public MyNumber evaluateConstant(MyNumber previousResult){
        if (lastBinaryNodeTemplate == null || lastConstantValue == null) {
            return previousResult;
        }

        return lastBinaryNodeTemplate.apply(previousResult, lastConstantValue);
    }

    @Override
    public void clearConstant(){
        this.lastBinaryNodeTemplate = null;
        this.lastConstantValue = null;
    }

    @Override
    public String getLastConstantExpressionSnippet() {
        // 定数データがまだ無い場合は空文字を返す
        if (lastBinaryNodeTemplate == null || lastConstantValue == null) {
            return "";
        }

        String operator = lastBinaryNodeTemplate.toString();
        String constant = lastConstantValue.toString();

        return operator + constant;
    }
}
