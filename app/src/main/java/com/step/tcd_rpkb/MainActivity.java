package com.step.tcd_rpkb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.os.Build;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.step.tcd_rpkb.base.BaseFullscreenActivity;
import com.step.tcd_rpkb.UI.main.fragments.LoginDialogFragment;
import com.step.tcd_rpkb.UI.main.activity.MainViewModel;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends BaseFullscreenActivity implements LoginDialogFragment.LoginDialogListener {
    private TextView testView;
    private MainViewModel mainViewModel;

    public void goTolc(View view){
        Intent intent = new Intent(this, LC_menu.class);
        startActivity(intent);
    }
    public void goTolvk(View view){
        Intent intent = new Intent(this, LVC_menu.class);
        startActivity(intent);
    }
    public void goTosp(View view) {
        Intent intent = new Intent(this, SP_menu.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        testView = findViewById(R.id.textView1);
        
        // Добавляем кнопку настроек
        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });
        
        // Наблюдаем за LiveData для отображения Toast сообщений
        mainViewModel.toastMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
        

    }
    
    @Override
    protected void onResume() {
        super.onResume();

    }
    

    
    /**
     * Показывает диалог настроек подключения
     */
    private void showLoginDialog() {
        LoginDialogFragment dialog = new LoginDialogFragment();
        dialog.show(getSupportFragmentManager(), "LoginDialogFragment");
    }
    
    @Override
    public void onLoginDialogPositiveClick(String username, String password, boolean onlineMode) {
        // Делегируем обработку результата в ViewModel
        mainViewModel.handleLoginResult(username, password, onlineMode);
    }
}
