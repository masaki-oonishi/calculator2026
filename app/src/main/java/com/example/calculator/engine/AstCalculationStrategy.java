package com.example.calculator.engine;

import com.example.calculator.exception.CalculatorIllegalArgumentException;
import com.example.calculator.model.MyNumber;

import net.objecthunter.exp4j.tokenizer.NumberToken;
import net.objecthunter.exp4j.tokenizer.OperatorToken;
import net.objecthunter.exp4j.tokenizer.Token;
import net.objecthunter.exp4j.tokenizer.Tokenizer;

import java.util.Collections;

public class AstCalculationStrategy implements CalculationStrategy {
    private BinaryNode lastBinaryNodeTemplate = null;
    private MyNumber lastConstantValue = null;
    private interface Expression {
        MyNumber eval();
    }

    private static class NumberNode implements Expression {
        private final MyNumber value;
        private NumberNode(MyNumber value) { this.value = value; };
        @Override
        public MyNumber eval(){ return this.value; };
    }

    private static class UnaryMinusNode implements Expression {
        private final Expression child;
        private UnaryMinusNode(Expression child) { this.child = child; };

        @Override
        public MyNumber eval() { return this.child.eval().unaryMinus(); };
    }

    private abstract static class BinaryNode implements Expression {
        protected final Expression left;
        protected final Expression right;

        protected BinaryNode(Expression left, Expression right){
            this.left = left;
            this.right = right;
        }

        @Override
        public MyNumber eval(){
            return calculate(left.eval(), right.eval());
        }

        public abstract MyNumber calculate(MyNumber leftVal, MyNumber rightVal);
    }

    private static class AddNode extends BinaryNode {
        protected AddNode(Expression left, Expression right){ super(left, right); };
        @Override
        public MyNumber calculate(MyNumber l, MyNumber r){ return l.add(r); };

    }

    private static class SubtractNode extends BinaryNode {
        private SubtractNode(Expression left, Expression right){ super(left, right); };
        @Override
        public MyNumber calculate(MyNumber l, MyNumber r) { return l.subtract(r); };
    }

    private static class MultiplyNode extends BinaryNode {
        private MultiplyNode(Expression left, Expression right){ super(left, right); };
        @Override
        public MyNumber calculate(MyNumber l, MyNumber r) { return l.multiply(r); };
    }

    private static class DivideNode extends BinaryNode {
        private DivideNode(Expression left, Expression right) { super(left, right); };
        @Override
        public MyNumber calculate(MyNumber l, MyNumber r) { return l.divide(r); };
    }

    private static class PowNode extends BinaryNode {
        private PowNode(Expression left, Expression right) { super(left, right); };
        @Override
        public MyNumber calculate(MyNumber l, MyNumber r) { return l.pow(r); };
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
                x = new PowNode(x, parsePower());
            }

            return x;
        }

        private Expression parseTerm(){
            Expression x = parsePower();
            for(;;){
                if(eatOperator("*")) x = new MultiplyNode(x, parsePower());
                else if(eatOperator("/")) x = new DivideNode(x, parsePower());
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
                if(eatOperator("+")) x = new AddNode(x, parseTerm());
                else if(eatOperator("-")) x = new SubtractNode(x, parseTerm());
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

        if (rootNode instanceof BinaryNode) {
            BinaryNode binaryRoot = (BinaryNode) rootNode;

            this.lastBinaryNodeTemplate = binaryRoot;
            this.lastConstantValue = binaryRoot.right.eval();
        }

        return rootNode.eval();
    }

    @Override
    public MyNumber evaluateConstant(MyNumber previousResult){
        if (lastBinaryNodeTemplate == null || lastConstantValue == null) {
            return previousResult;
        }

        return lastBinaryNodeTemplate.calculate(previousResult, lastConstantValue);
    }

    @Override
    public void clearConstant(){
        this.lastBinaryNodeTemplate = null;
        this.lastConstantValue = null;
    }
}
