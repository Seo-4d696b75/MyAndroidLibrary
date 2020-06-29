package jp.ac.u_tokyo.t.seo.customdialog;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

/**
 * {@link #commit()}が実行時のタイミングに依らず安全に実行できるような実装に変えたラッパークラス.
 * @author Seo-4d696b75
 * @version 2019/06/17.
 */
class CustomTransaction extends FragmentTransaction{

    CustomTransaction(FragmentTransaction transaction, CustomFragmentManager handler){
        mTransaction = transaction;
        mTransactionHandler = handler;
    }

    private FragmentTransaction mTransaction;
    private CustomFragmentManager mTransactionHandler;
    private boolean isAddToBackStack = false;
    private String mBackStackTag = null;


    @Override
    public FragmentTransaction add(Fragment fragment, String tag){
        /*
        DialogFragment#show(FragmentManager,String)はここを呼び出して実現している

        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commit();

         */
        return mTransaction.add(fragment, tag);
    }

    @Override
    public FragmentTransaction add(int containerViewId, Fragment fragment){
        return mTransaction.add(containerViewId, fragment);
    }

    @Override
    public FragmentTransaction add(int containerViewId, Fragment fragment, @Nullable String tag){
        return mTransaction.add(containerViewId, fragment, tag);
    }

    @Override
    public FragmentTransaction replace(int containerViewId, Fragment fragment){
        return mTransaction.replace(containerViewId, fragment);
    }

    @Override
    public FragmentTransaction replace(int containerViewId, Fragment fragment, @Nullable String tag){
        return mTransaction.replace(containerViewId, fragment, tag);
    }

    @Override
    public FragmentTransaction remove(Fragment fragment){
        return mTransaction.remove(fragment);
    }

    @Override
    public FragmentTransaction hide(Fragment fragment){
        return mTransaction.hide(fragment);
    }

    @Override
    public FragmentTransaction show(Fragment fragment){
        return mTransaction.show(fragment);
    }

    @Override
    public FragmentTransaction detach(Fragment fragment){
        return mTransaction.detach(fragment);
    }

    @Override
    public FragmentTransaction attach(Fragment fragment){
        return mTransaction.attach(fragment);
    }

    @Override
    public FragmentTransaction setPrimaryNavigationFragment(Fragment fragment){
        return mTransaction.setPrimaryNavigationFragment(fragment);
    }

    @Override
    public boolean isEmpty(){
        return mTransaction.isEmpty();
    }

    @Override
    public FragmentTransaction setCustomAnimations(int enter, int exit){
        return mTransaction.setCustomAnimations(enter, exit);
    }

    @Override
    public FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit){
        return mTransaction.setCustomAnimations(enter, exit, popEnter, popExit);
    }

    @Override
    public FragmentTransaction addSharedElement(View sharedElement, String name){
        return mTransaction.addSharedElement(sharedElement, name);
    }

    @Override
    public FragmentTransaction setTransition(int transit){
        return mTransaction.setTransition(transit);
    }

    @Override
    public FragmentTransaction setTransitionStyle(int styleRes){
        return mTransaction.setTransitionStyle(styleRes);
    }

    @Override
    public FragmentTransaction addToBackStack(@Nullable String name){
        isAddToBackStack = true;
        mBackStackTag = name;
        return mTransaction.addToBackStack(name);
    }

    @Override
    public boolean isAddToBackStackAllowed(){
        return mTransaction.isAddToBackStackAllowed();
    }

    @Override
    public FragmentTransaction disallowAddToBackStack(){
        return mTransaction.disallowAddToBackStack();
    }

    @Override
    public FragmentTransaction setBreadCrumbTitle(int res){
        return mTransaction.setBreadCrumbTitle(res);
    }

    @Override
    public FragmentTransaction setBreadCrumbTitle(CharSequence text){
        return mTransaction.setBreadCrumbTitle(text);
    }

    @Override
    public FragmentTransaction setBreadCrumbShortTitle(int res){
        return mTransaction.setBreadCrumbShortTitle(res);
    }

    @Override
    public FragmentTransaction setBreadCrumbShortTitle(CharSequence text){
        return mTransaction.setBreadCrumbShortTitle(text);
    }

    @Override
    public FragmentTransaction setReorderingAllowed(boolean reorderingAllowed){
        return mTransaction.setReorderingAllowed(reorderingAllowed);
    }

    @Deprecated
    @Override
    public FragmentTransaction setAllowOptimization(boolean allowOptimization){
        return mTransaction.setAllowOptimization(allowOptimization);
    }

    @Override
    public FragmentTransaction runOnCommit(Runnable runnable){
        return mTransaction.runOnCommit(runnable);
    }

    /**
     * 安全にcommit()する.<br>
     * 通常の{@link FragmentTransaction#commit()}はActivityが状態を保存した後の状態（#onSaveInstanceState()~の期間）では例外を投げる.
     * この実装では#onPause()~#onResume()間にcommit()されたTransactionは#onResume()まで待ってからcommit()する.<br>
     * <strong>注意 </strong>返値は{@link android.support.v4.app.FragmentManager.BackStackEntry バックスタック}へのIDではない.
     *
     * @return always 0, not ID to {@link android.support.v4.app.FragmentManager.BackStackEntry}
     * @throws IllegalStateException if called twice
     */
    @Override
    public int commit(){
        if ( mTransactionHandler == null ) throw new IllegalStateException("this transaction committed already");
        mTransactionHandler.addTransaction(mTransaction, isAddToBackStack, mBackStackTag);
        mTransactionHandler = null;
        return 0;
    }



    @Override
    public int commitAllowingStateLoss(){
        mTransactionHandler = null;
        return mTransaction.commitAllowingStateLoss();
    }

    @Override
    public void commitNow(){
        mTransaction.commitNow();
        mTransactionHandler = null;
    }

    @Override
    public void commitNowAllowingStateLoss(){
        mTransaction.commitNowAllowingStateLoss();
        mTransactionHandler = null;
    }
}
