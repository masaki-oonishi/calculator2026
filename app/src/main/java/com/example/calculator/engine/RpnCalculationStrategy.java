package com.example.calculator.engine;

import com.example.calculator.exception.CalculatorIllegalArgumentException;
import com.example.calculator.model.MyNumber;

import net.objecthunter.exp4j.tokenizer.NumberToken;
import net.objecthunter.exp4j.tokenizer.OperatorToken;
import net.objecthunter.exp4j.tokenizer.Token;
import net.objecthunter.exp4j.tokenizer.Tokenizer;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

public class RpnCalculationStrategy implements CalculationStrategy {
    private MyOperator lastConstantOp = null;
    private MyNumber lastConstantValue = null;

    enum MyOperator{
        // 開きカッコを優先度0（一番低い）として定義
        // これにより、通常の演算子が来ても、開きカッコを勝手にポップして計算してしまうのを防げる
        PAREN_OPEN(0, 1),
        ADD(1, 2){ @Override MyNumber apply(MyNumber a, MyNumber b) { return a.add(b); } },
        SUBTRACT(1, 2){ @Override MyNumber apply(MyNumber a, MyNumber b) { return a.subtract(b); } },
        MULTIPLY(2, 2){ @Override MyNumber apply(MyNumber a, MyNumber b) { return a.multiply(b); } },
        DIVIDE( 2, 2){ @Override MyNumber apply(MyNumber a, MyNumber b) { return a.divide(b); } },
        POW(4, 2){ @Override MyNumber apply(MyNumber a, MyNumber b) { return a.pow(b); }},
        UNARY_MINUS(3, 1) { @Override MyNumber apply(MyNumber a, MyNumber b) { return b.unaryMinus(); }};

        //　計算の優先順位を定義
        final int priority;
        // 数値を１つしか使わない演算子を実装する場合に必要
        final int numOperands;
        MyOperator(int priority, int numOperands){
            this.priority = priority;
            this.numOperands = numOperands;
        }

        //パッケージprivate
        MyNumber apply(MyNumber a, MyNumber b) { return null; }

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

    /**
     *
     * @param expression
     * @return MyNumber
     * @throws CalculatorIllegalArgumentException
     */
    @Override
    public MyNumber evaluate(String expression){
        Deque<MyNumber> valueStack = new ArrayDeque<>();
        Deque<MyOperator> operatorStack = new ArrayDeque<>();

        Tokenizer tokenizer = new Tokenizer(expression, Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());

        Token previousToken = null;

        while (tokenizer.hasNext()) {
            Token token = tokenizer.nextToken();
//            System.out.println(token);

            if (token instanceof NumberToken) {
                double value = ((NumberToken) token).getValue();
                MyNumber myNumber = MyNumber.parseToMyNumber(value);
                valueStack.push(myNumber);

//                NumberToken numToken = (NumberToken) token;
//                System.out.println("[Operand (数値)] : " + numToken.getValue());

            } else if (token.getType() == Token.TOKEN_PARENTHESES_OPEN) {
                operatorStack.push(MyOperator.PAREN_OPEN);

            } else if (token.getType() == Token.TOKEN_PARENTHESES_CLOSE){
                if (previousToken != null && previousToken.getType() == Token.TOKEN_PARENTHESES_OPEN) {
                    throw new CalculatorIllegalArgumentException("カッコの中身が空です");
                }
                while(!operatorStack.isEmpty() && operatorStack.peek() != MyOperator.PAREN_OPEN){
                    executeTopCaluculation(valueStack, operatorStack);
                }

                if (operatorStack.isEmpty()) {
                    throw new CalculatorIllegalArgumentException("対応する開きカッコがありません");
                }
                // ( をスタックから出す
                operatorStack.pop();

            } else if (token instanceof OperatorToken) {
                String symbol = ((OperatorToken) token).getOperator().getSymbol();
                MyOperator currentOp;

                boolean isUnaryPosition = (previousToken == null
                        || previousToken.getType() == Token.TOKEN_PARENTHESES_OPEN);

                if (symbol.equals("-") && isUnaryPosition) {
                    currentOp = MyOperator.UNARY_MINUS;
                }
                else if (symbol.equals("+") && isUnaryPosition) {
                    throw new CalculatorIllegalArgumentException("単項プラスは未対応です");
                }
                else {
                    currentOp = MyOperator.fromSymbol(symbol);
                }

                while(!operatorStack.isEmpty() && operatorStack.peek().priority >= currentOp.priority){
                    executeTopCaluculation(valueStack, operatorStack);
                }

                operatorStack.push(currentOp);

//                OperatorToken opToken = (OperatorToken) token;
//                System.out.println("[Operator (演算子)] : " + opToken.getOperator().getSymbol()
//                        + " (優先度: " + opToken.getOperator()+ ")");
            }

            previousToken = token;
        }



        while(!operatorStack.isEmpty()){
            if (operatorStack.peek() == MyOperator.PAREN_OPEN) {
                throw new CalculatorIllegalArgumentException("対応する閉じカッコがありません");
            }

            if (operatorStack.size() == 1 && valueStack.size() >= 2) {
                this.lastConstantOp = operatorStack.peek();
                this.lastConstantValue = valueStack.peek();
            }

            executeTopCaluculation(valueStack, operatorStack);
        }

        if (valueStack.size() != 1) {
            throw new CalculatorIllegalArgumentException("数式の構文が不正です");
        }

        return valueStack.pop();
    }

    // ２つの値と演算子で計算処理
    private void executeTopCaluculation(Deque<MyNumber> values, Deque<MyOperator> operators){
        if(operators.isEmpty()) return;

        MyOperator op = operators.pop();

        if(op.numOperands == 1){
            if (values.isEmpty()) {
                throw new CalculatorIllegalArgumentException("数式が不完全です（数値が足りません）");
            }
            MyNumber b = values.pop();
            values.push(op.apply(null, b));
        }

        else {
            if(values.size() < 2){
                throw new CalculatorIllegalArgumentException("数式が不完全です（数値が足りません）");
            }
            MyNumber b = values.pop();
            MyNumber a = values.pop();

            MyNumber result = op.apply(a, b);
            values.push(result);
        }
    }

    @Override
    public MyNumber evaluateConstant(MyNumber previousResult){
        if(lastConstantOp == null || lastConstantValue == null){
            return previousResult;
        }

        return this.lastConstantOp.apply(previousResult ,this.lastConstantValue);
    }

    @Override
    public void clearConstant() {
        this.lastConstantOp = null;
        this.lastConstantValue = null;
    }
}
