package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringDef;
import android.util.AttributeSet;
import android.widget.NumberPicker;

import java.util.Locale;

/**
 * A widget that enables the user to select float value.<br>
 * This widget extends {@link CustomNumberPicker}, which supports integer value only, and
 * this has the same setter and getter about the displayed, min, max value.
 * But these getter and setter handle the value in integer, not in float, so use instead;<br>
 * displayed value on this view : {@link #getValueFloat()} {@link #setValueFloat(float)}<br>
 * min value in the range : {@link #getMinFloat()} {@link #setMinFloat(float)}<br>
 * max value in the range : {@link #getMaxFloat()} {@link #setMaxFloat(float)}
 * @author Seo-4d696b75
 * @version 1.0 on 2017/10/15.
 */
public class FloatPicker extends CustomNumberPicker implements NumberPicker.OnValueChangeListener{

    public FloatPicker (Context context){
        super(context);
    }

    public FloatPicker (Context context, AttributeSet set){
        super(context,set);
        init(context,set);
    }

    public FloatPicker (Context context, AttributeSet set, int defaultAttr){
        super(context,set,defaultAttr);
        init(context,set);
    }

    private void init (Context context, AttributeSet set){
        if ( !isInEditMode() ) {
            TypedArray array = context.obtainStyledAttributes(set, R.styleable.FloatPicker,0,0);
            max = array.getFloat(R.styleable.FloatPicker_maxFloat,10f);
            min = array.getFloat(R.styleable.FloatPicker_minFloat,0f);
            float step = array.getFloat(R.styleable.FloatPicker_stepFloat,0.1f);
            value = array.getFloat(R.styleable.FloatPicker_valueFloat,0f);
            array.recycle();
            super.setStep(1);
            setStepFloat(step);
        }
    }

    @Override
    public final void onValueChange(NumberPicker picker, int oldVal, int newVal){
        if ( listener != null ){
            listener.onValueChange(this, oldVal*step, newVal*step);
        }
    }

    public interface OnFloatValueChangeListener {
        void onValueChange(FloatPicker picker, float oldVar, float newVar);
    }

    public void setOnFloatValueChangeListener(OnFloatValueChangeListener listener){
        if ( listener == null ){
            this.listener = null;
            super.setOnValueChangedListener(null);
            return;
        }
        this.listener = listener;
        super.setOnValueChangedListener(this);
    }

    /**
     * Use {@link #setOnFloatValueChangeListener(OnFloatValueChangeListener)} instead.
     * @param listener ignored
     */
    @Deprecated
    @Override
    public void setOnValueChangedListener(OnValueChangeListener listener){

    }


    private OnFloatValueChangeListener listener;
    private float step;
    private float min;
    private float max;
    private float value;
    private boolean initialized = false;
    private int digits;

    @Override
    protected String[] getDisplayedValues(int length){
        if ( initialized ){
            String[] array = new String[length];
            String format = String.format(Locale.US,"%%.%df",digits);
            for ( int i=0 ; i<length ; i++){
                array[i] = String.format(format,min+step*i);
            }
            return array;
        }else{
            return super.getDisplayedValues(length);
        }
    }

    /**
     * Sets the step value in Integer.
     * This calls {@link #setStepFloat(float)} with passed value.
     * @see #setStepFloat(float)
     * @param step the step value
     */
    @Override
    public final void setStep(int step){
        setStepFloat(step);
    }

    /**
     * Sets the step value in float.
     * @param step must be positive
     */
    public void setStepFloat(float step){
        if ( step <= 0 ) return;
        this.step = step;
        digits = -(int)Math.log10(step);
        double val = step * Math.pow(10.0,digits);
        while ( Math.abs(val-Math.round(val)) > 0.0001){
            digits++;
            val *= 10.0;
        }
        initialized = true;
        setMinFloat(min);
        setMaxFloat(max);
        setValueFloat(value);
    }
    /**
     * Returns the step value in float.
     * @return the step value
     */
    public float getStepFloat(){
        return step;
    }

    /**
     * Sets the lower value.
     * This call {@link #setMinFloat(float)} with passed value.
     * @param min the lower value
     * @see #setMinFloat(float)
     */
    @Override
    public final void setMinValue(int min){
        setMinFloat(min);
    }

    /**
     * Sets the lower value.<br>
     * <strong>NOTE </strong> min value may NOT be set exactly as passed value.
     * If the argument is not multiple of {@link #setStepFloat(float) step value},
     * the argument is modified to nearest value in multiples of step.
     * @param min the lower value
     */
    public void setMinFloat(float min){
        super.setMinValue(Math.round(min / step));
        this.min = super.getDisplayedMinValue() * step;
    }

    /**
     * Gets the lower value of the range, in which the user can select float value.
     * @return the lower value in float
     */
    public float getMinFloat(){
        return min;
    }

    /**
     * Sets the upper value.
     * This calls {@link #setMaxFloat(float)} with passed value.
     * @see #setMaxFloat(float)
     * @param max the upper value
     */
    @Override
    public void setMaxValue(int max){
        super.setMaxValue(max);
    }

    /**
     * Sets the upper value.<br>
     * <strong>NOTE </strong> max value may NOT be set exactly as passed value.
     *      If the argument is not multiple of {@link #setStepFloat(float) step value},
     *    the argument is modified to nearest value in multiples of step.
     * @param max the upper value
     */
    public void setMaxFloat(float max){
        super.setMaxValue(Math.round(max / step));
        this.max = super.getDisplayedMaxValue() * step;
    }

    /**
     * Gets the upper value of the range, in which the user can select float value.
     * @return the upper value in float
     */
    public float getMaxFloat(){
        return max;
    }

    /**
     * Sets the value of this picker.
     * <strong>NOTE </strong> displayed value may NOT be set exactly as passed value.
     *    If the argument is not multiple of {@link #setStepFloat(float) step value},
     *    the argument is modified to nearest value in multiples of step.
     * @param value the float value to be displayed on this widget
     */
    public void setValueFloat(float value){
        super.setValue(Math.round(value / step));
        this.value = super.getDisplayedValue() * step;
    }

    /**
     * Gets the value of this picker.
     * @return the float value being displayed on this widget
     */
    public float getValueFloat(){
        this.value = super.getDisplayedValue() * step;
        return value;
    }

    /**
     * Sets the value of this picker.
     * This calls {@link #setValueFloat(float)} with passed value.
     * @param value the integer value to be converted to float
     * @see #setValueFloat(float)
     */
    @Override
    public void setValue(int value){
        setValueFloat(value);
    }

    @Deprecated
    @Override
    public int getDisplayedMaxValue(){
        return super.getDisplayedMaxValue();
    }

    @Deprecated
    @Override
    public int getDisplayedMinValue(){
        return super.getDisplayedMinValue();
    }

    @Deprecated
    @Override
    public int getDisplayedValue(){
        return super.getDisplayedValue();
    }

    static class SavedState extends BaseSavedState {

        SavedState(Parcelable superState){
            super(superState);
        }

        private SavedState(Parcel source){
            super(source);
            min = source.readFloat();
            max = source.readFloat();
            value = source.readFloat();
            step = source.readFloat();
        }

        private float min;
        private float max;
        private float value;
        private float step;

        @Override
        public void writeToParcel(Parcel out, int flags){
            super.writeToParcel(out,flags);
            out.writeFloat(min);
            out.writeFloat(max);
            out.writeFloat(value);
            out.writeFloat(step);
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US,
                    "FloatPicker.SavedState{%s value=%f, min=%f, max=%f, step=%f}",
                    Integer.toHexString(System.identityHashCode(this)),value,min,max,step
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
        state.value = getValueFloat();
        state.step = getStepFloat();
        state.min = getMinFloat();
        state.max = getMaxFloat();
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state){
        SavedState myState = (SavedState)state;
        super.onRestoreInstanceState(myState.getSuperState());
        value = myState.value;
        min = myState.min;
        max = myState.max;
        super.setStep(1);
        setStepFloat(myState.step);
        requestLayout();
    }



}
