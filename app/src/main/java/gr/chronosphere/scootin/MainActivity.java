package gr.chronosphere.scootin;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.navigation.NavigationView;

/**
 * MainActivity — the single Activity that hosts the entire app.
 *
 * Architecture: one Activity + multiple Fragments (single-activity pattern).
 *   • StopwatchFragment  — default / home screen
 *   • AboutFragment      — about, privacy policy, GitHub link
 *   (future: CountdownFragment, SettingsFragment, …)
 *
 * Navigation: Navigation Drawer (hamburger ☰ in the ActionBar).
 *   Fragments are switched using hide/show — NOT replace — so that
 *   StopwatchFragment stays alive in memory while the user browses
 *   About, keeping the timer running without interruption.
 *
 * Back-press handling:
 *   1. If the drawer is open  → close the drawer.
 *   2. If About is visible    → return to Stopwatch.
 *   3. Otherwise              → default behaviour (exit / back stack).
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // The DrawerLayout wraps the entire screen; swiping from the left
    // edge or tapping the hamburger icon opens/closes the drawer.
    private DrawerLayout      drawerLayout;
    private NavigationView    navigationView;

    // Fragment instances are created once in onCreate and reused for
    // the lifetime of the Activity.  After a rotation we recover them
    // from the FragmentManager by their tag strings.
    private StopwatchFragment stopwatchFragment;
    private AboutFragment     aboutFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   // DrawerLayout shell

        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        // ActionBarDrawerToggle draws the animated hamburger ↔ arrow icon
        // and synchronises its state with the DrawerLayout automatically.
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();   // make the icon match the current drawer state on launch
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // show the icon

        // ── Fragment setup ────────────────────────────────────────────────
        if (savedInstanceState == null) {
            // First launch: create both fragments and add them to the container.
            // AboutFragment starts hidden; only StopwatchFragment is visible.
            stopwatchFragment = new StopwatchFragment();
            aboutFragment     = new AboutFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, stopwatchFragment, "stopwatch")
                    .add(R.id.fragmentContainer, aboutFragment,     "about")
                    .hide(aboutFragment)   // drawer shows Stopwatch by default
                    .commit();
            navigationView.setCheckedItem(R.id.nav_stopwatch);  // highlight in drawer
        } else {
            // After rotation: the FragmentManager has already recreated both
            // fragments; we just need to grab references to them by their tags.
            stopwatchFragment = (StopwatchFragment) getSupportFragmentManager()
                    .findFragmentByTag("stopwatch");
            aboutFragment     = (AboutFragment) getSupportFragmentManager()
                    .findFragmentByTag("about");
        }

        // ── Back-press handling ───────────────────────────────────────────
        // Using the modern OnBackPressedDispatcher API (onBackPressed() is
        // deprecated since API 33 / Android 13).
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    // Priority 1: close the drawer if it is open.
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (aboutFragment != null && aboutFragment.isVisible()) {
                    // Priority 2: return to Stopwatch from About.
                    showFragment(stopwatchFragment, aboutFragment);
                    navigationView.setCheckedItem(R.id.nav_stopwatch);
                } else {
                    // Priority 3: fall through to the system default behaviour
                    // (finish the Activity or navigate up the back stack).
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // ── NavigationView item selection ─────────────────────────────────────

    /**
     * Called when the user taps a row in the navigation drawer.
     * We show the selected fragment, hide the other, and close the drawer.
     * Adding new screens in the future only requires adding an else-if here
     * and creating the corresponding Fragment + menu item.
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_stopwatch) {
            showFragment(stopwatchFragment, aboutFragment);
        } else if (id == R.id.nav_about) {
            showFragment(aboutFragment, stopwatchFragment);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;   // mark the item as selected (highlights it in the drawer)
    }

    /**
     * Shows [target] and hides [other] in a single atomic transaction.
     *
     * We use hide/show instead of replace so that the hidden fragment keeps
     * its view and instance state alive — critical for StopwatchFragment
     * because its timer Handler must keep running while About is visible.
     */
    private void showFragment(Fragment target, Fragment other) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.show(target).hide(other).commit();
    }

    // ── ActionBar home button (hamburger icon) ───────────────────────────

    /**
     * Intercepts taps on the home/hamburger icon in the ActionBar to
     * toggle the navigation drawer open or closed.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START))
                drawerLayout.closeDrawer(GravityCompat.START);
            else
                drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
