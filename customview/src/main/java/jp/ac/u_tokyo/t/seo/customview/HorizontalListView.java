package jp.ac.u_tokyo.t.seo.customview;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Seo-4d696b75
 * @version 2018/10/31.
 */

public class HorizontalListView extends RecyclerView{

    public interface OnItemClickListener{
        void onItemClick(View view, int position);
    }

    public HorizontalListView(Context context){
        super(context);
        initialize(context);
    }

    public HorizontalListView(Context context, AttributeSet set){
        super(context, set);
        initialize(context);
    }

    public HorizontalListView(Context context, AttributeSet set, int defaultAttr){
        super(context, set, defaultAttr);
        initialize(context);
    }

    private void initialize(Context context){
        LinearLayoutManager manager = new LinearLayoutManager(context);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        setLayoutManager(manager);
    }

    private OnItemClickListener mListener;

    public void setOnItemClickListener(OnItemClickListener listener){
        mListener = listener;
        Adapter adapter = getAdapter();
        if ( adapter instanceof ArrayAdapter ){
            ArrayAdapter a = (ArrayAdapter)adapter;
            a.mListener = this.mListener;
        }
    }

    @Override
    public void setAdapter(Adapter adapter){
        super.setAdapter(adapter);
        if ( adapter instanceof ArrayAdapter ){
            ArrayAdapter a = (ArrayAdapter)adapter;
            a.mListener = this.mListener;
        }
    }

    @Override
    protected void onDetachedFromWindow(){
        super.onDetachedFromWindow();
        setOnItemClickListener(null);
        setAdapter(null);
        setLayoutManager(null);
    }

    private static class SimpleViewHolder extends ViewHolder{

        private SimpleViewHolder(View view){
            super(view);
        }


    }

    public static abstract class ArrayAdapter<E> extends Adapter<ViewHolder> {

        public ArrayAdapter(List<E> list){
            mDataList = new ArrayList<>(list.size());
            mViews = new ArrayList<>(list.size());
            mDataList.addAll(list);
        }

        private List<E> mDataList;
        private OnItemClickListener mListener;
        private List<View> mViews;

        /**
         * リスト要素となるViewをインスタンス化する
         * @param parent
         * @return null not acceptable
         */
        public abstract View getView(ViewGroup parent);

        /**
         * リストに表示するデータをViewへ反映する
         * @param view 反映先のView
         * @param data 反映させるデータ{@link #getItem(int)}でも取得可能
         * @param position リスト上での位置
         */
        public abstract void onBindView(View view, E data, int position);

        @Override
        public final ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
            return new SimpleViewHolder(getView(parent));

        }


        @Override
        public final void onBindViewHolder(final ViewHolder holder, int position){

            final View view = holder.itemView;
            E data = getItem(position);
            onBindView(view, data, position);

            mViews.add(holder.itemView);
            holder.itemView.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v){
                    if ( mListener != null ){
                        mListener.onItemClick(view, holder.getAdapterPosition());
                    }
                }
            });
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView){
            super.onDetachedFromRecyclerView(recyclerView);
            for ( View view : mViews ) view.setOnClickListener(null);
            mViews = null;
            mListener = null;
            mDataList = null;
        }

        @Override
        public final int getItemCount(){
            return mDataList.size();
        }

        public E getItem(int position){
            return mDataList.get(position);
        }

    }

}
