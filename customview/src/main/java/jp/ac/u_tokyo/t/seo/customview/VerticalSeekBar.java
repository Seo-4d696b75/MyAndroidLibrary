package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * @author Seo-4d696b75
 * @version 2017/11/19.
 */

public class VerticalSeekBar extends AbsVerticalSeekBar{

    /**
     * A callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    public interface OnSeekBarChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         *
         * @param seekBar The SeekBar whose progress has changed
         * @param progress The current progress level. This will be in the range 0..max where max
         *        was set by {@link VerticalProgressBar#setMax(int)}. (The default value for max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean fromUser);

        /**
         * Notification that the user has started a touch gesture. Clients may want to use this
         * to disable advancing the seekbar.
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStartTrackingTouch(VerticalSeekBar seekBar);

        /**
         * Notification that the user has finished a touch gesture. Clients may want to use this
         * to re-enable advancing the seekbar.
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStopTrackingTouch(VerticalSeekBar seekBar);
    }

    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    public VerticalSeekBar(Context context) {
        this(context, null);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, Resources.getSystem().getIdentifier("seekBarStyle","attr","android"));
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onProgressUpdate(float scale, boolean fromUser, int progress){
        super.onProgressUpdate(scale, fromUser, progress);
        if ( mOnSeekBarChangeListener != null ){
            mOnSeekBarChangeListener.onProgressChanged(this,progress,fromUser);
        }
    }

    /**
     * Sets a listener to receive notifications of changes to the SeekBar's progress level. Also
     * provides notifications of when the user starts and stops a touch gesture within the SeekBar.
     *
     * @param l The seek bar notification listener
     *
     * @see OnSeekBarChangeListener
     */
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }

    @Override
    protected void onStartTrackingTouch() {
        super.onStartTrackingTouch();
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    @Override
    protected void onStopTrackingTouch() {
        super.onStopTrackingTouch();
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return SeekBar.class.getName();
    }


}
