package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;

import java.util.Locale;

/**
 * 文字列の長さに応じてViewの横幅や文字列の{@link android.widget.TextView#setTextScaleX(float) 横方向の比率}を動的に制御できるTextViewの拡張.  <br>
 * <strong>NOTE </strong>{@link #setText(char[], int, int)}は非対応なので他のオーバロードを呼ぶ<br>
 * {@code layout_width}による挙動の違いは <br>
 * <ul>
 * <li>wrap_content 文字列の長さが許容される最大幅を超える場合は文字列を伸縮させるが、超えないなら文字列長さにView幅を合わせる</li>
 * <li>それ以外 このLayoutParamから計算される値で横幅を決定し、この長さを超える文字列は横方向に縮小させて収める</li>
 * </ul>
 * XMLで定義できる属性一覧<br>
 * <ul>
 * <li>maxWidth {@code layout_width=wrap_content}時のみ有効でこの幅を超える文字列が{@link #setText(CharSequence)}されるとこの幅に収まるように{@link android.widget.TextView#setTextScaleX(float)}で制御する</li>
 * <li>minTextScaleX この比率を下回る値で{@link android.widget.TextView#setTextScaleX(float)}しないように指定する.デフォルト値は0.5 この比率でも文字列がView幅を超える場合は超過部分を'…'で置換する</li>
 * </ul>
 *
 * @author Seo-4d696b75
 * @version 2019/04/18.
 */

public class ExpandableTextView extends AppCompatTextView{

    public ExpandableTextView(Context context){
        this(context, null);
    }

    public ExpandableTextView(Context context, AttributeSet attr){
        this(context, attr, 0);
    }

    public ExpandableTextView(Context context, AttributeSet attr, int defaultAttr){
        super(context, attr, defaultAttr);

        final TypedArray array = context.obtainStyledAttributes(attr, R.styleable.ExpandableTextView, defaultAttr, 0);
        mMaxWidth = array.getDimensionPixelSize(R.styleable.ExpandableTextView_maxWidth, Integer.MAX_VALUE);
        mMinTextScaleX = array.getFloat(R.styleable.ExpandableTextView_minTextScaleX, 0.6f);
        array.recycle();

        if ( mMinTextScaleX < 0.1f ) mMinTextScaleX = 0.1f;
        if ( mMinTextScaleX > 1f ) mMinTextScaleX = 1f;


        mTextPaint = getPaint();
        super.setLines(1);
        mCurrentText = "";
        mDisplayedText = "";
        // これを呼び出さないと描画時に textScaleX = 1f と勝手に修正される
        // 現在の値と異なる値を指定すればいいので適当に getTextScaleX() / 2f
        super.setTextScaleX(getTextScaleX() / 2f);
    }

    private TextPaint mTextPaint;

    private int mMaxWidth;
    private float mMinTextScaleX;

    private boolean mRequestMeasure = false;
    private int mRawWidthMeasureSpec = 0, mCalcWidthMeasureSpec = 0;


    private CharSequence mCurrentText, mDisplayedText;

    /**
     * このViewが表示しようとしている文字列を取得する.<br>
     * @return {@link #setText(CharSequence, BufferType)}でセットした文字列
     */
    @Override
    public CharSequence getText(){
        return mCurrentText;
    }

    /**
     * このViewが表示している文字列を取得する.<br>
     * Viewよ横幅に制限があり、かつ文字列を横方向に限界まで収縮しても収まらない場合は
     * {@link #setText(CharSequence, BufferType)}とは別の代替文字列を表示している.
     * @return 実際に表示している文字列
     */
    public CharSequence getDisplayedText(){
        return mDisplayedText;
    }

    @Override
    public void setMaxWidth(int width){
        // super#mMaxWidth is private, so keep the value by itself
        if ( width <= 0 ) return;
        if ( width != mMaxWidth ){
            mMaxWidth = width;
            mRequestMeasure = true;
            super.setMaxWidth(width);
            // maxWidth is valid only when view's width depends on its contents,
            // in such case, super will call layout()
        }
    }

    /**
     * 文字列の横方向のScaleの最小値を指定する.
     * @param scale in range of (0,1)
     * @see TextPaint#setTextScaleX(float)
     */
    public void setMinTextScaleX(float scale){
        if ( scale <= 0 || scale > 1 ) return;
        if ( mMinTextScaleX != scale ){
            mMinTextScaleX = scale;
            if ( MeasureSpec.getMode(mRawWidthMeasureSpec) == MeasureSpec.EXACTLY ){
                // no layout() will be called
                updateText(mCurrentText, BufferType.NORMAL, MeasureSpec.getSize(mRawWidthMeasureSpec));
            }else{
                mRequestMeasure = true;
                requestLayout();
            }
        }
    }


    //#setText(char[], int, int) <- finalでオーバーライド不可
    //           以外はここを経由している
    @Override
    public void setText(CharSequence text, BufferType type){
        if ( text == null ) return;
        if ( mCurrentText != null && mCurrentText.equals(text) ) return;

        mCurrentText = text;

        if ( MeasureSpec.getMode(mRawWidthMeasureSpec) == MeasureSpec.EXACTLY ){
            // no layout() will be called
            updateText(text, type, MeasureSpec.getSize(mRawWidthMeasureSpec));
        }else{
            // super class will call layout() in future
            mRequestMeasure = true;
            super.setText(text, type);
        }
    }

    // 必要に応じて文字列を伸縮させたり代替文字列に置換する
    private void updateText(CharSequence text, BufferType type, int widthSize){
        mTextPaint.setTextScaleX(1f);
        float length = mTextPaint.measureText(text, 0, text.length());
        int padding = getCompoundPaddingLeft() + getCompoundPaddingRight();
        if ( length + padding > widthSize ){
            float scale = (widthSize - padding) / length;
            text =  modifyText(scale, text, widthSize - padding);
        }
        mDisplayedText = text;
        super.setText(text, type);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        /* 幅に関してのみ要求を追加 */

        // measure text length if only text or requested spec has been changed
        if ( mRequestMeasure || widthMeasureSpec != mRawWidthMeasureSpec ){

            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);

            mRequestMeasure = false;
            CharSequence text = mCurrentText;
            mTextPaint.setTextScaleX(1f);
            float length = mTextPaint.measureText(text, 0, text.length());

            int padding = getCompoundPaddingLeft() + getCompoundPaddingRight();

            int requestedWidth = 0;

            switch ( widthMode ){
                case MeasureSpec.EXACTLY:
                    // no need to care about its content
                    requestedWidth = widthSize;
                    if ( widthMeasureSpec != mRawWidthMeasureSpec ){
                        // if view width changes, update its content
                        updateText(mCurrentText, BufferType.NORMAL, widthSize);
                    }
                    break;
                case MeasureSpec.AT_MOST:
                    int max = Math.min(widthSize, mMaxWidth);
                    if ( length + padding > max ){
                        requestedWidth = max;
                        float scale = (max - padding) / length;
                        CharSequence modified = modifyText(scale, text, max - padding);
                        if ( !mDisplayedText.equals(modified) ){
                            mDisplayedText = modified;
                            super.setText(modified, BufferType.NORMAL);
                        }
                    }else{
                        requestedWidth = (int)Math.ceil(length + padding);
                    }
                    break;
                case MeasureSpec.UNSPECIFIED:
                    requestedWidth = (int)Math.ceil(length + padding);
                    mTextPaint.setTextScaleX(1f);
                    mDisplayedText = text;
                    break;
            }

            mRawWidthMeasureSpec = widthMeasureSpec;
            mCalcWidthMeasureSpec = MeasureSpec.makeMeasureSpec(requestedWidth, MeasureSpec.EXACTLY);
        }

        /* super.onMeasure()の細かい実装は親に任せる */
        super.onMeasure(mCalcWidthMeasureSpec, heightMeasureSpec);
    }


    /**
     * 指定された最大幅に合うように文字列を横方向へ収縮させる.
     * 収縮率が{@link #mMinTextScaleX}を下回る場合は、その最小比率でちょうど最大幅に合うような代替文字列に置換する
     * @param scale     rough value, with which text width can be adjusted to maxLength
     * @param text      raw text
     * @param maxLength max length of text
     * @return modified text
     */
    private CharSequence modifyText(float scale, CharSequence text, float maxLength){
        if ( scale <= 0 || maxLength <= 0 ) return "";
        if ( calcTextScaleX(scale, text, maxLength) ){
            // 適切なスケールで全体が収まるならOK
            return text;
        }else{
            // ダメなら短縮
            return reduceString(text, maxLength);
        }
    }

    /**
     * 指定された最大幅に合うように文字列の横方向への収縮を試みる.<br>
     * {@link android.widget.TextView#setTextScaleX(float)}で指定するTextScaleXと
     * {@link TextPaint#measureText(String)}で測定される文字列の横幅は比例しない？
     * 適当に当たりをつけて倍率を探索して、許容範囲内で最善の値を採択する
     *
     * @return true if [{@link #mMinTextScaleX},1]の範囲で適切な倍率が存在する
     */
    private boolean calcTextScaleX(float rough, CharSequence text, float maxLength){
        if ( rough < mMinTextScaleX || rough > 1f ) return false;
        mTextPaint.setTextScaleX(rough);
        float length = mTextPaint.measureText(text, 0, text.length());
        final float step = 0.01f;
        if ( length < maxLength ){
            int cnt = 1;
            while ( true ){
                float scale = rough * (1f + cnt * step);
                if ( scale > 1f ){
                    mTextPaint.setTextScaleX(1f);
                    return true;
                }
                mTextPaint.setTextScaleX(scale);
                length = mTextPaint.measureText(text, 0, text.length());
                if ( length > maxLength ) break;
                cnt++;
            }
            mTextPaint.setTextScaleX(rough * (1f + (cnt - 1) * step));
            return true;
        }else if ( length == maxLength ){
            return true;
        }else{
            int cnt = 1;
            while ( true ){
                float scale = rough * (1f - cnt * step);
                if ( scale < mMinTextScaleX ) return false;
                mTextPaint.setTextScaleX(scale);
                length = mTextPaint.measureText(text, 0, text.length());
                if ( length <= maxLength ) return true;
                cnt++;
            }
        }
    }

    /**
     * 最低倍率で文字列を伸縮してもViewの横幅に収まらない場合は代替文字列を用意する.
     * 具体的には、文字列の末端から1文字以上を削除して'…'に置換する.
     * 当然文字によって横幅は異なるので、削るべき文字数は簡単には計算できない.
     * そこで、View横幅に収まるまで削る文字数を1文字ずつ増やしながら探索する
     * @return modified text, whose width can be adjusted with proper scaleX
     */
    private CharSequence reduceString(CharSequence text, float maxLength){
        StringBuilder builder = new StringBuilder();
        builder.append(text);
        int i = text.length();
        while ( i > 0 ){
            builder.deleteCharAt(i - 1);
            builder.append('…');
            String str = builder.toString();
            mTextPaint.setTextScaleX(mMinTextScaleX);
            float length = mTextPaint.measureText(str);
            if ( length <= maxLength ){
                calcTextScaleX(mMinTextScaleX, str, maxLength);
                return str;
            }
            builder.deleteCharAt(i - 1);
            i--;
        }
        return "";
    }

    /**
     * Lines value will be ignored.
     * @param lines
     */
    @Deprecated
    @Override
    final public void setLines(int lines){
        // 複数行はダメ
        Log.e("Warning", "setLines(int) not acceptable in ExpandableTextView");
    }

    @Override
    public String toString(){
        return String.format(
                Locale.US, "ExpandableTextView@%x{displayed:\"%s\",text:\"%s\",textScaleX=%.2f}",
                System.identityHashCode(this),
                mDisplayedText, mCurrentText, mTextPaint.getTextScaleX()
        );
    }

    @Deprecated
    @Override
    final public void setTextScaleX(float size){
        // 自動で調節するからユーザは指定するな
        Log.e("Warning", "setTextScaleX(float) not acceptable in ExpandableTextView");
    }

    static class SavedState extends BaseSavedState{

        SavedState(Parcelable superState){
            super(superState);
        }

        private SavedState(Parcel source){
            super(source);
            mMinScale = source.readFloat();
            mMaxWidth = source.readInt();
            mText = source.readString();
        }

        private float mMinScale;
        private int mMaxWidth;
        private String mText;

        @Override
        public void writeToParcel(Parcel out, int flags){
            super.writeToParcel(out, flags);
            out.writeFloat(mMinScale);
            out.writeInt(mMaxWidth);
            out.writeString(mText);
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US,
                    "ExpandableTextView#SavedState{%s text:%s}",
                    Integer.toHexString(System.identityHashCode(this)), mText
            );
        }

        public static final Parcelable.Creator<ExpandableTextView.SavedState> CREATOR = new Parcelable.Creator<ExpandableTextView.SavedState>(){

            @Override
            public ExpandableTextView.SavedState createFromParcel(Parcel source){
                return new ExpandableTextView.SavedState(source);
            }

            @Override
            public ExpandableTextView.SavedState[] newArray(int size){
                return new ExpandableTextView.SavedState[size];
            }
        };

    }

    @Override
    public Parcelable onSaveInstanceState(){
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.mText = mCurrentText.toString();
        state.mMaxWidth = mMaxWidth;
        state.mMinScale = mMinTextScaleX;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state){
        SavedState myState = (SavedState)state;
        super.onRestoreInstanceState(myState.getSuperState());
        mMinTextScaleX = myState.mMinScale;
        mMaxWidth = myState.mMaxWidth;
        setText(myState.mText);
    }

}
