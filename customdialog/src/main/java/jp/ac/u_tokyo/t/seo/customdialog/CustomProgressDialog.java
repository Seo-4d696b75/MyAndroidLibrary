package jp.ac.u_tokyo.t.seo.customdialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * @author Seo-4d696b75
 * @version 2017/10/09.
 */

public class CustomProgressDialog extends CustomDialog{

    public static CustomProgressDialog getInstance(){
        return new CustomProgressDialog();
    }

    private TextView mTextMessage;
    private TextView mTextProgress;
    private ProgressBar mProgressBar;

    @Override
    protected View onInflateView(LayoutInflater inflater, int layoutID){
        return inflater.inflate(R.layout.progress_dialog_custom, null, false);
    }

    @Override
    protected void onCreateContentView (View view){
        mTextMessage = (TextView)view.findViewById(R.id.textProgressMessage);
        mTextProgress = (TextView)view.findViewById(R.id.textProgressState);
        mProgressBar = (ProgressBar)view.findViewById(R.id.progressBar);
    }

    public void setMessageText(final String text){
        if ( mTextMessage != null ){
            mTextMessage.post(new Runnable(){
                @Override
                public void run(){
                    mTextMessage.setText(text);
                }
            });
        }
    }

    public void setProgressText(final String text){
        if ( mTextProgress != null ){
            mTextProgress.post(new Runnable(){
                @Override
                public void run(){
                    mTextProgress.setText(text);
                }
            });
        }
    }

    public void setProgressValue(final int progress){
        if ( mProgressBar != null ){
            mProgressBar.post(new Runnable(){
                @Override
                public void run(){
                    mProgressBar.setProgress(progress);
                }
            });
        }
    }
}
