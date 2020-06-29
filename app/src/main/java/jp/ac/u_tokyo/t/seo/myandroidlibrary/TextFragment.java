package jp.ac.u_tokyo.t.seo.myandroidlibrary;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import jp.ac.u_tokyo.t.seo.customview.CustomNumberPicker;
import jp.ac.u_tokyo.t.seo.customview.ExpandableTextView;
import jp.ac.u_tokyo.t.seo.customview.FloatPicker;

/**
 * @author Seo-4d696b75
 * @version 2017/10/30.
 */

public class TextFragment extends Fragment{

    private int index = 0;
    private ExpandableTextView expand1,expand2,expand3;
    private TextView text1,text2,text3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        return inflater.inflate(R.layout.fragment_text,container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle state){

        final String[] str = new String[]{
                "This is my custom view, in which text flows horizontally, and ist properties can be customized.",
                "This is custom view",
                "This is FlowTextView. This view can properly show a text whose length is over the width of view."
        };

        expand1 = view.findViewById(R.id.expandText1);
        expand2 = view.findViewById(R.id.expandText2);
        expand3 = view.findViewById(R.id.expandText3);
        text1 = view.findViewById(R.id.text1);
        text2 = view.findViewById(R.id.text2);
        text3 = view.findViewById(R.id.text3);
        final float density = getResources().getDisplayMetrics().density;
        view.findViewById(R.id.buttonChangeText).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String mes = str[index];
                index = (index + 1)%3;
                expand1.setText(mes);
                expand2.setText(mes);
                expand3.setText(mes);
            }
        });
        
        ((CustomNumberPicker)view.findViewById(R.id.numberWith)).setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker numberPicker, int oldVar, int newVar){
                int width = (int)(density * newVar);
                Log.d("width", String.format(Locale.US, "%ddp = %dpx", newVar, width));

                ViewGroup.LayoutParams params = expand1.getLayoutParams();
                params.width = width;
                expand1.setLayoutParams(params);
                expand2.setMaxWidth(width);

                width = newVar;
                text1.setText(String.format(Locale.US, "width = %ddp", width));
                text2.setText(String.format(Locale.US, "width = wrap_content, maxWidth = %ddp", width));
            }
        });
        ((FloatPicker)view.findViewById(R.id.floatTextScaleX)).setOnFloatValueChangeListener(new FloatPicker.OnFloatValueChangeListener(){
            @Override
            public void onValueChange(FloatPicker picker, float oldVar, float newVar){
                Log.d("scale",String.valueOf(newVar));
                expand1.setMinTextScaleX(newVar);
                expand2.setMinTextScaleX(newVar);
                expand3.setMinTextScaleX(newVar);
            }
        });
    }

}
