package com.step.tcd_rpkb.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.step.tcd_rpkb.R;

public class CustomCheckBox extends FrameLayout implements Checkable {
    private boolean mChecked = false;
    private CardView container;
    private ImageView checkIcon;
    private OnCheckedChangeListener mOnCheckedChangeListener;

    public CustomCheckBox(Context context) {
        super(context);
        init(context);
    }

    public CustomCheckBox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomCheckBox(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_custom_checkbox_movelist, this, true);
        container = view.findViewById(R.id.checkbox_container);
        checkIcon = view.findViewById(R.id.check_icon);
        
        setClickable(true);
        setFocusable(true);
        
        setOnClickListener(v -> toggle());
        
        updateView();
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            updateView();
            
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
            }
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }
    
    private void updateView() {
        container.setBackgroundResource(mChecked ? 
                R.drawable.bg_select_button_selected : 
                R.drawable.bg_select_button);
        checkIcon.setVisibility(mChecked ? View.VISIBLE : View.INVISIBLE);
    }
    
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }
    
    public interface OnCheckedChangeListener {
        void onCheckedChanged(CustomCheckBox checkBox, boolean isChecked);
    }
} 