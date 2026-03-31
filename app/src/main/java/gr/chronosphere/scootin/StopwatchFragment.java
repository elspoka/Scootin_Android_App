package gr.chronosphere.scootin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.Locale;

/**
 * StopwatchFragment — the main (and default) screen of the app.
 *
 * Displays a running stopwatch with millisecond precision and supports
 * recording lap times. The fragment is always kept alive in memory
 * (never replaced, only hidden/shown) so the timer continues ticking
 * even when the user navigates to another fragment (e.g. About).
 *
 * Timing strategy:
 *   We never rely on a counter that increments each tick. Instead we
 *   snapshot SystemClock.elapsedRealtime() when the user presses START,
 *   and compute elapsed = timeBuff + (now - startTime) on every tick.
 *   This means pausing, rotating the screen, or going to About all
 *   produce the correct time when the user returns.
 *
 * State persistence:
 *   All mutable state is saved to a Bundle in onSaveInstanceState()
 *   so the fragment survives Activity recreation (screen rotation,
 *   system-initiated process death, etc.).
 */
public class StopwatchFragment extends Fragment {

    // How often the UI refreshes in milliseconds.
    // 10 ms gives us three digits of precision (hundredths are stable).
    private static final int TICK_MS = 10;

    // ── View references ──────────────────────────────────────────────────
    private Button       btnStart, btnPause, btnReset, btnLap;
    private TextView     tvTimer, tvLapsHeader;
    private LinearLayout lapContainer;   // direct parent of each lap row
    private ScrollView   lapScrollView;  // wraps lapContainer, hidden until first lap
    private Handler      handler;        // posts the tick runnable on the main thread

    // ── Timing state ─────────────────────────────────────────────────────
    // isRunning   – true while the stopwatch is counting
    // startTime   – elapsedRealtime() snapshot taken when START (or resume) was pressed
    // timeBuff    – milliseconds accumulated before the last pause
    // elapsed     – total ms shown in the UI; updated every tick
    private boolean isRunning   = false;
    private long    startTime   = 0L;
    private long    timeBuff    = 0L;
    private long    elapsed     = 0L;

    // ── Lap state ────────────────────────────────────────────────────────
    // lapCount    – number of laps recorded so far
    // lastLapTime – total elapsed ms at the moment the previous lap was recorded;
    //               used to calculate the split time for the next lap
    // lapEntries  – formatted strings for each lap; index 0 = newest lap
    //               kept in memory so we can restore the list after rotation
    private int               lapCount    = 0;
    private long              lastLapTime = 0L;
    private ArrayList<String> lapEntries  = new ArrayList<>();

    // ── Fragment lifecycle ───────────────────────────────────────────────

    /**
     * Inflate the correct layout.
     * Android automatically picks layout-land/fragment_stopwatch.xml when
     * the device is in landscape orientation.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stopwatch, container, false);
    }

    /**
     * Called immediately after onCreateView(). The view tree is fully built here,
     * so it is safe to call findViewById() and attach click listeners.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Wire up the Handler to the main (UI) thread looper so all UI updates
        // happen on the correct thread without needing runOnUiThread().
        handler       = new Handler(Looper.getMainLooper());
        tvTimer       = view.findViewById(R.id.textView);
        btnStart      = view.findViewById(R.id.startbutton);
        btnPause      = view.findViewById(R.id.pausebutton);
        btnReset      = view.findViewById(R.id.resetbutton);
        btnLap        = view.findViewById(R.id.lapbutton);
        tvLapsHeader  = view.findViewById(R.id.lapsHeader);
        lapContainer  = view.findViewById(R.id.lapContainer);
        lapScrollView = view.findViewById(R.id.lapScrollView);

        // ── Restore state after screen rotation ──────────────────────────
        // savedInstanceState is non-null only when the Activity (and therefore
        // this Fragment) is being recreated — e.g. after a screen rotation.
        // If the timer was running before rotation we restart the Handler so
        // the display keeps updating seamlessly.
        if (savedInstanceState != null) {
            timeBuff    = savedInstanceState.getLong("timeBuff");
            isRunning   = savedInstanceState.getBoolean("isRunning");
            lapCount    = savedInstanceState.getInt("lapCount");
            lastLapTime = savedInstanceState.getLong("lastLapTime");
            ArrayList<String> saved = savedInstanceState.getStringArrayList("lapEntries");
            if (saved != null) lapEntries = saved;

            elapsed = timeBuff;
            updateDisplay(elapsed);
            restoreLaps();   // rebuild the lap list from the saved strings

            if (isRunning) {
                // Reset startTime to NOW so elapsed = timeBuff + (now - startTime)
                // gives the correct running total from the moment the view is ready.
                startTime = SystemClock.elapsedRealtime();
                handler.postDelayed(runnable, TICK_MS);
                view.setKeepScreenOn(true);  // prevent the screen from sleeping while running
            }
            updateButtonStates();
        }

        // ── Button click listeners ────────────────────────────────────────

        btnStart.setOnClickListener(v -> {
            if (!isRunning) {
                // Snapshot the current uptime so we can compute elapsed time on every tick.
                startTime = SystemClock.elapsedRealtime();
                isRunning = true;
                handler.postDelayed(runnable, TICK_MS);
                // Keep the screen on as long as the stopwatch is running.
                if (getView() != null) getView().setKeepScreenOn(true);
                updateButtonStates();
            }
        });

        btnPause.setOnClickListener(v -> {
            if (isRunning) {
                // Accumulate the time that has passed since the last START/resume
                // into the buffer so it is not lost when we restart later.
                timeBuff += SystemClock.elapsedRealtime() - startTime;
                handler.removeCallbacks(runnable);  // stop the tick loop
                isRunning = false;
                if (getView() != null) getView().setKeepScreenOn(false);
                updateButtonStates();
            }
        });

        btnLap.setOnClickListener(v -> {
            if (!isRunning) return;  // safety guard; button should be disabled anyway

            // Capture the current total elapsed time and the split since the last lap.
            long now   = timeBuff + (SystemClock.elapsedRealtime() - startTime);
            long split = now - lastLapTime;
            lastLapTime = now;   // move the split baseline forward
            lapCount++;

            // Build the display string and store it at index 0 (newest first).
            String entry = formatLapEntry(lapCount, now, split);
            lapEntries.add(0, entry);
            addLapView(entry, lapCount);

            // Show the laps section if this is the very first lap.
            tvLapsHeader.setVisibility(View.VISIBLE);
            lapScrollView.setVisibility(View.VISIBLE);

            // Scroll to the top so the freshly added row is always visible.
            // post() defers the scroll until after the new view has been laid out.
            lapScrollView.post(() -> lapScrollView.scrollTo(0, 0));
        });

        btnReset.setOnClickListener(v -> {
            // Stop any ongoing tick loop before touching shared state.
            handler.removeCallbacks(runnable);

            // Reset all timing variables to their initial state.
            timeBuff    = 0L;
            startTime   = 0L;
            elapsed     = 0L;
            isRunning   = false;

            // Clear all lap data and remove every lap view from the container.
            lapCount    = 0;
            lastLapTime = 0L;
            lapEntries.clear();
            lapContainer.removeAllViews();

            // Hide the laps section until the next lap is recorded.
            tvLapsHeader.setVisibility(View.GONE);
            lapScrollView.setVisibility(View.GONE);

            if (getView() != null) getView().setKeepScreenOn(false);
            tvTimer.setText(getString(R.string.timer_default));
            updateButtonStates();
        });
    }

    /**
     * Called by the system before the Activity (and Fragment) is destroyed
     * and recreated (e.g. screen rotation, language change).
     * We save every piece of state we need to restore a pixel-perfect UI.
     *
     * Note: if the timer is currently running we save the live elapsed time
     * (timeBuff + delta) rather than just timeBuff, so the restored value
     * is accurate even if some time passes between save and restore.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putLong("timeBuff", isRunning
                ? timeBuff + (SystemClock.elapsedRealtime() - startTime)
                : timeBuff);
        out.putBoolean("isRunning", isRunning);
        out.putInt("lapCount", lapCount);
        out.putLong("lastLapTime", lastLapTime);
        out.putStringArrayList("lapEntries", lapEntries);
    }

    /**
     * Last chance to clean up. We remove any pending Handler callbacks here
     * to prevent the runnable from firing after the Fragment is gone, which
     * would cause a NullPointerException (tvTimer would be null).
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacks(runnable);
    }

    // ── Timer tick runnable ──────────────────────────────────────────────

    /**
     * Posted to the Handler every TICK_MS milliseconds while the stopwatch
     * is running. Computes the current elapsed time and refreshes the display,
     * then schedules itself again — creating a self-perpetuating loop that
     * stops only when removeCallbacks() is called (on pause or reset).
     */
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            elapsed = timeBuff + (SystemClock.elapsedRealtime() - startTime);
            updateDisplay(elapsed);
            handler.postDelayed(this, TICK_MS);
        }
    };

    // ── Helper methods ───────────────────────────────────────────────────

    /** Pushes the formatted time string to the timer TextView. */
    private void updateDisplay(long ms) {
        if (tvTimer != null) tvTimer.setText(formatTime(ms));
    }

    /**
     * Converts a raw millisecond value into a human-readable "MM:SS.mmm" string.
     * Locale.US is explicit so the output is never affected by the device locale
     * (some locales use different digit separators).
     */
    private String formatTime(long ms) {
        int mins = (int) (ms / 60000);
        int secs = (int) ((ms % 60000) / 1000);
        int msec = (int) (ms % 1000);
        return String.format(Locale.US, "%02d:%02d.%03d", mins, secs, msec);
    }

    /**
     * Builds the display string for a single lap row.
     * Format:  #1    00:13.429   +00:13.429
     *           ^    ^           ^
     *           lap# total time  split time
     */
    private String formatLapEntry(int num, long total, long split) {
        return String.format(Locale.US, "#%-3d  %s   +%s", num, formatTime(total), formatTime(split));
    }

    /**
     * Creates and inserts a new TextView at the TOP of lapContainer (position 0)
     * so that the most recently recorded lap always appears first.
     * Alternating background colors (even/odd) make rows easier to scan.
     */
    private void addLapView(String text, int lapNum) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        int px = dpToPx(10);
        tv.setPadding(dpToPx(8), px, dpToPx(8), px);
        tv.setBackgroundColor(ContextCompat.getColor(requireContext(),
                lapNum % 2 == 0 ? R.color.lap_row_even : R.color.lap_row_odd));
        lapContainer.addView(tv, 0);   // insert at top so newest is always first
    }

    /**
     * Rebuilds the entire lap list from the saved lapEntries list after rotation.
     * lapEntries is ordered newest-first (index 0 = highest lap number).
     * We iterate oldest-first (reverse) and insert each one at position 0,
     * which naturally recreates the correct newest-at-top order.
     */
    private void restoreLaps() {
        if (lapEntries.isEmpty()) return;
        for (int i = lapEntries.size() - 1; i >= 0; i--) {
            int lapNum = lapCount - i;   // map list index back to lap number
            addLapView(lapEntries.get(i), lapNum);
        }
        tvLapsHeader.setVisibility(View.VISIBLE);
        lapScrollView.setVisibility(View.VISIBLE);
    }

    /**
     * Synchronises every button's enabled/disabled state with the current
     * isRunning flag. Called after every state transition so the UI is
     * always consistent (e.g. LAP is only enabled while running).
     */
    private void updateButtonStates() {
        btnStart.setEnabled(!isRunning);
        btnPause.setEnabled(isRunning);
        btnLap.setEnabled(isRunning);
        btnReset.setEnabled(!isRunning);
    }

    /** Converts dp units to pixels using the current screen density. */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
