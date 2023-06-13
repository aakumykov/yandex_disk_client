package com.github.aakumykov.yandex_disk_client_demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.github.aakumykov.yandex_disk_client.CloudClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyListAdapter extends ListAdapter<String,MyViewHolder> {

    protected MyListAdapter(@NonNull DiffUtil.ItemCallback<String> diffCallback) {
        super(diffCallback);
    }

    public MyListAdapter(@NonNull AsyncDifferConfig<String> config) {
        super(config);
    }


    @NonNull @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final String item = getItem(position);
        holder.fillWith(item);
    }


    public void appendList(List<String> addedList, @NonNull CloudClient.SortingMode sortingMode) {
        final List<String> list = new ArrayList<>(getCurrentList());
        list.addAll(addedList);
        sortList(list, sortingMode);
        submitList(list);
    }

    private void sortList(List<String> list, CloudClient.SortingMode sortingMode) {
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                switch (sortingMode) {
                    case NAME_DIRECT:
                    case C_TIME_FROM_OLD_TO_NEW:
                        return o1.compareTo(o2);
                    case NAME_REVERSE:
                    case C_TIME_FROM_NEW_TO_OLD:
                        return o2.compareTo(o1);
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
        final List<String> currentList = new ArrayList<>(getCurrentList());
        sortList(currentList, sortingMode);
        submitList(currentList);
    }
}
