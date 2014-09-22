package com.sagar.lockpattern_gridview;

import android.graphics.Color;

import java.util.List;

/**
 * Created by aravind on 20/9/14.
 * An interface which specifies the functionality which will be available in PatternGridView
 */
public interface PatternInterface {
    public void clearPattern();
    public List<Integer> getPattern();
    public void setRingColor(Color color);
    public void setOnPatternEnteredListener(OnPatternEnteredListener listener);
    public boolean isInputEnabled();
    public void setInputEnabled(boolean mIsInputEnabled);
    public int getPatternType();
    public void setPatternType(int mPatternType);

    public interface OnPatternEnteredListener{
        public void onPatternEntered(List<Integer> pattern);
    }
}
