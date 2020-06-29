package jp.ac.u_tokyo.t.seo.myandroidlibrary;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

/**
 * @author Seo-4d696b75
 * @version 2018/03/28.
 */

public class NumberPickerTest extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        super.onCreateView(inflater,container,state);
        return inflater.inflate(R.layout.fragment_number,container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle state){
        super.onViewCreated(view, state);

        NumberPicker numberPicker = (NumberPicker)view.findViewById(R.id.numberPicker);
        final TextView textView = (TextView)view.findViewById(R.id.textNumber);

        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                textView.setText("value : " + newVal);
            }
        });
    }
}
