package com.github.aakumykov.yandex_disk_client_demo;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MyViewHolder extends RecyclerView.ViewHolder {

    private final TextView mTextView;

    public MyViewHolder(@NonNull View itemView) {
        super(itemView);
        mTextView = itemView.findViewById(R.id.textView);
    }

    public void fillWith(String s) {
        mTextView.setText(s);
    }
}
