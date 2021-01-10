package jp.seo.android.mylibrary.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import jp.seo.android.mylibrary.R
import jp.seo.android.widget.CustomNumberPicker
import jp.seo.android.widget.FloatPicker
import jp.seo.android.widget.HorizontalListView

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { ctx ->

            pageViewModel = PageViewModel.getFactory(ctx).create(PageViewModel::class.java)
            pageViewModel.setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 0)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_test, container, false)
        val titleView: TextView = root.findViewById(R.id.text_title)
        pageViewModel.title.observe(viewLifecycleOwner) {
            titleView.text = it
        }
        val messageView: TextView = root.findViewById(R.id.text_message)
        pageViewModel.message.observe(viewLifecycleOwner) {
            messageView.text = it
        }
        val holder = root.findViewById<RelativeLayout>(R.id.widget_container)
        val index = arguments?.getInt(ARG_SECTION_NUMBER) ?: 0
        context?.let { ctx ->
            getTestWidget(index, ctx, holder)
        }

        val fab: FloatingActionButton = root.findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(holder, "Value changed", Snackbar.LENGTH_SHORT).show()
            pageViewModel.updateValue()
        }

        return root
    }

    /**
     * return view(s) which should be tested here
     */
    private fun getTestWidget(index: Int, context: Context, parent: ViewGroup) {
        val inflater = LayoutInflater.from(context)
        when (index) {
            0 -> {
                val list = HorizontalListView(context)
                val adapter = StringAdapter(context, 10)
                adapter.setOnItemSelectedListener { view, data, position ->
                    pageViewModel.updateMessage("selected: $data")
                }
                list.adapter = adapter
                parent.addView(list)
            }
            1 -> {
                val frame = inflater.inflate(R.layout.fragment_textview, parent, true)
                val ids = listOf(
                    R.id.text_test_1,
                    R.id.text_test_2,
                    R.id.text_test_3,
                    R.id.text_test_4,
                    R.id.text_test_5
                )
                val views = ids.map { frame.findViewById<TextView>(it) }
                pageViewModel.text.observe(viewLifecycleOwner) { str ->
                    views.forEach { it.text = str }
                }

            }
            2 -> {
                val frame = inflater.inflate(R.layout.fragment_picker, parent, true)
                val number = frame.findViewById<CustomNumberPicker>(R.id.number_picker)
                val float = frame.findViewById<FloatPicker>(R.id.float_picker)
                val numberValue = frame.findViewById<TextView>(R.id.text_number_picker)
                val floatValue = frame.findViewById<TextView>(R.id.text_float_picker)
                number.setOnValueChangedListener { numberPicker, old, new ->
                    numberValue.text = new.toString()
                }
                float.setOnValueChangedListener { picker, old, new ->
                    floatValue.text = String.format("%.1f", new)
                }
            }
            else -> Log.w("Index", "unknown: $index")
        }
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    class StringAdapter(context: Context, size: Int) :
        HorizontalListView.ArrayAdapter<String>(
            Array(size) { "Item-${it}" }.toList()
        ) {

        private val inflater = LayoutInflater.from(context)

        override fun getView(group: ViewGroup): View {
            return inflater.inflate(R.layout.cell_list, null, false)
        }

        override fun onBindView(view: View, data: String, position: Int) {
            view.findViewById<TextView>(R.id.text_cell).text = data
        }

    }

}
