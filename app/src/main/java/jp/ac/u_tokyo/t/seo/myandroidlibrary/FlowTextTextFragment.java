package jp.ac.u_tokyo.t.seo.myandroidlibrary;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import jp.ac.u_tokyo.t.seo.customview.FlowTextSurfaceView;
import jp.ac.u_tokyo.t.seo.customview.MultiToggleButton;
import jp.ac.u_tokyo.t.seo.customview.MultipleStateButton;

/**
 * @author Seo-4d696b75
 * @version 2017/10/30.
 */

public class FlowTextTextFragment extends Fragment{



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        super.onCreateView(inflater,container,state);
        return inflater.inflate(R.layout.fragment_text_test,container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle state){
        super.onViewCreated(view,state);

        final String[] str = new String[]{
                "This is my custom view, in which text flows horizontally, and ist properties can be customized.",
                "This is custom view",
                "This is FlowTextView. This view can properly show a text whose length is over the width of view."
        };

        MultiToggleButton toggle = (MultiToggleButton)view.findViewById(R.id.toggle);
        MultiToggleButton toggle2 = (MultiToggleButton)view.findViewById(R.id.toggle2);
        final FlowTextSurfaceView text = (FlowTextSurfaceView)view.findViewById(R.id.flowText);

        toggle.setOnStateChangedListener(new MultipleStateButton.OnStateChangedListener(){
            @Override
            public void onStateChanged(MultipleStateButton button, int state, int allState){

                text.setText(str[state]);
            }
        });

        toggle2.setOnStateChangedListener(new MultipleStateButton.OnStateChangedListener(){
            @Override
            public void onStateChanged(MultipleStateButton button, int state, int allState){
                text.setTextSize(20+4*state);
            }
        });
    }

}
