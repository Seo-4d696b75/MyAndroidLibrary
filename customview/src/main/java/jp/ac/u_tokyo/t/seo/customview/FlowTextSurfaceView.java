package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
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
 *
 * 文字の大きさや文字列の速さなど大きさを指定する際には以下の定数値で単位を同時に指定してください.
 * <ul>
 *     <li>{@link TypedValue#COMPLEX_UNIT_DIP} dp単位</li>
 *     <li>{@link TypedValue#COMPLEX_UNIT_SP} sp単位</li>
 *     <li>{@link TypedValue#COMPLEX_UNIT_PX} pixel単位</li>
 * </ul>
 *
 * このViewは{@link SurfaceView}を継承しているため、通常のViewと違う点に注意.
 * 特に{@link SurfaceHolder.Callback#surfaceDestroyed(SurfaceHolder)}以降では描写スレッドが動いていないためViewが暗転してしまい、
 * FragmentやActivity遷移時に見た目が悪くなる.
 * {@link SurfaceView#setZOrderOnTop(boolean)}でウィンドウのトップレイヤーに張り付けて後ろのViewを透過させる方法では、
 * NavigationDrawerなど他Viewの上に描写されるべきViewまでも上書きされてしまいよろしくない.
 * あと正直、昨今ではSurfaceViewを用いても描写高速化の利点はおよそ皆無.
 * ですから、Viewを継承して実装した{@link FlowTextView}を使いましょう.ツラい.初めから.....
 *
 * @author Seo-4d696b75
 * @version 2017/10/16.
 */

public class FlowTextSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

    public FlowTextSurfaceView(Context context){
        this(context,null);
    }

    public FlowTextSurfaceView(Context context, AttributeSet attrs){
        this(context,attrs,0);
    }

    public FlowTextSurfaceView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);

        mPaint = new Paint();
        mClearPaint = new Paint();
        mClearPaint.setColor(Color.TRANSPARENT);
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mFadingRightPaint = new Paint();
        mFadingLeftPaint = new Paint();
        mBackPaint = new Paint();
        getHolder().addCallback(this);
        //setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

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
            mBackColor = array.getColor(R.styleable.FlowTextView_back_color,0xFFFFFFFF);
        }else{
            mBackColor = 0xFFFFFFFF;
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

        //other initialization, which depends on canvas size, is called in surfaceChanged, right after surfaceCreated

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
    private volatile boolean mIsDrawing = false;
    private SurfaceHolder mHolder;
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
    private final Paint mClearPaint;
    private final Paint mFadingRightPaint,mFadingLeftPaint;
    private final Paint mBackPaint;

    private RectF mDrawTarget;
    private RectF mClearRight;
    private RectF mClearLeft;
    private RectF mFadingRight;
    private RectF mFadingLeft;

    /**
     * <strong>NOTE</strong> The implementation of this function in super class {@link SurfaceView#onMeasure(int, int)}
     * does NOT care the size of its content, and view is forced to be expanded to the maximum size unless having called {@link SurfaceHolder#setFixedSize(int, int)}.
     * So this implementation will ignore and not call {@code super.onMeasured(int,int)}
     * @param widthSpec
     * @param heightSpec
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
            onDrawStop();
            mText = text;
            final float oldValue = mTextWidth;
            onTextSizeChanged(mText,mTextSize);
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
                startDrawingThread();
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
     * {@link TypedValue#COMPLEX_UNIT_SP}単位で表示する文字列のサイズをしてします.
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
            onDrawStop();
            mTextSize = value;
            onTextSizeChanged(mText, mTextSize);
            //Viewをリサイズ -> surfaceChanged()から描写再開
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
            onDrawStop();
            mFlowSpeed = value;
            onDrawStart(mIsFlowRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN);
            startDrawingThread();
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
            onDrawStop();
            mIsFlowRepeat = isRepeat;
            onDrawStart(isRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN );
            startDrawingThread();
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
            onDrawStop();
            mFlowInterval = time;
            onDrawStart(mIsFlowRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN);
            startDrawingThread();
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
            onDrawStop();
            mIsFlowOut = value;
            onDrawStart(mIsFlowRepeat, value ? FLOW_STYLE_OVER : FLOW_STYLE_TURN );
            startDrawingThread();
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
            onDrawStop();
            mEdgeFadingWidth = value;
            //Viewをリサイズ -> surfaceChanged()から描写再開
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
        mFadingRightPaint.setShader(new LinearGradient(mDrawTarget.right,0,mDrawTarget.right+mEdgeFadingWidth,0,backColor,fadingColor, Shader.TileMode.CLAMP));
        mFadingLeftPaint.setShader(new LinearGradient(mDrawTarget.left-mEdgeFadingWidth,0,mDrawTarget.left,0,fadingColor,backColor, Shader.TileMode.CLAMP));
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
     * この文字列サイズ変更の処理が終わると{@link #onDrawStart(boolean, int)}での初期化を経て描写スレッドが開始されます.
     * @param text 表示する文字列
     * @param textSize 表示する文字のサイズ ピクセル単位
     */
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
     * UIスレッドとは独立したスレッドが{@link #onDrawStart(boolean, int)}で初期化・開始されてから、
     * {@link #onDrawStop()}で停止するまで繰り返し呼ばれます.
     * @param canvas 描写先のオブジェクト
     * @param deltaTime 以前の描写からの経過時間 ミリ秒
     */
    protected void onDrawCanvas(Canvas canvas,int deltaTime){
        canvas.drawColor(mBackColor);
        //canvas.drawRect(mDrawTarget,mBackPaint);
        if ( mEdgeFadingWidth > 0f ){
            canvas.drawRect(mFadingLeft, mBackPaint);
            canvas.drawRect(mFadingRight, mBackPaint);
        }
        final float top = mDrawTarget.top;
        final float bottom = mDrawTarget.bottom;
        final float right = mDrawTarget.right;
        final float left = mDrawTarget.left;
        //mTextX += deltaTime * mFlowVelocity;
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
                    canvas.drawText(mText, mTextX, (top + bottom) / 2 + mTextBaseLineOffset, mPaint);
                    if ( mTextX + mTextWidth + gap < right ){
                        canvas.drawText(mText, mTextX + mTextWidth + gap, (top + bottom) / 2 + mTextBaseLineOffset, mPaint);
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
                    canvas.drawText(mText, mTextX, (top + bottom) / 2 + mTextBaseLineOffset, mPaint);
                }
            }else{
                mWaitCnt -= deltaTime;
                canvas.drawText(mText, mTextX, (top + bottom) / 2 + mTextBaseLineOffset, mPaint);
            }
        }else{
            canvas.drawText(mText, mTextX, (top + bottom)/2 + mTextBaseLineOffset , mPaint);
            //動かないなら一度描写して終了
            mIsLoop = false;
        }
        //canvas.drawRect(mClearLeft,mClearPaint);
        //canvas.drawRect(mClearRight,mClearPaint);
        canvas.drawRect(mClearLeft,mBackPaint);
        canvas.drawRect(mClearRight,mBackPaint);
        if ( mEdgeFadingWidth > 0f ){
            canvas.drawRect(mFadingLeft, mFadingLeftPaint);
            canvas.drawRect(mFadingRight, mFadingRightPaint);
        }
        //canvas.drawText(String.format("time:%04d",time),left+10,top+10,mPaint);
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
     * サイズ変更処理のあとには{@link #onDrawStart(boolean, int)}で初期化を経て描写スレッドが開始されます.
     * @see #onDrawStart(boolean, int)
     * @param width View全体の幅 in pixels
     * @param height View全体の高さ in pixels
     * @param fadingWidth 左右両端のグラデーション幅 in pixels
     */
    protected void onDrawSizeChanged(int width, int height, float fadingWidth){
        //initialize the rect, in which text is to be drawn
        mDrawTarget = new RectF(getPaddingLeft()+fadingWidth,getPaddingTop(),width-getPaddingRight()-fadingWidth,height-getPaddingBottom());
        mClearLeft = new RectF(0,0,mDrawTarget.left-fadingWidth,height);
        mClearRight = new RectF(mDrawTarget.right+fadingWidth,0,width,height);
        mFadingLeft = new RectF(mClearLeft.right,mDrawTarget.top,mDrawTarget.left,mDrawTarget.bottom);
        mFadingRight = new RectF(mDrawTarget.right,mDrawTarget.top,mClearRight.left,mDrawTarget.bottom);
        syncFadingDrawing(mBackColor,mFadingColor);
    }

    /**
     * 描写スレッド開始時に初期化を行います.<br>
     * <strong>注意</strong> Override時は{@code super.onDrawStart(boolean,int)}を必ず呼ぶこと<br>
     * UIスレッドとは独立してViewを描写しているスレッドの開始をコールバックします.
     * 以下のような文字表示の方法を変更する操作を行うと、 <br>
     * {@link #onDrawStop()}で描写スレッド停止 -> 変更処理の反映 -> ここで描写スレッドの初期化 -> 描写スレッド開始<br>
     * という流れで処理されます.<br>
     * <ul>
     *     <li>{@link #setFlowInterval(int)}流しながら表示する文字列の間隔</li>
     *     <li>{@link #setFlowRepeat(boolean)}文字列を繰り返し流すか設定</li>
     *     <li>{@link #setFlowSpeed(int, float)}文字列を流す速さ</li>
     *     <li>{@link #setFlowStyle(int)}文字列をどう流して表示するか</li>
     *     <li>{@link #setText(String)}表示する文字列の変更</li>
     *     <li>その他{@link #onDrawSizeChanged(int, int, float)}での文字表示範囲サイズ変更を伴う操作</li>
     * </ul>
     * @see #onDrawStop()
     * @see #onDrawSizeChanged(int, int, float)
     * @param isFlowRepeat 文字列を繰り返し流して表示するか
     * @param flowStyle 文字列を流して表示するスタイル
     */
    protected void onDrawStart(boolean isFlowRepeat, int flowStyle){
        mTextX = mDrawTarget.left;
        mFlowVelocity = mTextWidth <= mDrawTarget.width() ? 0f : -mFlowSpeed;
        mWaitCnt = mFlowInterval;
    }

    private void startDrawingThread(){
        mIsLoop = true;
        new Thread(mRoutine).start();
    }

    /**
     * 描写スレッドが停止したときにコールされます.<br>
     * <strong>注意</strong> Override時は{@code super.onDrawStop()}を必ず呼ぶこと<br>
     * {@link SurfaceView}を継承したこのViewではUIスレッドとは独立したスレッドでViewを描写していますが、
     * {@link SurfaceHolder.Callback#surfaceDestroyed(SurfaceHolder)}で画面が破棄されたときや、
     * Viewのリサイズと伴うsetterが呼ばれると、その描写スレッドは停止されます.
     * 新たな{@link SurfaceHolder}を取得したら、また別の描写スレッドが開始され
     * {@link #onDrawStart(boolean, int)}がコールバックされます.
     *
     * @see #onDrawStart(boolean, int)
     */
    protected void onDrawStop(){
        //現在まわっている描写スレッドがロックしているCanvasを返すまで待つ
        mIsLoop = false;
        while ( mIsDrawing ){}
    }

    @Override
    public final void surfaceCreated(SurfaceHolder holder){
        mHolder = holder;
    }

    @Override
    public final void surfaceChanged(SurfaceHolder holder, int format, int width, int height){
        if ( !mIsInitialized ){

            //initialize size of drawing area
            mWidth = width;
            mHeight = height;
            mHolder = holder;
            onDrawSizeChanged(width,height,mEdgeFadingWidth);

            //initialize color of text and background
            setTextColor(mTextColor);
            //setBackColor(mBackColor); <- avoid to call onBackColorChanged() twice
            mBackPaint.setColor(mBackColor);
            setFadingColor(mFadingColor);

            mIsInitialized = true;

            //start drawing thread
            onDrawStart(mIsFlowRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN );
            startDrawingThread();

        }else{
            onDrawStop();
            mHolder = holder;
            mWidth = width;
            mHeight = height;
            onDrawSizeChanged(mWidth, mHeight, mEdgeFadingWidth);
            onDrawStart(mIsFlowRepeat, mIsFlowOut ? FLOW_STYLE_OVER : FLOW_STYLE_TURN);
            startDrawingThread();
        }
    }

    @Override
    public final void surfaceDestroyed(SurfaceHolder holder){
        onDrawStop();
    }

    private final Runnable mRoutine = new Runnable(){
        @Override
        public void run(){

            mHolder.setSizeFromLayout();

            long time = 0L;
            long before = SystemClock.uptimeMillis();
            mIsDrawing = true;
            while ( mIsLoop ){
                Canvas canvas = mHolder.lockCanvas();
                if ( canvas != null ){
                    time = SystemClock.uptimeMillis();
                    onDrawCanvas(canvas,(int)(time-before));
                    before = time;
                    mHolder.unlockCanvasAndPost(canvas);
                }else{
                    Log.d("surfaceView","canvas null");
                }
            }
            mIsDrawing = false;
        }
    };

}


