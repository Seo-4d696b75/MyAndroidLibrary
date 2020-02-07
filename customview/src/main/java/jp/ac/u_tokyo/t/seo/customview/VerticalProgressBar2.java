package jp.ac.u_tokyo.t.seo.customview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.v4.util.Pools;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.ProgressBar;

import java.util.ArrayList;

/**
 * ProgressBarの縦バージョン.
 *
 * <strong>注意</strong>
 * XMLタグの{@code minWidth,minHeight,maxWidth,maxHeight}の縦横は
 * Viewの縦横ではなく、ProgressBarを横から見たときの縦横と解釈する.
 *
 * @author Seo-4d696b75
 * @version 2017/11/18.
 */
public class VerticalProgressBar2 extends View{

    private static final int MAX_LEVEL = 10000;
    private static final int TIMEOUT_SEND_ACCESSIBILITY_EVENT = 200;

    /**
     * paddingを除いたViewの内側のサイズに関する最大・最小の大きさ
     * たたし、縦横は回転した座標系から見ること
     */
    private int mMinWidth,mMaxWidth,mMinHeight,mMaxHeight;

    private int mProgress;
    private int mSecondaryProgress;
    private int mMax;

    private int mBehavior;
    private int mDuration;
    private boolean mIndeterminate;
    private boolean mOnlyIndeterminate;
    private Transformation mTransformation;
    private AlphaAnimation mAnimation;
    private boolean mHasAnimation;
    private Drawable mIndeterminateDrawable;
    private Drawable mProgressDrawable;
    private Drawable mCurrentDrawable;
    Bitmap mSampleTile;
    private boolean mNoInvalidate;
    private Interpolator mInterpolator;
    private RefreshProgressRunnable mRefreshProgressRunnable;
    private long mUiThreadId;
    private boolean mShouldStartAnimationDrawable;

    private boolean mInDrawing;
    private boolean mAttached;
    private boolean mRefreshIsPosted;


    private final ArrayList<RefreshData> mRefreshData = new ArrayList<RefreshData>();

    private AccessibilityEventSender mAccessibilityEventSender;

    private final int mProgressID,mSecondaryProgressID;

    private static int getInternalID(String type, String name){
        return Resources.getSystem().getIdentifier(name,type,"android");
    }

    /**
     * Create a new progress bar with range 0...100 and initial progress of 0.
     * @param context the application environment
     */
    public VerticalProgressBar2(Context context) {
        this(context, null);
    }

    public VerticalProgressBar2(Context context, AttributeSet attrs) {
        this(context, attrs, getInternalID("attr","progressBarStyle"));
    }

    public VerticalProgressBar2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mUiThreadId = Thread.currentThread().getId();
        mProgressID = getInternalID("id","progress");
        mSecondaryProgressID = getInternalID("id","secondaryProgress");
        initProgressBar();

        TypedArray a =
                context.obtainStyledAttributes(attrs, R.styleable.ProgressBar, defStyle, 0);

        mNoInvalidate = true;

        Drawable drawable = a.getDrawable(R.styleable.ProgressBar_android_progressDrawable);
        if (drawable != null) {
            drawable = tileify(drawable, false);
            // Calling this method can set mMaxHeight, make sure the corresponding
            // XML attribute for mMaxHeight is read after calling this method
            setProgressDrawable(drawable);
        }


        mDuration = a.getInt(R.styleable.ProgressBar_android_indeterminateDuration, mDuration);

        mMinWidth = a.getDimensionPixelSize(R.styleable.ProgressBar_android_minWidth, mMinWidth);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.ProgressBar_android_maxWidth, mMaxWidth);
        mMinHeight = a.getDimensionPixelSize(R.styleable.ProgressBar_android_minHeight, mMinHeight);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.ProgressBar_android_maxHeight, mMaxHeight);

        mBehavior = a.getInt(R.styleable.ProgressBar_android_indeterminateBehavior, mBehavior);

        final int resID = a.getResourceId(
                getInternalID("styleable", "ProgressBar_interpolator"),
                getInternalID("anim","linear_interpolator")); // default to linear interpolator
        if (resID > 0) {
            setInterpolator(context, resID);
        }

        setMax(a.getInt(R.styleable.ProgressBar_android_max, mMax));

        setProgress(a.getInt(R.styleable.ProgressBar_android_progress, mProgress));

        setSecondaryProgress(
                a.getInt(R.styleable.ProgressBar_android_secondaryProgress, mSecondaryProgress));

        drawable = a.getDrawable(R.styleable.ProgressBar_android_indeterminateDrawable);
        if (drawable != null) {
            drawable = tileifyIndeterminate(drawable);
            setIndeterminateDrawable(drawable);
        }

        mOnlyIndeterminate = a.getBoolean(
                R.styleable.ProgressBar_android_indeterminateOnly, mOnlyIndeterminate);

        mNoInvalidate = false;

        setIndeterminate(mOnlyIndeterminate || a.getBoolean(R.styleable.ProgressBar_android_indeterminate, mIndeterminate));

        //mMirrorForRtl = a.getBoolean(getInternalID("styleable", "ProgressBar_mirrorForRtl"), mMirrorForRtl);

        a.recycle();
    }

    protected int getMaxHeight(){return mMaxHeight;}
    protected int getMinHeight(){return mMinHeight;}
    protected int getMaxWidth(){return mMaxWidth;}
    protected int getMinWidth(){return mMinWidth;}

    protected Rect getBiasedPadding(){
        return new Rect(
                getPaddingBottom(),
                getPaddingLeft(),
                getPaddingTop(),
                getPaddingRight()
        );
    }

    /**
     * Converts a drawable to a tiled version of itself. It will recursively
     * traverse layer and state list drawables.
     */
    private Drawable tileify(Drawable drawable, boolean clip) {

        if (drawable instanceof LayerDrawable ) {
            LayerDrawable background = (LayerDrawable) drawable;
            final int N = background.getNumberOfLayers();
            Drawable[] outDrawables = new Drawable[N];

            Log.d("tile","Layer > " + N);
            for (int i = 0; i < N; i++) {
                int id = background.getId(i);
                outDrawables[i] = tileify(background.getDrawable(i),
                        (id == mProgressID || id == mSecondaryProgressID));
            }

            LayerDrawable newBg = new LayerDrawable(outDrawables);

            for (int i = 0; i < N; i++) {
                newBg.setId(i, background.getId(i));
            }

            return newBg;

        } else if (drawable instanceof StateListDrawable ) {
            /*
            StateListDrawable in = (StateListDrawable) drawable;
            StateListDrawable out = new StateListDrawable();
            int numStates = in.getStateCount();
            for (int i = 0; i < numStates; i++) {
                out.addState(in.getStateSet(i), tileify(in.getStateDrawable(i), clip));
            }
            return out;*/
            Log.d("tile","StateList");
            return (clip) ? new ClipDrawable(drawable, Gravity.LEFT, ClipDrawable.HORIZONTAL) : drawable;

        } else if (drawable instanceof BitmapDrawable ) {
            Log.d("tile","Bitmap");
            final Bitmap tileBitmap = ((BitmapDrawable) drawable).getBitmap();
            if (mSampleTile == null) {
                mSampleTile = tileBitmap;
            }

            final ShapeDrawable shapeDrawable = new ShapeDrawable(getDrawableShape());

            final BitmapShader bitmapShader = new BitmapShader(tileBitmap,
                    Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            shapeDrawable.getPaint().setShader(bitmapShader);

            return (clip) ? new ClipDrawable(shapeDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL) : shapeDrawable;
        }

        Log.d("tile","other > " + drawable.getClass().toString());
        return drawable;
    }

    Shape getDrawableShape() {
        final float[] roundedCorners = new float[] { 5, 5, 5, 5, 5, 5, 5, 5 };
        return new RoundRectShape(roundedCorners, null, null);
    }

    /**
     * Convert a AnimationDrawable for use as a barberpole animation.
     * Each frame of the animation is wrapped in a ClipDrawable and
     * given a tiling BitmapShader.
     */
    private Drawable tileifyIndeterminate(Drawable drawable) {
        if (drawable instanceof AnimationDrawable ) {
            AnimationDrawable background = (AnimationDrawable) drawable;
            final int N = background.getNumberOfFrames();
            AnimationDrawable newBg = new AnimationDrawable();
            newBg.setOneShot(background.isOneShot());

            for (int i = 0; i < N; i++) {
                Drawable frame = tileify(background.getFrame(i), true);
                frame.setLevel(10000);
                newBg.addFrame(frame, background.getDuration(i));
            }
            newBg.setLevel(10000);
            drawable = newBg;
        }
        return drawable;
    }

    /**
     * <p>
     * Initialize the progress bar's default values:
     * </p>
     * <ul>
     * <li>progress = 0</li>
     * <li>max = 100</li>
     * <li>animation duration = 4000 ms</li>
     * <li>indeterminate = false</li>
     * <li>behavior = repeat</li>
     * </ul>
     */
    private void initProgressBar() {
        mMax = 100;
        mProgress = 0;
        mSecondaryProgress = 0;
        mIndeterminate = false;
        mOnlyIndeterminate = false;
        mDuration = 4000;
        mBehavior = AlphaAnimation.RESTART;
        mMinWidth = 24;
        mMaxWidth = 48;
        mMinHeight = 24;
        mMaxHeight = 48;
    }

    /**
     * <p>Indicate whether this progress bar is in indeterminate mode.</p>
     *
     * @return true if the progress bar is in indeterminate mode
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized boolean isIndeterminate() {
        return mIndeterminate;
    }

    /**
     * <p>Change the indeterminate mode for this progress bar. In indeterminate
     * mode, the progress is ignored and the progress bar shows an infinite
     * animation instead.</p>
     *
     * If this progress bar's style only supports indeterminate mode (such as the circular
     * progress bars), then this will be ignored.
     *
     * @param indeterminate true to enable the indeterminate mode
     */
    public synchronized void setIndeterminate(boolean indeterminate) {
        if ((!mOnlyIndeterminate || !mIndeterminate) && indeterminate != mIndeterminate) {
            mIndeterminate = indeterminate;

            if (indeterminate) {
                // swap between indeterminate and regular backgrounds
                mCurrentDrawable = mIndeterminateDrawable;
                startAnimation();
            } else {
                mCurrentDrawable = mProgressDrawable;
                stopAnimation();
            }
        }
    }

    /**
     * <p>Get the drawable used to draw the progress bar in
     * indeterminate mode.</p>
     *
     * @return a {@link android.graphics.drawable.Drawable} instance
     *
     * @see #setIndeterminateDrawable(android.graphics.drawable.Drawable)
     * @see #setIndeterminate(boolean)
     */
    public Drawable getIndeterminateDrawable() {
        return mIndeterminateDrawable;
    }

    /**
     * <p>Define the drawable used to draw the progress bar in
     * indeterminate mode.</p>
     *
     * @param d the new drawable
     *
     * @see #getIndeterminateDrawable()
     * @see #setIndeterminate(boolean)
     */
    public void setIndeterminateDrawable(Drawable d) {
        if (d != null) {
            d.setCallback(this);
        }
        mIndeterminateDrawable = d;
        if (mIndeterminate) {
            mCurrentDrawable = d;
            postInvalidate();
        }
    }

    /**
     * <p>Get the drawable used to draw the progress bar in
     * progress mode.</p>
     *
     * @return a {@link android.graphics.drawable.Drawable} instance
     *
     * @see #setProgressDrawable(android.graphics.drawable.Drawable)
     * @see #setIndeterminate(boolean)
     */
    public Drawable getProgressDrawable() {
        return mProgressDrawable;
    }

    /**
     * <p>Define the drawable used to draw the progress bar in
     * progress mode.</p>
     *
     * @param d the new drawable
     *
     * @see #getProgressDrawable()
     * @see #setIndeterminate(boolean)
     */
    public void setProgressDrawable(Drawable d) {
        boolean needUpdate;
        if (mProgressDrawable != null && d != mProgressDrawable) {
            mProgressDrawable.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = false;
        }

        if (d != null) {
            d.setCallback(this);

            // Make sure the ProgressBar is always tall enough
            int drawableHeight = d.getMinimumHeight();
            if (mMaxWidth < drawableHeight) {
                mMaxWidth = drawableHeight;
                requestLayout();
            }
        }
        mProgressDrawable = d;
        if (!mIndeterminate) {
            mCurrentDrawable = d;
            postInvalidate();
        }

        if (needUpdate) {
            updateDrawableBounds(getHeight(), getWidth());
            updateDrawableState();
            doRefreshProgress(mProgressID, mProgress, false, false);
            doRefreshProgress(mSecondaryProgressID, mSecondaryProgress, false, false);
        }
    }

    /**
     * @return The drawable currently used to draw the progress bar
     */
    protected Drawable getCurrentProgressDrawable() {
        return mCurrentDrawable;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mProgressDrawable || who == mIndeterminateDrawable
                || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mProgressDrawable != null) mProgressDrawable.jumpToCurrentState();
        if (mIndeterminateDrawable != null) mIndeterminateDrawable.jumpToCurrentState();
    }

    @Override
    public void postInvalidate() {
        if (!mNoInvalidate) {
            super.postInvalidate();
        }
    }

    private class RefreshProgressRunnable implements Runnable {
        public void run() {
            synchronized (VerticalProgressBar2.this) {
                final int count = mRefreshData.size();
                for (int i = 0; i < count; i++) {
                    final RefreshData rd = mRefreshData.get(i);
                    doRefreshProgress(rd.id, rd.progress, rd.fromUser, true);
                    rd.recycle();
                }
                mRefreshData.clear();
                mRefreshIsPosted = false;
            }
        }
    }

    private static class RefreshData {
        private static final int POOL_MAX = 24;
        private static final Pools.SynchronizedPool<RefreshData> sPool =
                new Pools.SynchronizedPool<RefreshData>(POOL_MAX);

        public int id;
        public int progress;
        public boolean fromUser;

        public static RefreshData obtain(int id, int progress, boolean fromUser) {
            RefreshData rd = sPool.acquire();
            if (rd == null) {
                rd = new RefreshData();
            }
            rd.id = id;
            rd.progress = progress;
            rd.fromUser = fromUser;
            return rd;
        }

        public void recycle() {
            sPool.release(this);
        }
    }

    private synchronized void doRefreshProgress(int id, int progress, boolean fromUser,
                                                boolean callBackToApp) {
        float scale = mMax > 0 ? (float) progress / (float) mMax : 0;
        final Drawable d = mCurrentDrawable;
        if (d != null) {
            Drawable progressDrawable = null;

            if (d instanceof LayerDrawable) {
                progressDrawable = ((LayerDrawable) d).findDrawableByLayerId(id);
            }

            final int level = (int) (scale * MAX_LEVEL);
            (progressDrawable != null ? progressDrawable : d).setLevel(level);
        } else {
            invalidate();
        }

        if (callBackToApp && id == mProgressID ) {
            onProgressUpdate(scale, fromUser, progress);
        }
    }

    protected void onProgressUpdate(float scale, boolean fromUser, int progress) {

    }

    private synchronized void refreshProgress(int id, int progress, boolean fromUser) {
        if (mUiThreadId == Thread.currentThread().getId()) {
            doRefreshProgress(id, progress, fromUser, true);
        } else {
            if (mRefreshProgressRunnable == null) {
                mRefreshProgressRunnable = new RefreshProgressRunnable();
            }

            final RefreshData rd = RefreshData.obtain(id, progress, fromUser);
            mRefreshData.add(rd);
            if (mAttached && !mRefreshIsPosted) {
                post(mRefreshProgressRunnable);
                mRefreshIsPosted = true;
            }
        }
    }

    /**
     * <p>Set the current progress to the specified value. Does not do anything
     * if the progress bar is in indeterminate mode.</p>
     *
     * @param progress the new progress, between 0 and {@link #getMax()}
     *
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #getProgress()
     * @see #incrementProgressBy(int)
     */
    public synchronized void setProgress(int progress) {
        setProgress(progress, false);
    }

    /**
     * Set the current progress to the specified value, which has changed by the user's touch.
     * @param progress the new progress level, between 0 and {@link #getMax()}
     */
    protected final void onUserUpdateProgress(int progress){
        setProgress(progress,true);
    }

    private synchronized void setProgress(int progress, boolean fromUser) {
        if (mIndeterminate) {
            return;
        }

        if (progress < 0) {
            progress = 0;
        }

        if (progress > mMax) {
            progress = mMax;
        }

        if (progress != mProgress) {
            mProgress = progress;
            refreshProgress(mProgressID, mProgress, fromUser);
        }
    }

    /**
     * <p>
     * Set the current secondary progress to the specified value. Does not do
     * anything if the progress bar is in indeterminate mode.
     * </p>
     *
     * @param secondaryProgress the new secondary progress, between 0 and {@link #getMax()}
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #getSecondaryProgress()
     * @see #incrementSecondaryProgressBy(int)
     */
    public synchronized void setSecondaryProgress(int secondaryProgress) {
        if (mIndeterminate) {
            return;
        }

        if (secondaryProgress < 0) {
            secondaryProgress = 0;
        }

        if (secondaryProgress > mMax) {
            secondaryProgress = mMax;
        }

        if (secondaryProgress != mSecondaryProgress) {
            mSecondaryProgress = secondaryProgress;
            refreshProgress(mSecondaryProgressID, mSecondaryProgress, false);
        }
    }

    /**
     * <p>Get the progress bar's current level of progress. Return 0 when the
     * progress bar is in indeterminate mode.</p>
     *
     * @return the current progress, between 0 and {@link #getMax()}
     *
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #setProgress(int)
     * @see #setMax(int)
     * @see #getMax()
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized int getProgress() {
        return mIndeterminate ? 0 : mProgress;
    }

    /**
     * <p>Get the progress bar's current level of secondary progress. Return 0 when the
     * progress bar is in indeterminate mode.</p>
     *
     * @return the current secondary progress, between 0 and {@link #getMax()}
     *
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #setSecondaryProgress(int)
     * @see #setMax(int)
     * @see #getMax()
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized int getSecondaryProgress() {
        return mIndeterminate ? 0 : mSecondaryProgress;
    }

    /**
     * <p>Return the upper limit of this progress bar's range.</p>
     *
     * @return a positive integer
     *
     * @see #setMax(int)
     * @see #getProgress()
     * @see #getSecondaryProgress()
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized int getMax() {
        return mMax;
    }

    /**
     * <p>Set the range of the progress bar to 0...<tt>max</tt>.</p>
     *
     * @param max the upper range of this progress bar
     *
     * @see #getMax()
     * @see #setProgress(int)
     * @see #setSecondaryProgress(int)
     */
    public synchronized void setMax(int max) {
        if (max < 0) {
            max = 0;
        }
        if (max != mMax) {
            mMax = max;
            postInvalidate();

            if (mProgress > max) {
                mProgress = max;
            }
            refreshProgress(mProgressID, mProgress, false);
        }
    }

    /**
     * <p>Increase the progress bar's progress by the specified amount.</p>
     *
     * @param diff the amount by which the progress must be increased
     *
     * @see #setProgress(int)
     */
    public synchronized final void incrementProgressBy(int diff) {
        setProgress(mProgress + diff);
    }

    /**
     * <p>Increase the progress bar's secondary progress by the specified amount.</p>
     *
     * @param diff the amount by which the secondary progress must be increased
     *
     * @see #setSecondaryProgress(int)
     */
    public synchronized final void incrementSecondaryProgressBy(int diff) {
        setSecondaryProgress(mSecondaryProgress + diff);
    }

    /**
     * <p>Start the indeterminate progress animation.</p>
     */
    protected void startAnimation() {
        if (getVisibility() != VISIBLE) {
            return;
        }

        if (mIndeterminateDrawable instanceof Animatable ) {
            mShouldStartAnimationDrawable = true;
            mHasAnimation = false;
        } else {
            mHasAnimation = true;

            if (mInterpolator == null) {
                mInterpolator = new LinearInterpolator();
            }

            if (mTransformation == null) {
                mTransformation = new Transformation();
            } else {
                mTransformation.clear();
            }

            if (mAnimation == null) {
                mAnimation = new AlphaAnimation(0.0f, 1.0f);
            } else {
                mAnimation.reset();
            }

            mAnimation.setRepeatMode(mBehavior);
            mAnimation.setRepeatCount(Animation.INFINITE);
            mAnimation.setDuration(mDuration);
            mAnimation.setInterpolator(mInterpolator);
            mAnimation.setStartTime(Animation.START_ON_FIRST_FRAME);
        }
        postInvalidate();
    }

    /**
     * <p>Stop the indeterminate progress animation.</p>
     */
    protected void stopAnimation() {
        mHasAnimation = false;
        if (mIndeterminateDrawable instanceof Animatable) {
            ((Animatable) mIndeterminateDrawable).stop();
            mShouldStartAnimationDrawable = false;
        }
        postInvalidate();
    }

    /**
     * Sets the acceleration curve for the indeterminate animation.
     * The interpolator is loaded as a resource from the specified context.
     *
     * @param context The application environment
     * @param resID The resource identifier of the interpolator to load
     */
    public void setInterpolator(Context context, int resID) {
        setInterpolator(AnimationUtils.loadInterpolator(context, resID));
    }

    /**
     * Sets the acceleration curve for the indeterminate animation.
     * Defaults to a linear interpolation.
     *
     * @param interpolator The interpolator which defines the acceleration curve
     */
    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    /**
     * Gets the acceleration curve type for the indeterminate animation.
     *
     * @return the {@link Interpolator} associated to this animation
     */
    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    @Override
    public void setVisibility(int v) {
        if (getVisibility() != v) {
            super.setVisibility(v);

            if (mIndeterminate) {
                // let's be nice with the UI thread
                if (v == GONE || v == INVISIBLE) {
                    stopAnimation();
                } else {
                    startAnimation();
                }
            }
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (mIndeterminate) {
            // let's be nice with the UI thread
            if (visibility == GONE || visibility == INVISIBLE) {
                stopAnimation();
            } else {
                startAnimation();
            }
        }
    }

    @Override
    public void invalidateDrawable(Drawable dr) {
        if (!mInDrawing) {
            if (verifyDrawable(dr)) {
                final Rect dirty = dr.getBounds();
                final int scrollX = getScrollX() + getPaddingLeft();
                final int scrollY = getScrollY() + getPaddingTop();

                invalidate(dirty.left + scrollX, dirty.top + scrollY,
                        dirty.right + scrollX, dirty.bottom + scrollY);
            } else {
                super.invalidateDrawable(dr);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateDrawableBounds(h, w);
    }

    private void updateDrawableBounds(int w, int h) {
        // onDraw will translate the canvas so we draw starting at 0,0.
        // Subtract out padding for the purposes of the calculations below.
        Rect padding = getBiasedPadding();
        int right = w - padding.right - padding.left;
        int bottom = h - padding.top - padding.bottom;
        int top = 0;
        int left = 0;

        if (mIndeterminateDrawable != null) {
            // Aspect ratio logic does not apply to AnimationDrawables
            if (mOnlyIndeterminate && !(mIndeterminateDrawable instanceof AnimationDrawable)) {
                // Maintain aspect ratio. Certain kinds of animated drawables
                // get very confused otherwise.
                final int intrinsicWidth = mIndeterminateDrawable.getIntrinsicWidth();
                final int intrinsicHeight = mIndeterminateDrawable.getIntrinsicHeight();
                final float intrinsicAspect = (float) intrinsicWidth / intrinsicHeight;
                final float boundAspect = (float) w / h;
                if (intrinsicAspect != boundAspect) {
                    if (boundAspect > intrinsicAspect) {
                        // New width is larger. Make it smaller to match height.
                        final int width = (int) (h * intrinsicAspect);
                        left = (w - width) / 2;
                        right = left + width;
                    } else {
                        // New height is larger. Make it smaller to match width.
                        final int height = (int) (w * (1 / intrinsicAspect));
                        top = (h - height) / 2;
                        bottom = top + height;
                    }
                }
            }
            mIndeterminateDrawable.setBounds(left, top, right, bottom);
        }

        if (mProgressDrawable != null) {
            mProgressDrawable.setBounds(0, 0, right, bottom);
        }
    }

    @Override
    protected final synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int saveCnt = canvas.save();
        final int h = getHeight();
        canvas.rotate(-90);
        canvas.translate(-h, 0);
        onDrawRotated(canvas);
        canvas.restoreToCount(saveCnt);
    }

    /**
     * {@link #onDraw(Canvas)}から座標系を回転した状態でコールされる.
     * 描写処理はこちらにどうぞ
     * @param canvas ９０度回転させたうえで原点を調整した状態
     */
    @CallSuper
    protected void onDrawRotated(Canvas canvas){
        Drawable d = mCurrentDrawable;
        if (d != null) {
            // Translate canvas so a indeterminate circular progress bar with padding
            // rotates properly in its animation
            Rect padding = getBiasedPadding();
            canvas.translate(padding.left, padding.top);
            long time = getDrawingTime();
            if (mHasAnimation) {
                mAnimation.getTransformation(time, mTransformation);
                float scale = mTransformation.getAlpha();
                try {
                    mInDrawing = true;
                    d.setLevel((int) (scale * MAX_LEVEL));
                } finally {
                    mInDrawing = false;
                }
                requestInvalidateAnimation();
            }
            d.draw(canvas);
            if (mShouldStartAnimationDrawable && d instanceof Animatable) {
                ((Animatable) d).start();
                mShouldStartAnimationDrawable = false;
            }
        }
    }

    @TargetApi(16)
    private void requestInvalidateAnimation(){
        postInvalidateOnAnimation();
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = mCurrentDrawable;

        //w,h はView全体から見たwidth,height
        int dw = 0;
        int dh = 0;
        if (d != null) {
            //縦横注意
            dh = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicWidth()));
            dw = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicHeight()));
        }
        updateDrawableState();
        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0));
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }

    private void updateDrawableState() {
        int[] state = getDrawableState();

        if (mProgressDrawable != null && mProgressDrawable.isStateful()) {
            mProgressDrawable.setState(state);
        }

        if (mIndeterminateDrawable != null && mIndeterminateDrawable.isStateful()) {
            mIndeterminateDrawable.setState(state);
        }
    }

    static class SavedState extends BaseSavedState {
        int progress;
        int secondaryProgress;

        /**
         * Constructor called from {@link ProgressBar#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            progress = in.readInt();
            secondaryProgress = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
            out.writeInt(secondaryProgress);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // Force our ancestor class to save its state
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.progress = mProgress;
        ss.secondaryProgress = mSecondaryProgress;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setProgress(ss.progress);
        setSecondaryProgress(ss.secondaryProgress);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mIndeterminate) {
            startAnimation();
        }
        if (mRefreshData != null) {
            synchronized (this) {
                final int count = mRefreshData.size();
                for (int i = 0; i < count; i++) {
                    final RefreshData rd = mRefreshData.get(i);
                    doRefreshProgress(rd.id, rd.progress, rd.fromUser, true);
                    rd.recycle();
                }
                mRefreshData.clear();
            }
        }
        mAttached = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mIndeterminate) {
            stopAnimation();
        }
        if (mRefreshProgressRunnable != null) {
            removeCallbacks(mRefreshProgressRunnable);
        }
        if (mRefreshProgressRunnable != null && mRefreshIsPosted) {
            removeCallbacks(mRefreshProgressRunnable);
        }
        if (mAccessibilityEventSender != null) {
            removeCallbacks(mAccessibilityEventSender);
        }
        // This should come after stopAnimation(), otherwise an invalidate message remains in the
        // queue, which can prevent the entire view hierarchy from being GC'ed during a rotation
        super.onDetachedFromWindow();
        mAttached = false;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(ProgressBar.class.getName());
        event.setItemCount(mMax);
        event.setCurrentItemIndex(mProgress);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(ProgressBar.class.getName());
    }

    /**
     * Schedule a command for sending an accessibility event.
     * </br>
     * Note: A command is used to ensure that accessibility events
     *       are sent at most one in a given time frame to save
     *       system resources while the progress changes quickly.
     */
    private void scheduleAccessibilityEventSender() {
        if (mAccessibilityEventSender == null) {
            mAccessibilityEventSender = new AccessibilityEventSender();
        } else {
            removeCallbacks(mAccessibilityEventSender);
        }
        postDelayed(mAccessibilityEventSender, TIMEOUT_SEND_ACCESSIBILITY_EVENT);
    }

    /**
     * Command for sending an accessibility event.
     */
    private class AccessibilityEventSender implements Runnable {
        public void run() {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        }
    }

}
