package jp.ac.u_tokyo.t.seo.myandroidlibrary;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Locale;

import jp.ac.u_tokyo.t.seo.customview.FlowTextView;
import jp.ac.u_tokyo.t.seo.customview.MultiToggleButton;
import jp.ac.u_tokyo.t.seo.customview.MultipleStateButton;

/**
 * @author Seo-4d696b75
 * @version 2017/10/16.
 */

public class MultiToggleTestFragment extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        super.onCreateView(inflater,container,state);
        return inflater.inflate(R.layout.fragment_multi_toggle_test,container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle state){
        super.onViewCreated(view, state);

    }

}
