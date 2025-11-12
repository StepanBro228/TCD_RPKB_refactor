package com.step.tcd_rpkb.UI.movelist.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
// import androidx.recyclerview.widget.DiffUtil; // Больше не нужен здесь
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.step.tcd_rpkb.UI.movelist.activity.MoveListActivity;
import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.UI.movelist.adapters.MoveListAdapter;
import com.step.tcd_rpkb.UI.movelist.viewmodel.MoveListViewModel;
// import com.step.tcd_rpkb.utils.MoveDiffCallback; // Больше не нужен здесь

import java.util.ArrayList;
import java.util.List;

public class MoveListFragment extends Fragment implements MoveListAdapter.OnSelectionChangeListener, MoveListAdapter.OnMoveItemClickListener {
    
    private RecyclerView recyclerView;
    private MoveListAdapter adapter;
    private String status;
    
    private MoveListViewModel moveListViewModel;
    
    // Константы для статусов, чтобы избежать опечаток при сравнении
    private static final String STATUS_FORMIROVAN = "Сформирован";
    private static final String STATUS_KOMPLEKTUETSA = "Комплектуется";
    private static final String STATUS_PODGOTOVLEN = "Подготовлен";
    
    public static MoveListFragment newInstance(String status) {
        MoveListFragment fragment = new MoveListFragment();
        Bundle args = new Bundle();
        args.putString("status", status);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            status = getArguments().getString("status");
        }
        
        moveListViewModel = new ViewModelProvider(requireActivity()).get(MoveListViewModel.class);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_move_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        setupRecyclerView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeViewModel();
    }
    
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setItemPrefetchEnabled(true);
        layoutManager.setInitialPrefetchItemCount(15);
        
        adapter = new MoveListAdapter(getContext());
        adapter.setSelectionChangeListener(this);
        adapter.setOnMoveItemClickListener(this);
        
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(30);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void observeViewModel() {
        if (STATUS_FORMIROVAN.equals(status)) {
            moveListViewModel.filteredFormirovanList.observe(getViewLifecycleOwner(), newList -> {
                if (newList != null) {
                    updateAdapterData(newList);
                }
            });
        } else if (STATUS_KOMPLEKTUETSA.equals(status)) {
            moveListViewModel.filteredKomplektuetsaList.observe(getViewLifecycleOwner(), newList -> {
                if (newList != null) {
                    updateAdapterData(newList);
                }
            });
        } else if (STATUS_PODGOTOVLEN.equals(status)) {
            moveListViewModel.filteredPodgotovlenList.observe(getViewLifecycleOwner(), newList -> {
                if (newList != null) {
                    updateAdapterData(newList);
                }
            });
        }
    }

    /**
     * Обновляет данные в адаптере, используя DiffUtil для эффективности.
     * @param newList Новый список элементов.
     */
    private void updateAdapterData(List<MoveItem> newList) {
        if (adapter == null) {
            return;
        }
        adapter.submitList(newList != null ? new ArrayList<>(newList) : new ArrayList<>()); // Передаем копию
    }
    
    @Override
    public void onDestroyView() {
        if (recyclerView != null) {
             recyclerView.setAdapter(null); // Важно для очистки ссылок в RecyclerView
        }
        adapter = null;
        recyclerView = null; // Явное обнуление ссылок
        super.onDestroyView();
    }

    public List<MoveItem> getSelectedItems() {
        return adapter != null ? adapter.getSelectedItems() : new ArrayList<>();
    }
    
    public void clearSelection() {
        if (adapter != null) {
            adapter.clearSelection();
        }
    }
    
    @Override
    public void onSelectionChanged(int count) {
        if (getActivity() instanceof MoveListActivity) {
            ((MoveListActivity) getActivity()).updateSelectionPanel(count);
        }
    }
    
    public MoveListAdapter getAdapter() {
        return adapter;
    }

    /**
     * Возвращает RecyclerView фрагмента для прокрутки
     * @return RecyclerView или null если не инициализирован
     */
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    @Override
    public void onItemClicked(MoveItem moveItem) {
        if (moveItem != null && moveListViewModel != null) {
            Log.d("MoveListFragment_" + status, "Item clicked: " + moveItem.getMovementId());
            moveListViewModel.processMoveItemClick(moveItem);
        }
    }
} 