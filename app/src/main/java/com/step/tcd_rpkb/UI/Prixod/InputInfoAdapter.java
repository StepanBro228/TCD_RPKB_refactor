package com.step.tcd_rpkb.UI.Prixod;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.animation.ObjectAnimator;

public class InputInfoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_BUTTON = 1;
    private List<Product> products;
    private Set<String> currentErrorUuids = new HashSet<>();

    private RecyclerView recyclerView;

    private PrixodActivity prixodActivity;

    public interface AdapterButtonListener {
        void onSendDataClicked();
        void onGoBackClicked();
    }
    private AdapterButtonListener buttonListener;
    
    public interface OnProductDataChangedListener {
        void onProductDataConfirmed(String nomenclatureUuid, int value, int positionInAdapter, boolean isValid, boolean byEnterKey);
    }
    private OnProductDataChangedListener dataChangedListener;

    public InputInfoAdapter(PrixodActivity prixodActivity, List<Product> products, AdapterButtonListener buttonListener, OnProductDataChangedListener dataChangedListener) {
        this.prixodActivity = prixodActivity;
        this.products = new ArrayList<>(products != null ? products : new ArrayList<>());
        this.buttonListener = buttonListener;
        this.dataChangedListener = dataChangedListener;
        
        if (prixodActivity != null) {
            this.recyclerView = prixodActivity.findViewById(R.id.rv_info);
        }
    }

    public InputInfoAdapter(List<Product> products, RecyclerView recyclerView, AdapterButtonListener buttonListener, OnProductDataChangedListener dataChangedListener){
        this.products = new ArrayList<>(products);
        this.recyclerView = recyclerView;
        this.buttonListener = buttonListener;
        this.dataChangedListener = dataChangedListener;
        
        // recyclerView.setOnFocusChangeListener и recyclerView.addOnScrollListener удалены,
        // так как управление фокусом и состоянием прокрутки более не требуется
        // или управляется из PrixodActivity.java/PrixodViewModel.
        /*
        if (recyclerView != null) {
            recyclerView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < products.size(); i++) {
                                    RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
                                    if (holder instanceof InputinfoViewHolder) {
                                        InputinfoViewHolder viewHolder = (InputinfoViewHolder) holder;
                                        if (errorProducts.contains(products.get(i).getNomenclatureUuid())) {
                                            viewHolder.list_carried.requestFocus();
                                            viewHolder.list_carried.setSelection(viewHolder.list_carried.getText().length());
                                            
                                            viewHolder.list_carried.setEnabled(false);
                                            viewHolder.list_carried.setEnabled(true);
                                            return;
                                        }
                                    }
                                }
                                
                                if (isProcessingEnterKey) {
                                    for (int i = 0; i < recyclerView.getChildCount(); i++) {
                                        View child = recyclerView.getChildAt(i);
                                        RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
                                        if (holder instanceof InputinfoViewHolder) {
                                            InputinfoViewHolder viewHolder = (InputinfoViewHolder) holder;
                                            viewHolder.list_carried.requestFocus();
                                            viewHolder.list_carried.setSelection(viewHolder.list_carried.getText().length());
                                            return;
                                        }
                                    }
                                }
                            }
                        }, 50);
                    }
                }
            });
            
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    
                    /* // Логика фокусировки на элементе с ошибкой после скролла теперь управляется PrixodActivity.java через ViewModel
                    if (newState == RecyclerView.SCROLL_STATE_IDLE && isProcessingEnterKey) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < products.size(); i++) {
                                    if (errorProducts.contains(products.get(i).getNomenclatureUuid())) {
                                        // requestFocusAt(i); // Метод удален
                                        return;
                                    }
                                }
                            }
                        }, 50);
                    }
                    * /
                }
            });
            
            Context context = recyclerView.getContext();
            if (context instanceof Prixod) {
                this.prixodActivity = (Prixod) context;
            }
        }
        */
    }
    
    public void updateData(List<Product> newProducts, Set<String> errorUuids) {
        this.products.clear();
        if (newProducts != null) {
            this.products.addAll(newProducts);
        }
        this.currentErrorUuids.clear();
        if (errorUuids != null) {
            this.currentErrorUuids.addAll(errorUuids);
        }
        notifyDataSetChanged();
    }

    private void shakeView(final View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", 
                0, -10f, 10f, -10f, 10f, -5f, 5f, -5f, 5f, 0);
        animator.setDuration(500);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (view instanceof EditText) {
                    final EditText editText = (EditText) view;
                    
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (editText.getWindowToken() != null) {
                                    editText.requestFocus();
                                    editText.setSelection(editText.getText().length());
                                }
                            } catch (Exception e) {
                                Log.e("InputInfoAdapter", "Ошибка при восстановлении фокуса", e);
                            }
                        }
                    }, 50);
                }
            }
        });
        
        animator.start();
    }

    class InputinfoViewHolder extends RecyclerView.ViewHolder {
        TextView list_name, list_mesure, list_amount;
        TextView list_series;
        TextView sender_storage;
        TextView cell_balance, series_balance, free_balance, total_balance;
        EditText list_carried;
        View itemView;
        View seriesStorageContainer;

        public InputinfoViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            list_name = itemView.findViewById(R.id.list_name);
            list_series = itemView.findViewById(R.id.list_series);
            sender_storage = itemView.findViewById(R.id.sender_storage);
            seriesStorageContainer = itemView.findViewById(R.id.series_storage_container);
            list_mesure = itemView.findViewById(R.id.list_mesure);
            list_amount = itemView.findViewById(R.id.list_amount);
            list_carried = itemView.findViewById(R.id.carried);
            
            cell_balance = itemView.findViewById(R.id.cell_balance);
            series_balance = itemView.findViewById(R.id.series_balance);
            free_balance = itemView.findViewById(R.id.free_balance);
            total_balance = itemView.findViewById(R.id.total_balance);
            
            list_carried.setBackgroundResource(0);
            
            try {
                list_carried.setShowSoftInputOnFocus(false);
            } catch (Exception e) {
            }
        }
        
        @SuppressLint("DefaultLocale")
        public void bind(final Product product, final Set<String> errorUuids) {
            list_name.setText(product.getNomenclatureName());
            
            boolean hasSeriesName = product.getSeriesName() != null && !product.getSeriesName().isEmpty();
            boolean hasSenderStorage = product.getSenderStorageName() != null && !product.getSenderStorageName().isEmpty();
            
            if (hasSeriesName) {
                list_series.setText("Серия: " + product.getSeriesName());
                list_series.setVisibility(View.VISIBLE);
            } else {
                list_series.setVisibility(View.GONE);
            }
            
            if (hasSenderStorage) {
                String prefix = "Ячейка: ";
                String cellNumber = product.getSenderStorageName();
                
                android.text.SpannableString spannableString = new android.text.SpannableString(prefix + cellNumber);
                
                spannableString.setSpan(
                    new android.text.style.RelativeSizeSpan(1.4f),
                    prefix.length(), 
                    prefix.length() + cellNumber.length(), 
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                
                spannableString.setSpan(
                    new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    prefix.length(), 
                    prefix.length() + cellNumber.length(), 
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                
                spannableString.setSpan(
                    new android.text.style.ForegroundColorSpan(
                        android.graphics.Color.rgb(0, 122, 255)
                    ),
                    prefix.length(), 
                    prefix.length() + cellNumber.length(), 
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                
                sender_storage.setText(spannableString);
                sender_storage.setVisibility(View.VISIBLE);
            } else {
                sender_storage.setVisibility(View.GONE);
            }
            
            if (hasSeriesName || hasSenderStorage) {
                seriesStorageContainer.setVisibility(View.VISIBLE);
            } else {
                seriesStorageContainer.setVisibility(View.GONE);
            }
            
            list_amount.setText(String.format("%.1f", product.getQuantity()));
            list_mesure.setText(product.getUnitName());
            
            cell_balance.setText(String.format("%.1f", product.getFreeBalanceInCell()));
            series_balance.setText(String.format("%.1f", product.getFreeBalanceBySeries()));
            free_balance.setText(String.format("%.1f", product.getFreeBalance()));
            total_balance.setText(String.format("%.1f", product.getTotalBalance()));
            
            if (product.getTaken() > 0) {
                list_carried.setText(String.valueOf(product.getTaken()));
            } else {
                list_carried.setText("");
            }

            if (errorUuids != null && errorUuids.contains(product.getNomenclatureUuid())) {
                list_carried.setBackgroundResource(R.drawable.edit_text_error);
            } else {
                list_carried.setBackgroundResource(R.drawable.edit_text_default);
            }

            itemView.setOnClickListener(null);
            
            PrixodActivity.disableKeyboardForNumericField(list_carried);
            
            list_carried.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || 
                       (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                        
                        final Context context = v.getContext();
                        String valueStr = list_carried.getText().toString();
                        int carriedValue = 0;
                        boolean locallyValid = true;

                        if (!valueStr.isEmpty()) {
                            try {
                                carriedValue = Integer.parseInt(valueStr);
                                if (carriedValue < 0) {
                                    locallyValid = false;
                                } else if (carriedValue > product.getQuantity()) {
                                    locallyValid = false;
                                }
                            } catch (NumberFormatException e) {
                                carriedValue = product.getTaken();
                                locallyValid = false;
                            }
                        } else {
                            carriedValue = 0;
                        }
                        
                        if (dataChangedListener != null) {
                            dataChangedListener.onProductDataConfirmed(product.getNomenclatureUuid(), carriedValue, getBindingAdapterPosition(), locallyValid, true);
                        }

                            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            }
                            
                        return true;
                    }
                    return false;
                }
            });

            list_carried.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        Context context = v.getContext();
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                        
                        String valueStr = list_carried.getText().toString();
                        int carriedValue = 0;
                        boolean locallyValid = true;

                        if (!valueStr.isEmpty()) {
                           try {
                                carriedValue = Integer.parseInt(valueStr);
                                if (carriedValue < 0) {
                                    locallyValid = false;
                                } else if (carriedValue > product.getQuantity()) {
                                    locallyValid = false;
                                    }
                            } catch (NumberFormatException e) {
                                carriedValue = product.getTaken();
                                locallyValid = false;
                            }
                        } else {
                            carriedValue = 0;
                        }
                        
                        if (dataChangedListener != null) {
                            dataChangedListener.onProductDataConfirmed(product.getNomenclatureUuid(), carriedValue, getBindingAdapterPosition(), locallyValid, false);
                        }
                    } else {
                        String value = list_carried.getText().toString();
                        if (!value.equals("0") && !value.isEmpty()) {
                            list_carried.setSelection(value.length());
                        } else {
                            list_carried.setText("");
                        }
                        list_carried.setBackgroundResource(R.drawable.edit_text_focused);
                        
                        PrixodActivity.disableKeyboardForNumericField(list_carried);
                        
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                if (list_carried.hasFocus() && list_carried.getText().length() > 0) {
                                    list_carried.setSelection(list_carried.getText().length());
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    class InputinfoButtonViewHolder extends RecyclerView.ViewHolder{
        private Button btn;
        private Button btnBackToMoveList;

        public InputinfoButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            btn = itemView.findViewById(R.id.btn_send_info);
            btnBackToMoveList = itemView.findViewById(R.id.btn_back_to_move_list);
        }

        void bind() {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (buttonListener != null) {
                            buttonListener.onSendDataClicked();
                                            }
                                    } catch (Exception e) {
                        Log.e("InputInfoAdapter", "Ошибка при вызове onSendDataClicked: " + e.getMessage());
                    }
                }
            });
            
            btnBackToMoveList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (buttonListener != null) {
                        buttonListener.onGoBackClicked();
                                    }
                            }
            });
        }
    }
    @Override
    public int getItemViewType(int position) {
        if (position == products.size()){return TYPE_BUTTON;}
        else{return TYPE_ITEM;}

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.inputfileinformation_list, parent, false);
            return new InputinfoViewHolder(view);
        } else  {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.inputinformation_list_button, parent, false);
            return new InputinfoButtonViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_ITEM) {
            if (products != null && position < products.size()) {
                Product product = products.get(position);
                ((InputinfoViewHolder) holder).bind(product, currentErrorUuids);
    }
        } else if (holder.getItemViewType() == TYPE_BUTTON) {
            ((InputinfoButtonViewHolder) holder).bind();
        }
    }

    @Override
    public int getItemCount() {
        return  products.size() + 1;
    }
}
