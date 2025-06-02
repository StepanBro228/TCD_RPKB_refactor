package com.step.tcd_rpkb.UI.Prixod;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.UI.Prixod.viewmodel.SortCriteria;

/**
 * Пользовательский компонент для отображения плавающей кнопки сортировки 
 * с выезжающей панелью параметров сортировки
 */
public class FloatingSortView extends FrameLayout {

    private FloatingActionButton sortButton;
    private CardView sortPanel;
    private View sortIndicator;
    private TextView headerName, headerMeasure, headerAmount, headerStorage;
    private ImageButton headerClearSort;
    
    // Флаги для отслеживания направления сортировки
    private boolean isNameSortAscending = true;
    private boolean isMeasureSortAscending = true;
    private boolean isAmountSortAscending = true;
    private boolean isStorageSortAscending = true;
    
    // Текущий отсортированный заголовок
    private TextView currentSortedHeader = null;
    
    // Слушатель для обратного вызова при изменении сортировки
    private SortChangeListener sortChangeListener;
    
    // Флаг, показывающий, развернута ли панель сортировки
    private boolean isPanelExpanded = false;

    public FloatingSortView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public FloatingSortView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FloatingSortView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Инициализация представления
     */
    private void init(Context context) {
        // Инфлейтим разметку
        LayoutInflater.from(context).inflate(R.layout.floating_sort_button, this, true);
        
        // Получаем ссылки на элементы UI
        sortButton = findViewById(R.id.sort_button);
        sortPanel = findViewById(R.id.sort_panel);
        sortIndicator = findViewById(R.id.sort_indicator);
        headerName = findViewById(R.id.header_name);
        headerMeasure = findViewById(R.id.header_measure);
        headerAmount = findViewById(R.id.header_amount);
        headerStorage = findViewById(R.id.header_storage);
        headerClearSort = findViewById(R.id.header_clear_sort);
        
        // Устанавливаем начальное состояние
        headerClearSort.setVisibility(View.INVISIBLE);
        sortIndicator.setVisibility(View.INVISIBLE);
        
        // Настраиваем слушатели нажатий
        setupClickListeners();
    }

    /**
     * Настройка обработчиков нажатий для всех элементов
     */
    private void setupClickListeners() {
        // Нажатие на основную кнопку сортировки
        sortButton.setOnClickListener(v -> toggleSortPanel());
        
        // Обработчик для сортировки по наименованию
        headerName.setOnClickListener(v -> {
            if (sortChangeListener != null) {
                sortChangeListener.onSortChanged(SortCriteria.NAME);
            }
            // Локальное управление UI (иконки, флаги) будет удалено или пересмотрено,
            // так как состояние будет приходить извне через setViewState
        });

        // Обработчик для сортировки по единицам измерения
        headerMeasure.setOnClickListener(v -> {
            if (sortChangeListener != null) {
                sortChangeListener.onSortChanged(SortCriteria.MEASURE);
            }
        });

        // Обработчик для сортировки по количеству
        headerAmount.setOnClickListener(v -> {
            if (sortChangeListener != null) {
                sortChangeListener.onSortChanged(SortCriteria.AMOUNT);
            }
        });

        // Обработчик для сортировки по ячейке
        headerStorage.setOnClickListener(v -> {
            if (sortChangeListener != null) {
                sortChangeListener.onSortChanged(SortCriteria.STORAGE);
            }
        });

        // Обработчик для кнопки очистки сортировки
        headerClearSort.setOnClickListener(v -> {
            if (sortChangeListener != null) {
                sortChangeListener.onClearSort();
            }
            // Логика скрытия кнопки и индикаторов также будет управляться через setViewState
        });
    }
    
    /**
     * Переключает видимость панели сортировки с анимацией
     */
    protected void toggleSortPanel() {
        if (isPanelExpanded) {
            // Скрываем панель
            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator translateY = ObjectAnimator.ofFloat(sortPanel, "translationY", 0f, -100f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(sortPanel, "alpha", 1f, 0f);
            
            animatorSet.playTogether(translateY, alpha);
            animatorSet.setDuration(300);
            animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
            animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    sortPanel.setVisibility(GONE);
                }
            });
            animatorSet.start();
        } else {
            // Показываем панель
            sortPanel.setVisibility(VISIBLE);
            sortPanel.setAlpha(0f);
            sortPanel.setTranslationY(-100f);
            
            // Проверяем, должна ли кнопка очистки быть видимой
            if (currentSortedHeader != null) {
                // Принудительно отображаем кнопку очистки
                headerClearSort.setVisibility(View.VISIBLE);
                headerClearSort.setAlpha(1.0f);
                headerClearSort.setScaleX(1.0f);
                headerClearSort.setScaleY(1.0f);
            } else {
                headerClearSort.setVisibility(View.INVISIBLE);
            }
            
            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator translateY = ObjectAnimator.ofFloat(sortPanel, "translationY", -100f, 0f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(sortPanel, "alpha", 0f, 1f);
            
            animatorSet.playTogether(translateY, alpha);
            animatorSet.setDuration(350);
            animatorSet.setInterpolator(new OvershootInterpolator(1.0f));
            animatorSet.start();
        }
        
        // Анимация кнопки
        animateSortButton();
        
        // Переключаем флаг состояния панели
        isPanelExpanded = !isPanelExpanded;
    }
    
    /**
     * Анимирует кнопку сортировки при нажатии
     */
    private void animateSortButton() {
        AnimatorSet animatorSet = new AnimatorSet();
        float endRotation = isPanelExpanded ? 0f : 180f; // Полный поворот для более выразительной анимации
        
        // Создаем более выразительную анимацию
        ObjectAnimator rotation = ObjectAnimator.ofFloat(sortButton, "rotation", sortButton.getRotation(), endRotation);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(sortButton, "scaleX", 1f, 0.8f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(sortButton, "scaleY", 1f, 0.8f, 1.2f, 1f);
        
        // Добавляем анимацию возвышения (на Android 5.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            float startElevation = sortButton.getElevation();
            float endElevation = isPanelExpanded ? 8f : 16f;
            ObjectAnimator elevation = ObjectAnimator.ofFloat(sortButton, "elevation", startElevation, endElevation);
            animatorSet.playTogether(rotation, scaleX, scaleY, elevation);
        } else {
            animatorSet.playTogether(rotation, scaleX, scaleY);
        }
        
        animatorSet.setDuration(400);
        animatorSet.setInterpolator(new OvershootInterpolator(2f)); // Более выраженный эффект отскока
        animatorSet.start();
        
        // Меняем цвет кнопки при открытии/закрытии панели
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // Обратите внимание, здесь исправлены цвета - всегда начинаем с текущего цвета
            int colorFrom = (int) sortButton.getBackgroundTintList().getDefaultColor();
            int colorTo = isPanelExpanded ? getResources().getColor(R.color.prixodPrimary) : getResources().getColor(R.color.prixodAccent);
            
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.setDuration(400);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    sortButton.setBackgroundTintList(ColorStateList.valueOf((int) animator.getAnimatedValue()));
                }
            });
            colorAnimation.start();
        }
    }
    
    /**
     * Устанавливает уменьшенную иконку сортировки для TextView слева от текста с анимацией
     */
    private void setCompactDrawableWithAnimation(final TextView textView, final int drawableRes) {
        // Получаем размер иконки (значительно уменьшенный для экономии места)
        int size = (int) (20 * getResources().getDisplayMetrics().density); // 20dp вместо 24dp
        
        // Убрана установка нейтральной иконки и postDelayed для мгновенного применения
        // так как этот метод теперь будет вызываться из setViewState, который уже знает конечное состояние
        Drawable drawable = getResources().getDrawable(drawableRes);
        drawable.setBounds(0, 0, size, size);
        
        textView.setCompoundDrawables(drawable, null, null, null);
        textView.setCompoundDrawablePadding(2);
        
        // Анимация при установке остается
        textView.setAlpha(0.7f);
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(textView, "scaleX", 0.7f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(textView, "scaleY", 0.7f, 1.2f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(textView, "alpha", 0.7f, 1.0f);
        
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setInterpolator(new OvershootInterpolator(1.2f));
        animatorSet.setDuration(350);
        animatorSet.start();
        
        // Логика показа/скрытия headerClearSort теперь будет в setViewState
    }
    
    /**
     * Метод для очистки индикаторов сортировки во всех заголовках
     */
    private void clearSortIndicators() {
        // Устанавливаем пустые drawable для всех заголовков с небольшой анимацией
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX1 = ObjectAnimator.ofFloat(headerName, "scaleX", headerName.getScaleX(), 0.9f, 1.0f);
        ObjectAnimator scaleY1 = ObjectAnimator.ofFloat(headerName, "scaleY", headerName.getScaleY(), 0.9f, 1.0f);
        ObjectAnimator scaleX2 = ObjectAnimator.ofFloat(headerMeasure, "scaleX", headerMeasure.getScaleX(), 0.9f, 1.0f);
        ObjectAnimator scaleY2 = ObjectAnimator.ofFloat(headerMeasure, "scaleY", headerMeasure.getScaleY(), 0.9f, 1.0f);
        ObjectAnimator scaleX3 = ObjectAnimator.ofFloat(headerAmount, "scaleX", headerAmount.getScaleX(), 0.9f, 1.0f);
        ObjectAnimator scaleY3 = ObjectAnimator.ofFloat(headerAmount, "scaleY", headerAmount.getScaleY(), 0.9f, 1.0f);
        ObjectAnimator scaleX4 = ObjectAnimator.ofFloat(headerStorage, "scaleX", headerStorage.getScaleX(), 0.9f, 1.0f);
        ObjectAnimator scaleY4 = ObjectAnimator.ofFloat(headerStorage, "scaleY", headerStorage.getScaleY(), 0.9f, 1.0f);
        
        animatorSet.playTogether(scaleX1, scaleY1, scaleX2, scaleY2, scaleX3, scaleY3, scaleX4, scaleY4);
        animatorSet.setDuration(200);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
        
        headerName.setCompoundDrawables(null, null, null, null);
        headerMeasure.setCompoundDrawables(null, null, null, null);
        headerAmount.setCompoundDrawables(null, null, null, null);
        headerStorage.setCompoundDrawables(null, null, null, null);
        
        // Сбрасываем масштаб и прозрачность
        headerName.setScaleX(1.0f);
        headerName.setScaleY(1.0f);
        headerName.setAlpha(1.0f);
        
        headerMeasure.setScaleX(1.0f);
        headerMeasure.setScaleY(1.0f);
        headerMeasure.setAlpha(1.0f);
        
        headerAmount.setScaleX(1.0f);
        headerAmount.setScaleY(1.0f);
        headerAmount.setAlpha(1.0f);
        
        headerStorage.setScaleX(1.0f);
        headerStorage.setScaleY(1.0f);
        headerStorage.setAlpha(1.0f);
    }
    
    /**
     * Показывает кнопку очистки сортировки с анимацией
     */
    private void showClearButtonWithAnimation() {
        // Принудительно устанавливаем видимость
        headerClearSort.setVisibility(View.VISIBLE);
        headerClearSort.setAlpha(1f);
        headerClearSort.setScaleX(1f);
        headerClearSort.setScaleY(1f);
        headerClearSort.setRotation(0f);
        
        // Если анимация не нужна, просто выходим
        if (headerClearSort.getVisibility() == View.VISIBLE && headerClearSort.getAlpha() > 0.9f) {
            return;
        }
        
        // Подготавливаем кнопку для анимации
        headerClearSort.setAlpha(0f);
        headerClearSort.setScaleX(0.5f);
        headerClearSort.setScaleY(0.5f);
        headerClearSort.setRotation(-45f);
        
        // Создаем анимацию появления
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(headerClearSort, "scaleX", 0.5f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(headerClearSort, "scaleY", 0.5f, 1.2f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(headerClearSort, "alpha", 0f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(headerClearSort, "rotation", -45f, 15f, 0f);
        
        animatorSet.playTogether(alpha, scaleX, scaleY, rotation);
        animatorSet.setInterpolator(new OvershootInterpolator(1.2f));
        animatorSet.setDuration(350);
        animatorSet.start();
    }
    
    /**
     * Скрывает кнопку очистки сортировки с анимацией
     */
    private void hideClearButtonWithAnimation() {
        // Создаем анимацию исчезновения
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(headerClearSort, "scaleX", 1.0f, 0.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(headerClearSort, "scaleY", 1.0f, 0.5f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(headerClearSort, "alpha", 1f, 0f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(headerClearSort, "rotation", 0f, -45f); // Добавляем анимацию поворота
        
        // По окончании анимации скрываем кнопку
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                headerClearSort.setVisibility(View.INVISIBLE);
            }
        });
        
        animatorSet.playTogether(alpha, scaleX, scaleY, rotation);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.setDuration(250);
        animatorSet.start();
    }
    
    /**
     * Обновляет видимость индикатора сортировки
     */
    private void updateSortIndicator() {
        if (currentSortedHeader != null) {
            // Если сортировка активна, показываем индикатор с анимацией
            if (sortIndicator.getVisibility() != View.VISIBLE || sortIndicator.getAlpha() < 0.9f) {
                sortIndicator.setScaleX(0f);
                sortIndicator.setScaleY(0f);
                sortIndicator.setAlpha(0f);
                sortIndicator.setVisibility(View.VISIBLE);
                
                // Анимируем появление
                AnimatorSet animatorSet = new AnimatorSet();
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(sortIndicator, "scaleX", 0f, 1.3f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(sortIndicator, "scaleY", 0f, 1.3f, 1f);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(sortIndicator, "alpha", 0f, 1f);
                
                animatorSet.playTogether(scaleX, scaleY, alpha);
                animatorSet.setDuration(400);
                animatorSet.setInterpolator(new OvershootInterpolator(1.5f));
                animatorSet.start();
            }
        } else {
            // Если сортировка не активна, скрываем индикатор с анимацией
            if (sortIndicator.getVisibility() == View.VISIBLE && sortIndicator.getAlpha() > 0.1f) {
                AnimatorSet animatorSet = new AnimatorSet();
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(sortIndicator, "scaleX", 1f, 0f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(sortIndicator, "scaleY", 1f, 0f);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(sortIndicator, "alpha", 1f, 0f);
                
                animatorSet.playTogether(scaleX, scaleY, alpha);
                animatorSet.setDuration(250);
                animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
                animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        sortIndicator.setVisibility(View.INVISIBLE);
                    }
                });
                animatorSet.start();
            }
        }
    }
    
    /**
     * Устанавливает слушатель изменений сортировки
     */
    public void setSortChangeListener(SortChangeListener listener) {
        this.sortChangeListener = listener;
    }

    /**
     * Проверяет, открыта ли панель сортировки
     * @return true если панель открыта, false в противном случае
     */
    public boolean isPanelExpanded() {
        return isPanelExpanded;
    }

    /**
     * Обрабатывает нажатие кнопки "назад"
     * @return true если событие было обработано, false в противном случае
     */
    public boolean onBackPressed() {
        if (isPanelExpanded) {
            // Если панель открыта, закрываем её
            toggleSortPanel();
            return true; // Событие обработано
        }
        return false; // Не обрабатываем событие
    }

    /**
     * Обновляет состояние UI компонента на основе данных из ViewModel
     * @param criteria Текущий критерий сортировки
     * @param isAscending Текущее направление сортировки
     */
    public void setViewState(SortCriteria criteria, boolean isAscending) {
        clearSortIndicators(); // Сбрасываем все предыдущие иконки на заголовках
        currentSortedHeader = null; // Сбрасываем текущий активный заголовок

        // Сбрасываем локальные флаги направления (они больше не нужны для принятия решений, но могут использоваться для UI)
        isNameSortAscending = isAscending; 
        isMeasureSortAscending = isAscending;
        isAmountSortAscending = isAscending;
        isStorageSortAscending = isAscending;

        TextView targetHeader = null;
        int sortIconRes = isAscending ? R.drawable.ic_sort_asc : R.drawable.ic_sort_desc;

        switch (criteria) {
            case NAME:
                targetHeader = headerName;
                isNameSortAscending = isAscending; // Обновляем флаг для этого конкретного критерия
                break;
            case MEASURE:
                targetHeader = headerMeasure;
                isMeasureSortAscending = isAscending;
                break;
            case AMOUNT:
                targetHeader = headerAmount;
                isAmountSortAscending = isAscending;
                break;
            case STORAGE:
                targetHeader = headerStorage;
                isStorageSortAscending = isAscending;
                break;
            case NONE:
                // Ничего не делаем, currentSortedHeader уже null, индикаторы сброшены
                break;
        }

        if (targetHeader != null) {
            currentSortedHeader = targetHeader;
            setCompactDrawableWithAnimation(targetHeader, sortIconRes);
            showClearButtonWithAnimation(); // Показываем кнопку сброса, если есть активная сортировка
        } else {
            hideClearButtonWithAnimation(); // Скрываем кнопку сброса, если сортировка отсутствует
        }
        updateSortIndicator(); // Обновляем главный индикатор на FAB
        
        // Если панель открыта, и критерий сменился, стоит её оставить открытой.
        // Если панель была закрыта, она останется закрытой.
        // Логика открытия/закрытия панели (toggleSortPanel) остается на пользователе (клик по FAB).
    }

    /**
     * Интерфейс для обратного вызова при изменении сортировки
     */
    public interface SortChangeListener {
        void onSortChanged(SortCriteria criteria);
        void onClearSort();
    }
} 