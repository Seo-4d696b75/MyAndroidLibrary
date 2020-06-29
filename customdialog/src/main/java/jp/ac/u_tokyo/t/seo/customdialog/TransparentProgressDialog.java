package jp.ac.u_tokyo.t.seo.customdialog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Window;

/**
 * @author Seo-4d696b75
 * @version 2017/10/09.
 */

public class TransparentProgressDialog extends DialogFragment{

    public static TransparentProgressDialog getInstance(){
        return new TransparentProgressDialog();
    }

    @Override
    public final Dialog onCreateDialog(Bundle b){
        Dialog dialog = new Dialog(getActivity());
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.progress_dialog_transparent);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }
}
