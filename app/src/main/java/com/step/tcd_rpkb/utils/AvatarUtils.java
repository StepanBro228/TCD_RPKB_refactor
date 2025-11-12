package com.step.tcd_rpkb.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.step.tcd_rpkb.R;

public class AvatarUtils {



    /**
     * Создает текстовый аватар с инициалами пользователя
     * @param context контекст приложения
     * @param text инициалы для отображения
     * @return Drawable с аватаром
     */
    public static Drawable createTextAvatar(Context context, String text) {

        int size = (int) (26 * context.getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Рисуем фон с градиентом
        Drawable background = context.getResources().getDrawable(R.drawable.custom_avatar_background);
        background.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        background.draw(canvas);

        // Настраиваем стиль текста
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(size / 2f);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        // Центрируем текст
        float xPos = canvas.getWidth() / 2f;
        float yPos = (canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);

        // Рисуем текст
        canvas.drawText(text, xPos, yPos, paint);


        return new android.graphics.drawable.BitmapDrawable(context.getResources(), bitmap);
    }

    /**
     * Извлекает инициалы из полного имени
     * @param fullName Полное имя пользователя
     * @return Инициалы (1-2 символа)
     */
    public static String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "?";
        }

        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();


        if (parts.length > 0 && !parts[0].isEmpty()) {
            initials.append(parts[0].charAt(0));
        }


        if (parts.length > 1 && !parts[1].isEmpty()) {
            initials.append(parts[1].charAt(0));
        }
        
        if (initials.length() == 0) {
            return "?";
        }

        return initials.toString().toUpperCase();
    }
} 