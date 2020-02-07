package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

/**
 * @author Seo-4d696b75
 * @version 2017/10/16.
 */

public class MultiToggleButton extends MultipleStateButton{

    public MultiToggleButton(Context context){
        this(context,null);
    }

    public MultiToggleButton(Context context, AttributeSet set){
        this(context,set,0);
    }

    public MultiToggleButton(Context context, AttributeSet set, int defaultAttr){
        super(context, set, defaultAttr);
        mText = new String[MultipleStateButton.STATE_MAX];

        TypedArray array = context.obtainStyledAttributes(set,R.styleable.MultiToggleButton,0,0);

        mIsTextVisible = !array.hasValue(ATTR_TEXT_VISIBLE) || array.getBoolean(ATTR_TEXT_VISIBLE,true);

        for ( int i=0 ; i<MultipleStateButton.STATE_MAX ; i++ ){
            mText[i] = array.hasValue(ATTR_TEXT[i]) ? array.getString(ATTR_TEXT[i]) : String.valueOf(i+1);
        }

        array.recycle();

        syncTextState();
    }

    private static final int ATTR_TEXT_VISIBLE = R.styleable.MultiToggleButton_text_visible;

    private static final int[] ATTR_TEXT = new int[]{
            R.styleable.MultiToggleButton_text_one,
            R.styleable.MultiToggleButton_text_two,
            R.styleable.MultiToggleButton_text_three,
            R.styleable.MultiToggleButton_text_four,
            R.styleable.MultiToggleButton_text_five
    };

    private boolean mIsTextVisible;
    private String[] mText;

    public void setStateTextVisibility(boolean isVisible){
        mIsTextVisible = isVisible;
    }

    public boolean isTextVisible(){
        return mIsTextVisible;
    }

    @Override
    public void setState(int state){
        super.setState(state);
        syncTextState();
    }

    private void syncTextState(){
        if ( isTextVisible() ){
            int state = getState();
            setText(mText[state]);
        }else{
            setText(null);
        }
    }

    public String getStateText(int state){
        if ( state >= 0 && state < MultipleStateButton.STATE_MAX ){
            return mText[state];
        }
        return null;
    }

    public void setStateText(int state, String text){
        if ( state >= 0 && state < MultipleStateButton.STATE_MAX ){
            mText[state] = text;
        }
    }


}
