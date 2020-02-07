package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.Locale;

/**
 *
 * Viewの横幅を超える長さをもつ文字列を左右に流しながら表示するウェジェットです.<br>
 *
 * View横幅と文字列長さの大小関係で以下のように表示方法が変化します.
 * <ul>
 *     <li>文字列の長さがView横幅に収まる場合は流し表示はせず固定して表示</li>
 *     <li>Viewの横幅が固定されており、表示する文字列の長さがその横幅以下の場合はViewの左端に文字列を寄せて表示</li>
 *     <li>Viewの横幅が{@code wrap_content}等の指定で可変の場合、文字列の長さが許された横幅最大値以下ならView横幅を文字列幅に合わせて文字列を固定して表示</li>
 * </ul>
 *
 * このViewには次のようなXML属性による設定または対応するsetterが用意されています.
 * <ul>
 *     <li>{@code text} : 表示する文字列(string) / {@link #setText(String)}</li>
 *     <li>{@code text_size} : 表示する文字サイズ(dimension) / {@link #setTextSize(int, float)}</li>
 *     <li>{@code text_color} : 表示する文字の色(color) / {@link #setTextColor(int)}</li>
 *     <li>{@code flow_repeat} : 文字列を繰り返し流すか(boolean) / {@link #setFlowRepeat(boolean)}</li>
 *     <li>{@code flow_speed} : 文字列を流す速さ(dimension) / {@link #setFlowSpeed(int, float)}</li>
 *     <li>{@code flow_interval} : 文字列の間隔(integer) / {@link #setFlowInterval(int)}</li>
 *     <li>{@code flow_style} : 流し表示のスタイル(enum) / {@link #setFlowStyle(int)}<br>
 *         選択可能なスタイルは次の定数で定義されます<br> {@link #FLOW_STYLE_OVER}, {@link #FLOW_STYLE_TURN}</li>
 *     <li>{@code back_color} : 背景の色(color) / {@link #setBackColor(int)}</li>
 *     <li>{@code fade_width} : 左右両端のグラデーションの幅(dimension) / {@link #setEdgeFadingWidth(int, float)}</li>
 *     <li>{@code fade_color} : グラデーションの色(color) / {@link #setFadingColor(int)}</li>
 * </ul>
 * <strong>注意</strong> これら文字の表示方法を変更する操作を行うと、現在の文字の描写を中断して新たな表示方法で初めの位置から描写を開始します.
 * <br>
 * 文字の大きさや文字列の速さなど大きさを指定する際には以下の定数値で単位を同時に指定してください.
 * <ul>
 *     <li>{@link TypedValue#COMPLEX_UNIT_DIP} dp単位</li>
 *     <li>{@link TypedValue#COMPLEX_UNIT_SP} sp単位</li>
 *     <li>{@link TypedValue#COMPLEX_UNIT_PX} pixel単位</li>
 * </ul>
 *
 * @author Seo-4d696b75
 * @version 2017/10/16.
 */

public class FlowTextView extends View{

    public FlowTextView(Context context){
        this(context,null);
    }

    public FlowTextView(Context context, AttributeSet attrs){
        this(context,attrs,0);
    }

    public FlowTextView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);

        setWillNotDraw(false);

        mPaint = new Paint();
        mFadingRightPaint = new Paint();
        mFadingLeftPaint = new Paint();
        mBackPaint = new Paint();

        //get each value from attribute set
        final TypedArray array = context.obtainStyledAttributes(attrs,R.styleable.FlowTextView,defStyleAttr,0);

        if ( array.hasValue(R.styleable.FlowTextView_text) ){
            String text = array.getString(R.styleable.FlowTextView_text);
            mText = text == null ? "" : text;
        }else{
            mText = "";
        }
        if ( array.hasValue(R.styleable.FlowTextView_text_color) ){
            mTextColor = array.getColor(R.styleable.FlowTextView_text_color,0xff000000);
        }else{
            mTextColor = 0xff000000;
        }
        if ( array.hasValue(R.styleable.FlowTextView_flow_repeat) ){
            mIsFlowRepeat = array.getBoolean(R.styleable.FlowTextView_flow_repeat, true);
        }else{
            mIsFlowRepeat = true;
        }
        if ( array.hasValue(R.styleable.FlowTextView_flow_speed) ){
            mFlowSpeed = array.getDimensionPixelSize(R.styleable.FlowTextView_flow_speed,100);
        }else{
            mFlowSpeed = convertToPixels(TypedValue.COMPLEX_UNIT_DIP,10f);
        }
        if ( array.hasValue(R.styleable.FlowTextView_flow_interval) ){
            mFlowInterval = array.getInteger(R.styleable.FlowTextView_flow_interval,100);
        }else{
            mFlowInterval = 100;
        }
        if ( array.hasValue(R.styleable.FlowTextView_flow_style) ){
            mIsFlowOut = FLOW_STYLE_TURN != array.getInt(R.styleable.FlowTextView_flow_style,FLOW_STYLE_OVER);
        }else{
            mIsFlowOut = true;
        }
        if ( array.hasValue(R.styleable.FlowTextView_fading_width) ){
            mEdgeFadingWidth = array.getDimensionPixelSize(R.styleable.FlowTextView_fading_width,0);
        }else{
            mEdgeFadingWidth = 0f;
        }
        if ( array.hasValue(R.styleable.FlowTextView_back_color) ){
            mBackColor = array.getColor(R.styleable.FlowTextView_back_color,0x00FFFFFF);
        }else{
            mBackColor = 0x00FFFFFF;
        }
        if ( array.hasValue(R.styleable.FlowTextView_fading_color) ){
            mFadingColor = array.getColor(R.styleable.FlowTextView_fading_color,mBackColor);
        }else{
            mFadingColor = mBackColor;
        }
        if ( array.hasValue(R.styleable.FlowTextView_text_size) ){
            mTextSize = array.getDimensionPixelSize(R.styleable.FlowTextView_text_size,100);
        }else{
            mTextSize = convertToPixels(TypedValue.COMPLEX_UNIT_SP,16);
        }

        array.recycle();

        //initialize size and width of text
        onTextSizeChanged(mText,mTextSize);

        //other initialization, which depends on canvas size, will be called when its size is determined.
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override
            public void onGlobalLayout(){
                if ( !mIsInitialized ){
                    mIsInitialized = true;
                    mWidth = getWidth();
                    mHeight = getHeight();

                    onDrawSizeChanged(mWidth,mHeight,mEdgeFadingWidth);

                    //initialize color of text and background
                    setTextColor(mTextColor);
                    //setBackColor(mBackColor); <- avoid to call onBackColorChanged() twice
                    mBackPaint.setColor(mBackColor);
                    setFadingColor(mFadingColor);

                    mIsInitialized = true;

                    //start drawing
                    onDrawStart(mIsFlowRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN );

                    requestInvalidate();
                }else{
                    int width = getWidth();
                    int height = getHeight();
                    if ( width != mWidth || height != mHeight ){
                        mWidth = width;
                        mHeight = height;
                        onDrawSizeChanged(mWidth, mHeight, mEdgeFadingWidth);
                        onDrawStart(mIsFlowRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN);
                        requestInvalidate();
                    }
                }
            }
        });

    }

    private boolean mIsInitialized = false;

    /**
     * 文字列を右から左へひたすら流し続ける表示スタイルを定義します.
     * このとき{@link #setFlowInterval(int)} やXML属性{@code flow_interval}で指定した時間だけ文字列どうしの間隔をあけて流します.
     */
    public static final int FLOW_STYLE_OVER = 0;
    /**
     * 文字列を右から左へ流してゆき文字列右端がView右端に達したら折り返して左から右へ流し返すのを繰り返す表示スタイルを定義します.
     * このとき{@link #setFlowInterval(int)} やXML属性{@code flow_interval}で指定した時間だけ折り返し時に待機します.
     */
    public static final int FLOW_STYLE_TURN = 1;

    private volatile boolean mIsLoop = false;
    private int mWidth, mHeight;

    private String mText;
    private int mTextColor;
    private float mTextSize;
    private float mTextWidth;
    private float mTextMaxHeight;

    private boolean mIsFlowRepeat = true;
    private boolean mIsFlowOut = true;
    private float mFlowSpeed = 50f;
    private int mFlowInterval = 800;
    private float mTextX;
    private float mTextBaseLineOffset;
    private float mEdgeFadingWidth;
    private int mBackColor;
    private int mFadingColor;
    private float mFlowVelocity;
    private int mWaitCnt;

    private boolean mIsViewWidthFixed = false;

    private final Paint mPaint;
    private final Paint mFadingRightPaint,mFadingLeftPaint;
    private final Paint mBackPaint;
    private Bitmap mTextBitmap;
    private Canvas mTextCanvas;

    private Rect mDrawTarget;
    private RectF mFadingRight;
    private RectF mFadingLeft;

    /**
     * このViewの最適な大きさを計算します.
     * @param widthSpec 親Viewから要求される横幅の制限　{@link View.MeasureSpec}でエンコードされている
     * @param heightSpec 親Viewから要求される縦幅の制限　{@link View.MeasureSpec}でエンコードされている
     */
    @Override
    protected void onMeasure(int widthSpec, int heightSpec){
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        int measuredWidth = width;
        int measuredHeight = height;
        int fitWidth = (int)(mTextWidth+2*mEdgeFadingWidth+getPaddingLeft()+getPaddingRight());
        int fitHeight = (int)(mTextMaxHeight+getPaddingBottom()+getPaddingTop());
        if ( !mIsInitialized ){
            mIsViewWidthFixed = MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY;
        }
        switch ( MeasureSpec.getMode(widthSpec) ){
            case MeasureSpec.AT_MOST:
                measuredWidth = MeasureSpec.AT_MOST + ( fitWidth < width ? fitWidth : width);
                break;
            case MeasureSpec.EXACTLY:
                measuredWidth = MeasureSpec.EXACTLY + width;
                break;
            case MeasureSpec.UNSPECIFIED:
                measuredWidth = MeasureSpec.EXACTLY + (int)(mTextWidth+2*mEdgeFadingWidth);
                break;
        }
        switch ( MeasureSpec.getMode(heightSpec) ){
            case MeasureSpec.AT_MOST:
                measuredHeight = MeasureSpec.AT_MOST + ( fitHeight < height ? fitHeight : height );
                break;
            case MeasureSpec.EXACTLY:
                measuredHeight = MeasureSpec.EXACTLY + height;
                break;
            case MeasureSpec.UNSPECIFIED:
                measuredHeight = MeasureSpec.EXACTLY + fitHeight;
                break;
        }
        setMeasuredDimension(measuredWidth,measuredHeight);
        //super.onMeasure(widthSpec, heightSpec);
    }

    /**
     * 表示する文字列の色を指定します.
     * @param color 文字の色 上位から8bitずつα,R,G,B
     */
    public void setTextColor(int color){
        if ( color != mTextColor ){
            mTextColor = color;
            mPaint.setColor(color);
        }
    }

    /**
     * 表示している文字列の色を取得します.
     * @return 文字の色
     */
    public int getTextColor(){
        return mTextColor;
    }

    /**
     * 表示する文字列を指定します.
     * @param text 表示させる文字列 {@code null}は無視されます
     */
    public void setText(String text){
        if ( text != null && !text.equals(mText) ){
            mText = text;
            final float oldValue = mTextWidth;
            onTextSizeChanged(mText, mTextSize);
            if ( mIsInitialized ){
                //リサイズが必要な場合のみリクエスト
                if ( oldValue <= mDrawTarget.width() ){
                    if ( !mIsViewWidthFixed && oldValue != mTextWidth ){
                        requestLayout();
                        return;
                    }
                }else{
                    if ( !mIsViewWidthFixed && mTextWidth < mDrawTarget.width() ){
                        requestLayout();
                        return;
                    }
                }

                //リサイズが不必要ならそのまま描写再開
                onDrawStart(mIsFlowRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN);
                requestInvalidate();
            }
        }
    }

    /**
     * 表示している文字列を取得します.
     * @return 文字列 not {@code null}
     */
    public String getText(){
        return mText;
    }

    /**
     * {@link TypedValue#COMPLEX_UNIT_SP}単位で表示する文字列のサイズを指定します.
     * @param size sp単位のサイズ　0以下の値は無効
     */
    public void setTextSize(float size){
        setTextSize(TypedValue.COMPLEX_UNIT_SP,size);
    }

    /**
     * 表示する文字の大きさを指定します.
     * @param unit 大きさの単位　定数TypedValue.COMPLEX_##
     * @param size サイズ値 0以下の値は無効です
     */
    public void setTextSize(int unit, float size){
        float value = convertToPixels(unit,size);
        if ( value > 0 && value != mTextSize ){
            mTextSize = value;
            onTextSizeChanged(mText, mTextSize);
            //Viewをリサイズ
            requestLayout();
        }
    }

    /**
     * {@link TypedValue#COMPLEX_UNIT_SP}単位で表示している文字のサイズを取得します.
     * @return sp単位の文字サイズ
     */
    public float getTextSize(){
        return getTextSize(TypedValue.COMPLEX_UNIT_SP);
    }

    /**
     * 指定した単位で表示している文字サイズを取得します.
     * @param unit 単位 定数TypedValue.COMPLEX_##
     * @return 文字サイズ 0より大きい値
     */
    public float getTextSize(int unit){
        return convertFromPixels(unit,mTextSize);
    }

    /**
     * {@link TypedValue#COMPLEX_UNIT_DIP}単位で文字列を流す速さを指定します.
     * @param speed　速さ 0未満の値は無効
     */
    public void setFlowSpeed(float speed){
        setFlowSpeed(TypedValue.COMPLEX_UNIT_DIP,speed);
    }

    /**
     * 指定した単位で文字列を流す速さを指定します.
     * @param unit 単位 定数TypedValue.COMPLEX_##
     * @param speed 速さ 0未満の値は無効
     */
    public void setFlowSpeed(int unit, float speed){
        float value = convertToPixels(unit,speed);
        if ( value >= 0f && value != mFlowSpeed ){
            mFlowSpeed = value;
            onDrawStart(mIsFlowRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN);
            requestInvalidate();
        }
    }

    /**
     * {@link TypedValue#COMPLEX_UNIT_DIP}単位で文字列を流す速さを取得します.
     * @return 速さ　0以上の値
     */
    public float getFlowSpeed(){
        return getFlowSpeed(TypedValue.COMPLEX_UNIT_DIP) ;
    }

    /**
     * 指定した単位で文字列を流す速さを取得します.
     * @param unit 単位 定数TypedValue.COMPLEX_##
     * @return 速さ　0以上の値
     */
    public float getFlowSpeed(int unit){
        return convertFromPixels(unit,mFlowSpeed);
    }

    /**
     * 文字列を繰り返し流して表示するか指定します
     * @param isRepeat {@code true}の場合は指定されたスタイルで繰り返し流し表示します
     *                             {@code false}の場合は初めに一回のみ流して表示してあとは表示開始時と同じ位置で文字列を固定表示します.
     */
    public void setFlowRepeat(boolean isRepeat){
        if ( isRepeat != mIsFlowRepeat ){
            mIsFlowRepeat = isRepeat;
            onDrawStart(isRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN);
            requestInvalidate();
        }
    }

    /**
     * 文字列を繰り返し流して表示しているか取得します.
     * @return 繰り返しの場合は{@code true}
     */
    public boolean isFlowRepeat(){
        return mIsFlowRepeat;
    }

    /**
     * 文字列を繰り返し流して表示する時、その間隔をミリ秒で指定します.
     * この値が具体的にどのように間隔を定義するかは{@link #setFlowStyle(int)}やXML属性{@code flow_style}で指定した表示スタイルに拠ります.
     * @param time 0未満の値は無効
     */
    public void setFlowInterval(int time){
        if ( time >= 0 && time != mFlowInterval ){
            mFlowInterval = time;
            onDrawStart(mIsFlowRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN);
            requestInvalidate();
        }
    }

    /**
     * 文字列を繰り返し流して表示する時、その間隔をミリ秒で取得します.
     * この値が具体的にどのように間隔を定義するかは{@link #setFlowStyle(int)}やXML属性{@code flow_style}で指定した表示スタイルに拠ります.
     * @return 0以上の値
     */
    public int getFlowInterval(){
        return mFlowInterval;
    }

    /**
     * 文字列を流して表示する方法を指定します.
     * @param style 定数FLOW_STYLE_#####
     */
    public void setFlowStyle(int style){
        boolean value = style == FLOW_STYLE_OVER;
        if ( value != mIsFlowOut ){
            mIsFlowOut = value;
            onDrawStart(mIsFlowRepeat, value ? FLOW_STYLE_OVER : FLOW_STYLE_TURN);
            requestInvalidate();
        }
    }

    /**
     * 文字列の表示スタイルを取得します
     * @return スタイルを定義した定数FLOW_STYLE_####
     */
    public int getFlowStyle(){
        return mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN;
    }

    /**
     * {@link TypedValue#COMPLEX_UNIT_DIP}単位で左右両端のグラデーション幅を指定します.
     * @param width 0未満の値は無効
     */
    public void setEdgeFadingWidth(float width){
        setEdgeFadingWidth(TypedValue.COMPLEX_UNIT_DIP,width);
    }

    /**
     * 指定した単位で左右両端のグラデーション幅を指定します.
     * @param unit　単位 TypedValue.COMPLEX_###
     * @param width 0未満の値は無効
     */
    public void setEdgeFadingWidth(int unit, float width){
        float value = convertToPixels(unit,width);
        if ( value >= 0f && value != mEdgeFadingWidth ){
            mEdgeFadingWidth = value;
            //Viewをリサイズ
            requestLayout();
        }
    }

    /**
     * {@link TypedValue#COMPLEX_UNIT_DIP}単位で左右両端のグラデーション幅を取得します.
     * @return グラデーションの幅 0以上の値
     */
    public float getEdgeFadingWidth(){
        return getEdgeFadingWidth(TypedValue.COMPLEX_UNIT_DIP);
    }

    /**
     * 指定した単位で左右両端のグラデーション幅を取得します.
     * @param unit 単位 TypedValue.COMPLEX_###
     * @return グラデーションの幅 0以上の値
     */
    public float getEdgeFadingWidth(int unit){
        return convertFromPixels(unit,mEdgeFadingWidth);
    }

    /**
     * 背景の色を指定します
     * @param color 背景色
     */
    public void setBackColor(int color){
        mBackColor = color;
        mBackPaint.setColor(color);
        syncFadingDrawing(color,mFadingColor);
    }

    /**
     * 背景の色を取得します.
     * @return 背景色
     */
    public int getBackColor(){
        return mBackColor;
    }

    /**
     * 左右両端のグラデーションの色を指定します.
     * {@link #setEdgeFadingWidth(int, float)}で正の値が指定されていれば、
     * {@link #getBackColor()　背景色}から指定した色へのグラデーション効果かViewの両端にかかります.
     * @param color グラデーションの背景色と対になる色
     */
    public void setFadingColor(int color){
        mFadingColor = color;
        syncFadingDrawing(mBackColor,color);
    }

    /**
     * グラデーションの色を取得します.
     * @return view左右両端のグラデーションの色で背景色と対になる方の色
     */
    public int getFadingColor(){
        return mFadingColor;
    }

    private void syncFadingDrawing(int backColor, int fadingColor){
        //色の変更や描写位置の変更に対するグラデーション処理
        backColor &= 0x00ffffff;
        fadingColor |= 0xff000000;
        mFadingRightPaint.setShader(new LinearGradient(mFadingRight.left,0,mFadingRight.right,0,backColor,fadingColor, Shader.TileMode.CLAMP));
        mFadingLeftPaint.setShader(new LinearGradient(mFadingLeft.left,0,mFadingLeft.right,0,fadingColor,backColor, Shader.TileMode.CLAMP));
    }

    private float convertToPixels(int unit, float value){
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        switch ( unit ){
            case TypedValue.COMPLEX_UNIT_DIP:
                value *= metrics.density;
                break;
            case TypedValue.COMPLEX_UNIT_SP:
                value *= metrics.scaledDensity;
                break;
            case TypedValue.COMPLEX_UNIT_PX:
            default:
        }
        return value;
    }

    private float convertFromPixels(int unit, float value){
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        switch ( unit ){
            case TypedValue.COMPLEX_UNIT_DIP:
                value /= metrics.density;
                break;
            case TypedValue.COMPLEX_UNIT_SP:
                value /= metrics.scaledDensity;
                break;
            case TypedValue.COMPLEX_UNIT_PX:
            default:
        }
        return value;
    }

    /**
     * 表示する文字列の大きさの変更を処理します.<br>
     * <strong>注意</strong> Overrideする場合は{@code super.onTextSizeChanged(String,float)}を呼ぶこと<br>
     * 以下のような表示する文字列の幅や高さを変更する操作のあとにコールされます.
     * <ul>
     *     <li>{@link #setText(String)}文字列の変更</li>
     *     <li>{@link #setTextSize(float)}文字のサイズ変更</li>
     *     <li>このViewがInflateされた直後</li>
     * </ul>
     * この文字列サイズ変更の処理が終わると
     * {@link #onDrawStart(boolean, int)}での初期化を経て文字列を初めから描写します.
     * そのため、文字列が流れている途中に文字サイズ変更の操作を行うと今の描写を中断して新たなサイズで初めの位置から文字描写を開始します.
     * @param text 表示する文字列
     * @param textSize 表示する文字のサイズ ピクセル単位
     */
    @CallSuper
    protected void onTextSizeChanged(String text, float textSize){
        mPaint.setTextSize(textSize);
        mPaint.setAntiAlias(true);
        Paint.FontMetrics metrics = mPaint.getFontMetrics();
        mTextBaseLineOffset = -( metrics.ascent + metrics.descent )/2f;
        mTextMaxHeight = metrics.bottom - metrics.top;
        mTextWidth = mPaint.measureText(text);
    }

    /**
     * Viewを描写します.<br>
     * {@link #onDrawStart(boolean, int)}で初期化を経てから表示する文字の移動が無くなるまで繰り返し呼ばれます.
     * @param canvas 描写先のオブジェクト
     * @param deltaTime 以前の描写からの経過時間 ミリ秒
     */
    protected void onDrawCanvas(Canvas canvas, int deltaTime){
        canvas.drawRect(mDrawTarget,mBackPaint);
        if ( mEdgeFadingWidth > 0f ){
            canvas.drawRect(mFadingLeft, mBackPaint);
            canvas.drawRect(mFadingRight, mBackPaint);
        }
        final float top = mDrawTarget.top;
        final float bottom = mDrawTarget.bottom;
        final float right = mDrawTarget.right - mEdgeFadingWidth;
        final float left = mDrawTarget.left + mEdgeFadingWidth;
        //mTextX += deltaTime * mFlowVelocity;
        mTextCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if ( mFlowVelocity != 0f){
            if ( mWaitCnt <= 0 ){
                if ( mIsFlowOut ){
                    mTextX += deltaTime * mFlowVelocity / 1000f;
                    float gap = mFlowInterval * mFlowSpeed / 1000f;
                    if ( mTextX + mTextWidth + gap <= left ){
                        if ( mIsFlowRepeat ){
                            mTextX += (mTextWidth + gap);
                        }else{
                            mTextX = left;
                            mFlowVelocity = 0f;
                        }
                    }
                    mTextCanvas.drawText(mText, mTextX, (top + bottom) / 2 + mTextBaseLineOffset, mPaint);
                    if ( mTextX + mTextWidth + gap < right ){
                        mTextCanvas.drawText(mText, mTextX + mTextWidth + gap, (top + bottom) / 2 + mTextBaseLineOffset, mPaint);
                    }
                }else{
                    mTextX += deltaTime * mFlowVelocity / 1000f;
                    if ( mFlowVelocity < 0f && mTextX + mTextWidth <= right ){
                        mTextX = right - mTextWidth;
                        mWaitCnt = mFlowInterval;
                        mFlowVelocity = mFlowSpeed;
                    }else if ( mFlowVelocity > 0f && mTextX >= left ){
                        mTextX = left;
                        if ( mIsFlowRepeat ){
                            mWaitCnt = mFlowInterval;
                            mFlowVelocity = -mFlowSpeed;
                        }else{
                            mFlowVelocity = 0f;
                        }
                    }
                    mTextCanvas.drawText(mText, mTextX, (top + bottom) / 2 + mTextBaseLineOffset, mPaint);
                }
            }else{
                mWaitCnt -= deltaTime;
                mTextCanvas.drawText(mText, mTextX, (top + bottom) / 2 + mTextBaseLineOffset, mPaint);
            }
        }else{
            mTextCanvas.drawText(mText, mTextX, (top + bottom)/2 + mTextBaseLineOffset , mPaint);
            //動かないなら一度描写して終了
            mIsLoop = false;
        }
        canvas.drawBitmap(mTextBitmap,mDrawTarget,mDrawTarget,null);
        if ( mEdgeFadingWidth > 0f && mTextWidth > mDrawTarget.width() ){
            canvas.drawRect(mFadingLeft, mFadingLeftPaint);
            canvas.drawRect(mFadingRight, mFadingRightPaint);
        }
    }


    /**
     * 文字列を描写する範囲のサイズ変更を処理します.<br>
     * <strong>注意</strong> Override時は{@code super.onDrawSizeChanged(int,int,float)}を必ず呼ぶこと<br>
     * 通常は以下のような描写範囲サイズの変更を伴う操作後にコールされます<br>
     * <ul>
     *     <li>このViewがinflateされた直後</li>
     *     <li>{@link #setTextSize(float)}での文字サイズ変更後</li>
     *     <li>{@link #setText(String)}で表示する文字列を変更しかつリサイズが必要な時</li>
     *     <li>{@link #setEdgeFadingWidth(int, float)}で左右両端のグラデーション幅を変更した時</li>
     * </ul><br>
     * サイズ変更処理のあとには{@link #onDrawStart(boolean, int)}で初期化を経て初めの位置から文字を描写します.
     * @see #onDrawStart(boolean, int)
     * @param width View全体の幅 in pixels
     * @param height View全体の高さ in pixels
     * @param fadingWidth 左右両端のグラデーション幅 in pixels
     */
    @CallSuper
    protected void onDrawSizeChanged(int width, int height, float fadingWidth){
        //initialize the rect, in which text is to be drawn
        mDrawTarget = new Rect(getPaddingLeft(),getPaddingTop(),width-getPaddingRight(),height-getPaddingBottom());
        mFadingLeft = new RectF(mDrawTarget.left,mDrawTarget.top,mDrawTarget.left+fadingWidth,mDrawTarget.bottom);
        mFadingRight = new RectF(mDrawTarget.right-fadingWidth,mDrawTarget.top,mDrawTarget.right,mDrawTarget.bottom);
        mTextBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        mTextCanvas = new Canvas(mTextBitmap);
        syncFadingDrawing(mBackColor,mFadingColor);
    }

    /**
     * 新たな文字の表示設定で描写を開始するときに初期化を行います.<br>
     * <strong>注意</strong> Override時は{@code super.onDrawStart(boolean,int)}を必ず呼ぶこと<br>
     * 以下のような文字表示の方法を変更する操作を行うと、 現在の描写を中断して新たな表示方法で初めの位置から描写が開始されます.
     * <ul>
     *     <li>{@link #setFlowInterval(int)}流しながら表示する文字列の間隔</li>
     *     <li>{@link #setFlowRepeat(boolean)}文字列を繰り返し流すか設定</li>
     *     <li>{@link #setFlowSpeed(int, float)}文字列を流す速さ</li>
     *     <li>{@link #setFlowStyle(int)}文字列をどう流して表示するか</li>
     *     <li>{@link #setText(String)}表示する文字列の変更</li>
     *     <li>その他{@link #onDrawSizeChanged(int, int, float)}での文字表示範囲サイズ変更を伴う操作</li>
     * </ul>
     * @see #onDrawSizeChanged(int, int, float)
     * @param isFlowRepeat 文字列を繰り返し流して表示するか
     * @param flowStyle 文字列を流して表示するスタイル
     */
    @CallSuper
    protected void onDrawStart(boolean isFlowRepeat, int flowStyle){
        final boolean isInside = mTextWidth <= mDrawTarget.width();
        mTextX = isInside ? mDrawTarget.left : mDrawTarget.left + mEdgeFadingWidth;
        mFlowVelocity = isInside ? 0f : -mFlowSpeed;
        mWaitCnt = mFlowInterval;
    }

    private void requestInvalidate(){
        if ( !mIsLoop ){
            mIsLoop = true;
            mTime = SystemClock.uptimeMillis();
            postInvalidate();
        }
    }

    private long mTime;

    @Override
    public final void onDraw(Canvas canvas){
        if ( mIsInitialized ){
            long time = SystemClock.uptimeMillis();
            onDrawCanvas(canvas, (int)(time - mTime));
            mTime = time;
            if ( mIsLoop ){
                postInvalidate();
            }
        }
    }

    static class SavedState extends BaseSavedState {

        SavedState(Parcelable superState){
            super(superState);
        }

        private SavedState(Parcel source){
            super(source);
            mText = source.readString();
            mTextColor = source.readInt();
            mTextSize = source.readFloat();
            mIsFlowRepeat = source.readInt() == 1;
            mFlowStyle = source.readInt();
            mFlowSpeed = source.readFloat();
            mFlowInterval = source.readInt();
            mEdgeFadingWidth = source.readFloat();
            mBackColor = source.readInt();
            mFadingColor = source.readInt();
        }

        private String mText;
        private int mTextColor;
        private float mTextSize;

        private boolean mIsFlowRepeat;
        private int mFlowStyle;
        private float mFlowSpeed;
        private int mFlowInterval;
        private float mEdgeFadingWidth;
        private int mBackColor;
        private int mFadingColor;

        @Override
        public void writeToParcel(Parcel out, int flags){
            super.writeToParcel(out,flags);
            out.writeString(mText);
            out.writeInt(mTextColor);
            out.writeFloat(mTextSize);
            out.writeInt( mIsFlowRepeat ? 1 : 0 );
            out.writeInt(mFlowStyle);
            out.writeFloat(mFlowSpeed);
            out.writeInt(mFlowInterval);
            out.writeFloat(mEdgeFadingWidth);
            out.writeInt(mBackColor);
            out.writeInt(mFadingColor);
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US,
                    "FlowTextView.SavedState{%s text=%s, style=%d, speed=%f}",
                    Integer.toHexString(System.identityHashCode(this)),mText,mFlowStyle,mFlowSpeed
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
        state.mText = getText();
        state.mTextColor = getTextColor();
        state.mTextSize = getTextSize(TypedValue.COMPLEX_UNIT_PX);
        state.mIsFlowRepeat = isFlowRepeat();
        state.mFlowStyle = getFlowStyle();
        state.mFlowSpeed = getFlowSpeed(TypedValue.COMPLEX_UNIT_PX);
        state.mFlowInterval = getFlowInterval();
        state.mEdgeFadingWidth = getEdgeFadingWidth(TypedValue.COMPLEX_UNIT_PX);
        state.mBackColor = getBackColor();
        state.mFadingColor = getFadingColor();
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable data){
        SavedState state = (SavedState)data;
        super.onRestoreInstanceState(state.getSuperState());
        mText = state.mText;
        mTextColor = state.mTextColor;
        mTextSize = state.mTextSize;
        mIsFlowRepeat = state.mIsFlowRepeat;
        mIsFlowOut = FLOW_STYLE_TURN != state.mFlowStyle;
        mFlowSpeed = state.mFlowSpeed;
        mFlowInterval = state.mFlowInterval;
        mEdgeFadingWidth = state.mEdgeFadingWidth;
        mBackColor = state.mBackColor;
        mFadingColor = state.mFadingColor;
        requestLayout();
    }
}
