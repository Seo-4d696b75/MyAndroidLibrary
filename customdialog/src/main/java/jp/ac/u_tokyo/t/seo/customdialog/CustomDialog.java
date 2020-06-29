package jp.ac.u_tokyo.t.seo.customdialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * フラグメントを用いて実装したダイアログのテンプレート.<br><br>
 * 使い方概要<br>
 * 1. {@link Builder}クラスを用いて動的に生成する<br>
 * ダイアログ内のViewへの操作が不要な場合において、もっとも簡単な表示方法です.
 * ダイアログ上の題名、メッセージテキスト、Positive/Neutral/Negativeボタンなど基本的なＵＩの設置を指定できます.
 * <br><br>
 *
 * 2. XMLファイルで静的に制御する<br>
 * {@link DialogFrameLayout}をルートに持つViewを規定するレイアウトファイルから静的に指定できます.
 * 指定できる各種値は{@link DialogFrameLayout こちらから参照}.<br>
 * ダイアログを定義するレイアウトファイルが用意てきたら、{@link #getInstance(int)}からインスタンス化する.<br><br>
 *
 * 3.継承して細かくカスタマイズする<br>
 * このダイアログを継承することでさらに細かくカスタムできる.
 * ダイアログ生成時に以下のように各メソッドがコールされるので適当に弄る.
 * <ul>
 *     <li>Viewをインフレートする{@link #onInflateView(LayoutInflater, int)}</li>
 *     <li>ダイアログ内部のViewの初期化{@link #onCreateContentView(View)}</li>
 *     <li>ダイアログのフレーム部分の初期化{@link #onCreateFrameView(DialogFrameLayout)}</li>
 *     <li>ダイアログの大きさを計測{@link #onMeasuredDialogSize(Dialog, int, int)}</li>
 * </ul>
 * <br>
 *
 * <strong>ボタン押下時のコールバック</strong><br>
 * Negative,Positive,Neutralボタンが押下された時は{@link #onButtonClicked(int)}がコールされる.
 * このダイアログフラグメントを呼び出した元へコールバックさせる場合は、
 * Activityに{@link OnClickListener}をimplementしておく、
 * もしくは{@link OnClickListener}をimplementしたフラグメントを{@link CustomDialog#setTargetFragment(Fragment, int)}に設定しておくことで、
 * {@link OnClickListener#onDialogButtonClicked(String, CustomDialog, int)}がコールバックされる.<br>
 * {@link DialogFragment#show(FragmentManager, String)}で渡す文字列はダイアログを識別する値であり、
 * Negative,Neutral,Positiveボタンが押されたときコールされる{@link OnClickListener#onDialogButtonClicked(String, CustomDialog, int)}
 * ではこの文字列でどのダイアログのボタンかを判定できる.<br><br>
 *
 * <strong>ダイアログとのデータのやり取り</strong><br>
 * メモリ枯渇や画面回転時などにこのDialogFragmentが再生成されるとき、
 * Androidシステムは引数なしのpublicなコンストラクタを呼び出してインスタンス化する.
 * フラグメント内でのデータの保持、呼び出し元とのデータのやり取りには注意が要る.<br>
 * <ul>
 *     <li>ダイアログにデータを渡す: 必ず{@link Fragment#setArguments(Bundle)}を通して渡す</li>
 *     <li>フラグメント内でのデータの保持: {@link Fragment#onSaveInstanceState(Bundle)}で保存して{@link Fragment#onCreate(Bundle)}などで読み出す</li>
 *     <li>ダイアログのデータを呼び出し元から参照: {@link CustomDialog#onSaveResult(Bundle)}で保存したデータが登録したコールバック{@link OnClickListener#onDialogButtonClicked(String, CustomDialog, int)}から{@link CustomDialog#getResult()}で得られる.</li>
 * </ul>
 *
 * @author Seo-4d696b75
 * @version 2017/10/05.
 */

public class CustomDialog extends DialogFragment{

    public static final String LAYOUT = "layout";
    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String BUTTON_POSITIVE = "buttonPositive";
    public static final String BUTTON_NEGATIVE = "buttonNegative";
    public static final String BUTTON_NEUTRAL = "buttonNeutral";

    /**
     * このダイアログのインスタンスはここから取得します.
     * この{@link CustomDialog カスタムダイアログ}を継承する場合も
     * インスタンスはコンストラクタからではなくstaticなメソッドで返すように実装します.
     * @return ダイアログオブジェクトを生成
     */
    public static CustomDialog getInstance (){
        return new CustomDialog();
    }

    /**
     * 指定したレイアウトファイルで定義されたダイアログをインスタンス化する.
     * ダイアログのＵＩをXMLで静的に指定する場合は、
     * レイアウトファイルが定義するViewのルートが{@link DialogFrameLayout}になるようにする.
     * もしそうでない場合は、レイアウトファイルの定めたViewを内部にもつダイアログを代わりに生成する.
     * @param layoutResID レイアウトファイルのＩＤ
     */
    public static CustomDialog getInstance(@LayoutRes int layoutResID){
        Bundle args = new Bundle();
        args.putInt(LAYOUT, layoutResID);
        CustomDialog dialog = new CustomDialog();
        dialog.setArguments(args);
        return dialog;
    }

    /**
     * 適当なパラメータを与えてダイアログを生成するためのビルダークラス.<br>
     * 指定できるパラメータは、
     * <ul>
     *     <li>題名 {@link #setTitle(String)}</li>
     *     <li>メッセージテキスト {@link #setMessage(String)}</li>
     *     <li>ダイアログ内のView {@link #setContentView(int)}</li>
     *     <li>Positiveボタン {@link #setPositiveButton(String)}</li>
     *     <li>Neutralボタン {@link #setNeutralButton(String)}</li>
     *     <li>Negativeボタン {@link #setNegativeButton(String)}</li>
     * </ul>
     * パラメータを指定したら、{@link #create()}からインスタンス化するか、
     * {@link #show(FragmentManager, String)}から直接ダイアログを表示する.
     */
    public static class Builder {

        private final Bundle args;

        public Builder(){
            args = new Bundle();
        }

        /**
         * ダイアログの題名を指定します.
         * @param title タイトルテキスト nullの場合はタイトルを表示しない
         */
        public Builder setTitle(String title){
            args.putString(TITLE, title);
            return this;
        }

        /**
         * ダイアログのメッセージを指定します.
         * @param message メッセージテキスト nullの場合は表示なし
         */
        public Builder setMessage(String message){
            args.putString(MESSAGE, message);
            return this;
        }

        /**
         * ダイアログ内のViewを指定します
         * @param layoutID 内部のViewを定めたレイアウトファイルのＩＤ
         */
        public Builder setContentView(@LayoutRes int layoutID){
            args.putInt(LAYOUT, layoutID);
            return this;
        }

        /**
         * ダイアログ下部のPositiveボタンを設定します.
         * @param buttonText ボタンのテキスト nullの場合はボタンを設置しない
         */
        public Builder setPositiveButton(String buttonText){
            args.putString(BUTTON_POSITIVE, buttonText);
            return this;
        }

        /**
         * ダイアログ下部のNeutralボタンを設定します.
         * @param buttonText ボタンのテキスト nullの場合はボタンを設置しない
         */
        public Builder setNeutralButton(String buttonText){
            args.putString(BUTTON_NEUTRAL, buttonText);
            return this;
        }

        /**
         * ダイアログ下部のNegativeボタンを設定します.
         * @param buttonText ボタンのテキスト nullの場合はボタンを設置しない
         */
        public Builder setNegativeButton(String buttonText){
            args.putString(BUTTON_NEGATIVE, buttonText);
            return this;
        }

        /**
         * 与えられたパラメータでダイアログをインスタンス化します.
         * @return 各メソッドで与えられたパラメータを {@link Bundle}オブジェクトとして {@link DialogFragment#setArguments(Bundle)}したダイアログ
         */
        public CustomDialog create(){
            CustomDialog dialog = getInstance();
            dialog.setArguments(args);
            return dialog;
        }

        /**
         * 与えられたパラメータを持つダイアログを表示します.
         * @see DialogFragment#show(FragmentManager, String)
         * @param manager このダイアログが追加されるFragmentManager
         * @param tag このフラグメントのタグ may be null
         */
        public void show(FragmentManager manager, String tag){
            create().show(manager, tag);
        }

    }


    /**
     * ダイアログのNegative,Neutral,Positiveボタンのコールバックを定義します
     */
    public interface OnClickListener {
        /**
         * Negative,Neutral,Positiveボタンが押下されたときにコールされます.
         * @param tag {@link DialogFragment#show(FragmentManager, String)}で渡したダイアログ固有(のはず)の文字列
         * @param dialog ダイアログ本体
         * @param which Negative,Neutral,Positiveのどれか 定数{@link DialogInterface#BUTTON_NEGATIVE}で返す
         */
        void onDialogButtonClicked(String tag, CustomDialog dialog, int which);
    }

    private OnClickListener mListener;
    private Bundle mResult;
    private DialogFrameLayout mFrame;
    private Point mDisplaySize;

    /**
     * @deprecated 代わりにこのDialogFragmentを呼び出すActivityにimplementしましょう。
     * <strong>警告</strong> ここから登録したリスナーはFragment再生成時に再登録される保証はない.
     */
    public void setOnClickListener(OnClickListener listener){
        this.mListener = listener;
    }

    @Override
    @NonNull
    public final Dialog onCreateDialog(Bundle b){
        //this fragment does not keep its current state, or care for saved state
        //instead, DialogFrameLayout view on this dialog has that responsibility


        FragmentActivity context = getActivity();
        Fragment fragment = getTargetFragment();
        Bundle args = getArguments();
        if ( fragment instanceof  OnClickListener ){
            mListener = (OnClickListener)fragment;
        }else if ( context instanceof OnClickListener){
            mListener = (OnClickListener)context;
        }

        Dialog dialog = new Dialog(context);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        WindowManager manager = getActivity().getWindowManager();
        Display display = manager.getDefaultDisplay();
        mDisplaySize = new Point();
        display.getSize(mDisplaySize);


        LayoutInflater inflater = context.getLayoutInflater();
        View content = null;
        mFrame = null;
        View requestedView = onInflateView(inflater, args == null ? 0 : args.getInt(LAYOUT, 0));
        if ( requestedView != null ){
            if ( requestedView instanceof DialogFrameLayout ){
                mFrame = (DialogFrameLayout)requestedView;
                content = mFrame.getContentView();
            }else{
                content = requestedView;
            }
        }
        if ( mFrame == null ){
            mFrame = new DialogFrameLayout(context);
            mFrame.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        if ( content != null ) {
            onCreateContentView(content);
            mFrame.addContentView(content);
        }


        if ( args != null ){
            if ( args.containsKey(TITLE) ){
                mFrame.setTitleText(args.getString(TITLE, null));
            }
            if ( args.containsKey(MESSAGE) ){
                mFrame.setMessageText(args.getString(MESSAGE, null));
            }
            if ( args.containsKey(BUTTON_POSITIVE) ){
                mFrame.setPositiveButton(args.getString(BUTTON_POSITIVE, null));
            }
            if ( args.containsKey(BUTTON_NEUTRAL) ){
                mFrame.setNeutralButton(args.getString(BUTTON_NEUTRAL, null));
            }
            if ( args.containsKey(BUTTON_NEGATIVE) ){
                mFrame.setNegativeButton(args.getString(BUTTON_NEGATIVE, null));
            }
        }
        onCreateFrameView(mFrame);


        mFrame.setOnClickListener(new OnClickListener(){
            @Override
            public void onDialogButtonClicked(String tag, CustomDialog dialog, int which){
                tag = getTag();
                mResult = new Bundle();
                onSaveResult(mResult);
                if ( onButtonClicked(which) ){
                    dismiss();
                }
                if ( mListener != null ){
                    mListener.onDialogButtonClicked(tag, CustomDialog.this, which);
                }
            }
        });
        mFrame.onSavedStateRestored();

        //dialog.getWindow().setBackgroundDrawable(mFrame.getDialogColorDrawable());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(mFrame.isCancelableOutsideTouch());

        return dialog;
    }

    /**
     * ダイアログのViewをインフレートします.
     * この返り値のviewのルートが{@link DialogFrameLayout}の場合は、そのViewGroupに設定されているパラメータで
     * ダイアログのフレーム部分のＵＩが初期化され、ViewGroup直下にChildViewが存在するならばダイアログ内部に設定します.
     * それ以外のViewの場合は、返り値のViewを内部にもつダイアログになります.
     * このとき、ダイアログ内部のViewがインフレートされた場合は{@link #onCreateContentView(View)}が後にコールされます.<br>
     * デフォルトの実装では{@link #getInstance(int)}もしくは{@link Builder#setContentView(int)}で渡したLayoutIDで指定されたリソースファイルからインフレートします.
     * このダイアログを継承して用いる場合で、内部のViewをカスタムしたい時はこのメソッドを適宜オーバライドしましょう.
     * @param inflater default inflater
     * @param layoutID layout resource ID, パラメータとして与えれた値　もしくはデフォルト値0
     * @return nullを返すとタイトルやメッセージ、ボタンなど最低限のＵＩを除いて
     *          ダイアログ内のViewが無くなる {@link #onCreateContentView(View)}もコールされない
     */
    protected View onInflateView(LayoutInflater inflater, int layoutID){
        return layoutID == 0 ? null : inflater.inflate(layoutID, null, false);
    }

    /**
     * このダイアログのフレームを成すViewがインフレートされたときにコールされます.
     * このフレームには{@link Builder}やXML上の{@link DialogFrameLayout}の属性で指定した各種ＵＩのデータが既に設定されています.
     * ダイアログのタイトル、メインメッセージ、Positive/Negative/Neutralボタンなどを動的に設定する場合は、
     * このタイミングで処理できます.
     * @see DialogFrameLayout#setTitleText(String)
     * @see DialogFrameLayout#setMessageText(String)
     * @see DialogFrameLayout#setPositiveButton(String)
     * @param view ダイアログのタイトル、メインメッセージ、Positive/Negative/Neutralボタンなど基本的なＵＩを提供するViewGroup
     */
    protected void onCreateFrameView(@NonNull DialogFrameLayout view){}

    /**
     * ダイアログ内のViewを初期化します.
     * {@link #getInstance(int)}や{@link Builder#setContentView(int)}でレイアウトIDを
     * 指定していない場合や、{@link #onInflateView(LayoutInflater, int)}でダイアログ内部のViewをインフレートしなかった場合は、
     * ダイアログの中身はタイトル・メッセージ表示と下部のボタンのみとなり
     * このメソッドはコールされません.
     * @param view {@link #getInstance(int)}や{@link Builder#setContentView(int)}
     *              で指定されたレイアウトIDを基に{@link #onInflateView(LayoutInflater, int)}でインフレートしたView
     */
    protected void onCreateContentView (@NonNull View view){}

    /**
     * ダイアログ下部のボタンPositive,Negative,Neutralが押下されたときコールします.
     * 登録したリスナーの
     * {@link OnClickListener#onDialogButtonClicked(String, CustomDialog, int)}の直前のタイミングでコールされる.
     * @param which 定数{@link DialogInterface#BUTTON_NEGATIVE}などどのボタンか判定する値
     * @return true:このダイアログをdismissする false:ダイアログを継続して表示する
     */
    protected boolean onButtonClicked(int which){
        return true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstance){
        super.onActivityCreated(savedInstance);


        mFrame.measure(mDisplaySize.x, mDisplaySize.y);

        Dialog dialog = getDialog();
        onMeasuredDialogSize(dialog, mFrame.getMeasuredWidth(), mFrame.getMeasuredHeight());

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        dialog.addContentView(mFrame, params);
        mFrame.setId(R.id.customDialogRoot);

    }

    /**
     * このダイアログの結果を受け取る.
     * {@link #onSaveResult(Bundle)}で詰め込んだデータを引き出す.
     * @return Null if {@link #onSaveResult(Bundle)} has not been called yet
     */
    public Bundle getResult(){
        return mResult;
    }

    /**
     * このダイアログで得た結果を集約する.
     * この{@link Bundle}オブジェクトは{@link #getResult()}から参照できるので、
     * 登録しておいたリスナーの呼び出し{@link OnClickListener#onDialogButtonClicked(String, CustomDialog, int)}から
     * ダイアログでの結果を引き継げる.ここは{@link #onButtonClicked(int)}直前に呼ばれる.
     * @param result
     */
    protected void onSaveResult(Bundle result){

    }

    /**
     * ダイアログのサイズを決定する.
     * このダイアログ上のViewGroupである{@link DialogFrameLayout}を{@link View#measure(int, int)}して得られたサイズに従ってダイアログのサイズも決定する.
     * <strong>Note</strong> 測定したサイズを適用するには{@link #setDialogSize(Dialog, int, int)}を呼ぶ.<br>
     * デフォルト実装ではそのままのパラメータで{@link #setDialogSize(Dialog, int, int)}を呼んでいる.
     * @param width ルートViewを{@link View#measure(int, int)}したのち得られた値{@link View#getMeasuredWidth()}
     * @param height ルートViewを{@link View#measure(int, int)}したのち得られた値{@link View#getMeasuredHeight()}
     */
    protected void onMeasuredDialogSize(Dialog dialog, int width, int height){
        setDialogSize(dialog, width, height);
    }

    protected final void setDialogSize(Dialog dialog, int width, int height){
        WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
        layoutParams.width = width;
        layoutParams.height = height;
        dialog.getWindow().setAttributes(layoutParams);
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mListener = null;
    }

}
