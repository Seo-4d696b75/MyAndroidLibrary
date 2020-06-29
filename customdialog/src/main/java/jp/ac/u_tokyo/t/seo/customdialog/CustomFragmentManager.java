package jp.ac.u_tokyo.t.seo.customdialog;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.Pair;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * {@link #beginTransaction()}において安全に{@link FragmentTransaction#commit()}が出来る実装クラスを返すように変更したラッパークラス.
 * Fragmentの遷移を行っていると<pre>java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState</pre>
 * と例外を投げられる場合がある。これは{@link FragmentTransaction#commit()}を{@code Activity#onSaveInstanceState(Bundle)}
 * より後のタイミングで呼ぶと状態を正しく保存できないためらしい。解決策として
 * {@code Activity#onPause()}と{@code Activity#onResume()}の間は呼び出さないようにスケジューリングする.<br>
 *  参考：https://stackoverflow.com/questions/8040280/how-to-handle-handler-messages-when-activity-fragment-is-paused
 *
 * 加えて{@link Activity#onBackPressed()}における遷移を改良
 * https://stackoverflow.com/questions/13418436/android-4-2-back-stack-behaviour-with-nested-fragments
 * @author Seo-4d696b75
 * @version 2019/06/17.
 */
class CustomFragmentManager extends FragmentManager implements FragmentManager.OnBackStackChangedListener{



    private CustomFragmentManager(FragmentManager manager, Bundle savedState){
        // this manager is not wrapped
        mManager = manager;
        mManager.addOnBackStackChangedListener(this);

        mStack = new LinkedList<>();

        if ( savedState != null ){
            long[] array = savedState.getLongArray(KEY_STATE_TIME);
            String[] name = savedState.getStringArray(KEY_STATE_NAME);
            if ( name == null || array == null || array.length != mManager.getBackStackEntryCount() ){
                throw new RuntimeException("fail to restore state");
            }
            for ( int i=0 ; i<array.length ; i++ ){
                mStack.add(new StackEntry(name[i], array[i]));
            }
        }
    }

    CustomFragmentManager(FragmentManager manager, CompatFragment parent, Bundle savedState){
        // this manager is not wrapped
        this(manager, savedState);
        this.mFragment = parent;
    }

    CustomFragmentManager(FragmentManager manager, FragmentCompatActivity activity, Bundle savedState){
        // this manager is not wrapped
        this(manager, savedState);
        this.mActivity = activity;
    }

    private final String KEY_STATE_TIME = "jp.ac.u_tokyo.t.seo.customdialog.CustomFragmentManager:time";
    private final String KEY_STATE_NAME = "jp.ac.u_tokyo.t.seo.customdialog.CustomFragmentManager:name";


    // 付加情報の保存   元のManagerに加えて各Fragmentの遷移時刻を保持している
    void onSaveInstanceState(Bundle bundle){
        final int size = mStack.size();
        long[] time = new long[size];
        String[] name = new String[size];
        for ( int i=0 ; i<size ; i++ ){
            StackEntry entry = mStack.get(i);
            time[i] = entry.time;
            name[i] = entry.tag;
        }
        bundle.putLongArray(KEY_STATE_TIME, time);
        bundle.putStringArray(KEY_STATE_NAME, name);
    }


    // ラップされる前のManager
    private FragmentManager mManager;
    // バックスタックを別途保持する
    private LinkedList<StackEntry> mStack;

    private static class StackEntry {

        private StackEntry(String tag){
            this.time = SystemClock.uptimeMillis();
            this.tag = tag;
        }

        private StackEntry(String tag, long time){
            this.time = time;
            this.tag = tag;
        }

        final long time;
        final String tag;

    }

    // Contextの状態に合わせて安全にcommit()する
    private boolean mIsActive = false;
    private Queue<TransactionRequest> mQueue = new LinkedList<>();

    // callback
    private CompatFragment mFragment;
    private FragmentCompatActivity mActivity;

    /**
     * {@code Activity#onPause()}が呼ばれるタイミングでコールすること
     */
    synchronized void onPause(){
        mIsActive = false;
    }

    /**
     * {@code Activity#onResume()}が呼ばれるタイミングでコールすること.
     * Fragmentの遷移やタスクが待ち行列に溜まっている場合はこの時ハンドルされる.
     */
    synchronized void onResume(){
        mIsActive = true;
        while ( !mQueue.isEmpty() ){
            TransactionRequest request = mQueue.poll();
            if ( request.addBackStack ) onCommitTransactionToBackStack(request.tag);
            FragmentTransaction transaction = request.transaction;
            transaction.commit();

        }
    }

    private static class TransactionRequest {

        FragmentTransaction transaction;
        boolean addBackStack;
        String tag;

    }

    synchronized void addTransaction(FragmentTransaction transaction, boolean addBackStack, String tag){
        if ( mManager == null ){
            throw new IllegalStateException("FragmentManager has already been released.");
        }
        if ( mIsActive ){
            if ( addBackStack ) onCommitTransactionToBackStack(tag);
            transaction.commit();
        }else{
            TransactionRequest request = new TransactionRequest();
            request.transaction = transaction;
            request.addBackStack = addBackStack;
            request.tag = tag;
            mQueue.offer(request);
        }
    }


    @Override
    @SuppressWarnings("CommitTransaction")
    public FragmentTransaction beginTransaction(){
        return new CustomTransaction(mManager.beginTransaction(), this);
    }

    @Override
    public boolean executePendingTransactions(){
        return mManager.executePendingTransactions();
    }

    @Override
    public Fragment findFragmentById(int id){
        return mManager.findFragmentById(id);
    }

    @Override
    public Fragment findFragmentByTag(String tag){
        return mManager.findFragmentByTag(tag);
    }

    @Override
    public void popBackStack(){
        mManager.popBackStack();
    }

    @Override
    public boolean popBackStackImmediate(){
        return mManager.popBackStackImmediate();
    }

    /*
     Be sure to call from the instance, which wraps a manager in root Activity
     Search all the nested fragments for the fragmentManager, which has latest backStackEntry
     */
    CustomFragmentManager tryPopBackStack(){
        TraverseResult result = new TraverseResult();
        this.traverseFragments(result);
        return result.target;
    }

    private static class TraverseResult {

        TraverseResult(){
            latest = 0;
            target = null;
        }

        private long latest;
        private CustomFragmentManager target;

        void onTraverse(StackEntry entry, CustomFragmentManager manager){
            if ( entry.time > latest ){
                latest = entry.time;
                target = manager;
            }
        }

    }

    private void traverseFragments(TraverseResult result){
        if ( getBackStackEntryCount() > 0 ){
            result.onTraverse(mStack.getLast(), this);
        }
        for ( Fragment fragment : mManager.getFragments() ){
            if ( fragment.isVisible() && fragment instanceof CompatFragment ){
                CustomFragmentManager manager = ((CompatFragment)fragment).getCustomFragmentManager();
                manager.traverseFragments(result);
            }
        }
    }


    @Override
    public void popBackStack(String name, int flags){
        mManager.popBackStack(name, flags);
    }

    @Override
    public boolean popBackStackImmediate(String name, int flags){
        return mManager.popBackStackImmediate(name, flags);
    }

    @Override
    public void popBackStack(int id, int flags){
        mManager.popBackStack(id, flags);
    }

    @Override
    public boolean popBackStackImmediate(int id, int flags){
        return mManager.popBackStackImmediate(id, flags);
    }

    @Override
    public int getBackStackEntryCount(){
        return mManager.getBackStackEntryCount();
    }

    @Override
    public BackStackEntry getBackStackEntryAt(int index){
        return mManager.getBackStackEntryAt(index);
    }

    @Override
    public void addOnBackStackChangedListener(OnBackStackChangedListener listener){
        mManager.addOnBackStackChangedListener(listener);
    }

    @Override
    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener){
        mManager.removeOnBackStackChangedListener(listener);
    }

    @Override
    public void putFragment(Bundle bundle, String key, Fragment fragment){
        mManager.putFragment(bundle, key, fragment);
    }

    @Override
    public Fragment getFragment(Bundle bundle, String key){
        return mManager.getFragment(bundle, key);
    }

    @Override
    public List<Fragment> getFragments(){
        return mManager.getFragments();
    }

    @Override
    public Fragment.SavedState saveFragmentInstanceState(Fragment f){
        return mManager.saveFragmentInstanceState(f);
    }

    @Override
    public boolean isDestroyed(){
        return mManager.isDestroyed();
    }

    @Override
    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive){
        mManager.registerFragmentLifecycleCallbacks(cb, recursive);
    }

    @Override
    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb){
        mManager.unregisterFragmentLifecycleCallbacks(cb);
    }

    @Override
    public Fragment getPrimaryNavigationFragment(){
        return mManager.getPrimaryNavigationFragment();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args){
        mManager.dump(prefix, fd, writer, args);
    }

    @Override
    public boolean isStateSaved(){
        return mManager.isStateSaved();
    }

    void release(){
        mManager.removeOnBackStackChangedListener(this);
        mManager = null;
        mQueue.clear();
    }

    // BackStackへの追加はここから
    private void onCommitTransactionToBackStack(@Nullable String tag){
        if ( tag == null ) tag = "";
        mStack.addLast(new StackEntry(tag));
    }

    @Override
    public void onBackStackChanged(){
        int a = mStack.size();
        int b = mManager.getBackStackEntryCount();
        Log.d("FragmentManager", "size:" + b);
        if ( b > 0 ) Log.d("FragmentManager", "fragment:" + mManager.getBackStackEntryAt(b-1));
        if ( a > b ){
            // if any back stack popped, remove it and callback to fragment/activity
            while ( mStack.size() > b ){
                StackEntry entry = mStack.pollLast();
                if ( mFragment != null ) mFragment.onBackStackPop(entry.tag);
                if ( mActivity != null ) mActivity.onBackStackPop(entry.tag);
            }
        }else if ( a < b ){
            throw new RuntimeException("stack size mismatched");
        }
    }

}
