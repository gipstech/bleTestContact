package com.gipstech.bletestcontact;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;
import androidx.recyclerview.widget.SortedListAdapterCallback;

import java.util.Locale;

/**
 * An adapter to display ble scans avoiding duplicates
 */
public class RssiListAdapter extends RecyclerView.Adapter<RssiListAdapter.ViewHolder>
{
    static final int RSSI_THRESHOLD = -65;
    static final double R0 = -65;
    static final double N = 2;

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        public TextView tvName;
        public TextView tvLevel;

        public ViewHolder(View v, TextView name, TextView level)
        {
            super(v);

            tvName = name;
            tvLevel = level;
        }
    }

    static class ScanValue
    {
        String uuid;
        int rssi;
    }

    Context context;
    ColorStateList defColors;
    SortedList<ScanValue> dataset;

    public RssiListAdapter(Context context)
    {
        this.context = context;

        // We use a sorted list to avoid duplicates
        dataset = new SortedList<>(ScanValue.class, new SortedListAdapterCallback<ScanValue>(this)
        {
            @Override
            public int compare(ScanValue o1, ScanValue o2)
            {
                return o1.uuid.compareTo(o2.uuid);
            }

            @Override
            public boolean areContentsTheSame(ScanValue oldItem, ScanValue newItem)
            {
                return oldItem.rssi == newItem.rssi;
            }

            @Override
            public boolean areItemsTheSame(ScanValue item1, ScanValue item2)
            {
                return item1.uuid.equals(item2.uuid);
            }
        });
    }

    public void add(ScanResult result)
    {
        String uuid = Utils.getEddystoneUid(result.getScanRecord().getBytes());

        if (uuid != null)
        {
            ScanValue value = new ScanValue();
            value.rssi = result.getRssi();
            value.uuid = uuid;
            dataset.add(value);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(context);
        tvName.setTypeface(Typeface.MONOSPACE);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvLevel = new TextView(context);
        tvLevel.setTypeface(Typeface.MONOSPACE);
        defColors = tvLevel.getTextColors();

        layout.addView(tvName);
        layout.addView(tvLevel);

        return new ViewHolder(layout, tvName, tvLevel);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        ScanValue scan = dataset.get(position);

        // Convert RSSI to distance in meters
        double distance = Math.pow(10, (R0 - scan.rssi) / (N * 10));

        holder.tvName.setText(scan.uuid);
        holder.tvName.setBackgroundColor(0x00);
        holder.tvLevel.setText(String.format(Locale.US, "%d %.1fm", scan.rssi, distance));

        if (scan.rssi > RSSI_THRESHOLD)
        {
            holder.tvLevel.setTextColor(0xFFFF0000);
        }
        else
        {
            holder.tvLevel.setTextColor(defColors);
        }
    }

    @Override
    public int getItemCount()
    {
        return dataset.size();
    }
}