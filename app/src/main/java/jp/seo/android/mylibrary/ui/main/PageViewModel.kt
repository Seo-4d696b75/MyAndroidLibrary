package jp.seo.android.mylibrary.ui.main

import android.content.Context
import androidx.lifecycle.*
import jp.seo.android.mylibrary.R

class PageViewModel(
    context: Context
) : ViewModel() {

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PageViewModel(context) as T
            }

        }
    }

    private val titleList: Array<String>
    private val textList: Array<String>

    init {
        val res = context.resources
        titleList = res.getStringArray(R.array.test_title)
        textList = res.getStringArray(R.array.test_text)
    }

    private val _index = MutableLiveData<Int>()

    val title: LiveData<String> = Transformations.map(_index) {
        "Hello world!\nTest: ${titleList[it]}"
    }

    fun setIndex(index: Int) {
        _index.value = index
    }

    private var _valueIndex = 0

    fun updateValue() {
        _valueIndex++
        val v = textList[_valueIndex % textList.size]
        _text.value = v
        updateMessage("value changed: $v")
    }

    private val _text = MutableLiveData<String>(textList[0])
    val text: LiveData<String> = _text

    fun updateMessage(mes: String) {
        _message.value = mes
    }

    private val _message = MutableLiveData<String>("")
    val message: LiveData<String> = _message

}
