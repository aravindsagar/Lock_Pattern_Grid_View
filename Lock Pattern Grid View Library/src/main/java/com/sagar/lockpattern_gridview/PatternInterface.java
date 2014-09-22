package com.sagar.lockpattern_gridview;

import java.util.List;

/**
 * Created by aravind on 20/9/14.
 * An interface which specifies the functionality which will be available in PatternGridView
 */
public interface PatternInterface {
    public void clearPattern();
    public List<Integer> getPattern();
    public void setRingColor(int color);
    public void setPatternListener(PatternListener listener);
    public boolean isInputEnabled();
    public void setInputEnabled(boolean mIsInputEnabled);
    public int getPatternType();
    public void setPatternType(int mPatternType);

    public interface PatternListener{
        public void onPatternStarted();
        public void onPatternEntered(List<Integer> pattern);
        public void onPatternCleared();
    }
}
