package jp.ac.u_tokyo.t.seo.customdialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;


/**
 * @author Seo-4d696b75
 * @version 2019/06/17.
 */
public class FragmentCompatActivity extends AppCompatActivity{

    private CustomFragmentManager mFragmentManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if ( mFragmentManager == null ){

            // こいつにはラップしてないmanagerを渡す

            mFragmentManager = new CustomFragmentManager(super.getSupportFragmentManager(), this, savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        // カスタムしたフラグメントが保持する付加情報を保存する
        mFragmentManager.onSaveInstanceState(outState);
    }

    /**
     * このオブジェクトはラップされていない.<br>
     * 特別な事情がない限り{@link #getCompatFragmentManager() 代替メソッド}の使用を推奨する.
     *
     * @return unwrapped object
     * @see #getCompatFragmentManager()
     */
    @Deprecated
    @Override
    public FragmentManager getSupportFragmentManager(){
        return super.getSupportFragmentManager();
    }

    /**
     * このActivityにFragmentを配置するのに使うオブジェクトを返す.<br>
     * 返値は{@link CustomFragmentManager ラップされたオブジェクト}であり同様のAPIを提供しているので,
     * 通常通りに使用できそれだけで安全にFragmentの遷移が行える.
     *
     * @return object wrapping {@code #getSupportFragmentManager()}
     */
    public FragmentManager getCompatFragmentManager(){
        return mFragmentManager;
    }

    @Override
    protected void onPause(){
        super.onPause();
        mFragmentManager.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mFragmentManager.onResume();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mFragmentManager.release();
        mFragmentManager = null;
    }

    /**
     * 戻るボタン押下時にBackStackが取り出されると呼ばれる.<br>
     * {@link #onBackPressed() ユーザの戻るボタン操作のイベント}からもっとも最近にスタックに積まれたTransactionを
     * {@link FragmentManager#popBackStackImmediate()}で戻した直後に呼び出す.<br>
     * <strong>注意 </strong>{@link #getCompatFragmentManager() このActivityのFragmentManager}以外にもActivityに載っている
     * {@link CompatFragment#getCompatFragmentManager() FragmentのManager}のスタックに積まれていたTransactionも含む
     *
     * @param manager one from which latest transaction in stack has been popped
     * @param entry   latest transaction having been popped from stack
     */
    protected void onBackStackPop(FragmentManager manager, FragmentManager.BackStackEntry entry){
        // 戻るボタンからバックスタックをポップした時の処理はここ
    }


    /**
     * このActivityのFragmentManagerのBackStackが取り出される時に呼ばれる.<br>
     * スタックに積まれたTransactionを
     * {@code FragmentManager#popBackStack***}で取り出し遷移を戻すときに呼び出す.
     *
     * @param name tag of transaction set at {@link android.support.v4.app.FragmentTransaction#addToBackStack(String)}
     */
    protected void onBackStackPop(@Nullable String name){

    }

    /**
     * バックスタックに積まれたTransactionをPopする.<br>
     * このActivityおよびActivityに載っているすべての{@link CompatFragment Fragment}の
     * FragmentManagerのバックスタックにおいて、もっとも最近に積まれたTransactionを探す.<br>
     * <strong>注意 </strong>ユーザが戻るボタンを押したときのコールバック{@link #onBackPressed()}
     * でも同様の操作を行う実装をしているので、Overrideするとき{@code super.onBackPressed()}しながら
     * {@code popBackStack()}も呼ぶのはNG
     *
     * @return true if any transaction popped, otherwise false
     */
    public boolean popBackStack(){
        CustomFragmentManager manager = mFragmentManager.tryPopBackStack();
        if ( manager != null ){
            FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(manager.getBackStackEntryCount() - 1);
            // only from Main thread
            manager.popBackStackImmediate();
            onBackStackPop(manager, entry);
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed(){
        // if any backStack popped, onBackPressed() event is to be consumed
        // this solution is inspired by https://stackoverflow.com/questions/13418436/android-4-2-back-stack-behaviour-with-nested-fragments
        if ( popBackStack() ) return;
        super.onBackPressed();
    }
}
