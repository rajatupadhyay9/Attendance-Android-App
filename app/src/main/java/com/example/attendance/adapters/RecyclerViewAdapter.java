package com.example.attendance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendance.R;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private List<List<String>> data;
    private LayoutInflater inflater;

    public RecyclerViewAdapter(Context context, List<List<String>> data) {
        this.data = data;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.table_rowview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        List<String> vals = data.get(position);
        holder.textView1.setText(vals.get(0));
        holder.textView2.setText(vals.get(1));
        holder.textView3.setText(vals.get(2));
        holder.textView4.setText(vals.get(3));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView1, textView2, textView3, textView4;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView4 = itemView.findViewById(R.id.rowtextView4);
            textView1 = itemView.findViewById(R.id.rowtextView1);
            textView2 = itemView.findViewById(R.id.rowtextView2);
            textView3 = itemView.findViewById(R.id.rowtextView3);
        }
    }
}
