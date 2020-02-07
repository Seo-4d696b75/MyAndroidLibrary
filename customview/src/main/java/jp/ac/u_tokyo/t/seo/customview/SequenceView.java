package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Seo-4d696b75
 * @version 2018/01/07.
 */

public class SequenceView extends ExpandableListView{

    public SequenceView(Context context){
        super(context);
    }

    public SequenceView(Context context, AttributeSet set){
        super(context, set);
    }

    public SequenceView(Context context, AttributeSet set, int defaultStyle){
        super(context, set, defaultStyle);
    }

    private SequenceAdapter mAdapter;

    public void setAdapter(SequenceAdapter adapter){
        mAdapter = adapter;
        super.setAdapter(adapter);
    }

    public void freeAdapter(){
        mAdapter = null;
    }

    public void setItemCheckable(boolean checkable){
        if ( mAdapter != null ){
            mAdapter.onCheckableChange(checkable);
        }
    }

    public void setItemChecked(View view, boolean isChecked){
        if ( mAdapter != null ){
            mAdapter.onItemChecked(view, isChecked);
        }
    }

    public List getCheckedItems(){
        if ( mAdapter == null ){
            return null;
        }else{
            return mAdapter.getCheckedItems();
        }
    }

    public void setItemChecked(int groupPosition, int childPosition, boolean isChecked){
        if ( mAdapter != null ){
            mAdapter.setItemChecked(groupPosition, childPosition, isChecked);
        }
    }

    public void setItemChecked(Object item, boolean isChecked){
        if ( mAdapter != null ){
            mAdapter.setItemChecked(item, isChecked);
        }
    }

    public static abstract class SequenceAdapter<E, V extends View> extends BaseExpandableListAdapter{

        private final List<E> mGroups;
        private boolean mCheckable = false;
        private Set<Integer> mCheckedID;
        private SparseArray<V> mViewMap;
        private final int CHILD_ID_PARENT = 0xffff;

        public SequenceAdapter(List<E> entries){
            mGroups = entries;
            mViewMap = new SparseArray<>();
        }

        /**
         * 各要素が選択可能状態かどうか取得する
         * @see #setItemCheckable(boolean)
         */
        public boolean isCheckable(){
            return  mCheckable;
        }

        protected abstract E getChild(E group, int position);

        protected abstract int getChildrenCnt(E group);

        /**
         * List表示するＵＩオブジェクトを生成する.
         * @param item 表示する要素
         * @param groupPosition [0-(要素の数-1)]
         * @param childPosition グループのparentの場合は負数、childの場合は[0-(childの数-1)]
         * @param convertView nullでない場合は再利用しましょう
         * @return
         */
        protected abstract V getView(E item, int groupPosition, int childPosition, V convertView);

        /**
         * 各要素の選択可能状態が変更されたときに処理を行う.
         * {@link #isCheckable()} == true　の状態で{@link #getView(Object, int, int, View)}が呼ばれた直後、
         * または{@link #onCheckableChange(boolean)}で状態が変更されたときに呼ばれる.
         * {@link #onCheckableChange(boolean)} から各Viewごとにコールされる
         * @param checkable 選択できるかどうか
         * @param item 要素オブジェクト
         * @param view 要素を表現しているＵＩオブジェクト
         */
        protected abstract void onItemCheckableChange(boolean checkable, E item, V view);

        /**
         *
         * @param isChecked 要素が現在選択されているか この属性はAdapterが管理しておりViewの再利用を通じて正しく保持されています
         */
        protected abstract void onItemCheckedChange(boolean isChecked, E item, V view);

        /**
         * 指定した要素を選択します.
         * {@code Object#equals(item)}がtrueを返すような要素がこのアダプターに存在するとき、
         * その位置の要素を{@code isChecked}で指定した選択状態に変更します.
         * 該当する要素が存在しない場合は変更がありません.
         * @param item 選択した要素
         * @param isChecked 選択するかどうか
         */
        public void setItemChecked(Object item, boolean isChecked){
            if ( mCheckable && item != null){
                for ( int i = 0; i < getGroupCount(); i++ ){
                    E parent = mGroups.get(i);
                    if ( parent.equals(item) ){
                        onItemChecked(getItemID(i, CHILD_ID_PARENT), isChecked);
                        return;
                    }
                    for ( int j = 0; j < getChildrenCnt(parent); j++ ){
                        if ( getChild(parent, j).equals(item) ){
                            onItemChecked(getItemID(i, j), isChecked);
                            return;
                        }
                    }
                }
            }
        }

        /**
         * 指定した位置の要素を選択します.
         * @param groupPosition 親グループの位置
         * @param childPosition 親グループ内の位置 親要素を指定するときは負数
         * @param isChecked 要素の選択
         */
        public void setItemChecked(int groupPosition, int childPosition, boolean isChecked){
            if ( mCheckable ){
                if ( groupPosition >= 0 && groupPosition < getGroupCount() && childPosition < getChildrenCount(groupPosition) ){
                    int id = getItemID(groupPosition, childPosition < 0 ? CHILD_ID_PARENT : childPosition);
                    onItemChecked(id, isChecked);
                    V view = mViewMap.get(id);
                    if ( view != null ){
                        E group = mGroups.get(groupPosition);
                        E item = groupPosition < 0 ? group : getChild(group, childPosition);
                        onItemCheckedChange(isChecked, item, view);
                    }
                }
            }
        }

        /**
         * 各要素が選択可能な状態である要素が選択されたとき.
         * 各要素を表しているViewが選択された時はここに通知しましょう.
         * そうすることで、選択された要素をAdapterが記憶してリストをスクロールしても正しく選択の状態が反映されます.
         * @param view 選択された要素を表現しているUI {@link #getView(Object, int, int, View)} で返したオブジェクト
         * @param isChecked 選択されたのかどうか
         */
        @CallSuper
        protected void onItemChecked(View view, boolean isChecked){
            if ( mCheckable && view.getTag() != null ){
                Integer id = (Integer)view.getTag();
                onItemChecked(id, isChecked);
            }
        }

        private void onItemChecked(Integer id, boolean isChecked){
            if ( isChecked ){
                mCheckedID.add(id);
            }else{
                mCheckedID.remove(id);
            }
        }

        /**
         * 選択可能状態が変更されたときに処理を行う
         * @param checkable 選択できる？
         */
        protected void onCheckableChange(boolean checkable){
            mCheckable = checkable;
            if ( checkable ){
                mCheckedID = new HashSet<>();
            }else{
                mCheckedID = null;
            }
            for ( int i=0 ; i<mViewMap.size() ; i++ ){
                int key = mViewMap.keyAt(i);
                int groupPosition = key >>> 16;
                int childPosition = key & 0xffff;
                E group = mGroups.get(groupPosition);
                E item = childPosition == CHILD_ID_PARENT ? group : getChild(group, childPosition);
                onItemCheckableChange(checkable, item, mViewMap.get(key));
            }
        }

        public List<E> getCheckedItems(){
            if ( mCheckable ){
                List<E> list = new ArrayList<>();
                final Map<Object, Integer> map = new HashMap<>();
                for ( int key : mCheckedID ){
                    int groupPosition = key >>> 16;
                    int childPosition =  key & 0xffff;
                    E group = mGroups.get(groupPosition);
                    E item = childPosition == CHILD_ID_PARENT ? group : getChild(group, childPosition);
                    list.add(item);
                    map.put(item, key);
                }
                Collections.sort(list, new Comparator<E>(){
                    @Override
                    public int compare(E o1, E o2){
                        int id1 = map.get(o1);
                        int id2 = map.get(o2);
                        if ( (id1 >>> 16) == (id2 >>> 16) ){
                            id1 = id1 & 0xfff;
                            id2 = id2 & 0xfff;
                            return id1 == id2 ? 0 : (id1 < id2 ? -1 : 1);
                        }else{
                            return (id1 >>> 16) < (id2 >>> 16) ? -1 : 1;
                        }
                    }
                });
                return list;
            }else{
                return null;
            }
        }

        @Override
        public Object getGroup(int position){
            return mGroups.get(position);
        }

        @Override
        public int getGroupCount(){
            return mGroups.size();
        }

        @Override
        public int getChildrenCount(int position){
            return getChildrenCnt(mGroups.get(position));
        }

        @Override
        public Object getChild(int groupPosition, int childPosition){
            return getChild(mGroups.get(groupPosition), childPosition);
        }

        @Override
        public long getGroupId(int groupPosition){
            return getItemID(groupPosition, CHILD_ID_PARENT);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition){
            return getItemID(groupPosition, childPosition);
        }

        private int getItemID(int groupPosition, int childPosition){
            return (groupPosition << 16 ) | (childPosition & 0xffff);
        }

        @Override
        public boolean hasStableIds(){
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition){
            return true;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent){
            return getViewInternal(getChild(mGroups.get(groupPosition), childPosition), groupPosition, childPosition, convertView);
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent){
            return getViewInternal(mGroups.get(groupPosition), groupPosition, -1, convertView);
        }

        /**
         * {@link #getChildView(int, int, boolean, View, ViewGroup)}, {@link #getGroupView(int, boolean, View, ViewGroup)}
         * をまとめて実装
         * @param item
         * @param groupPosition
         * @param childPosition {@link #getGroupView(int, boolean, View, ViewGroup)}から呼ぶときは負数にしくりゃれ
         * @param convertView
         * @return サブクラスの{@link #getView(Object, int, int, View, boolean, boolean)}の実装による
         */
        private View getViewInternal(E item, int groupPosition, int childPosition, View convertView){
            Integer id = getItemID(groupPosition, childPosition < 0 ? CHILD_ID_PARENT : childPosition);
            V convert = null;
            if ( convertView != null ){
                Integer tag = (Integer)convertView.getTag();
                if ( mViewMap.indexOfKey(tag) >= 0 ){
                    convert = mViewMap.get(tag);
                    convert.setTag(null);
                    mViewMap.remove(tag);
                }
            }
            V view = getView(item, groupPosition, childPosition, convert);
            view.setTag(id);
            mViewMap.put(id, view);
            onItemCheckableChange(mCheckable, item, view);
            if ( mCheckable ){
                onItemCheckedChange(mCheckedID.contains(id), item, view);
            }
            return view;
        }
    }
}
