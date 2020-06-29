package jp.ac.u_tokyo.t.seo.myandroidlibrary;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import jp.ac.u_tokyo.t.seo.customview.VerticalProgressBar;
import jp.ac.u_tokyo.t.seo.customview.VerticalProgressBar2;
import jp.ac.u_tokyo.t.seo.customview.VerticalSeekBar;

/**
 * @author Seo-4d696b75
 * @version 2017/11/17.
 */

public class SeekBarTestFragment extends Fragment{

    private Timer mTimer;
    private int mCnt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        super.onCreateView(inflater,container,state);
        return inflater.inflate(R.layout.fragment_seekbar_test,container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle state){
        super.onViewCreated(view, state);

        mTimer = new Timer(true);


        final TextView mes1 = (TextView)view.findViewById(R.id.textMes1);
        final TextView mes2 = (TextView)view.findViewById(R.id.textMes2);
        SeekBar vertical = (SeekBar)view.findViewById(R.id.seekBarVertical);
        vertical.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                mes2.setText("vertical > progress update : " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar){
                mes2.setText("vertical > onStartTrackingTouch()");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar){
                mes2.setText("vertical > onStopTrackingTouch()");
            }
        });

        final ProgressBar progressBar = (ProgressBar)view.findViewById(R.id.progressNormal);
        final VerticalProgressBar verticalProgressBar = (VerticalProgressBar)view.findViewById(R.id.progressVertical);
        final VerticalProgressBar2 verticalProgressBar2 = (VerticalProgressBar2)view.findViewById(R.id.progressVertical2);
        mes1.setText("start to progress");
        mes1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mCnt = 0;
                mTimer.schedule(new TimerTask(){
                    @Override
                    public void run(){
                        progressBar.post(new Runnable(){
                            @Override
                            public void run(){
                                progressBar.setProgress(mCnt*2);
                            }
                        });
                        verticalProgressBar.post(new Runnable(){
                            @Override
                            public void run(){
                                verticalProgressBar.setProgress(mCnt*2);
                            }
                        });
                        verticalProgressBar2.post(new Runnable(){
                            @Override
                            public void run(){
                                verticalProgressBar2.setProgress(mCnt*2);
                            }
                        });
                        mes1.post(new Runnable(){
                            @Override
                            public void run(){
                                mes1.setText("counting... "+mCnt);
                            }
                        });
                        if ( mCnt == 50 ){
                            cancel();
                        }else{
                            mCnt++;
                        }
                    }
                },0,100);
            }
        });
    }

}
