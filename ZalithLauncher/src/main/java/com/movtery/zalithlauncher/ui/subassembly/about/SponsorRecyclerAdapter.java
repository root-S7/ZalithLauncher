package com.movtery.zalithlauncher.ui.subassembly.about;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.databinding.ItemSponsorViewBinding;

import java.util.List;

public class SponsorRecyclerAdapter extends RecyclerView.Adapter<SponsorRecyclerAdapter.Holder> {
    private final List<SponsorItemBean> mData;

    public SponsorRecyclerAdapter(List<SponsorItemBean> items) {
        this.mData = items;
    }

    @NonNull
    @Override
    public SponsorRecyclerAdapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemSponsorViewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SponsorRecyclerAdapter.Holder holder, int position) {
        holder.setData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        if (mData != null) {
            return mData.size();
        }
        return 0;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateItems(List<SponsorItemBean> items) {
        mData.clear();
        mData.addAll(items);
        notifyDataSetChanged();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        private final ItemSponsorViewBinding binding;

        public Holder(@NonNull ItemSponsorViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        public void setData(SponsorItemBean itemBean) {
            float amount = itemBean.getAmount();

            binding.nameView.setText(itemBean.getName());
            binding.timeView.setText(itemBean.getTime());
            binding.amountView.setText(String.format("￥%s", amount));

            Drawable background = itemView.getBackground();
            if (amount >= 12.0f) {
                int color;
                if (amount >= 18.0f) {
                    color = ContextCompat.getColor(itemView.getContext(), R.color.background_sponsor_advanced);
                } else {
                    color = ContextCompat.getColor(itemView.getContext(), R.color.background_sponsor_intermediate);
                }
                background.setTint(color);
            } else {
                background.setTintList(null);
            }
        }
    }
}
