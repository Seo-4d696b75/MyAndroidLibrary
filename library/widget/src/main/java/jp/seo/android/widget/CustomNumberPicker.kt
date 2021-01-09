package jp.seo.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.NumberPicker
import androidx.annotation.CallSuper
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

/**
 * @author Seo-4d696b75
 * @version 2021/01/09.
 */
class CustomNumberPicker : NumberPicker {

    constructor(context: Context) : this(context, null) {
    }

    constructor(context: Context, set: AttributeSet?) : this(context, set, 0) {
    }

    constructor(context: Context, set: AttributeSet?, defaultAttr: Int) : super(
        context,
        set,
        defaultAttr
    ) {

        val array = context.obtainStyledAttributes(set, R.styleable.CustomNumberPicker, 0, 0)
        min = array.getInteger(R.styleable.CustomNumberPicker_min, 0)
        max = array.getInteger(R.styleable.CustomNumberPicker_max, 100)
        step = array.getInteger(R.styleable.CustomNumberPicker_step, 10)
        speed = array.getFloat(R.styleable.CustomNumberPicker_scrollSpeed, 1f)
        shiftPoint = array.getInteger(R.styleable.CustomNumberPicker_speedShiftPoint, 1)
        shiftRate = array.getFloat(R.styleable.CustomNumberPicker_speedShiftRate, 1f)
        val value = array.getInteger(R.styleable.CustomNumberPicker_value, 0)
        array.recycle()
        setValues(false)
        setValue(value)

    }

    private var min: Int
    private var max: Int
    private var step: Int
    private var valueSet: Array<Int> = arrayOf()
    private var speed: Float = 1.0f
    private var lastYPos: Float = 1.0f
    private var _width: Int = 100
    private var shiftPoint: Int
    private var shiftRate: Float = 1.0f
    private var currentSpeed: Float = 0.0f

    private fun setValues(keepValue: Boolean) {
        var value = 0
        if (keepValue) {
            value = this.value
        }
        //最大最小の逆転を修正
        if (min > max) {
            val temp: Int = min
            min = max
            max = temp
        }
        //ステップ
        if (step <= 0 || step > max - min) {
            step = 1
        }
        //最大最小をステップに合わせる、つまり上限下限と同じ扱いにする
        if (max > 0) {
            max = max / step * step
        } else if (max < 0) {
            max = if (-max % step == 0) max else -(-max / step + 1) * step
        }
        if (min > 0) {
            min = if (min % step == 0) min else (min / step + 1) * step
        } else if (min < 0) {
            min = -(-min / step) * step
        }
        //空集合の場合は初期値に変更
        if (min > max) {
            min = 0
            max = if (step > 100) step else 100
        }
        val length: Int = (max - min) / step + 1
        valueSet = Array(length) {
            min + step * it
        }
        super.setDisplayedValues(null)
        // super#setDisplayedValues した配列の長さより大きな範囲になる場合、IndexOutOfBounds で死ぬので一度nullにしておく
        super.setMinValue(0)
        super.setMaxValue(length - 1)
        super.setDisplayedValues(getDisplayedValues(length))
        if (keepValue) {
            setValue(value)
        }
    }

    open fun getDisplayedValues(length: Int): Array<String> {
        return Array(length) {
            (min + step * it).toString()
        }
    }

    private fun getClosestIndex(request: Int): Int {
        val value = max(min(this.max, request), this.min)
        val v = if (value > 0) {
            if (value % step >= (step + 1) / 2) (value / step + 1) * step else value / step * step
        } else if (value < 0) {
            if (-value % step <= step / 2) -(-value / step) * step else -(-value / step + 1) * step
        } else {
            0
        }
        return (v - min) / step
    }

    override fun getValue(): Int {
        val i = super.getValue()
        return valueSet[i]
    }

    override fun setValue(value: Int) {
        val i = getClosestIndex(value)
        super.setValue(i)
    }

    override fun setMinValue(minValue: Int) {
        min = minValue
        setValues(true)
    }

    override fun getMinValue(): Int {
        return min
    }

    override fun setMaxValue(maxValue: Int) {
        max = maxValue
        setValues(true)
    }

    override fun getMaxValue(): Int {
        return max
    }

    var stepValue: Int
        get() = step
        set(value) {
            step = value
            setValues(true)
        }

    var scrollSpeed: Float
        get() = speed
        set(value) {
            if (abs(value) >= 0.1f) {
                speed = value
            }
        }

    var scrollSpeedShiftSteps: Int
        get() = shiftPoint
        set(shift) {
            if (shift > 0) {
                shiftPoint = shift
            }
        }

    var scrollSpeedShiftRate: Float
        get() = shiftRate
        set(rate) {
            if (rate > 1f) {
                shiftRate = rate
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    @CallSuper
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastYPos = event.y
                val section = shiftPoint - 1 - (event.x * shiftPoint / _width).toInt()
                val index = shiftRate.pow(section)
                currentSpeed = speed * index
            }
            MotionEvent.ACTION_MOVE -> {
                val currentMoveY = event.y
                val deltaMoveY = ((currentMoveY - lastYPos) * (currentSpeed - 1f)).toInt()
                super.scrollBy(0, deltaMoveY)
                lastYPos = currentMoveY
            }
            else -> {
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        _width = width
    }


    internal class SavedState : BaseSavedState {
        constructor(superState: Parcelable?) : super(superState) {}
        private constructor(source: Parcel) : super(source) {
            min = source.readInt()
            max = source.readInt()
            step = source.readInt()
            speed = source.readFloat()
            shiftPoint = source.readInt()
            shiftRate = source.readFloat()
            value = source.readInt()
        }

        var min = 0
        var max = 0
        var step = 0
        var speed = 0f
        var shiftPoint = 0
        var shiftRate = 0f
        var value = 0
        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(min)
            out.writeInt(max)
            out.writeInt(step)
            out.writeFloat(speed)
            out.writeInt(shiftPoint)
            out.writeFloat(shiftRate)
            out.writeInt(value)
        }

        override fun toString(): String {
            return String.format(
                Locale.US,
                "CustomNumberPicker.SavedState{%s value=%d, min=%d, max=%d, step=%d, speed=%f}",
                Integer.toHexString(System.identityHashCode(this)), value, min, max, step, speed
            )
        }

        companion object {
            val CREATOR: Parcelable.Creator<SavedState?> =
                object : Parcelable.Creator<SavedState?> {
                    override fun createFromParcel(source: Parcel): SavedState? {
                        return SavedState(source)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(size)
                    }
                }
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.min = minValue
        state.max = maxValue
        state.value = value
        state.step = stepValue
        state.speed = scrollSpeed
        state.shiftPoint = shiftPoint
        state.shiftRate = shiftRate
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        min = myState.min
        max = myState.max
        step = myState.step
        speed = myState.speed
        shiftPoint = myState.shiftPoint
        shiftRate = myState.shiftRate
        setValues(false)
        value = myState.value
        requestLayout()
    }


}
