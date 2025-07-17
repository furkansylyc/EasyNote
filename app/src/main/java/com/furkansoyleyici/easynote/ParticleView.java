package com.furkansoyleyici.easynote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.Random;

public class ParticleView extends View {
    private static final int PARTICLE_COUNT = 24;
    private static final int PARTICLE_SIZE = 8;
    private static final int PARTICLE_COLOR = 0x3388B3E5; // pastel blue, semi-transparent
    private static final int FRAME_RATE = 16; // ~60fps

    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private final Paint paint = new Paint();
    private final Random random = new Random();
    private final Handler handler = new Handler();

    public ParticleView(Context context) { super(context); init(); }
    public ParticleView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public ParticleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        paint.setColor(PARTICLE_COLOR);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles[i] = new Particle();
        }
        handler.post(updateRunnable);
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            for (Particle p : particles) {
                p.y -= p.speed;
                if (p.y < -PARTICLE_SIZE) {
                    p.reset(getWidth(), getHeight(), random);
                }
            }
            invalidate();
            handler.postDelayed(this, FRAME_RATE);
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Particle p : particles) {
            canvas.drawCircle(p.x, p.y, PARTICLE_SIZE, paint);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        for (Particle p : particles) {
            p.reset(w, h, random);
        }
    }

    private static class Particle {
        float x, y, speed;
        void reset(int width, int height, Random random) {
            x = random.nextInt(Math.max(1, width));
            y = height + random.nextInt(height / 2);
            speed = 1.2f + random.nextFloat() * 2.5f;
        }
    }
} 