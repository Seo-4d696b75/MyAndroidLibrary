package jp.ac.u_tokyo.t.seo.customdialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

/**
 * ダイアログのフレーム部分のＵＩを定義するViewGroup.<br>
 * ダイアログの題名やメッセージ、下部のPositive/Neutral/Negativeボタンなど基本的なＵＩを管理します.
 * このViewGroupをルートにもつViewをXMLファイルで定義することで、{@link CustomDialog}のＵＩを静的に指定できます.<br>
 * <strong>注意 </strong>このViewGroupのchildViewがダイアログの内部のviewとしてインフレートされますが、
 * 直接のchildViewはひとつのみ持つことができます.<br>
 * ダイアログの基本ＵＩに関する各XML属性は、
 * <ul>
 *     <li>題名 :title (string)</li>
 *     <li>題名のテキストサイズ :title_text_size (dimension)</li>
 *     <li>題名のテキストカラー :title_text_color (color)</li>
 *     <li>メッセージテキスト :message (string)</li>
 *     <li>メッセージテキストのサイズ :message_text_size (dimension)</li>
 *     <li>メッセージテキストのカラー :message_text_color (color)</li>
 *     <li>背景画像 :dialog_background (drawable)</li>
 *     <li>題名部分の背景 :header_background (drawable)</li>
 *     <li>Positiveボタン :button_positive (string)</li>
 *     <li>Neutralボタン :button_neutral (string)</li>
 *     <li>Negativeボタン :button_negative (string)</li>
 *     <li>ボタンの背景 :button_background (drawable)</li>
 *     <li>ボタンのテキストサイズ :button_text_size (dimension)</li>
 *     <li>ボタンのテキストカラー :button_text_color (color)</li>
 *     <li>外部をタッチした時ダイアログを消すかどうか :cancelable_outsize_touch (boolean)</li>
 * </ul>
 * @see CustomDialog
 * @author Seo-4d696b75
 * @version 2017/12/10.
 */
public class DialogFrameLayout extends ViewGroup{

    public DialogFrameLayout(Context context){
        this(context, null);
    }

    public DialogFrameLayout(Context context, AttributeSet attributeSet){
        this(context, attributeSet, 0);
    }

    public DialogFrameLayout(Context context, AttributeSet attributeSet, int defaultAttr){
        super(context, attributeSet, defaultAttr);
        initialize(context, attributeSet, defaultAttr);
    }

    private ViewGroup mHeader;
    private ViewGroup mFooter;
    private boolean mContentAdded = false;
    private View mContentView;

    private CustomDialog.OnClickListener mListener;

    private boolean mInitialized = false;

    private boolean mCancelable;
    private String mTitleText;
    private int mTitleTextSize;
    private int mTitleTextColor;
    private String mMessageText;
    private int mMessageTextSize;
    private int mMessageTextColor;
    private int mDialogBackgroundResID;
    private int mHeaderBackgroundResID;
    private String mButtonPositive,mButtonNegative,mButtonNeutral;
    private int mButtonTextSize;
    private int mButtonTextColor;
    private int mButtonBackgroundResID;

    private int mRequestWidth = 0;
    private int mRequestHeight = 0;
    private int mWidth = -1;
    private int mHeight = -1;
    private int mHeaderHeight;
    private int mFooterHeight;
    //private Point mContentSize;

    private void initialize(Context context, AttributeSet attr, int defaultAttr){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.dialog_custom_frame, this, true);
        synchronized( this ){
            mHeader = (ViewGroup)findViewById(R.id.customDialogHeaderContainer);
            mFooter = (ViewGroup)findViewById(R.id.customDialogButtonRoot);
            if ( mContentView != null ){
                onContentViewAdded(mContentView);
            }
        }

        TypedArray array = context.obtainStyledAttributes(attr, R.styleable.DialogFrameLayout, defaultAttr, 0);

        mTitleText = array.getString(R.styleable.DialogFrameLayout_title);
        mTitleTextSize = array.getDimensionPixelSize(R.styleable.DialogFrameLayout_title_text_size, -1);
        mTitleTextColor = array.getColor(R.styleable.DialogFrameLayout_title_text_color, 0x00FFFFFF);

        mMessageText = array.getString(R.styleable.DialogFrameLayout_message);
        mMessageTextSize= array.getDimensionPixelSize(R.styleable.DialogFrameLayout_message_text_size, -1);
        mMessageTextColor= array.getColor(R.styleable.DialogFrameLayout_message_text_color, 0x00FFFFFF);

        mDialogBackgroundResID = array.getResourceId(R.styleable.DialogFrameLayout_dialog_background, R.drawable.custom_dialog_back);

        mHeaderBackgroundResID = array.getResourceId(R.styleable.DialogFrameLayout_header_background, 0);


        mButtonBackgroundResID = array.getResourceId(R.styleable.DialogFrameLayout_button_background, 0);
        mButtonTextSize = array.getDimensionPixelSize(R.styleable.DialogFrameLayout_button_text_size, -1);
        mButtonTextColor = array.getColor(R.styleable.DialogFrameLayout_button_text_color, 0x00000000);
        mButtonPositive = array.getString(R.styleable.DialogFrameLayout_button_positive);
        mButtonNeutral = array.getString(R.styleable.DialogFrameLayout_button_neutral);
        mButtonNegative = array.getString(R.styleable.DialogFrameLayout_button_negative);

        mCancelable = array.getBoolean(R.styleable.DialogFrameLayout_cancelable_outside_touch, false);

        array.recycle();

        int[] attrsArray = new int[]{
                android.R.attr.layout_width,
                android.R.attr.layout_height
        };
        array = context.obtainStyledAttributes(attr, attrsArray);
        String widthValue = array.getString(0);
        if ( widthValue == null ){
            mRequestWidth = LayoutParams.WRAP_CONTENT;
        }else{
            mRequestWidth = widthValue.contains("dip") ? array.getDimensionPixelSize(0, 0) : Integer.parseInt(widthValue);
        }
        String heightValue = array.getString(1);
        if ( heightValue == null ){
            mRequestHeight = LayoutParams.WRAP_CONTENT;
        }else{
            mRequestHeight = heightValue.contains("dip") ? array.getDimensionPixelSize(1, 0) : Integer.parseInt(heightValue);
        }
        array.recycle();
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        if ( !mInitialized ){
            onSavedStateRestored();
        }

        if ( mWidth < 0 || mHeight < 0 ){
            //初回のみLayoutParam, MeasureSpecに従って大きさを計測
            final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            int max = 0;
            int height = 0;
            int headerWidth = 0;
            int footerWidth = 0;
            switch ( mRequestWidth ){
                case LayoutParams.WRAP_CONTENT:
                    widthMeasureSpec = MeasureSpec.AT_MOST + widthSize;
                    break;
                case LayoutParams.MATCH_PARENT:
                    widthMeasureSpec = MeasureSpec.EXACTLY + widthSize;
                    break;
                default:
                    widthMeasureSpec = MeasureSpec.EXACTLY + mRequestWidth;
            }
            if ( mHeader != null ){
                mHeader.measure(widthMeasureSpec, MeasureSpec.AT_MOST + (heightSize - height));
                headerWidth = mHeader.getMeasuredWidth();
                mHeaderHeight = mHeader.getMeasuredHeight();
                height += mHeaderHeight;
                if ( max < headerWidth ){
                    max = headerWidth;
                }
            }
            if ( mFooter != null ){
                mFooter.measure(widthMeasureSpec, MeasureSpec.AT_MOST + (heightSize - height));
                footerWidth = mFooter.getMeasuredWidth();
                mFooterHeight = mFooter.getMeasuredHeight();
                height += mFooterHeight;
                if ( max < footerWidth ){
                    max = footerWidth;
                }
            }
            if ( mContentView != null ){
                switch ( mRequestHeight ){
                    case LayoutParams.WRAP_CONTENT:
                        mContentView.measure(widthMeasureSpec, MeasureSpec.AT_MOST + (heightSize - height));
                        break;
                    case LayoutParams.MATCH_PARENT:
                        mContentView.measure(widthMeasureSpec, MeasureSpec.EXACTLY + (heightSize - height));
                        break;
                    default:
                        if ( mRequestHeight <= heightSize - height){
                            mContentView.measure(widthMeasureSpec, MeasureSpec.EXACTLY + mRequestHeight);
                        }else{
                            mContentView.measure(widthMeasureSpec, MeasureSpec.EXACTLY + (heightSize - height));
                        }
                }
                int w = mContentView.getMeasuredWidth();
                if ( w > max ){
                    max = w;
                }
                height += mContentView.getMeasuredHeight();
            }
            if ( mHeader != null && headerWidth < max ){
                mHeader.measure(MeasureSpec.EXACTLY + max, MeasureSpec.EXACTLY + mHeaderHeight);
            }
            if ( mFooter != null && footerWidth < max ){
                mFooter.measure(MeasureSpec.EXACTLY + max, MeasureSpec.EXACTLY + mFooterHeight);
            }
            mWidth = max;
            mHeight = height;
        }

        //一度大きさを計測したら以降は固定サイズ
        if ( mContentView != null ){
            mContentView.measure(MeasureSpec.EXACTLY + mWidth, MeasureSpec.EXACTLY + (mHeight - mHeaderHeight - mFooterHeight));
        }

        setMeasuredDimension(mWidth, mHeight);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom){
        final int headerHeight = mHeaderHeight;
        final int footerHeight = mFooterHeight;
        if ( mHeader != null ){
            mHeader.layout(left, top, right, top + headerHeight);
        }
        if ( mFooter != null ){
            mFooter.layout(left, bottom - footerHeight, right, bottom);
        }
        if ( mContentView != null ){
            mContentView.layout(left, top + headerHeight, right, bottom - footerHeight);
        }
        invalidate();
    }

    /**
     * BundleやAttributeSetから復元されたViewの状態をＵＩに反映する.
     * 初めに初期化一回のみ有効
     */
    synchronized void onSavedStateRestored(){
        if ( mInitialized ){
            return;
        }
        mInitialized = true;

        TextView titleTextView = (TextView)mHeader.findViewById(R.id.textViewCustomDialogTitle);
        if ( mTitleText == null ){
            mHeader.removeView(titleTextView);
        }else{
            titleTextView.setText(mTitleText);
            if ( mTitleTextSize > 0 ){
                titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTitleTextSize);
            }
            if ( ( mTitleTextColor >>> 24 ) > 0  ){
                titleTextView.setTextColor(mTitleTextColor);
            }
            if ( mHeaderBackgroundResID != 0 ){
                titleTextView.setBackgroundResource(mHeaderBackgroundResID);
            }
        }

        TextView messageTextView = (TextView)mHeader.findViewById(R.id.textViewCustomDialogMessage);
        if ( mMessageText == null ){
            mHeader.removeView(messageTextView);
        }else{
            messageTextView.setText(mMessageText);
            if ( mMessageTextSize > 0 ){
                messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mMessageTextSize);
            }
            if ( (mMessageTextColor >>> 24) > 0 ){
                messageTextView.setTextColor(mMessageTextColor);
            }
        }

        if ( mDialogBackgroundResID != 0 ){
            setBackgroundResource(mDialogBackgroundResID);
        }

        setButton(DialogInterface.BUTTON_POSITIVE, mButtonPositive);
        setButton(DialogInterface.BUTTON_NEGATIVE, mButtonNegative);
        setButton(DialogInterface.BUTTON_NEUTRAL, mButtonNeutral);

    }

    /*
    private Drawable getDrawable(Context context, int id){
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ){
            return context.getDrawable(id);
        }else{
            return context.getResources().getDrawable(id);
        }
    }*/


    @Override
    public void addView(View view, ViewGroup.LayoutParams params){
        int id = view.getId();
        if ( id == R.id.customDialogButtonRoot || id  == R.id.customDialogHeaderContainer ){
            super.addView(view, params);
        }else{
            view.setLayoutParams(params);
            addContentView(view);
        }
    }

    synchronized void addContentView(View view){
        if ( mHeader != null && mFooter != null ){
            onContentViewAdded(view);
        }else{
            if ( mContentView == null ){
                mContentView = view;
            }else{
                throw new IllegalStateException("Dialog frame has one child view only.");
            }
        }
    }

    private void onContentViewAdded(View content){
        if ( mContentView != null && mContentView == content ){
            return;
        }
        if ( mContentAdded ){
            throw new IllegalStateException("Dialog frame has one child view only.");
        }
        //ViewGroup.LayoutParams params = content.getLayoutParams();
        //RelativeLayout.LayoutParams newParams = new RelativeLayout.LayoutParams(params.width, params.height);
        //newParams.addRule(RelativeLayout.ABOVE, R.id.customDialogButtonRoot);
        //newParams.addRule(RelativeLayout.BELOW, R.id.customDialogHeaderContainer);
        //content.setLayoutParams(newParams);
        super.addView(content);
        mContentView = content;
        mContentAdded = true;
    }

    public void setTitleText(String text){
        mTitleText = text;
    }

    public void setHeaderDrawable(int resID){
        mHeaderBackgroundResID = resID;
    }

    public void setMessageText(String text){
        mMessageText = text;
    }

    public void setPositiveButton(String text){
        mButtonPositive = text;
    }

    public void setNegativeButton(String text){
        mButtonNegative = text;
    }

    public void setNeutralButton(String text){
        mButtonNeutral = text;
    }

    private void setButton(final int which, String text){
        ViewGroup container = null;
        Button button = null;
        switch ( which ){
            case DialogInterface.BUTTON_NEGATIVE:
                container = (ViewGroup)mFooter.findViewById(R.id.customDialogNegativeButtonContainer);
                button = (Button)mFooter.findViewById(R.id.buttonCustomDialogNegative);
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                container = (ViewGroup)mFooter.findViewById(R.id.customDialogNeutralButtonContainer);
                button = (Button)mFooter.findViewById(R.id.buttonCustomDialogNeutral);
                break;
            case DialogInterface.BUTTON_POSITIVE:
                container = (ViewGroup)mFooter.findViewById(R.id.customDialogPositiveButtonContainer);
                button = (Button)mFooter.findViewById(R.id.buttonCustomDialogPositive);
                break;
            default:
                return;
        }
        if ( text == null ){
            container.removeView(button);
        }else{
            button.setText(text);
            if ( mButtonTextSize > 0 ){
                button.setTextSize(mButtonTextSize);
            }
            if ( ( mButtonTextColor >>> 24 ) > 0 ){
                button.setTextColor(mButtonTextColor);
            }
            if ( mButtonBackgroundResID != 0 ){
                button.setBackgroundResource(mButtonBackgroundResID);
            }
            button.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v){
                    if ( mListener != null ){
                        mListener.onDialogButtonClicked(null, null, which);
                    }
                }
            });
        }
    }

    void setOnClickListener(CustomDialog.OnClickListener listener){
        mListener = listener;
    }

    View getContentView(){
        return mContentView;
    }

    boolean isCancelableOutsideTouch(){
        return mCancelable;
    }

    @Override
    public void onDetachedFromWindow(){
        super.onDetachedFromWindow();
        mListener = null;
        mContentView = null;
    }

    static class SavedState extends BaseSavedState {

        SavedState(Parcelable superState){
            super(superState);
        }

        private SavedState(Parcel source){
            super(source);
            mDialogBackColor = source.readInt();
            mTitleText = source.readString();
            mTitleTextSize = source.readInt();
            mTitleTextColor = source.readInt();
            mMessageText = source.readString();
            mMessageTextSize = source.readInt();
            mMessageTextColor = source.readInt();
            mDialogBackgroundResID = source.readInt();
            mHeaderBackgroundResID = source.readInt();
            mButtonPositive = source.readString();
            mButtonNegative = source.readString();
            mButtonNeutral = source.readString();
            mButtonTextSize = source.readInt();
            mButtonTextColor = source.readInt();
            mButtonBackgroundResID = source.readInt();
            boolean[] array = new boolean[1];
            source.readBooleanArray(array);
            mCancelable = array[0];
        }

        private int mDialogBackColor;
        private boolean mCancelable;
        private String mTitleText;
        private int mTitleTextSize;
        private int mTitleTextColor;
        private String mMessageText;
        private int mMessageTextSize;
        private int mMessageTextColor;
        private int mDialogBackgroundResID;
        private int mHeaderBackgroundResID;
        private String mButtonPositive;
        private String mButtonNegative;
        private String mButtonNeutral;
        private int mButtonTextSize;
        private int mButtonTextColor;
        private int mButtonBackgroundResID;

        @Override
        public void writeToParcel(Parcel out, int flags){
            super.writeToParcel(out,flags);
            out.writeInt(mDialogBackColor);
            out.writeString(mTitleText);
            out.writeInt(mTitleTextSize);
            out.writeInt(mTitleTextColor);
            out.writeString(mMessageText);
            out.writeInt(mMessageTextSize);
            out.writeInt(mMessageTextColor);
            out.writeInt(mDialogBackgroundResID);
            out.writeInt(mHeaderBackgroundResID);
            out.writeString(mButtonPositive);
            out.writeString(mButtonNegative);
            out.writeString(mButtonNeutral);
            out.writeInt(mButtonTextSize);
            out.writeInt(mButtonTextColor);
            out.writeInt(mButtonBackgroundResID);
            out.writeBooleanArray(new boolean[]{mCancelable});
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US,
                    "DialogFrameLayout.SavedState{%s title=%s, message=%s}",
                    Integer.toHexString(System.identityHashCode(this)),
                    mTitleText == null ? "none" : mTitleText,
                    mMessageText == null ? "none" : mMessageText
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
        state.mCancelable = this.mCancelable;
        state.mTitleText = this.mTitleText;
        state.mTitleTextSize = this.mTitleTextSize;
        state.mTitleTextColor = this.mTitleTextColor;
        state.mMessageText = this.mMessageText;
        state.mMessageTextSize = this.mMessageTextSize;
        state.mMessageTextColor = this.mMessageTextColor;
        state.mDialogBackgroundResID = this.mDialogBackgroundResID;
        state.mHeaderBackgroundResID = this.mHeaderBackgroundResID;
        state.mButtonPositive = this.mButtonPositive;
        state.mButtonNegative = this.mButtonNegative;
        state.mButtonNeutral = this.mButtonNeutral;
        state.mButtonTextSize = this.mButtonTextSize;
        state.mButtonTextColor = this.mButtonTextColor;
        state.mButtonBackgroundResID = this.mButtonBackgroundResID;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state){
        SavedState myState = (SavedState)state;
        super.onRestoreInstanceState(myState.getSuperState());
        mCancelable = myState.mCancelable;
        mTitleText = myState.mTitleText;
        mTitleTextSize = myState.mTitleTextSize;
        mTitleTextColor = myState.mTitleTextColor;
        mMessageText = myState.mMessageText;
        mMessageTextSize = myState.mMessageTextSize;
        mMessageTextColor = myState.mMessageTextColor;
        mDialogBackgroundResID = myState.mDialogBackgroundResID;
        mHeaderBackgroundResID = myState.mHeaderBackgroundResID;
        mButtonPositive = myState.mButtonPositive;
        mButtonNegative = myState.mButtonNegative;
        mButtonNeutral = myState.mButtonNeutral;
        mButtonTextSize = myState.mButtonTextSize;
        mButtonTextColor = myState.mButtonTextColor;
        mButtonBackgroundResID = myState.mButtonBackgroundResID;
    }

}
