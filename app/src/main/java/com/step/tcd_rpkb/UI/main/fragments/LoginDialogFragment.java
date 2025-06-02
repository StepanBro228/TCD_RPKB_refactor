package com.step.tcd_rpkb.UI.main.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.UI.main.activity.MainViewModel;

/**
 * Диалог для ввода учетных данных и выбора режима работы
 */
public class LoginDialogFragment extends DialogFragment {
    
    private EditText etUsername;
    private EditText etPassword;
    private RadioGroup rgMode;
    private RadioButton rbOnline;
    private RadioButton rbOffline;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private View rootView;
    private Button btnCheckServer;
    private LinearLayout layoutAuth;
    
    private LoginDialogListener listener;
    private MainViewModel viewModel;
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (LoginDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " должен реализовать LoginDialogListener");
        }
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.dialog_login, null);
        
        bindViews();
        setupUIManipulation();
        setupListeners();
        
        builder.setView(rootView)
               .setTitle("Настройки подключения")
               .setPositiveButton("Сохранить", null)
               .setNegativeButton("Отмена", (dialog, id) -> dialog.cancel());
        
        final AlertDialog dialog = builder.create();
        if (!viewModel.username.getValue().isEmpty() && viewModel.username.getValue() != null){
            etUsername.setText((viewModel.username.getValue()));
        }
        if (!viewModel.password.getValue().isEmpty() && viewModel.password.getValue() != null){
            etPassword.setText((viewModel.password.getValue()));
        }
        setupSaveButton(dialog);
        
        return dialog;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeViewModel();
    }
    
    private void bindViews() {
        etUsername = rootView.findViewById(R.id.etUsername);
        etPassword = rootView.findViewById(R.id.etPassword);
        rgMode = rootView.findViewById(R.id.rgMode);
        rbOnline = rootView.findViewById(R.id.rbOnline);
        rbOffline = rootView.findViewById(R.id.rbOffline);
        progressBar = rootView.findViewById(R.id.progressBar);
        tvStatus = rootView.findViewById(R.id.tvStatus);
        btnCheckServer = rootView.findViewById(R.id.btnCheckServer);
        layoutAuth = rootView.findViewById(R.id.layoutAuth);
    }
    
    private void observeViewModel() {
        viewModel.username.observe(getViewLifecycleOwner(), uname -> etUsername.setText(uname));
        viewModel.password.observe(getViewLifecycleOwner(), pwd -> etPassword.setText(pwd));
        
        viewModel.isOnlineMode.observe(getViewLifecycleOwner(), isOnline -> {
            if (isOnline) {
                rbOnline.setChecked(true);
            } else {
                rbOffline.setChecked(true);
            }
            updateAuthFieldsVisibility(isOnline);
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if(isLoading) tvStatus.setVisibility(View.VISIBLE);
        });

        viewModel.serverStatusText.observe(getViewLifecycleOwner(), statusText -> {
            if (statusText != null && !statusText.isEmpty()) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(statusText);
            } else {
                if(!(viewModel.isOnlineMode.getValue() != null && viewModel.isOnlineMode.getValue())){
                    tvStatus.setVisibility(View.GONE); 
                 }
            }
        });

        viewModel.serverStatusTextColor.observe(getViewLifecycleOwner(), colorResId -> {
            if (colorResId != null && colorResId != 0) {
                tvStatus.setTextColor(getResources().getColor(colorResId));
            }
        });
    }
    
    private void setupListeners() {
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            boolean online = (checkedId == R.id.rbOnline);
            viewModel.onOnlineModeChanged(online);
        });
        
        btnCheckServer.setOnClickListener(v -> {
            hideKeyboard();
            clearFocus();
            viewModel.checkServerAvailability(
                etUsername.getText().toString().trim(),
                etPassword.getText().toString().trim()
            );
        });

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                   (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                hideKeyboard();
                clearFocus();
                if (rbOnline.isChecked()) {
                    viewModel.checkServerAvailability(
                        etUsername.getText().toString().trim(),
                        etPassword.getText().toString().trim()
                    );
                 }
                return true;
            }
            return false;
        });
    }
    
    private void setupSaveButton(AlertDialog dialog) {
        dialog.setOnShowListener(dialogInterface -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(view1 -> {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                boolean onlineMode = rbOnline.isChecked();
                
                hideKeyboard();
                
                if (onlineMode && (username.isEmpty() || password.isEmpty())) {
                    Toast.makeText(getContext(), "Введите логин и пароль для онлайн режима", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                viewModel.handleLoginResult(username, password, onlineMode);
                
                if (listener != null) {
                    listener.onLoginDialogPositiveClick(username, password, onlineMode);
                }
                
                dialog.dismiss();
            });
        });
    }
    
    private void updateAuthFieldsVisibility(boolean online) {
        layoutAuth.setVisibility(online ? View.VISIBLE : View.GONE);
        btnCheckServer.setVisibility(online ? View.VISIBLE : View.GONE);
        
        if (!online) {
            tvStatus.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } else {
            if (tvStatus.getText().toString().isEmpty()) {
                
            }
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            Window window = dialog.getWindow();
            
            if (window != null) {
                lp.copyFrom(window.getAttributes());
                lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.flags &= ~WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
                
                window.setAttributes(lp);
                
                int bottomMargin = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
                View decorView = window.getDecorView();
                decorView.setPadding(decorView.getPaddingLeft(), decorView.getPaddingTop(), 
                                    decorView.getPaddingRight(), bottomMargin + decorView.getPaddingBottom());
            }
            
            dialog.setCanceledOnTouchOutside(true);
        }
    }

    private void setupUIManipulation() {
        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
                clearFocus();
                return true;
            }
            return false;
        });
        
        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard();
            }
        };
        etUsername.setOnFocusChangeListener(focusChangeListener);
        etPassword.setOnFocusChangeListener(focusChangeListener);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View focusedView = getDialog() != null ? getDialog().getCurrentFocus() : null;
        if (focusedView != null) {
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        } else if (getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    private void clearFocus() {
        if (etUsername != null) etUsername.clearFocus();
        if (etPassword != null) etPassword.clearFocus();
    }
    
    public interface LoginDialogListener {
        void onLoginDialogPositiveClick(String username, String password, boolean onlineMode);
    }
} 