package com.piled.tessellation;

import java.util.ArrayList;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

public class MasterAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
    private static final String TAG = "tessellation";
    private static final long ADAPTER_PREFIX = 1000000l;
    private ListAdapter mMaster;
    private int[] mOffsets;
    private int mTotalCount;
    private Context mContext;
    private int mRowCount;
    private AdapterView.OnItemClickListener mOnItemClickListener;
    private ArrayList<BaseAdapter> mList;

    public MasterAdapter(ListAdapter master, int rowCount, Context context) {
        mMaster = master;
        if (mMaster == null) {
            Log.w(TAG, "MasterAdapter(): master adapter is null");
            return;
        }
        mList = new ArrayList<BaseAdapter>(mMaster.getCount());
        for (int i = 0; i < mMaster.getCount(); i++) {
            Object item = mMaster.getItem(i);
            if (item != null && item instanceof BaseAdapter) {
                mList.add((BaseAdapter)item);
            } else {
                Log.w(TAG, "MasterAdapter(): item #" + i + " don't implement required BaseAdapter");
            }
        }
        mRowCount = rowCount;
        mContext = context;
        calculateOffsets();
        for (int i = 0; i < mList.size(); i++) {
            mList.get(i).registerDataSetObserver(mObserver);
        }
    }
    
    public void setNumColumns(int columns) {
        mRowCount = columns;
        calculateOffsets();
    }

    @Override
    protected void finalize() throws Throwable {
        if (mList == null) {
            return;
        }
        for (int i = 0; i < mList.size(); i++) {
            mList.get(i).unregisterDataSetObserver(mObserver);
        }
    }

    @Override
    public int getCount() {
        if (mOffsets == null || mOffsets.length == 0) {
            return 0;
        }
        return mOffsets[mOffsets.length - 1];
    }

    @Override
    public Object getItem(int position) {
        try {
            int adapter = findAdapter(position);
            if (adapter < 0 || mList == null) {
                return null;
            }
            if (adapter > 0 && mOffsets != null) {
                position -= mOffsets[adapter - 1];
            }
            if (position == 0) {
                return null;
            }
            position--;
            return mList.get(adapter).getItem(position * mRowCount);
        } catch (Exception e) {
            Log.e(TAG, "getItem(" + position +"): ", e);
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        try {
            int adapter = findAdapter(position);
            if (adapter < 0 || mList == null) {
                return 0l;
            }
            if (adapter > 0 && mOffsets != null) {
                position -= mOffsets[adapter - 1];
            }
            if (position == 0) {
                return 0l;
            }
            position--;
            return mList.get(adapter).getItemId(position * mRowCount) + adapter * ADAPTER_PREFIX;
        } catch (Exception e) {
            Log.e(TAG, "getItemId(" + position +"): ", e);
            return 0l;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 1;
        }
        for (int i = 0; mOffsets != null && i < mOffsets.length - 1; i++) {
            if (position == mOffsets[i]) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        try {
            //Log.i(TAG, "getView(): " + position);
            int adapter = findAdapter(position);
            //Log.i(TAG, "getView(): adapter = " + adapter);
            if (adapter < 0 || mList == null || mMaster == null) {
                return null;
            }
            if (adapter > 0 && mOffsets != null) {
                position -= mOffsets[adapter - 1];
            }
            if (position == 0) {
                return (mMaster != null) ? mMaster.getView(adapter, convertView, parent) : null;
            }
            BaseAdapter baseAdapter = mList.get(adapter);
            if (baseAdapter == null) {
                return null;
            }
            position--;
            LinearLayout ll = (convertView != null && convertView instanceof LinearLayout) ? (LinearLayout)convertView : new LinearLayout(mContext);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            int rowCount = baseAdapter.getCount() - position * mRowCount;
            if (rowCount > mRowCount) {
                rowCount = mRowCount;
            }
            //Log.i(TAG, "getView(): update row count: " + rowCount);
            int existingViews = ll.getChildCount();
            if (existingViews > rowCount) {
                //Log.i(TAG, "getView(): removing: " + (existingViews - rowCount));
                for (int i = existingViews - 1; i >= rowCount; i--) {
                    View v = ll.getChildAt(i);
                    if (v != null) {
                        ll.removeView(v);  ///?
                    }
                }
                existingViews = rowCount;
            }
            for (int i = 0; i < existingViews; i++) {
                //Log.i(TAG, "getView(): update(replace): " + (position * mRowCount + i));
                baseAdapter.getView(position * mRowCount + i, ll.getChildAt(i), ll);
            }
            for (int i = existingViews; i < rowCount; i++) {
                //Log.i(TAG, "getView(): update(new): " + (position * mRowCount + i));
                View v = baseAdapter.getView(position * mRowCount + i, null, ll);
                if (v != null) {
                    //Log.i(TAG, "getView(): update(added): " + i);
                    ViewGroup p = (ViewGroup)v.getParent();
                    if (p != null) {
                        p.removeView(v);
                    }
                    ll.addView(v);
                }
            }
            return ll;
        } catch (Exception e) {
            Log.e(TAG, "getView()", e);
            return getView(position, null, parent);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.d(TAG, "onItemClick(): #" + id + " at " + position);
        if (mOnItemClickListener == null) {
            //Log.d(TAG, "onItemClick(): mOnItemClickListener is null");
            return;
        }
        int adapter = findAdapter(position);
        if (adapter < 0) {
            //Log.d(TAG, "onItemClick(): can't find adapter");
            return;
        }
        if (adapter > 0 && mOffsets != null) {
            position -= mOffsets[adapter - 1];
        }
        if (position == 0) { // header's click
            //Log.d(TAG, "onItemClick(): header's click");
            return;
        }
        position--;
        if (view instanceof LinearLayout) {
            LinearLayout ll = (LinearLayout)view;
            if (ll.getChildCount() > mAddon) {
                view = ll.getChildAt(mAddon);
                mOnItemClickListener.onItemClick(parent, view, position * mRowCount + mAddon, adapter);
            }
        }
        //Log.d(TAG, "onItemClick(): converted to #" + adapter + " at " + (position * mRowCount + mAddon));
    }
    
    private int mAddon;
    public void setAdjustment(float addon) {
        mAddon = (int)(addon * mRowCount);
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public long getAdapterPosition(int position, int addon) {
        //Log.i(TAG, "getAdapter(): " + position);
        int adapter = findAdapter(position);
        if (adapter < 0) {
            return -1;
        }
        if (adapter > 0 && mOffsets != null) {
            position -= mOffsets[adapter - 1];
        }
        if (position == 0) {
            return -1;
        }
        position--;
        return mList.get(adapter).getItemId(position * mRowCount + addon);
    }

    private void calculateOffsets() {
        //mTotalCount = 0;
        int accumulator = 0;
        int[] offsets = new int[mList.size()];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = (mList.get(i).getCount() + mRowCount - 1) / mRowCount + 1 + accumulator;
            accumulator = offsets[i];
        }
        mOffsets = offsets;
    }

    private int findAdapter(int position) {
        for (int i = 0; mOffsets != null && i < mOffsets.length; i++) {
            if (position < mOffsets[i]) {
                return i;
            }
        }
        // really bad
        Log.i(TAG, "findAdapter(): not found for " + position);
        return -1;
    }

    private DataSetObserver mObserver = new DataSetObserver() {
        public void onChanged() {
            notifyDataSetChanged();
        }
        public void onInvalided() {
            notifyDataSetInvalidated();
        }
    };

}
