package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.SeekBar;

/**
 * 縦SeekBar実現クラス.<br>
 *
 * {@link SeekBar}を継承しているので、いつも通りのSeekBarのインスタンスメソッドが使える.
 * XMLタグからも普通のSeekBar同様に各種値を指定できる
 * あと最低限Paddingの値もXMLから正しく指定できるようにはしてあるが、それ以外のタグは対象外<br><br>
 * <strong>注意</strong><br>
 * XMLタグの{@code minWidth,minHeight,maxWidth,maxHeight}の縦横は
 * Viewの縦横ではなく、Barを横から見たときの縦横と解釈する.<br>
 * Backgroundがうまく描写できないので何も指定せずデフォルトのDrawableが描画されると変な位置にthumbの影が現れます
 * android:background="@null" など適当に指定しましょう<br>
 *
 * また、Viewクラスのサイズに関わるメソッドは考慮していないので呼び出すと予期せぬ結果を得るかもしれない.
 * たとえばパディングを指定する{@link android.view.View#setPadding(int, int, int, int)}などは縦横がぐちゃぐちゃになる.
 *
 * @author Seo-4d696b75
 * @version 2017/11/15.
 */
public class VerticalSeekBar2 extends SeekBar{

    public VerticalSeekBar2(Context context){
        super(context);
        onInitialize(context,null);
    }

    public VerticalSeekBar2(Context context, AttributeSet attrs){
        super(context,attrs);
        //this(context,attrs,0);
        // defaultStyleをcom.android.internal.R.attr.seekBarStyleにしないとダメ　
        // onMeasure(int,int)内でのProgressBar#getCurrentDrawable() -> null でうまく計測できない
        // もしくはsuper呼ぶ
        onInitialize(context,attrs);
    }

    public VerticalSeekBar2(Context context, AttributeSet attrs, int defaultStyle){
        super(context, attrs, defaultStyle);
        onInitialize(context,attrs);
    }

    protected void onInitialize(Context context, AttributeSet set){

        int[] attr = new int[]{
                android.R.attr.paddingLeft,
                android.R.attr.paddingTop,
                android.R.attr.paddingRight,
                android.R.attr.paddingBottom
        };
        //Attribute経由でXMLから指定されたパディング値
        TypedArray a = context.obtainStyledAttributes(set, attr);
        int originalLeft = a.getDimensionPixelSize(0,0);
        int originalTop = a.getDimensionPixelSize(1,0);
        int originalRight = a.getDimensionPixelSize(2,0);
        int originalBottom = a.getDimensionPixelSize(3,0);
        a.recycle();

        //第一項がXMLからの値で第2・3項はスパークラスのコンストラクタ内でセットされたパディング
        //回転した座標系に合わせ直すのは前者のみ
        int left = originalBottom + getPaddingLeft() - originalLeft;
        int top = originalLeft + getPaddingTop() - originalTop;
        int right = originalTop + getPaddingRight() - originalRight;
        int bottom = originalRight + getPaddingBottom() - originalBottom;

        setPadding(left, top, right, bottom);


    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH){
        //親クラスの方々には縦横のサイズを逆に認識しておいてもらう
        super.onSizeChanged(h,w,oldH,oldW);
    }

    @Override
    protected synchronized void onMeasure(int widthSpec, int heightSpec){
        //縦横を入れ替えて計算してもらう
        //親クラスのAbsSeekBar ではThumbのDrawableの高さ分だけheightに考慮する必要がある実装になっているので、これ必要
        super.onMeasure(heightSpec,widthSpec);
        //計算結果を元に逆転させて通知
        setMeasuredDimension(getMeasuredHeightAndState(),getMeasuredWidthAndState());
    }

    @Override
    protected synchronized void onDraw(Canvas canvas){
        /*
         * onDraw(Canvas)を呼び出すView側は縦横寸法をonMeasure(int,int)内のsetMeasuredDimensions(int,int)で
         * 通知した通り正しく保持している
         * 一方、SeekBarを実際に描写するAbsSeekBar,ProgressBarなど親クラスは逆に認識している、つまり
         * この縦シークバーを通常の横向きにした状態と同様に処理してくれる
         * なので、カンバスを回転させて親クラスに描写させる
         * !カンバスの回転・移動はカンバスが抱えているBitmapへの描写位置を決定する時の座標系の回転・移動と解釈する!
         */
        int h = getHeight();
        canvas.rotate(-90);
        canvas.translate(-h, 0);
        super.onDraw(canvas);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        /*
        タッチイベントからシークバー位置を実際に計算しているAbsSeekBar#trackTouchEvent(MotionEvent)の処理から逆算して
        ダミー位置情報で上手くいけるかと思ったが、横幅が狭い場合だと
        int availableWidth = width - mPaddingLeft - mPaddingRight;
        の値が負になって以降のif文(Paddingを考慮して範囲外のタッチイベントを門前払いする)を突破できないから没
        final int paddingRight = getPaddingRight();
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        float touchPosNormalized = 1f - ( event.getY() - paddingTop ) / ( h - paddingBottom - paddingTop);
        float dummyX = ( w - paddingLeft - paddingRight ) * touchPosNormalized + paddingLeft;

        final int h = getHeight();
        final int w = getWidth();
        MotionEvent dummy = MotionEvent.obtain(
                event.getDownTime(),
                event.getEventTime(),
                event.getAction(),
                ( h - event.getY() ) * w / h,
                event.getX(),
                event.getMetaState()
        );
        return super.onTouchEvent(dummy);


        x,y座標を入れ替えてダミーのMotionEvent流せばよいのでは作戦失敗
        WHY?????
        親クラスでドラッグモーションから値の更新を処理するAbsSeekBar#trackTouchEvent(MotionEvent)ないで
        View#getWidth()が呼ばれてしまっている！縦横反転しているからgetHeight()にしたいがprivate.....
        加えてパディングの値もViewクラスのprotectedメンバを参照する形で取得されていて困った.
        タッチ処理は全て自前で書き直し.*/


        if ( !isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                    onStartDrag(event);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTracking();
                    setPressed(false);
                } else {
                    mIsDragging = true;
                    trackTouchEvent(event);
                    onStopTracking();
                }
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTracking();
                    setPressed(false);
                }
                invalidate();
                break;
        }
        return true;
    }

    /**
     * ユーザのタッチ操作から閾値を超えるドラッグモーションを検出したときに呼ばれる
     * @param event {@link #onTouchEvent(MotionEvent)}のイベント
     */
    protected void onStartDrag(MotionEvent event) {
        setPressed(true);

        if ( mListener != null ){
            mListener.onStartTrackingTouch(this);
        }

        mIsDragging = true;
        trackTouchEvent(event);
    }

    /**
     * ユーザのドラッグ操作が終了した時に呼ばれる
     */
    protected void onStopTracking(){
        mIsDragging = false;
        if ( mListener != null ){
            mListener.onStopTrackingTouch(this);
        }
    }

    /**
     * ユーザのドラッグ操作中に現在の値を計算して更新する.
     * @param event {@link #onTouchEvent(MotionEvent)}のイベント
     */
    protected void trackTouchEvent(MotionEvent event){
        final int y = Math.round(event.getY());
        final int height = getHeight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        final int availableHeight = height - paddingBottom - paddingTop;

        final float scale;
        if ( y < paddingTop ){
            scale = 1.0f;
        }else if ( y > height - paddingBottom ){
            scale = 0.0f;
        }else{
            scale = 1.0f - (y - paddingTop) / (float)availableHeight;
        }

        int progress = Math.round(scale * getMax());

        if ( progress != getProgress() ){
            setProgress(progress);
            if ( mListener != null ){
                mListener.onProgressChanged(this,progress,true);
            }
        }
        onSizeChanged(getWidth(),getHeight(),0,0);
    }

    @Override
    public void setProgress(int progress){
        super.setProgress(progress);
        onSizeChanged(getWidth(),getHeight(),0,0);
    }

    /**
     * ユーザが現在シークバーをドラッグしているか
     */
    private boolean mIsDragging;

    private OnSeekBarChangeListener mListener;

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.mListener = listener;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //こちらはダミーコード渡して親を騙す作戦でおけ
        switch ( keyCode ){
            case KeyEvent.KEYCODE_DPAD_UP:
                return super.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT,event);
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return super.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT,event);
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return super.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN,event);
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return super.onKeyDown(KeyEvent.KEYCODE_DPAD_UP,event);
            default:
                return super.onKeyDown(keyCode,event);
        }
    }

}
