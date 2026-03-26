package gr.chronosphere.scootin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int TICK_INTERVAL_MS = 10; // 10ms is plenty for millisecond display

    private Button start;
    private Button pause;
    private Button reset;
    private TextView textView;

    private long millisecondTime, startTime, timeBuff, updateTime = 0L;
    private Handler handler;
    private int seconds, minutes, milliSeconds;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fix: use Handler(Looper.getMainLooper()) — new Handler() is deprecated since API 30
        handler = new Handler(Looper.getMainLooper());

        textView = findViewById(R.id.textView);
        start    = findViewById(R.id.startbutton);
        pause    = findViewById(R.id.pausebutton);
        reset    = findViewById(R.id.resetbutton);

        // Restore timer state after rotation
        if (savedInstanceState != null) {
            timeBuff  = savedInstanceState.getLong("timeBuff");
            isRunning = savedInstanceState.getBoolean("isRunning");
            updateDisplay(timeBuff);
            if (isRunning) {
                startTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, TICK_INTERVAL_MS);
            }
            updateButtonStates();
        }

        start.setOnClickListener(v -> {
            if (!isRunning) {
                startTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, TICK_INTERVAL_MS);
                isRunning = true;
                updateButtonStates();
            }
        });

        pause.setOnClickListener(v -> {
            if (isRunning) {
                timeBuff += millisecondTime;
                handler.removeCallbacks(runnable);
                isRunning = false;
                updateButtonStates();
            }
        });

        reset.setOnClickListener(v -> {
            handler.removeCallbacks(runnable);
            millisecondTime = 0L;
            startTime       = 0L;
            timeBuff        = 0L;
            updateTime      = 0L;
            seconds         = 0;
            minutes         = 0;
            milliSeconds    = 0;
            isRunning       = false;
            textView.setText("00:00:00");
            updateButtonStates();
        });
    }

    private void updateButtonStates() {
        start.setEnabled(!isRunning);
        pause.setEnabled(isRunning);
        reset.setEnabled(!isRunning);
    }

    private void updateDisplay(long elapsed) {
        int secs  = (int) (elapsed / 1000);
        int mins  = secs / 60;
        secs      = secs % 60;
        int ms    = (int) (elapsed % 1000);
        textView.setText(
            String.format("%02d:%02d:%03d", mins, secs, ms)
        );
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            millisecondTime = SystemClock.uptimeMillis() - startTime;
            updateTime      = timeBuff + millisecondTime;
            updateDisplay(updateTime);
            // Fix: was postDelayed(this, 0) — a tight loop that pinned the CPU at 100%
            handler.postDelayed(this, TICK_INTERVAL_MS);
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("timeBuff", isRunning ? timeBuff + (SystemClock.uptimeMillis() - startTime) : timeBuff);
        outState.putBoolean("isRunning", isRunning);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }
}
