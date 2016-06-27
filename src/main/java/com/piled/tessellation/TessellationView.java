package com.piled.tessellation;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;

public class TessellationView extends GridView implements AdapterView.OnItemClickListener {
    private final static String TAG = "tessellation";
    private MasterAdapter mData;
    AdapterView.OnItemClickListener mOnItemClickListener;
    private int mRowCount = 3;

    public TessellationView(Context context) {
        this(context, null);
    }

    public TessellationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TessellationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        //setOnItemClickListener(this);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        //Log.d(TAG, "setAdapter()");
        ListAdapter data = adapter;
        super.setOnItemClickListener(this);
        mData = null;
        if (adapter != null && adapter.getCount() > 0) {
            Object item = adapter.getItem(0);
            if (item != null && item instanceof BaseAdapter) {
                mData = new MasterAdapter(adapter, mRowCount, getContext());
                super.setOnItemClickListener(mData);
                mData.setOnItemClickListener(mOnItemClickListener);
                data = mData;
            }
        }
        super.setNumColumns((mData != null) ? 1 : mRowCount);
        super.setAdapter(data);
    }
    
    @Override
    public void setNumColumns(int columns) {
        mRowCount = columns;
        if (mData != null && mData instanceof MasterAdapter) {
            mData.setNumColumns(columns);
            super.setNumColumns(1);
            requestLayout();
        } else {
            super.setNumColumns(columns);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.d(TAG, "onItemClick(): #" + id + " at " + position);
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(parent, view, position, id);
        }
    }

    public void setMasterAdapter(ListAdapter adapter) {
        //Log.d(TAG, "setAdapter()");
        mData = new MasterAdapter(adapter, mRowCount, getContext());
        super.setOnItemClickListener(mData);
        mData.setOnItemClickListener(mOnItemClickListener);
        super.setAdapter(mData);
    }

    public void setRowCount(int count) {
        mRowCount = count;
        //if (mData != null) {
            //mData.setRowCount(mRowCount);
        //}
    }

    @Override
    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        mOnItemClickListener = listener;
        if (mData != null) {
            mData.setOnItemClickListener(mOnItemClickListener);
        }
    }

    private float mLastX;
    private int mAddon;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            int index = event.findPointerIndex(event.getPointerId(0));
            if (index >= 0) {
                mLastX = event.getX(index);
                mAddon = (int)(mLastX * mRowCount / getWidth());
                if (mData != null) {
                    mData.setAdjustment(mLastX / getWidth());
                }
                //Log.i(TAG, "onInterceptTouchEvent() adjust is " + mAddon);
            } else {
                //Log.i(TAG, "onInterceptTouchEvent() index is negative");
            }
        }
        try {
            return super.onInterceptTouchEvent(event);
        } catch (IllegalArgumentException ex) {
            //Log.i(TAG, "Caught bogus pointer index from ViewPage");
        }
        return false;
    }
}
