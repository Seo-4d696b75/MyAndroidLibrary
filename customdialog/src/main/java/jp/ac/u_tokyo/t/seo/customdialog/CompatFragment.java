package jp.ac.u_tokyo.t.seo.customdialog;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

/**
 * @author Seo-4d696b75
 * @version 2019/06/17.
 */
public class CompatFragment extends Fragment{

    private CustomFragmentManager mManager;

    @Override
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mManager = new CustomFragmentManager(super.getChildFragmentManager(), this, savedInstanceState);
    }

    @Override
    @CallSuper
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        // save state of FragmentManager
        mManager.onSaveInstanceState(outState);
    }

    /**
     * このフラグメント内に別のフラグメントをネストさせるのに使う{@link FragmentManager}を返す.<br>
     * 返値は{@link CustomFragmentManager ラップされたオブジェクト}であり同様のAPIを提供しているので,
     * 通常通りに使用できそれだけで安全にFragmentの遷移が行える.
     * @return wrapped {@link #getChildFragmentManager()}
     */
    public FragmentManager getCompatFragmentManager(){
        return mManager;
    }

    CustomFragmentManager getCustomFragmentManager(){
        return mManager;
    }

    /**
     * このFragmentのFragmentManagerのBackStackが取り出される時に呼ばれる.<br>
     * スタックに積まれたTransactionを
     * {@code FragmentManager#popBackStack***}で取り出し遷移を戻すときに呼び出す.
     *
     * @param name tag of transaction set at {@link android.support.v4.app.FragmentTransaction#addToBackStack(String)}
     */
    protected void onBackStackPop(@Nullable String name){

    }

    @CallSuper
    @Override
    public void onResume(){
        super.onResume();
        mManager.onResume();
    }

    @CallSuper
    @Override
    public void onPause(){
        super.onPause();
        mManager.onPause();
    }

    @CallSuper
    @Override
    public void onDestroy(){
        super.onDestroy();
        mManager.release();
        mManager = null;
    }
}
