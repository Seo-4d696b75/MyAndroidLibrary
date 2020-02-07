package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsSeekBar;


/**
 * refer to API-19 {@link android.widget.AbsSeekBar}
 * @author Seo-4d696b75
 * @version 2017/11/18.
 */

public abstract class AbsVerticalSeekBar extends VerticalProgressBar{

    private Drawable mThumb;
    private int mThumbOffset;

    /**
     * On touch, this offset plus the scaled value from the position of the
     * touch will form the progress value. Usually 0.
     */
    float mTouchProgressOffset;

    /**
     * Whether this is user seekable.
     */
    boolean mIsUserSeekable = true;

    /**
     * On key presses (right or left), the amount to increment/decrement the
     * progress.
     */
    private int mKeyProgressIncrement = 1;

    private static final int NO_ALPHA = 0xFF;
    private float mDisabledAlpha;

    private int mScaledTouchSlop;
    private float mTouchDownY;
    private boolean mIsDragging;

    /**
     * com.android.internal.R以下のリソースＩＤを取得する
     * @param type com.android.internal.R.type.name
     * @param name com.android.internal.R.type.name
     * @return id
     */
    private static int getInternalID(String type, String name){
        return Resources.getSystem().getIdentifier(name,type,"android");
    }

    public AbsVerticalSeekBar(Context context){
        super(context);
    }

    public AbsVerticalSeekBar(Context context, AttributeSet attrs){
        super(context,attrs);
    }

    public AbsVerticalSeekBar(Context context, AttributeSet attrs, int defStyle){
        super(context,attrs,defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.SeekBar, defStyle, 0);
        Drawable thumb = a.getDrawable(R.styleable.SeekBar_android_thumb);
        setThumb(thumb); // will guess mThumbOffset if thumb != null...
        // ...but allow layout to override this
        int thumbOffset = a.getDimensionPixelOffset(R.styleable.SeekBar_android_thumbOffset, getThumbOffset());
        setThumbOffset(thumbOffset);
        a.recycle();

        mDisabledAlpha = 0.5f;

        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }


    /**
     * Sets the thumb that will be drawn at the end of the progress meter within the SeekBar.
     * <p>
     * If the thumb is a valid drawable (i.e. not null), half its width will be
     * used as the new thumb offset (@see #setThumbOffset(int)).
     *
     * @param thumb Drawable representing the thumb
     */
    public void setThumb(Drawable thumb) {
        boolean needUpdate;
        // This way, calling setThumb again with the same bitmap will result in
        // it recalcuating mThumbOffset (if for example it the bounds of the
        // drawable changed)
        if (mThumb != null && thumb != mThumb) {
            mThumb.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = false;
        }
        if (thumb != null) {
            thumb.setCallback(this);
            /*
            for simplicity, not care for whether LAYOUT_DIRECTION_LTR or LAYOUT_DIRECTION_RTL
            if (canResolveLayoutDirection()) {
                thumb.setLayoutDirection(getLayoutDirection());
            }*/

            // Assuming the thumb drawable is symmetric, set the thumb offset
            // such that the thumb will hang halfway off either edge of the
            // progress bar.
            mThumbOffset = thumb.getIntrinsicWidth() / 2;

            // If we're updating get the new states
            if (needUpdate &&
                    (thumb.getIntrinsicWidth() != mThumb.getIntrinsicWidth()
                            || thumb.getIntrinsicHeight() != mThumb.getIntrinsicHeight())) {
                requestLayout();
            }
        }
        mThumb = thumb;
        invalidate();
        if (needUpdate) {
            updateThumbPos(getHeight(),getWidth());
            if (thumb != null && thumb.isStateful()) {
                // Note that if the states are different this won't work.
                // For now, let's consider that an app bug.
                int[] state = getDrawableState();
                thumb.setState(state);
            }
        }
    }

    /**
     * Return the drawable used to represent the scroll thumb - the component that
     * the user can drag back and forth indicating the current value by its position.
     *
     * @return The current thumb drawable
     */
    public Drawable getThumb() {
        return mThumb;
    }

    /**
     * @see #setThumbOffset(int)
     */
    public int getThumbOffset() {
        return mThumbOffset;
    }

    /**
     * Sets the thumb offset that allows the thumb to extend out of the range of
     * the track.
     *
     * @param thumbOffset The offset amount in pixels.
     */
    public void setThumbOffset(int thumbOffset) {
        mThumbOffset = thumbOffset;
        invalidate();
    }

    /**
     * Sets the amount of progress changed via the arrow keys.
     *
     * @param increment The amount to increment or decrement when the user
     *            presses the arrow keys.
     */
    public void setKeyProgressIncrement(int increment) {
        mKeyProgressIncrement = increment < 0 ? -increment : increment;
    }

    /**
     * Returns the amount of progress changed via the arrow keys.
     * <p>
     * By default, this will be a value that is derived from the max progress.
     *
     * @return The amount to increment or decrement when the user presses the
     *         arrow keys. This will be positive.
     */
    public int getKeyProgressIncrement() {
        return mKeyProgressIncrement;
    }

    @Override
    public synchronized void setMax(int max) {
        super.setMax(max);

        if ((mKeyProgressIncrement == 0) || (getMax() / mKeyProgressIncrement > 20)) {
            // It will take the user too long to change this via keys, change it
            // to something more reasonable
            setKeyProgressIncrement(Math.max(1, Math.round((float) getMax() / 20)));
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mThumb || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mThumb != null) mThumb.jumpToCurrentState();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null) {
            if ( isEnabled() ){
                progressDrawable.setAlpha(NO_ALPHA);
            }else{
                int alpha = (int) (NO_ALPHA * mDisabledAlpha);
                progressDrawable.setAlpha( alpha );
            }
        }

        if (mThumb != null && mThumb.isStateful()) {
            int[] state = getDrawableState();
            mThumb.setState(state);
        }
    }

    /*
    @Override
    void onProgressRefresh(float scale, boolean fromUser) {
    }*/
    @Override
    @CallSuper
    protected void onProgressUpdate(float scale, boolean fromUser, int progress){
        Drawable thumb = mThumb;
        if (thumb != null) {
            setThumbPos(thumb, scale, Integer.MIN_VALUE);
            /*
             * Since we draw translated, the drawable's bounds that it signals
             * for invalidation won't be the actual bounds we want invalidated,
             * so just invalidate this whole view.
             */
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateThumbPos(h, w);
    }

    /**
     * Thumb画像の描写位置を計算している
     * 縦横逆転させるため、個々の処理を大きく変更した
     * @param w 回転した座標系における幅 viewのheight
     * @param h 回転した座標系における高さ viewのwidth
     */
    private void updateThumbPos(int w, int h) {
        final Rect padding = getBiasedPadding();
        final int paddedHeight = h - padding.top - padding.bottom;
        final Drawable track = getCurrentProgressDrawable();
        final Drawable thumb = mThumb;

        // The max height does not incorporate padding, whereas the height
        // parameter does.
        final int trackHeight = Math.min(getMaxWidth(), paddedHeight);
        final int thumbHeight = thumb == null ? 0 : thumb.getIntrinsicHeight();

        // Apply offset to whichever item is taller.
        final int trackOffset;
        final int thumbOffset;
        if (thumbHeight > trackHeight) {
            final int offsetHeight = (paddedHeight - thumbHeight) / 2;
            trackOffset = offsetHeight + (thumbHeight - trackHeight) / 2;
            thumbOffset = offsetHeight;
        } else {
            final int offsetHeight = (paddedHeight - trackHeight) / 2;
            trackOffset = offsetHeight;
            thumbOffset = offsetHeight + (trackHeight - thumbHeight) / 2;
        }

        if (track != null) {
            final int trackWidth = w - padding.right - padding.left;
            track.setBounds(0, trackOffset, trackWidth, trackOffset + trackHeight);
        }

        if (thumb != null) {
            int max = getMax();
            float scale = max > 0 ? (float) getProgress() / (float) max : 0;
            setThumbPos(thumb, scale, thumbOffset);
        }
    }

    /**
     * @param gap If set to {@link Integer#MIN_VALUE}, this will be ignored and
     */
    private void setThumbPos(Drawable thumb, float scale, int gap) {
        int thumbWidth = thumb.getIntrinsicWidth();
        int thumbHeight = thumb.getIntrinsicHeight();
        Rect padding = getBiasedPadding();
        int available = getHeight() - padding.right - padding.left;
        available -= thumbWidth;

        // The extra space for the thumb to move on the track
        available += mThumbOffset * 2;

        int left = (int) (scale * available);

        int topBound, bottomBound;
        if (gap == Integer.MIN_VALUE) {
            Rect oldBounds = thumb.getBounds();
            topBound = oldBounds.top;
            bottomBound = oldBounds.bottom;
        } else {
            topBound = gap;
            bottomBound = gap + thumbHeight;
        }

        // Canvas will be translated, so 0,0 is where we start drawing
        thumb.setBounds(left, topBound, left + thumbWidth, bottomBound);
    }

    @Override
    protected void onDrawRotated(Canvas canvas){
        super.onDrawRotated(canvas);
        if (mThumb != null) {
            canvas.save();
            // Translate the padding. For the x, we need to allow the thumb to
            // draw in its extra space
            Rect padding = getBiasedPadding();
            canvas.translate( padding.left - mThumbOffset, padding.top);
            mThumb.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getCurrentProgressDrawable();

        int thumbHeight = mThumb == null ? 0 : mThumb.getIntrinsicHeight();
        int dw = 0;
        int dh = 0;
        if (d != null) {
            //縦横注意
            dh = Math.max(getMinWidth(), Math.min(getMaxWidth(), d.getIntrinsicWidth()));
            dw = Math.max(getMinHeight(), Math.min(getMaxHeight(), d.getIntrinsicHeight()));
            dw = Math.max(thumbHeight, dw);
        }
        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0));
    }

    public boolean isInScrollingContainer() {
        ViewParent p = getParent();
        while (p != null && p instanceof ViewGroup ) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsUserSeekable || !isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if ( isInScrollingContainer() ){
                    mTouchDownY = event.getY();
                }else{
                    setPressed(true);
                    if (mThumb != null) {
                        invalidate(mThumb.getBounds()); // This may be within the padding region
                    }
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    attemptClaimDrag();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    final float y = event.getY();
                    if (Math.abs(y - mTouchDownY) > mScaledTouchSlop) {
                        setPressed(true);
                        if (mThumb != null) {
                            invalidate(mThumb.getBounds()); // This may be within the padding region
                        }
                        onStartTrackingTouch();
                        trackTouchEvent(event);
                        attemptClaimDrag();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold should
                    // be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private void trackTouchEvent(MotionEvent event) {
        final int width = getHeight();
        final Rect padding = getBiasedPadding();
        final int available = width - padding.right - padding.left;
        int x = width - (int)event.getY();
        float scale;
        float progress = 0;
        if ( x < padding.left ){
            scale = 0.0f;
        }else if ( x > width - padding.right ){
            scale = 1.0f;
        }else{
            scale = (float)(x - padding.left) / (float)available;
            progress = mTouchProgressOffset;
        }
        final int max = getMax();
        progress += scale * max;

        onUserUpdateProgress((int) progress);
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * This is called when the user has started touching this widget.
     */
    protected void onStartTrackingTouch() {
        mIsDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is
     * canceled.
     */
    protected void onStopTrackingTouch() {
        mIsDragging = false;
    }

    /**
     * Called when the user changes the seekbar's progress by using a key event.
     */
    protected void onKeyChange() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled()) {
            int progress = getProgress();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (progress <= 0) break;
                    onUserUpdateProgress(progress - mKeyProgressIncrement);
                    onKeyChange();
                    return true;

                case KeyEvent.KEYCODE_DPAD_UP:
                    if (progress >= getMax()) break;
                    onUserUpdateProgress(progress + mKeyProgressIncrement);
                    onKeyChange();
                    return true;
                default:
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(AbsSeekBar.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(AbsSeekBar.class.getName());

        if (isEnabled()) {
            final int progress = getProgress();
            if (progress > 0) {
                info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            }
            if (progress < getMax()) {
                info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            }
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (super.performAccessibilityAction(action, arguments)) {
            return true;
        }
        if (!isEnabled()) {
            return false;
        }
        final int progress = getProgress();
        final int increment = Math.max(1, Math.round((float) getMax() / 5));
        switch (action) {
            case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD: {
                if (progress <= 0) {
                    return false;
                }
                onUserUpdateProgress(progress - increment);
                onKeyChange();
                return true;
            }
            case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD: {
                if (progress >= getMax()) {
                    return false;
                }
                onUserUpdateProgress(progress + increment);
                onKeyChange();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        int max = getMax();
        float scale = max > 0 ? (float) getProgress() / (float) max : 0;

        Drawable thumb = mThumb;
        if (thumb != null) {
            setThumbPos(thumb, scale, Integer.MIN_VALUE);
            /*
             * Since we draw translated, the drawable's bounds that it signals
             * for invalidation won't be the actual bounds we want invalidated,
             * so just invalidate this whole view.
             */
            invalidate();
        }
    }

}
