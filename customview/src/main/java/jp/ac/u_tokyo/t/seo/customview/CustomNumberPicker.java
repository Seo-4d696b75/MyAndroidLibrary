package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.NumberPicker;

import java.util.Locale;

/**
 * A widget that enables the user to select a number from a predefined range.
 * Actually, Android SDK has a similar widget: {@link NumberPicker},
 * but the difference is that this widget also supports negative number
 * and that the step of scrolling number can be customized other than 1.
 * @author Seo-4d696b75
 * @version 1.0 on 2017/10/15
 */

public class CustomNumberPicker extends NumberPicker {

    public CustomNumberPicker (Context context){
        super(context);
    }

    public CustomNumberPicker (Context context, AttributeSet set){
        super(context,set);
        init(context,set);
    }

    public CustomNumberPicker(Context context, AttributeSet set, int defaultAttr){
        super(context,set,defaultAttr);
        init(context,set);
    }

    private void init(Context context,AttributeSet set){
        if ( !isInEditMode() ) {
            TypedArray array = context.obtainStyledAttributes(set, R.styleable.CustomNumberPicker, 0, 0);
            min = array.getInteger(R.styleable.CustomNumberPicker_min, 0);
            max = array.getInteger(R.styleable.CustomNumberPicker_max, 100);
            step = array.getInteger(R.styleable.CustomNumberPicker_step, 10);
            speed = array.getFloat(R.styleable.CustomNumberPicker_scrollSpeed,1f);
            shiftPoint = array.getInteger(R.styleable.CustomNumberPicker_speedShiftPoint,1);
            shiftRate = array.getFloat(R.styleable.CustomNumberPicker_speedShiftRate,1f);
            int value = array.getInteger(R.styleable.CustomNumberPicker_value, 0);
            array.recycle();
            setValues(false);
            setValue(value);
        }
    }

    private int min;
    private int max;
    private int step;
    private int[] valueSet;
    private float speed = 1f;
    private float lastYPos;
    private int width = 100;
    private int shiftPoint;
    private float shiftRate = 1f;
    private float currentSpeed;

    private OnValueChangeListener mListener;

    @Override
    public void setOnValueChangedListener(OnValueChangeListener listener){
        if ( listener == null ){
            mListener = null;
            super.setOnValueChangedListener(null);
            return;
        }
        mListener = listener;
        super.setOnValueChangedListener(new OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                if ( mListener != null ){
                    mListener.onValueChange(CustomNumberPicker.this, valueSet[oldVal], valueSet[newVal]);
                }
            }
        });
    }

    private void setValues(boolean keepValue){
        int value = 0;
        if ( keepValue ){
            value = getDisplayedValue();
        }
        //最大最小の逆転を修正
        if ( min > max ){
            int temp = min;
            min = max;
            max = temp;
        }
        //ステップ
        if ( step <= 0 || step > (max-min)){
            step = 1;
        }
        //最大最小をステップに合わせる、つまり上限下限と同じ扱いにする
        if ( max > 0 ){
            max = (max/step) * step;
        }else if ( max < 0 ){
            max = (-max%step == 0) ? max : -(-max/step + 1) * step;
        }
        if ( min > 0 ){
            min = (min%step == 0 ) ? min : (min/step + 1) * step;
        }else if ( min < 0 ){
            min = -(-min/step) * step;
        }
        //空集合の場合は初期値に変更
        if ( min > max ){
            min = 0;
            max = (step > 100) ? step : 100;
        }

        int length = (max-min)/step + 1;
        valueSet = new int[length];
        for ( int i=0 ; i<length ; i++){
            valueSet[i] = min + step*i;
        }
        super.setDisplayedValues(null);
        // super#setDisplayedValues した配列の長さより大きな範囲になる場合、IndexOutOfBounds で死ぬので一度nullにしておく
        super.setMinValue(0);
        super.setMaxValue(length-1);
        super.setDisplayedValues(getDisplayedValues(length));
        if ( keepValue ){
            setValue(value);
        }
    }

    protected String[] getDisplayedValues(int length){
        String[] displayedArray = new String[length];
        for ( int i=0 ; i<length ; i++){
            valueSet[i] = min + step*i;
            displayedArray[i] = String.valueOf(min + step*i);
        }
        return displayedArray;
    }

    private int getClosestIndex(int value){
        int val = 0;
        if ( value > max ){
            value = max;
        }else if ( value < min ){
            value = min;
        }
        if (value > 0) {
            val = (value%step >= (step+1)/2) ? (value/step + 1) * step : (value/step) * step;
        } else if ( value < 0 ){
            val = (-value%step <= step/2) ? -(-value/step) * step : -(-value/step + 1) * step;
        }
        return (val-min)/step;
    }


    /**
     * Returns the INDEX of current value.
     * @deprecated This does not returns the value displayed on widget.
     */
    @Override
    public int getValue(){
        return super.getValue();
    }

    /**
     * Returns the value of this picker.
     * @return the value
     */
    public int getDisplayedValue(){
        int pos = super.getValue();
        return valueSet[pos];
    }

    /**
     * Sets the current value for this picker.
     * <strong>Note </strong> This method DOES on the value displayed on this widget.
     * If the argument is not multiple of the step value set at {@link #setStep(int)},
     * the argument is modified to the closet value in multiples of the step.
     * @param value the current value
     */
    @Override
    public void setValue(int value){
        int pos = getClosestIndex(value);
        super.setValue(pos);
    }

    /**
     * This is elias of {@link #setValue(int)}
     */
    public void setDisplayedValue(int value){
        this.setValue(value);
    }

    /**
     * Sets the lower value.
     * If the argument is not multiple of the step value,
     * the argument is modified to the least value over it and in multiples of step.
     * @param min the lower value
     */
    @Override
    public void setMinValue(int min){
        this.min = min;
        setValues(true);
    }

    /**
     * Sets the upper value.
     * If the argument is not multiple of the step value,
     * the argument is modified to the largest value less than it and in multiples of step.
     * @param max the upper value
     */
    @Override
    public void setMaxValue(int max){
        this.max = max;
        setValues(true);
    }

    /**
     * Sets the step value.
     * If the argument is negative, it is modified to {@code 1}.
     * @param step the step value
     */
    public void setStep(int step){
        this.step = step;
        setValues(true);
    }

    /**
     * Returns the step value.
     * @return the step value
     */
    public int getStep(){
        return step;
    }

    /**
     * Returns min index of displayed-value array.
     * Normally, it is supposed that this always returns value 0.
     * <strong>Note </strong> This dose not return the min value displayed on the widget.
     * You can get min displayed-value from {@link #getDisplayedMinValue()}
     * @deprecated
     * This returns value different of min displayed-value
     */
    @Override
    public int getMinValue(){
        return super.getMinValue();
    }

    public int getDisplayedMinValue(){
        return min;
    }

    /**
     * Returns max index of displayed-value array
     * <strong>Note </strong> This dose not return the max value displayed on the widget.
     * You can get max displayed-value from {@link #getDisplayedMaxValue()}
     * @deprecated
     * This returns value different of min displayed-value
     */
    @Override
    public int getMaxValue(){
        return super.getMaxValue();
    }

    public int getDisplayedMaxValue(){
        return max;
    }

    /**
     * Sets the speed at which the value is incremented and decremented when scrolled.
     * The argument is on the basis of the usual speed.
     * If {@code 1.0} is passed, no change is applied.
     * If negative value is passed, the direction in which this picker drum scrolls is inverted.
     * Be careful that user cannot scroll this if {@code 0.0} is passed.
     * @param speed the scroll speed
     */
    public void setScrollSpeed(float speed){
        this.speed = speed;
    }

    /**
     * Returns the speed at which this picker drum is scrolled.
     * @return the scroll speed
     * @see #setScrollSpeed(float)
     */
    public float getScrollSpeed(){
        return speed;
    }

    /**
     * Sets how many points the scroll speed shifts up at.
     * If argument is less than 1, it is modified to 1.
     * The width of this view is divided horizontally in the same number as this argument,
     * the scroll speed shifts up when touched position goes next to felt.
     * @param shift the number of speed levels
     */
    public void setScrollSpeedShiftPoint(int shift){
        this.shiftPoint = shift;
    }

    /**
     * Sets how much the scroll speed shifts up.
     * @param rate
     * @see #setScrollSpeedShiftPoint(int)
     */
    public void setScrollSpeedShiftRate(float rate){
        this.shiftRate = rate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        switch ( event.getActionMasked() ){
            case MotionEvent.ACTION_DOWN:
                if ( shiftPoint < 1 ){
                    shiftPoint = 1;
                }
                if ( shiftRate < 1f ){
                    shiftRate = 1f;
                }
                lastYPos = event.getY();
                int section = shiftPoint-1-(int)(event.getX()*shiftPoint/width);
                float index = (float)Math.pow(shiftRate,section);
                currentSpeed = speed * index;
                break;
            case MotionEvent.ACTION_MOVE:
                float currentMoveY = event.getY();
                int deltaMoveY = (int) ((currentMoveY - lastYPos) * (currentSpeed - 1f));
                super.scrollBy(0, deltaMoveY);
                lastYPos = currentMoveY;
                break;
            default:
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        width = getWidth();
    }

    static class SavedState extends BaseSavedState {

        SavedState(Parcelable superState){
            super(superState);
        }

        private SavedState(Parcel source){
            super(source);
            min = source.readInt();
            max = source.readInt();
            step = source.readInt();
            speed = source.readFloat();
            shiftPoint = source.readInt();
            shiftRate = source.readFloat();
            value = source.readInt();
        }

        private int min;
        private int max;
        private int step;
        private float speed;
        private int shiftPoint;
        private float shiftRate;
        private int value;

        @Override
        public void writeToParcel(Parcel out, int flags){
            super.writeToParcel(out,flags);
            out.writeInt(min);
            out.writeInt(max);
            out.writeInt(step);
            out.writeFloat(speed);
            out.writeInt(shiftPoint);
            out.writeFloat(shiftRate);
            out.writeInt(value);
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US,
                    "CustomNumberPicker.SavedState{%s value=%d, min=%d, max=%d, step=%d, speed=%f}",
                    Integer.toHexString(System.identityHashCode(this)),value,min,max,step,speed
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
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.min = getDisplayedMinValue();
        state.max = getDisplayedMaxValue();
        state.value = getDisplayedValue();
        state.step = getStep();
        state.speed = getScrollSpeed();
        state.shiftPoint = this.shiftPoint;
        state.shiftRate = this.shiftRate;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state){
        SavedState myState = (SavedState)state;
        super.onRestoreInstanceState(myState.getSuperState());
        min = myState.min;
        max = myState.max;
        step = myState.step;
        speed = myState.speed;
        shiftPoint = myState.shiftPoint;
        shiftRate = myState.shiftRate;
        setValues(false);
        setValue(myState.value);
        requestLayout();
    }

}


