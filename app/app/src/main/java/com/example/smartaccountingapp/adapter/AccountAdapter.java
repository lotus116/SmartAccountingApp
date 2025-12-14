package com.example.smartaccountingapp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartaccountingapp.R;
import com.example.smartaccountingapp.model.Account;
import java.util.List;
import java.util.Locale;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {

    private final List<Account> accountList;
    private final Context context;
    private OnItemClickListener listener;

    // 【修改】接口：增加 onItemClick 方法，用于编辑功能
    public interface OnItemClickListener {
        void onDeleteClick(int accountId); // 点击删除图标
        void onItemClick(Account account); // 点击列表项
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public AccountAdapter(Context context, List<Account> accountList) {
        this.context = context;
        this.accountList = accountList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Account account = accountList.get(position);

        holder.tvCategory.setText(account.getCategory());
        holder.tvNote.setText(account.getNote());
        holder.tvDate.setText(account.getDate());

        // 根据类型设置金额显示颜色和符号
        if (account.getType().equals("支出")) {
            holder.tvAmount.setTextColor(Color.RED);
            holder.tvAmount.setText(String.format(Locale.getDefault(), "-%.2f", account.getAmount()));
        } else {
            holder.tvAmount.setTextColor(Color.GREEN);
            holder.tvAmount.setText(String.format(Locale.getDefault(), "+%.2f", account.getAmount()));
        }

        // 设置删除点击事件
        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(account.getId());
            }
        });

        // 【新增】设置列表项点击事件 (用于编辑)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(account);
            }
        });

        // 示例：根据类别设置图标
        holder.ivIcon.setImageResource(getCategoryIcon(account.getCategory()));
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    /**
     * 【新增】用于在 MainActivity 筛选或编辑后更新 RecyclerView 数据
     */
    public void updateData(List<Account> newAccountList) {
        this.accountList.clear();
        this.accountList.addAll(newAccountList);
        notifyDataSetChanged();
    }

    // 示例：获取类别图标 (您需要在 drawable 文件夹中添加图标资源)
    private int getCategoryIcon(String category) {
        switch (category) {
            case "餐饮": return R.drawable.ic_food;
            case "交通": return R.drawable.ic_transport;
            case "购物": return R.drawable.ic_shopping;
            case "学习": return R.drawable.ic_study;
            case "娱乐": return R.drawable.ic_entertainment;
            default: return R.drawable.ic_other;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCategory;
        final TextView tvNote;
        final TextView tvDate;
        final TextView tvAmount;
        final ImageView ivDelete; // 删除按钮 (ImageView)
        final ImageView ivIcon; // 类别图标 (ImageView)

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            ivDelete = itemView.findViewById(R.id.iv_delete);
            ivIcon = itemView.findViewById(R.id.iv_icon);
        }
    }
}