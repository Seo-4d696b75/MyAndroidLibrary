package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 1.0 on 2017/10/15.
 */

public abstract class MultipleStateButton extends View{

    public MultipleStateButton(Context context){
        this(context,null);
    }

    public MultipleStateButton(Context context, AttributeSet set){
        this(context,set,0);
    }

    public MultipleStateButton(Context context, AttributeSet set, int defaultAttr){
        super(context,set,defaultAttr);

        final TypedArray array = context.obtainStyledAttributes(set,R.styleable.MultipleStateButton,defaultAttr,0);

        Drawable defaultImage = context.getResources().getDrawable(R.drawable.button_back_default);
        mBackImages = new Drawable[5];
        for ( int i=0 ; i<5 ; i++ ){
            if ( array.hasValue(ATTR_BACKS[i]) ){
                Drawable drawable = array.getDrawable(ATTR_BACKS[i]);
                if ( drawable != null ){
                    setButtonDrawable(i,drawable);
                    continue;
                }
            }
            setButtonDrawable(i,defaultImage);
        }
        if ( array.hasValue(ATTR_STATE_NUM) ){
            int num = array.getInteger(ATTR_STATE_NUM,2);
            mStateNum = num <= 5 ? num : 2;
        }
        if ( array.hasValue(ATTR_STATE_ENTRY) ){
            int entry = array.getInteger(ATTR_STATE_ENTRY,0);
            mState = (entry >= 0 && entry < 5 ) ? entry : 0;
        }

        array.recycle();

        mTextPaint = new Paint();
        mTextPaint.setColor(0xff222222);
    }

    public static final int STATE_MAX = 5;
    private static final int ATTR_STATE_NUM = R.styleable.MultipleStateButton_state_num;
    private static final int ATTR_STATE_ENTRY = R.styleable.MultipleStateButton_state_entry;

    private static final int[] ATTR_BACKS = new int[]{
            R.styleable.MultipleStateButton_back_state_one,
            R.styleable.MultipleStateButton_back_state_two,
            R.styleable.MultipleStateButton_back_state_three,
            R.styleable.MultipleStateButton_back_state_four,
            R.styleable.MultipleStateButton_back_state_five
    };

    private int mStateNum = 2;
    private int mState;
    private boolean mBroadcasting;
    private String mText;
    private Paint mTextPaint;

    private OnStateChangedListener mListener;

    private final Drawable[] mBackImages;

    public void toggle(){
        setState((mState+1)%mStateNum);
    }

    @Override
    public boolean performClick(){
        toggle();
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if ( event.getAction() == MotionEvent.ACTION_UP ){
            performClick();
            return super.onTouchEvent(event);
        }
        super.onTouchEvent(event);
        return true;
    }

    public void setButtonDrawable(int state, Drawable back){
        if ( state >= 0 && state < 5 && back != null ){
            if ( mBackImages[state] != null ){
                mBackImages[state].setCallback(null);
                unscheduleDrawable(mBackImages[state]);
            }
            mBackImages[state] = back;
            back.setCallback(this);
            back.setVisible(getVisibility() == VISIBLE, false);
            //setMinHeight(back.getIntrinsicHeight());
        }
        refreshDrawableState();

    }

    public void setButtonDrawable(int state, @DrawableRes int resID){
        Drawable drawable = resID == 0 ? null : getResources().getDrawable(resID);
        setButtonDrawable(state, drawable);
    }

    public void setText(String text){
        this.mText = text;
    }

    public String getText(){
        return mText;
    }

    public int getState(){
        return mState;
    }

    public int getStateSize(){
        return mStateNum;
    }

    public void setStateSize(int size){
        if ( size > 1 && size <= STATE_MAX ){
            this.mStateNum = size;
        }
    }

    public void setState(int state){
        if ( state >= 0 && state < mStateNum && state != mState ){
            mState = state;
            refreshDrawableState();

            // Avoid infinite recursions if setState() is called from a listener
            if (mBroadcasting) {
                return;
            }
            mBroadcasting = true;
            if ( mListener != null ){
                mListener.onStateChanged(this,mState,mStateNum);
            }
            mBroadcasting = false;

        }
    }

    public void setOnStateChangedListener(OnStateChangedListener listener){
        this.mListener = listener;
    }

    /**
     * Interface definition for a callback to be invoked when the selected state
     * of a multiple-state button changed.
     */
    public interface OnStateChangedListener {
        /**
         * Called when the selected state of a multi-state button has changed.
         * @param button the multi-state button view whose state has changed
         * @param state the new selected state of buttonView
         * @param allState the total number of all the state
         */
        void onStateChanged(MultipleStateButton button, int state, int allState);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        final Drawable buttonDrawable = mBackImages[mState];
        if ( buttonDrawable != null ){

            //final int verticalGravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
            final int drawableHeight = buttonDrawable.getIntrinsicHeight();
            final int drawableWidth = buttonDrawable.getIntrinsicWidth();

            final int top = 0;
            final int bottom;
            final int left =  0;
            final int right = drawableWidth < getWidth() ? drawableWidth : getWidth();
            if ( drawableHeight < getHeight() ){

                bottom =  top + drawableHeight;
            }else{
                //top = 0;
                bottom = getHeight();
            }
            buttonDrawable.setBounds(left, top, right, bottom);
            buttonDrawable.draw(canvas);

            final String text = mText;
            if ( text != null){
                mTextPaint.setTextSize((float)(right - left) / text.length() / 2f);
                Paint.FontMetrics metrics = mTextPaint.getFontMetrics();
                float x = (right+left)/2 - mTextPaint.measureText(text)/2f;
                float y = (top + bottom) / 2 - (metrics.ascent + metrics.descent) / 2f;
                canvas.drawText(text, x, y, mTextPaint);
            }

        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mBackImages[getState()] != null) {
            invalidate();
        }
    }

    static class SavedState extends BaseSavedState {

        SavedState(Parcelable superState){
            super(superState);
        }

        private SavedState(Parcel source){
            super(source);
            state = source.readInt();
            stateNum = source.readInt();
        }

        private int state = 0;
        private int stateNum = 2;

        @Override
        public void writeToParcel(Parcel out, int flags){
            super.writeToParcel(out,flags);
            out.writeInt(state);
            out.writeInt(stateNum);
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US,
                    "MultipleStateButton.SavedState{%s state=%d, stateNum=%d}",
                    Integer.toHexString(System.identityHashCode(this)),state,stateNum
            );
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>(){

            @Override
            public SavedState createFromParcel(Parcel source){
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size){
                return new SavedState[size];
            }
        };

    }

    @Override
    public Parcelable onSaveInstanceState(){
        //setFreezesText(true);
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.state = getState();
        state.stateNum = getStateSize();
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state){
        SavedState myState = (SavedState)state;
        super.onRestoreInstanceState(myState.getSuperState());
        setStateSize(myState.stateNum);
        setState(myState.state);
        requestLayout();
    }




}
