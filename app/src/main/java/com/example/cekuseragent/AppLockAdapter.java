package com.example.cekuseragent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class AppLockAdapter extends RecyclerView.Adapter<AppLockAdapter.ViewHolder> {
    public interface OnLockToggleListener {
        void onLockToggled(AppInfoItem item, boolean isLocked);
    }

    private final List<AppInfoItem> fullList;
    private final List<AppInfoItem> filteredList;
    private final OnLockToggleListener listener;

    public AppLockAdapter(List<AppInfoItem> list, OnLockToggleListener listener) {
        this.fullList = new ArrayList<>(list);
        this.filteredList = new ArrayList<>(list);
        this.listener = listener;
    }

    public void updateData(List<AppInfoItem> newList) {
        fullList.clear();
        fullList.addAll(newList);
        filteredList.clear();
        filteredList.addAll(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String lower = query.toLowerCase().trim();
            for (AppInfoItem item : fullList) {
                if (item.getAppName().toLowerCase().contains(lower) ||
                    item.getPackageName().toLowerCase().contains(lower)) {
                    filteredList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_lock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfoItem item = filteredList.get(position);
        holder.tvAppName.setText(item.getAppName());
        holder.tvPackageName.setText(item.getPackageName());
        holder.ivAppIcon.setImageDrawable(item.getIcon());

        holder.switchLock.setOnCheckedChangeListener(null);
        holder.switchLock.setChecked(item.isChecked());
        holder.switchLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked);
            if (listener != null) {
                listener.onLockToggled(item, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> holder.switchLock.toggle());
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName;
        TextView tvPackageName;
        SwitchMaterial switchLock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvPackageName = itemView.findViewById(R.id.tvPackageName);
            switchLock = itemView.findViewById(R.id.switchLock);
        }
    }
}
