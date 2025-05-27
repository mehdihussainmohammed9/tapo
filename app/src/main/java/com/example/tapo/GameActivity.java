package com.example.tapo;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class GameActivity extends AppCompatActivity {

    private ImageView kangaroo, obstacle;
    private TextView scoreText;
    private Handler handler = new Handler();

    private float kangarooY;
    private float velocity = 0;
    private final float gravity = 2f;
    private final float jumpStrength = -50f;

    private boolean isJumping = false;
    private boolean isGameRunning = true;

    private int screenWidth, screenHeight, groundHeight;
    private int obstacleX;
    private final int obstacleSpeed = 20;

    private int score = 0;

    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        kangaroo = findViewById(R.id.kangaroo);
        obstacle = findViewById(R.id.obstacle);
        scoreText = findViewById(R.id.scoreText);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;
        groundHeight = (int) (80 * dm.density);

        // Set initial kangaroo position after layout
        kangaroo.post(() -> {
            kangarooY = screenHeight - groundHeight - kangaroo.getHeight();
            kangaroo.setY(kangarooY);
        });

        obstacleX = screenWidth + 100;
        obstacle.setX(obstacleX);

        handler.post(gameLoop);

        findViewById(R.id.gameLayout).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && !isJumping && isGameRunning) {
                velocity = jumpStrength;
                isJumping = true;
            }
            return true;
        });
    }

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (!isGameRunning) return;

            // Kangaroo jump physics
            if (isJumping) {
                velocity += gravity;
                kangarooY += velocity;

                if (kangarooY >= screenHeight - groundHeight - kangaroo.getHeight()) {
                    kangarooY = screenHeight - groundHeight - kangaroo.getHeight();
                    isJumping = false;
                    velocity = 0;
                }

                kangaroo.setY(kangarooY);
            }

            // Move obstacle left
            obstacleX -= obstacleSpeed;
            if (obstacleX < -obstacle.getWidth()) {
                obstacleX = screenWidth + (int) (Math.random() * 300);
                score++;
            }
            obstacle.setX(obstacleX);

            // Update score display
            scoreText.setText("Score: " + score);

            // Check collision
            if (checkCollision()) {
                gameOver();
                return;
            }

            handler.postDelayed(this, 30);
        }
    };

    private boolean checkCollision() {
        int padding = 20;

        Rect kangarooRect = new Rect(
                (int) kangaroo.getX() + padding,
                (int) kangaroo.getY() + padding,
                (int) kangaroo.getX() + kangaroo.getWidth() - padding,
                (int) kangaroo.getY() + kangaroo.getHeight() - padding
        );

        Rect obstacleRect = new Rect(
                (int) obstacle.getX() + padding,
                (int) obstacle.getY() + padding,
                (int) obstacle.getX() + obstacle.getWidth() - padding,
                (int) obstacle.getY() + obstacle.getHeight() - padding
        );

        return Rect.intersects(kangarooRect, obstacleRect);
    }

    private void gameOver() {
        isGameRunning = false;
        handler.removeCallbacks(gameLoop);

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }

        Intent intent = new Intent(GameActivity.this, GameOverActivity.class);
        intent.putExtra("score", score);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
}
