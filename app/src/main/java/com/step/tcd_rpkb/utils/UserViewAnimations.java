package com.step.tcd_rpkb.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context; // Необходим для getElevation в API < LOLLIPOP, но у нас >= LOLLIPOP
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;



public class UserViewAnimations {

    private UserViewAnimations() {

    }


    /**
     * Проигрывает улучшенную анимацию для карточки пользователя (из UserManager.playFancyAnimation)
     * @param cardView карточка пользователя (View)
     * @param avatarImageView изображение аватара
     * @param fullNameTextView текстовое поле с именем пользователя
     * @param roleTextView текстовое поле с ролью пользователя
     */
    public static void playFancyAnimation(View cardView, ImageView avatarImageView,
                                   TextView fullNameTextView, TextView roleTextView) {
        if (cardView == null || avatarImageView == null ||
                fullNameTextView == null || roleTextView == null) {
            return;
        }



        // Анимация карточки: подъем и пульсация
        AnimatorSet cardAnimSet = new AnimatorSet();

        ObjectAnimator scaleXCard = ObjectAnimator.ofFloat(cardView, "scaleX", 0.8f, 1.03f, 1.0f);
        ObjectAnimator scaleYCard = ObjectAnimator.ofFloat(cardView, "scaleY", 0.8f, 1.03f, 1.0f);
        ObjectAnimator translationCard = ObjectAnimator.ofFloat(cardView, "translationY", 50f, -10f, 0f);

        cardAnimSet.playTogether(scaleXCard, scaleYCard, translationCard);
        cardAnimSet.setDuration(800);
        cardAnimSet.setInterpolator(new android.view.animation.OvershootInterpolator(1.2f));

        // Анимация аватара: вращение и пульсация
        AnimatorSet avatarAnimSet = new AnimatorSet();

        ObjectAnimator rotateAvatar = ObjectAnimator.ofFloat(avatarImageView, "rotation", -15f, 15f, 0f);
        ObjectAnimator scaleXAvatar = ObjectAnimator.ofFloat(avatarImageView, "scaleX", 0f, 1.2f, 1.0f);
        ObjectAnimator scaleYAvatar = ObjectAnimator.ofFloat(avatarImageView, "scaleY", 0f, 1.2f, 1.0f);

        avatarAnimSet.playTogether(rotateAvatar, scaleXAvatar, scaleYAvatar);
        avatarAnimSet.setDuration(950);
        avatarAnimSet.setInterpolator(new android.view.animation.OvershootInterpolator(1.5f));

        // Анимация имени пользователя: слайд и появление
        AnimatorSet nameAnimSet = new AnimatorSet();
        fullNameTextView.setAlpha(0f);
        fullNameTextView.setTranslationX(-100f);
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(fullNameTextView, "alpha", 0f, 1.0f);
        ObjectAnimator nameTranslation = ObjectAnimator.ofFloat(fullNameTextView, "translationX", -100f, 0f);
        nameAnimSet.playTogether(nameAlpha, nameTranslation);
        nameAnimSet.setDuration(700);
        nameAnimSet.setStartDelay(300);
        nameAnimSet.setInterpolator(new android.view.animation.DecelerateInterpolator());

        // Анимация роли: слайд и появление с другой стороны
        AnimatorSet roleAnimSet = new AnimatorSet();
        roleTextView.setAlpha(0f);
        roleTextView.setTranslationX(100f);
        ObjectAnimator roleAlpha = ObjectAnimator.ofFloat(roleTextView, "alpha", 0f, 1.0f);
        ObjectAnimator roleTranslation = ObjectAnimator.ofFloat(roleTextView, "translationX", 100f, 0f);
        roleAnimSet.playTogether(roleAlpha, roleTranslation);
        roleAnimSet.setDuration(700);
        roleAnimSet.setStartDelay(500);
        roleAnimSet.setInterpolator(new android.view.animation.DecelerateInterpolator());

        // Добавляем эффект свечения для аватара
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ObjectAnimator glowAnimator = ObjectAnimator.ofFloat(
                    avatarImageView, "elevation", 0f, 25f, 15f, 25f, 15f, 5f);
            glowAnimator.setDuration(2000);
            glowAnimator.setRepeatCount(0);
            glowAnimator.start(); // Запускаем отдельно, чтобы не блокировать основной набор
        }

        AnimatorSet finalAnimSet = new AnimatorSet();
        finalAnimSet.play(cardAnimSet).with(avatarAnimSet);
        finalAnimSet.play(nameAnimSet).after(250);
        finalAnimSet.play(roleAnimSet).after(nameAnimSet); // Было after(nameAnimSet), возможно after(avatarAnimSet) или cardAnimSet?
                                                        // Оставляю как было, но это может быть неоптимально по времени.

        finalAnimSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                ObjectAnimator rolePulse = ObjectAnimator.ofFloat(roleTextView, "scaleX", 1.0f, 1.1f, 1.0f);
                ObjectAnimator rolePulseY = ObjectAnimator.ofFloat(roleTextView, "scaleY", 1.0f, 1.1f, 1.0f);
                AnimatorSet pulseAnimSet = new AnimatorSet();
                pulseAnimSet.playTogether(rolePulse, rolePulseY);
                pulseAnimSet.setDuration(500);
                pulseAnimSet.setInterpolator(new android.view.animation.OvershootInterpolator());
                pulseAnimSet.setStartDelay(200);
                pulseAnimSet.start();
            }
        });
        finalAnimSet.start();
    }

    /**
     * Добавляет эффект мерцания для аватара пользователя (из UserManager.addSparkleEffect)
     * @param avatarView ImageView с аватаром пользователя
     */
    public static void addSparkleEffect(ImageView avatarView) {
        if (avatarView == null) return;

        AnimatorSet sparkleAnimator = new AnimatorSet();

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(avatarView, "scaleX", 1.0f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(avatarView, "scaleY", 1.0f, 1.1f, 1.0f);

        ValueAnimator brightnessAnimator = ValueAnimator.ofFloat(1.0f, 1.5f, 1.0f);
        brightnessAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.set(new float[]{
                    value, 0, 0, 0, 0,
                    0, value, 0, 0, 0,
                    0, 0, value, 0, 0,
                    0, 0, 0, 1, 0
            });
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            avatarView.setColorFilter(filter);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ObjectAnimator elevationAnimator = ObjectAnimator.ofFloat(avatarView, "elevation",
                    avatarView.getElevation(), avatarView.getElevation() + 10f, avatarView.getElevation());
            sparkleAnimator.playTogether(scaleX, scaleY, brightnessAnimator, elevationAnimator);
        } else {
            sparkleAnimator.playTogether(scaleX, scaleY, brightnessAnimator);
        }

        sparkleAnimator.setDuration(800);
        sparkleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        sparkleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                avatarView.clearColorFilter();
            }
        });
        sparkleAnimator.start();
    }
} 