package com.sagar.lockpattern_gridview.demo_patterngridview.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.sagar.lockpattern_gridview.PatternGridView;
import com.sagar.lockpattern_gridview.PatternInterface;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MatchPattern extends Activity {
    private static final int COLOR_VALID_PATTERN = Color.rgb(80, 200, 70);
    private static final int COLOR_INVALID_PATTERN = Color.rgb(255, 80, 50);
    private List<Integer> mPattern;
    PatternGridView mPatternGridView;
    TextView mStatusView;
    Timer mExitTimer, mClearTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_pattern);
        mStatusView = (TextView) findViewById(R.id.text_view_status);
        mStatusView.setText("Draw the stored pattern");
        final TimerTask successfulTimerTask = new TimerTask() {
            @Override
            public void run() {
                setResult(RESULT_OK);
                finish();
            }
        };
        mPattern = getIntent().getIntegerArrayListExtra(StorePattern.EXTRA_PATTERN);
        mPatternGridView = (PatternGridView) findViewById(R.id.pattern_view_store);
        mPatternGridView.setPatternListener(new PatternInterface.PatternListener() {
            @Override
            public void onPatternStarted() {
                mStatusView.setText("Release finger when done");
                if(mClearTimer != null){
                    mClearTimer.cancel();
                    mClearTimer = null;
                }
            }

            @Override
            public void onPatternEntered(List<Integer> pattern) {
                if(pattern.equals(mPattern)){
                    mPatternGridView.setRingColor(COLOR_VALID_PATTERN);
                    mPatternGridView.setInputEnabled(false);
                    mStatusView.setText("Pattern entered is correct");
                    mExitTimer = new Timer();
                    mExitTimer.schedule(successfulTimerTask, 1500);
                } else {
                    mPatternGridView.setRingColor(COLOR_INVALID_PATTERN);
                    mStatusView.setText("Incorrect pattern");
                    mClearTimer = new Timer();
                    mClearTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mPatternGridView.clearPattern();
                                }
                            });
                        }
                    }, 1500);
                }
            }

            @Override
            public void onPatternCleared() {
                mStatusView.setText("Enter the stored pattern");
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(0.4f);
    }
}
