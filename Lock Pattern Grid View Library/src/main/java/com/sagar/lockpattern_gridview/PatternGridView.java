package com.sagar.lockpattern_gridview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aravind on 20/9/14.
 * A view which implements a pattern lock grid, like pattern lock in android lock screen
 */
public class PatternGridView extends View implements PatternInterface{

    public static final int PATTERN_TYPE_STORE = 0;
    public static final int PATTERN_TYPE_CHECK = 1;

    private static final int NUMBER_OF_COLUMNS = 3;
    private static final int NUMBER_OF_ROWS = 3;

    private final float minCellSize = getResources().getDimension(R.dimen.min_cell_size);

    private int mPatternType;

    private int mPaddingLeft, mPaddingRight, mPaddingTop, mPaddingBottom;

    private float mCellHeight, mCellWidth;

    private OnPatternEnteredListener mOnPatternEnteredListener;

    private Paint mInnerCirclePaint, mInnerCircleHollowPaint, mOuterCirclePaint,
            mOuterCircleCustomPaint, mLinePaint, mTrianglePaint;

    private int[] mRowCenters = new int[NUMBER_OF_ROWS];
    private int[] mColumnCenters = new int[NUMBER_OF_COLUMNS];
    private int[] mRowUpperBound = new int[NUMBER_OF_COLUMNS];
    private int[] mColumnUpperBound = new int[NUMBER_OF_COLUMNS];
    private int[] mRowLowerBound = new int[NUMBER_OF_COLUMNS];
    private int[] mColumnLowerBound = new int[NUMBER_OF_COLUMNS];

    private int mInnerCircleRadius = (int) getResources().getDimension(R.dimen.inner_circle_radius),
            mOuterCircleRadius = (int) getResources().getDimension(R.dimen.outer_circle_radius);

    private boolean mIsInputEnabled = true;

    private enum PatternState{BLANK, IN_PROGRESS, ENTERED};
    private PatternState mPatternState = PatternState.BLANK;

    private Path mPath = new Path();

    private int mCurrentX, mCurrentY;

    private Path[][] mTrianglePath = new Path[NUMBER_OF_ROWS][NUMBER_OF_COLUMNS];
    private Path[][] mTrianglePathBackup = new Path[NUMBER_OF_ROWS][NUMBER_OF_COLUMNS];
    private Matrix mTransformation = new Matrix();

    private CellTracker mCellTracker = new CellTracker();

    public PatternGridView(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(attributeSet,
                R.styleable.PatternGridView, 0, 0);
        try{
            mPatternType = styledAttributes.getInt(R.styleable.PatternGridView_patternType, 0);
        }finally {
            styledAttributes.recycle();
        }
        init();
    }

    private void init(){
        mInnerCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInnerCirclePaint.setStyle(Paint.Style.FILL);

        mInnerCircleHollowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInnerCircleHollowPaint.setStyle(Paint.Style.STROKE);
        mInnerCircleHollowPaint.setStrokeWidth(mInnerCircleRadius/4.0f);

        mOuterCirclePaint= new Paint(Paint.ANTI_ALIAS_FLAG);
        mOuterCirclePaint.setStyle(Paint.Style.STROKE);
        mOuterCirclePaint.setStrokeWidth(mInnerCircleRadius);

        mOuterCircleCustomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOuterCircleCustomPaint.setStyle(Paint.Style.STROKE);
        mOuterCircleCustomPaint.setStrokeWidth(mInnerCircleRadius);
        mOuterCircleCustomPaint.setColor(Color.BLUE);

        mTrianglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrianglePaint.setStyle(Paint.Style.FILL);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setDither(true);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStrokeWidth(mInnerCircleRadius * 1.5f);

        switch (mPatternType){
            case PATTERN_TYPE_STORE:
                mInnerCirclePaint.setColor(Color.BLACK);
                mInnerCircleHollowPaint.setColor(Color.BLACK);
                mOuterCirclePaint.setColor(Color.BLACK);
                mTrianglePaint.setColor(Color.BLACK);
                mLinePaint.setColor(Color.BLACK);
                break;
            case PATTERN_TYPE_CHECK:
                mInnerCirclePaint.setColor(Color.WHITE);
                mInnerCircleHollowPaint.setColor(Color.WHITE);
                mOuterCirclePaint.setColor(Color.WHITE);
                mTrianglePaint.setColor(Color.WHITE);
                mLinePaint.setColor(Color.WHITE);
                break;
        }

        mLinePaint.setAlpha(100);

        mPaddingLeft = getPaddingLeft();
        mPaddingRight = getPaddingRight();
        mPaddingTop = getPaddingTop();
        mPaddingBottom = getPaddingBottom();
    }

    @Override
    public int getPatternType() {
        return mPatternType;
    }

    @Override
    public void setPatternType(int mPatternType) {
        this.mPatternType = mPatternType;
        invalidate();
        requestLayout();
    }

    @Override
    public boolean isInputEnabled(){
        return mIsInputEnabled;
    }

    @Override
    public void setInputEnabled(boolean mIsInputEnabled){
        this.mIsInputEnabled = mIsInputEnabled;
    }

    @Override
    public void clearPattern() {
        mCellTracker.clear();
    }

    @Override
    public List<Integer> getPattern() {
        return mCellTracker.getCellNumberList();
    }

    @Override
    public void setRingColor(Color color) {

    }

    @Override
    public void setOnPatternEnteredListener(OnPatternEnteredListener listener){
        mOnPatternEnteredListener = listener;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return (int) (NUMBER_OF_ROWS * minCellSize);
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return (int) (NUMBER_OF_COLUMNS * minCellSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int width = w - (mPaddingLeft + mPaddingRight);
        int height = h - (mPaddingTop + mPaddingBottom);

        mCellHeight = height/(float) NUMBER_OF_ROWS;
        mCellWidth = width/(float) NUMBER_OF_COLUMNS;
        mOuterCircleRadius = Math.max ((int) (mCellWidth/4.0) + mInnerCircleRadius, mOuterCircleRadius);
        computePositions();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void computePositions(){
        int x = mPaddingLeft;
        for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
            mColumnCenters[i] = (int) (x + mCellWidth/2.0);
            mColumnLowerBound[i] = mColumnCenters[i] - mOuterCircleRadius;
            mColumnUpperBound[i] = mColumnCenters[i] + mOuterCircleRadius;
            x += mCellWidth;
        }
        int y = mPaddingTop;
        for (int i = 0; i < NUMBER_OF_ROWS; i++) {
            mRowCenters[i] = (int) (y + mCellHeight/2.0);
            mRowLowerBound[i] = mRowCenters[i] - mOuterCircleRadius;
            mRowUpperBound[i] = mRowCenters[i] + mOuterCircleRadius;
            y += mCellHeight;
        }
        for (int i = 0; i < NUMBER_OF_ROWS; i++) {
            for (int j = 0; j < NUMBER_OF_COLUMNS; j++) {
                mTrianglePathBackup[i][j] = new Path();
                int startX = mColumnCenters[j] + mOuterCircleRadius + mInnerCircleRadius,
                        startY = mRowCenters[i] + mInnerCircleRadius;
                mTrianglePathBackup[i][j].moveTo(startX, startY);
                mTrianglePathBackup[i][j].lineTo(startX, startY - 2 * mInnerCircleRadius);
                mTrianglePathBackup[i][j].lineTo(startX + mInnerCircleRadius, startY - mInnerCircleRadius);
                mTrianglePathBackup[i][j].lineTo(startX, startY);
                mTrianglePathBackup[i][j].close();
                mTrianglePathBackup[i][j].setFillType(Path.FillType.EVEN_ODD);

                mTrianglePath[i][j] = new Path(mTrianglePathBackup[i][j]);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int minimumWidth = getSuggestedMinimumWidth();
        final int minimumHeight = getSuggestedMinimumHeight();
        int viewWidth = resolveMeasured(widthMeasureSpec, minimumWidth);
        int viewHeight = resolveMeasured(heightMeasureSpec, minimumHeight);

        int viewDimension = Math.min(viewHeight, viewWidth);
        setMeasuredDimension(viewDimension, viewDimension);
    }

    private int resolveMeasured(int measureSpec, int desired) {
        int result = 0;
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (MeasureSpec.getMode(measureSpec)) {
            case MeasureSpec.UNSPECIFIED:
                if((float) measureSpec < 1.2 * desired)
                    result = Math.max(specSize, desired);
                else
                    result = desired;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(specSize, desired);
                break;
            case MeasureSpec.EXACTLY:
            default:
                result = specSize;
        }
        return result;
    }

    private class CellTracker {
        private final String LOG_TAG = CellTracker.class.getSimpleName();
        private static final int mCells = NUMBER_OF_ROWS * NUMBER_OF_COLUMNS;

        private boolean[] isIncluded = new boolean[mCells];
        private ArrayList<Integer> mCellList = new ArrayList<Integer>(mCells);

        public CellTracker(){
            clear();
        }

        public boolean addCell(int cellNumber){
            if(cellNumber >= mCells || cellNumber < 0){
                return false;
            }
            if(isIncluded[cellNumber]){
                return false;
            } else {
                if(mCellList.size() > 0){
                    int currentCell = cellNumber,
                            previousCell = mCellList.get(mCellList.size() - 1);
                    int currentRow = getRowFromCellNumber(currentCell),
                            currentColumn = getColumnFromCellNumber(currentCell),
                            previousRow = getRowFromCellNumber(previousCell),
                            previousColumn = getColumnFromCellNumber(previousCell);
                    int dRow = Math.abs(currentRow - previousRow),
                            dColumn = Math.abs(currentColumn - previousColumn);
                    //Filling in gaps in the pattern, and adding the cells in those gaps
                    boolean redoFlag = false;
                    if(dRow == 2 && dColumn != 1){
                        addCell(getCellNumberFromRowAndColumn(1,
                                (dColumn==0)?currentColumn:1));
                        redoFlag = true;
                    } else if(dColumn == 2 && dRow != 1){
                        addCell(getCellNumberFromRowAndColumn(currentRow,1));
                        redoFlag = true;
                    }
                    if(redoFlag){
                        previousCell = mCellList.get(mCellList.size() - 1);
                        previousRow = getRowFromCellNumber(previousCell);
                        previousColumn = getColumnFromCellNumber(previousCell);
                    }
                    //Now setting rotation for the cute little triangles
                    int cellCenterX = mColumnCenters[currentColumn],
                            cellCenterY = mRowCenters[currentRow],
                            previousCellCenterX = mColumnCenters[previousColumn],
                            previousCellCenterY = mRowCenters[previousRow];

                    int xDiff = cellCenterX - previousCellCenterX, yDiff = cellCenterY - previousCellCenterY;
                    double rotAngle;
                    if(xDiff == 0){
                        if(yDiff < 0)
                            rotAngle = -90;
                        else
                            rotAngle = 90;
                    } else {
                        rotAngle = Math.atan((double)yDiff/(double)xDiff);
                        rotAngle = rotAngle * 180 / Math.PI;
                    }
                    if(xDiff < 0){
                        rotAngle += 180;
                    }
                    mTransformation.reset();
                    mTransformation.postRotate((float) rotAngle,
                            mColumnCenters[previousColumn], mRowCenters[previousRow]);
                    mTrianglePath[previousRow][previousColumn].transform(mTransformation);
                }
                //Adding the cell
                isIncluded[cellNumber] = true;
                mCellList.add(cellNumber);
                return true;
            }
        }

        public List<Integer> getCellNumberList(){
            return mCellList;
        }

        public void clear(){
            mCellList.clear();
            for (int i = 0; i < mCells; i++) {
                isIncluded[i] = false;
            }
            for (int i = 0; i < NUMBER_OF_ROWS; i++) {
                for (int j = 0; j < NUMBER_OF_COLUMNS; j++) {
                    if(mTrianglePath[i][j] != null) {
                        mTrianglePath[i][j].set(mTrianglePathBackup[i][j]);
                    }
                }
            }
        }

        public boolean isCellIncluded(int cellNumber){
            if(cellNumber >= mCells || cellNumber < 0){
                return false;
            }
            return isIncluded[cellNumber];
        }

        public void setPath(Canvas canvas){
            if(mCellList.isEmpty())
                return;
            int currentRow = getRowFromCellNumber(mCellList.get(0)),
                    currentColumn = getColumnFromCellNumber(mCellList.get(0)),
                    previousRow, previousColumn;
            int cellCenterY = mRowCenters[currentRow],
                    cellCenterX = mColumnCenters[currentColumn];
            mPath.moveTo(cellCenterX, cellCenterY);
            for (int i = 1; i < mCellList.size(); i++) {
                //Setting path from previous point to current point
                previousRow = currentRow;
                previousColumn = currentColumn;
                currentRow = getRowFromCellNumber(mCellList.get(i));
                currentColumn = getColumnFromCellNumber(mCellList.get(i));
                cellCenterY = mRowCenters[currentRow];
                cellCenterX = mColumnCenters[currentColumn];
                mPath.lineTo(cellCenterX, cellCenterY);
                //Drawing the cute tiny triangles :)
                canvas.drawPath(mTrianglePath[previousRow][previousColumn], mTrianglePaint);
            }
            if(mPatternState == PatternState.IN_PROGRESS)
                mPath.lineTo(mCurrentX, mCurrentY);
        }

        private int getRowFromCellNumber(int cellNumber){
            if(cellNumber >= mCells || cellNumber < 0){
                return -1;
            }
            return cellNumber/NUMBER_OF_COLUMNS;
        }
        private int getColumnFromCellNumber(int cellNumber){
            if(cellNumber >= mCells || cellNumber < 0){
                return -1;
            }
            return cellNumber%3;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < NUMBER_OF_ROWS; i++) {
            for (int j = 0; j < NUMBER_OF_COLUMNS; j++) {
                if(!(mPatternState == PatternState.ENTERED)) {
                    canvas.drawCircle(mColumnCenters[j], mRowCenters[i], mInnerCircleRadius, mInnerCirclePaint);
                    if (mCellTracker.isCellIncluded(getCellNumberFromRowAndColumn(i, j))) {
                        canvas.drawCircle(mColumnCenters[j], mRowCenters[i], mOuterCircleRadius, mOuterCirclePaint);
                    }
                } else {
                    if (mCellTracker.isCellIncluded(getCellNumberFromRowAndColumn(i, j))) {
                        canvas.drawCircle(mColumnCenters[j], mRowCenters[i], mOuterCircleRadius, mOuterCircleCustomPaint);
                        canvas.drawCircle(mColumnCenters[j], mRowCenters[i], mInnerCircleRadius, mInnerCircleHollowPaint);
                    } else {
                        canvas.drawCircle(mColumnCenters[j], mRowCenters[i], mInnerCircleRadius, mInnerCirclePaint);
                    }
                }
            }
        }
        if(mPatternState != PatternState.BLANK){
            mPath.rewind();
            mCellTracker.setPath(canvas);
            canvas.drawPath(mPath, mLinePaint);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!mIsInputEnabled){
            return false;
        }
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                break;
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                break;
            case MotionEvent.ACTION_CANCEL:
                handleActionCancel(event);
        }
        return true;
    }

    private void handleActionDown(MotionEvent event){
        mCellTracker.clear();
        if(mCellTracker.addCell(getCell(event))){
            mPatternState = PatternState.IN_PROGRESS;
        } else if(mPatternState == PatternState.ENTERED){
            mPatternState = PatternState.BLANK;
        }
        invalidate();
        handleActionMove(event);
    }

    private void handleActionMove(MotionEvent event){
        for(int i=0; i<event.getHistorySize(); i++){
            mCellTracker.addCell(getHistoricalCell(event,i));
            if(mPatternState == PatternState.BLANK){
                mPatternState = PatternState.IN_PROGRESS;
            }
        }
        mCellTracker.addCell(getCell(event));
        mCurrentX = (int) event.getX();
        mCurrentY = (int) event.getY();
        invalidate();
    }

    private void handleActionUp(MotionEvent event){
        List<Integer> cellList = mCellTracker.getCellNumberList();
        if(cellList != null && !cellList.isEmpty() && mOnPatternEnteredListener != null){
            mOnPatternEnteredListener.onPatternEntered(cellList);
        }
        mPatternState = PatternState.ENTERED;
        invalidate();
    }

    private void handleActionCancel(MotionEvent event){
        Log.d(VIEW_LOG_TAG, "Cancel");
    }

    private int getCell(MotionEvent event){
        float x = event.getX(), y = event.getY();
        int row = getRowFromY(y);
        if(row < 0){
            return -1;
        }
        int column = getColumnFromX(x);
        if(column < 0){
            return -1;
        }
        return getCellNumberFromRowAndColumn(row, column);
    }

    private int getHistoricalCell(MotionEvent event, int pos){
        float x = event.getHistoricalX(pos), y = event.getHistoricalY(pos);
        int row = getRowFromY(y);
        if(row < 0){
            return -1;
        }
        int column = getColumnFromX(x);
        if(column < 0){
            return -1;
        }
        return getCellNumberFromRowAndColumn(row, column);
    }

    private int getRowFromY(float y){
        int row = -1;
        for (int i = 0; i < NUMBER_OF_ROWS; i++) {
            if(y >= mRowLowerBound[i] && y <= mRowUpperBound[i]){
                row = i;
                break;
            }
        }
        return row;
    }

    private int getColumnFromX(float x){
        int column = -1;
        for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
            if(x >= mColumnLowerBound[i] && x <= mColumnUpperBound[i]){
                column = i;
                break;
            }
        }
        return column;
    }

    private int getCellNumberFromRowAndColumn(int row, int column){
        return row * NUMBER_OF_COLUMNS + column;
    }

    private class TriangleHelper{

    }
}