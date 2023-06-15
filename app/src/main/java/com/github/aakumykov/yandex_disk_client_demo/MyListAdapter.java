package com.github.aakumykov.yandex_disk_client_demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.github.aakumykov.yandex_disk_client.CloudClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyListAdapter extends ListAdapter<DiskItem,MyViewHolder> {

    private ItemClickListener mItemClickListener;

    protected MyListAdapter(@NonNull DiffUtil.ItemCallback<DiskItem> diffCallback,
                            @NonNull ItemClickListener itemClickListener) {
        super(diffCallback);
        mItemClickListener = itemClickListener;
    }


    @NonNull @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final DiskItem diskItem = getItem(position);
        holder.fillWith(diskItem);
        holder.itemView.setOnClickListener(v -> mItemClickListener.onItemClicked(diskItem));
        holder.itemView.setOnLongClickListener(v -> mItemClickListener.onItemLongClicked(diskItem));
    }


    public void appendList(List<DiskItem> addedList, @NonNull CloudClient.SortingMode sortingMode) {
        final List<DiskItem> list = new ArrayList<>(getCurrentList());
        list.addAll(addedList);
        sortList(list, sortingMode);
        submitList(list);
    }

    private void sortList(List<DiskItem> list, CloudClient.SortingMode sortingMode) {
        Collections.sort(list, new Comparator<DiskItem>() {
            @Override
            public int compare(DiskItem o1, DiskItem o2) {
                switch (sortingMode) {
                    case NAME_DIRECT:
                        return o1.name.compareTo(o2.name);
                    case NAME_REVERSE:
                        return o2.name.compareTo(o1.name);
                    case C_TIME_FROM_OLD_TO_NEW:
                        return Long.compare(o1.cTime, o2.cTime);
                    case C_TIME_FROM_NEW_TO_OLD:
                        return Long.compare(o2.cTime, o1.cTime);
                    default:
                        throw new IllegalArgumentException("Неизвестное значение: "+sortingMode);
                }
            }
        });
    }


    public void clearList() {
        submitList(new ArrayList<>());
    }


    public void setSortingMode(final CloudClient.SortingMode sortingMode) {
        final List<DiskItem> currentList = new ArrayList<>(getCurrentList());
        sortList(currentList, sortingMode);
        submitList(currentList);
    }
}
