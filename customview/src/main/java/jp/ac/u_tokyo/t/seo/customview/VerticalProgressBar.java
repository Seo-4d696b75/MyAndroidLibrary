package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/**
 * {@link ProgressBar}を継承して縦プログレスバーを実現
 * 
 * <strong>注意</strong>
 * XMLタグの{@code minWidth,minHeight,maxWidth,maxHeight}の縦横は
 * Viewの縦横ではなく、ProgressBarを横から見たときの縦横と解釈する.
 * 
 * @author Seo-4d696b75
 * @version 2017/11/18.
 */

public class VerticalProgressBar extends ProgressBar{

    /**
     * UIスレッドの識別子
     * UIスレッドで実行する必要がある処理の前に確認する
     * superクラスにも同様のメンバがあるが参照できないので自前で用意
     */
    private long mUiThreadId;

    /**
     * 現在の描写に用いられている画像
     * superクラスにも同様のメンバがあるが参照できないので自前で用意
     */
    private Drawable mCurrentDrawable;

    /**
     * paddingを除いたViewの内側のサイズに関する最大・最小の大きさ.
     * たたし、縦横は回転した座標系から見ること
     */
    private int mMinWidth,mMaxWidth,mMinHeight,mMaxHeight;

    private Rect mPadding;

    public VerticalProgressBar(Context context){
        super(context);
        onInitialize(context, null, getInternalID("attr","progressBarStyle"));
    }

    public VerticalProgressBar(Context context, AttributeSet attrs){
        super(context, attrs);
        onInitialize(context, attrs, getInternalID("attr","progressBarStyle"));
    }

    public VerticalProgressBar(Context context, AttributeSet attrs, int defStyle){
        super(context,attrs,defStyle);
        onInitialize(context, attrs, defStyle);
    }

    private static int getInternalID(String type, String name){
        return Resources.getSystem().getIdentifier(name,type,"android");
    }

    /*
    private int convertPixel(String value, DisplayMetrics metrics){
        if ( value != null ){
            Matcher matcher = Pattern.compile("([0-9\\.]+)(.*)").matcher(value);
            if ( matcher.matches() ){
                switch ( matcher.group(2) ){
                    case "dip":
                        return (int)(Float.parseFloat(matcher.group(1)) * metrics.density);
                    case "sp":
                        return (int)(Float.parseFloat(matcher.group(1)) * metrics.scaledDensity);
                    case "":
                        return (int)(Float.parseFloat(matcher.group(1)));
                    default:
                }
            }
        }
        return 0;
    }

    private int resolveContentSize(int invertedValue, int xmlValue, TypedArray array, int index, boolean max, int defaultValue){
        if ( array.hasValue(index) ){
            int value = array.getDimensionPixelSize(index,0);
            if ( value != xmlValue ){
                if ( (max && value > invertedValue) || (!max && value < invertedValue) ){
                    return value;
                }
            }
        }
        return invertedValue <= 0 ? defaultValue : invertedValue;
    }*/

    private void onInitialize(Context context, AttributeSet set, int defStyle){

        mUiThreadId = Thread.currentThread().getId();

        /*
        final String NAME_SPACE = "http://schemas.android.com/apk/res/android";
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        int minHeight = convertPixel(set.getAttributeValue(NAME_SPACE,"minHeight"),metrics);
        int maxHeight = convertPixel(set.getAttributeValue(NAME_SPACE,"maxHeight"),metrics);
        int minWidth = convertPixel(set.getAttributeValue(NAME_SPACE,"minWidth"),metrics);
        int maxWidth = convertPixel(set.getAttributeValue(NAME_SPACE,"maxWidth"),metrics);

        TypedArray a = context.obtainStyledAttributes(set, R.styleable.ProgressBar);
        mMinWidth = resolveContentSize(minHeight,minWidth,a,R.styleable.ProgressBar_android_minWidth,false,24);
        mMaxWidth = resolveContentSize(maxHeight,maxWidth,a,R.styleable.ProgressBar_android_maxWidth,true, 48);
        mMinHeight = resolveContentSize(minWidth,minHeight,a,R.styleable.ProgressBar_android_minHeight,false, 24);
        mMaxHeight = resolveContentSize(maxWidth,maxHeight,a,R.styleable.ProgressBar_android_maxHeight,true, 48);
        a.recycle();*/
        TypedArray a = context.obtainStyledAttributes(set, R.styleable.ProgressBar);
        mMinWidth = a.getDimensionPixelSize(R.styleable.ProgressBar_android_minWidth,24);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.ProgressBar_android_maxWidth,48);
        mMinHeight = a.getDimensionPixelSize(R.styleable.ProgressBar_android_minHeight,24);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.ProgressBar_android_maxHeight,48);

        /*
        superクラスに縦横を逆にして処理してもらうとき、paddingの値には注意が必要
        superクラスはpadding値をviewのメンバを直接参照しているため
        Viewにpaddingを縦横逆に設定し直す　そうすればViewのメンバは縦横反転した値になる
         */
        int[] attr = new int[]{
                android.R.attr.paddingLeft,
                android.R.attr.paddingTop,
                android.R.attr.paddingRight,
                android.R.attr.paddingBottom
        };
        //Attribute経由でXMLから指定されたパディング値
        a = context.obtainStyledAttributes(set, attr);
        int originalLeft = a.getDimensionPixelSize(0,0);
        int originalTop = a.getDimensionPixelSize(1,0);
        int originalRight = a.getDimensionPixelSize(2,0);
        int originalBottom = a.getDimensionPixelSize(3,0);
        a.recycle();

        //第一項がXMLからの値で第2・3項はスパークラスのコンストラクタ内でセットされたパディング
        //回転した座標系に合わせ直すのは前者のみ
        int left = originalBottom + super.getPaddingLeft() - originalLeft;
        int top = originalLeft + super.getPaddingTop() - originalTop;
        int right = originalTop + super.getPaddingRight() - originalRight;
        int bottom = originalRight + super.getPaddingBottom() - originalBottom;

        mPadding = new Rect(left, top, right, bottom);
        super.setPadding(left, top, right, bottom);
    }

    @Override
    public synchronized void setPadding(int left,int top,int right,int bottom){
        super.setPadding(bottom,left,top,right);
        mPadding = new Rect(
                super.getPaddingLeft(),
                super.getPaddingTop(),
                super.getPaddingRight(),
                super.getPaddingBottom()
        );
    }

    @Override
    public synchronized void setPaddingRelative(int left,int top,int right,int bottom){
        super.setPaddingRelative(bottom,left,top,right);
        mPadding = new Rect(
                super.getPaddingLeft(),
                super.getPaddingTop(),
                super.getPaddingRight(),
                super.getPaddingBottom()
        );
    }

    //ViewのメンバmPaddingRight/Top/Left/Bottomや以下の各親クラスgetterは回転した座標系における値を返す
    //外側から見たときはView全体から見た値を正しく返すように調整
    @Override
    public int getPaddingRight(){
        return super.getPaddingBottom();
    }

    @Override
    public int getPaddingBottom(){
        return super.getPaddingLeft();
    }

    @Override
    public int getPaddingLeft(){
        return super.getPaddingTop();
    }

    @Override
    public int getPaddingTop(){
        return super.getPaddingRight();
    }

    protected int getMaxHeight(){return mMaxHeight;}
    protected int getMinHeight(){return mMinHeight;}
    protected int getMaxWidth(){return mMaxWidth;}
    protected int getMinWidth(){return mMinWidth;}

    /**
     * 回転した座標系におけるパディング値を取得する
     * @return not null
     */
    protected synchronized Rect getBiasedPadding(){
        if ( mPadding == null ){
            //before setting padding in bias, not collect value may be returned in this way
            mPadding = new Rect(
                    super.getPaddingBottom(),
                    super.getPaddingLeft(),
                    super.getPaddingTop(),
                    super.getPaddingRight()
            );
        }
        return mPadding;
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        
        int dw = 0;
        int dh = 0;

        final Drawable d = mCurrentDrawable;
        if (d != null) {
            dw = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicWidth()));
            dh = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicHeight()));
        }

        drawableStateChanged();

        final Rect padding = getBiasedPadding();
        dw += padding.right + padding.left;
        dh += padding.top + padding.bottom;

        final int measuredWidth = resolveSizeAndState(dh, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(dw, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        //親クラスProgressBarでは描写するDrawableの位置を計算している
        super.onSizeChanged(h,w,oldh,oldw);
    }

    /**
     * 継承先でも毎回同じようにCanvasを回転させてから描写するのは面倒であるうえに、
     * 縦横を混同しかねないので子クラスには予め回転させてから{@link #onDrawRotated(Canvas)}に渡して
     * そこで描写処理を行ってもらう.
     * @see #onDrawRotated(Canvas)
     */
    @Override
    protected final synchronized void onDraw(Canvas canvas){
        /*
         * onDraw(Canvas)を呼び出すView側は縦横寸法をonMeasure(int,int)内のsetMeasuredDimensions(int,int)で
         * 通知した通り正しく保持している
         * 一方、実際に描写する親クラスProgressBarは逆に認識している、つまり
         * 通常の横向きにした状態と同様に処理してくれる
         * なので、カンバスを回転させて親クラスに描写させる
         * !カンバスの回転・移動はカンバスが抱えているBitmapへの描写位置を決定する時の座標系の回転・移動と解釈する!
         */
        final int h = getHeight();
        canvas.rotate(-90);
        canvas.translate(-h, 0);
        drawBackground(canvas);
        super.onDraw(canvas);
        onDrawRotated(canvas);
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(getHeight() - y, x);
    }

    /**
     * 背景画像を描写し直す.
     * 本来はViewからonDraw()で描写を要求された段階で既に描写済みだが、位置が縦横逆なので書き直す
     */
    protected void drawBackground(Canvas canvas){
        final Drawable background = getBackground();
        if (background == null) {
            return;
        }

        background.setBounds(0, 0, getBottom() - getTop() , getRight() - getLeft());

        final int scrollX = getScrollY();
        final int scrollY = getScrollX();
        if ((scrollX | scrollY) == 0) {
            background.draw(canvas);
        } else {
            canvas.translate(scrollX, scrollY);
            background.draw(canvas);
            canvas.translate(-scrollX, -scrollY);
        }
    }

    /**
     * {@link #onDraw(Canvas)}から座標系を回転した状態でコールされる.
     * {@link VerticalProgressBar}など継承先での描写処理はこちらにどうぞ
     * @param canvas Viewの左下端点を原点として上方向をx軸正、右方向をy軸正方向に座標系を調整した状態
     */
    protected void onDrawRotated(Canvas canvas){}

    /*
    縦横逆のProgressBarを実現するだけならProgressBarを継承して以上のように部分をオーバーライドすれば十分だった
    しかし、ProgressBarを継承したAbsSeekBarに倣ってこのVerticalProgressBarを継承して縦のシークバーAbsVerticalSeekBarなるViewを作ろうとすると問題がある
    AbsSeekBarではシークバー(thumb)の更新のタイミングをProgressBarの値が更新される時と合わせるために、
    ProgressBarのprogress値更新を描写されるDrawableへ反映する処理を行うメソッド
    doRefreshProgress(int id, int progress, boolean fromUser,boolean callBackToApp)からコールされるメソッド
    onProgressRefresh(float scale, boolean fromUser)をオーバーライドする形で実現している

    しかし、doRefreshProgressはprivate,onProgressRefreshはpackage privateであり、
    いづれにせよ、このVerticalProgressBarからも継承先AbsVerticalSeekBarからもこれらProgressBarのメソッドをオーバーライドできない

    何とかして、継承先のAbsVerticalSeekBarでthumbの描写タイミングを取得するために
    ProgressBar#doRefreshProgressのcall-hierarchyを探してこれと同様のタイミングでコールされて、かつ子クラスでオーバーライド可能な
    メソッドを以下のようなonProgressUpdate(int)として実装する

    private void ProgressBar#doRefreshProgress(int id, int progress, boolean fromUser,boolean callBackToApp)
    を呼び出す関数一覧
    ただし、そこからonProgressRefresh()がコールされるのは
    if ( callBackToApp && id == R.id.progress ) .....
    の場合に限るので、trueと評価されるような引数を渡しているものを抽出してある
    (i) public onAttachedToWindow
        refreshProgress内でmRefreshDataにR.id.progressがaddされている場合
        RefreshProgressRunnable.run
        onAttachedToWindow  内でmRefreshDataはclearされている
        細かい条件は割愛して実装 false
    (ii) private refreshProgress(int,int,boolean)
        UIスレッドから呼ばれた場合は即座にdoRefreshProgressをコールしているが、
        そうでない場合はView#post(Runnable)してUIスレッドから呼び直している
        そこで、対応した関数updateProgress(int progress)を用意して
        refreshProgress()同様に実装する

        ただ、コイツモprivateなのでオーバーライドして呼ばれるタイミングを検知するのは不可
        どこから呼ばれているのかな...?
        setProgress(int,boolean),setMax(int) のふたつで各場合とも現在の値と新たな値が異なるときのみ false
        setProgress(int,boolean) はpackage private...oh
        呼び出しているのは......
        publicな方のsetProgress(int),
        あと、継承したシークバーから
        AbsSeekBar#trackTouchEvent,onKeyDown,performAccessibilityAction

        こいつらを適当にオーバーライドしてrefreshProgress(int,int,boolean)の代わりにonRefreshProgress(int)を呼べばよさそう
    (iii) private class RefreshProgressRunnable#run
        (ii)でUIスレッド以外からコールされた場合に該当

    @Override
    void onProgressRefresh(float scale, boolean fromUser) {

    }*/

    /**
     * progress levelが更新されて親クラス{@link ProgressBar}が描写処理を実行するタイミングでコールされる.
     * ただし、UIスレッドからコールされるものとする.
     * {@link VerticalProgressBar}など継承先でProgress level更新時と同時に描写処理を行う時に利用する
     * @param scale 新たな値 ProgressBar#doRefreshProgress(int id, int progress, boolean fromUser,boolean callBackToApp)のprogress値を正規化した値
     */
    protected void onProgressUpdate(float scale, boolean fromUser, int progress){

    }

    /**
     * progress levelが更新されて親クラス{@link ProgressBar}が描写処理を実行するタイミングで呼び出す
     * UIスレッドからのみ呼び出し可能
     * @see #updateProgress(int, boolean)
     */
    private synchronized void onProgressUpdate(int progress, boolean fromUser){
        int max = getMax();
        onProgressUpdate( max > 0 ? (float)progress / max : 0f, fromUser, progress);
    }

    /**
     * UIスレッド外からも呼ばれる場合はここに通知する.
     * @see #onProgressUpdate(int,boolean)
     * @param progress 新たな値
     * @param fromUser ユーザの操作か
     */
    private void updateProgress(final int progress, final boolean fromUser){
        if (mUiThreadId == Thread.currentThread().getId()) {
            onProgressUpdate(progress, fromUser);
        }else{
            post(new Runnable(){
                @Override
                public void run(){
                    onProgressUpdate(progress, fromUser);
                }
            });
        }
    }

    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();
        onProgressUpdate(getProgress(),false);
    }

    private synchronized void setProgressInternal(int progress, boolean fromUser){
        if ( !isIndeterminate() && progress >= 0 && progress <= getMax() && progress != getProgress() ){
            super.setProgress(progress);
            updateProgress(progress,fromUser);
        }
    }

    @Override
    public void setProgress(int progress){
        setProgressInternal(progress, false);
    }

    /**
     * ユーザからの操作でprogress levelが更新されたときに呼ぶ.
     * @param progress new level
     */
    protected void onUserUpdateProgress(int progress){
        setProgressInternal(progress, true);
    }

    @Override
    public synchronized void setMax(int max) {
        if ( max < 0 ){
            max = 0;
        }
        if ( max != getMax() ){
            super.setMax(max);
            updateProgress(getProgress(),false);
        }
    }

    /**
     * {@link ProgressBar}ではpackage privateであるが、このクラスを継承して縦シークバー
     * {@link AbsVerticalSeekBar}を実装する際にアクセスできなくて困る
     * @return The drawable currently used to draw the progress bar
     */
    protected Drawable getCurrentProgressDrawable() {
        return mCurrentDrawable;
    }

    @Override
    public synchronized void setIndeterminate(boolean indeterminate) {
        super.setIndeterminate(indeterminate);
        if ( isIndeterminate() ){
            mCurrentDrawable = getIndeterminateDrawable();
        }else{
            mCurrentDrawable = getProgressDrawable();
        }
    }

    @Override
    public void setIndeterminateDrawable(Drawable d) {
        super.setIndeterminateDrawable(d);
        if ( isIndeterminate() ){
            mCurrentDrawable = d;
        }
    }

    @Override
    public void setProgressDrawable(Drawable d) {
        super.setProgressDrawable(d);
        if ( !isIndeterminate()){
            mCurrentDrawable = d;
        }

        //superではこのタイミングでupdateDrawableBounds(getWidth(),getHeight());
        //が呼ばれているが、回転した座標系ではgetWidth(),getHeight()は逆でないと困る
        //上書きする
        onSizeChanged(getHeight(),getWidth(),0,0);

        if (d != null) {
            int drawableHeight = d.getMinimumHeight();
            //縦横注意
            if (mMaxHeight < drawableHeight) {
                mMaxHeight = drawableHeight;
            }
        }
    }



}
