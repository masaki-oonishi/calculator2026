package com.example.calculator.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.calculator.R;
import com.example.calculator.exception.CalculatorIllegalArgumentException;
import com.example.calculator.model.CalculationMemento;
import com.example.calculator.model.MyNumber;
import com.google.android.material.snackbar.Snackbar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private CalculatorViewModel viewModel;
    private TextView tvExpression;
    private TextView tvResult;

    private final int MAX_DISPLAY_DIGITS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(CalculatorViewModel.class);

        tvExpression = findViewById(R.id.tv_expression);
        tvResult = findViewById(R.id.tv_result);

        updateDisplay();
        if (viewModel.getLastResult() != null) {
            tvResult.setText(formatResultForDisplay(viewModel.getLastResult()));
        }

        int[] inputButtonIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
                R.id.btn_dot, R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply,
                R.id.btn_divide, R.id.btn_pow, R.id.btn_paren_open, R.id.btn_paren_close
        };

        for (int id : inputButtonIds) {
            findViewById(id).setOnClickListener(v -> {
                Button b = (Button) v;
                String inputText = b.getText().toString();

                if (viewModel.isLastActionEquals()) {
                    if (isOperator(inputText)) {
                        viewModel.clearExpression();
                        if (viewModel.getLastResult() != null) {
                            viewModel.appendToExpression(formatResultForDisplay(viewModel.getLastResult()));
                        }
                    } else {
                        viewModel.clearExpression();
                    }
                    viewModel.setLastActionEquals(false);
                }

                viewModel.appendToExpression(inputText);
                updateDisplay();
            });
        }

        Button btnClear = findViewById(R.id.btn_ac);
        findViewById(R.id.btn_ac).setOnClickListener(v -> {
            if(btnClear.getText().toString().equals("AC")){
                performAllClear();
            }
            else{
                performClear();
            }
        });

        findViewById(R.id.btn_equals).setOnClickListener(v -> {
            try {
                viewModel.executeEquals();

                if(viewModel.getLastResult() == null) return;

                String displayStr = formatResultForDisplay(viewModel.getLastResult());
                tvResult.setText(displayStr);

                if (displayStr.equals("Error")) {
                    showTopSnackbar(v, "表示可能な桁数を超えました");
                }

            } catch (CalculatorIllegalArgumentException e) {
                tvResult.setText("Error");
                showTopSnackbar(v, e.getMessage());
            } catch (Exception e) {
                tvResult.setText("Error");
                showTopSnackbar(v, "計算エラーが発生しました");
            } finally {
                updateDisplay();
            }
        });

        findViewById(R.id.btn_history).setOnClickListener(v -> showHistoryDialog());
    }

    /**
     * 画面上部（Statsの下）にポップアップを表示するヘルパーメソッド
     */
    private void showTopSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.TOP;
        params.topMargin = 140;

        snackbarView.setLayoutParams(params);
        snackbar.show();
    }


    private String formatResultForDisplay(MyNumber result) {
        try {
            BigDecimal bd = new BigDecimal(result.toString());
            BigDecimal absBd = bd.abs();

            BigDecimal maxLimit = BigDecimal.TEN.pow(MAX_DISPLAY_DIGITS).subtract(BigDecimal.ONE);
            if (absBd.compareTo(maxLimit) > 0) {
                return "Error";
            }

            BigDecimal minLimit = BigDecimal.ONE.divide(BigDecimal.TEN.pow(MyNumber.DEFAULT_PRECISION));
            if (absBd.compareTo(BigDecimal.ZERO) > 0 && absBd.compareTo(minLimit) < 0) {
                return "0";
            }

            int intLength = absBd.toBigInteger().toString().length();
            int maxFractionDigits = Math.max(0, MAX_DISPLAY_DIGITS - intLength);

            bd = bd.setScale(maxFractionDigits, RoundingMode.DOWN);
            bd = bd.stripTrailingZeros();

            return bd.toPlainString();
        } catch (Exception e) {
            return "Error";
        }
    }

    private void updateDisplay() {
        tvExpression.setText(viewModel.getCurrentExpression());

        Button btnClear = findViewById(R.id.btn_ac);

        if(viewModel.getCurrentExpression().isEmpty() || viewModel.isLastActionEquals()){
            btnClear.setText("AC");
        }
        else{
            btnClear.setText("C");
        }
    }

    private void performAllClear(){
        viewModel.executeAllClear();
        updateDisplay();
        tvResult.setText(getString(R.string.default_result));
    }

    private void performClear() {
        viewModel.executeClear();
        updateDisplay();
    }

    private boolean isOperator(String text) {
        return text.equals("+") || text.equals("-") || text.equals("*") ||
                text.equals("/") || text.equals("^");
    }

    private void showHistoryDialog(){
        List<CalculationMemento> history = viewModel.getHistory();
        if(history.isEmpty()){
            showTopSnackbar(findViewById(android.R.id.content), "履歴がありません");
            return;
        }

        String[] items = new String[history.size()];
        for(int i=0; i<history.size(); i++){
            CalculationMemento m = history.get(i);
            String resStr = formatResultForDisplay(m.getResult());
            items[i] = m.getExpression() + " = " + resStr;
        }

        new AlertDialog.Builder(this)
            .setTitle("計算履歴")
            .setItems(items, (dialog, which) -> {
                viewModel.restoreHistoryAt(which);
                updateDisplay();
                tvResult.setText(formatResultForDisplay(viewModel.getLastResult()));
                showTopSnackbar(findViewById(android.R.id.content), "履歴を復元しました");
            })
            .setNegativeButton("閉じる", null)
            .show();
    }
}