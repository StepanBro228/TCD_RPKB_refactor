package com.step.tcd_rpkb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.step.tcd_rpkb.UI.Prixod.activity.ProductsActivity;
import com.step.tcd_rpkb.UI.movelist.activity.MoveListActivity;

public class LC_menu extends com.step.tcd_rpkb.base.BaseFullscreenActivity {




    public void goToPrixod(View view){
        Intent intent = new Intent(this, ProductsActivity.class);
        startActivity(intent);
    }
    public void goToMoveList(View view){
        Intent intent = new Intent(this, MoveListActivity.class);
        startActivity(intent);
    }
    public void goToPeredacha_LVC(View view){
        Intent intent = new Intent(this, Peredacha_LVC.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lc_menu);

    }
}