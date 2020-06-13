package com.airbnb.lottie.samples;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;

import java.util.Objects;

import androidx.preference.PreferenceManager;

public class MinWallpaperService extends WallpaperService {

    private MyWallpaperEngine wallpaperEngine;
    private SharedPreferences prefs;

    @Override
    public Engine onCreateEngine() {
        prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        wallpaperEngine = new MyWallpaperEngine();
        prefs.registerOnSharedPreferenceChangeListener(wallpaperEngine);
        return wallpaperEngine;
    }


    private class MyWallpaperEngine extends Engine implements OnSharedPreferenceChangeListener, ValueAnimator.AnimatorUpdateListener {
        private final Handler handler = new Handler(Objects.requireNonNull(Looper.myLooper()));
        private final LottieDrawable lottieDrawable = new LottieDrawable();
        private final Runnable drawRunner = this::draw;
        private AnimationMode animationMode = AnimationMode.INFINITE;
        private int tx;
        private int ty;
        private int width;
        private int height;
        private boolean visible = true;


        public MyWallpaperEngine() {
            loadLottieFromURL(prefs.getString(Constants.PREF_LOTTIE_URL, "https://assets8.lottiefiles.com/datafiles/QeC7XD39x4C1CIj/data.json"));
            lottieDrawable.addAnimatorUpdateListener(this);
        }

        private void loadLottieFromURL(String url) {
            LottieCompositionFactory.fromUrl(MinWallpaperService.this, url).addListener(this::loadComposition);
        }

        private void loadComposition(LottieComposition result) {
            lottieDrawable.setComposition(result);
            rescaleAnimation();
            updateAnimationMode();
            lottieDrawable.setRepeatMode(LottieDrawable.RESTART);
            lottieDrawable.start();
        }

        private void updateAnimationMode(){
            if(AnimationMode.INFINITE.equals(animationMode)){
                lottieDrawable.setRepeatCount(LottieDrawable.INFINITE);
                lottieDrawable.playAnimation();
            } else {
                lottieDrawable.setRepeatCount(0);
            }
        }

        private void rescaleAnimation() {
            LottieComposition composition = lottieDrawable.getComposition();
            if(composition != null) {
                float imageWidth = composition.getBounds().width();
                float imageHeight = composition.getBounds().height();
                float scale = 1;
                if (imageWidth > 0) {
                    scale = Math.min(height / imageHeight, width / imageWidth);
                }
                lottieDrawable.setScale(scale);
                tx = (width - lottieDrawable.getIntrinsicWidth()) / 2;
                ty = (height - lottieDrawable.getIntrinsicHeight()) / 2;
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                lottieDrawable.start();
                if(AnimationMode.ON_UNLOCK.equals(animationMode)){
                    lottieDrawable.playAnimation();
                }
            } else {
                lottieDrawable.stop();
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format,
                                     int width, int height) {
            this.width = width;
            this.height = height;
            super.onSurfaceChanged(holder, format, width, height);
            rescaleAnimation();
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (AnimationMode.ON_TOUCH.equals(animationMode)) {
                if (!lottieDrawable.isAnimating()) {
                    lottieDrawable.playAnimation();
                }
            }
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK);
                    canvas.translate(tx, ty);
                    lottieDrawable.draw(canvas);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            handler.removeCallbacks(drawRunner);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ("lottie_url".equals(key)) {
                loadLottieFromURL(sharedPreferences.getString(key, null));
            } else if ("animation_mode".equals(key)) {
                animationMode = AnimationMode.valueOf(sharedPreferences.getString(key, null));
                updateAnimationMode();
            }
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
                lottieDrawable.stop();
            }
        }
    }

}