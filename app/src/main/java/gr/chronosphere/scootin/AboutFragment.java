package gr.chronosphere.scootin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * AboutFragment — informational screen accessible from the navigation drawer.
 *
 * Shows:
 *   • App name and dynamic version number (read from the manifest at runtime)
 *   • A button that opens the GitHub repository in the device's browser
 *   • Privacy & Data policy (no data collected, no network access)
 *   • License information (GPL-3.0)
 *
 * This fragment is kept hidden (not destroyed) when the user navigates away,
 * so it is instantly ready when they return without re-fetching anything.
 */
public class AboutFragment extends Fragment {

    /**
     * Inflate the static about layout.
     * The layout is a simple ScrollView so long text stays readable on
     * small screens or when the font size is increased in accessibility settings.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    /**
     * Wire up dynamic content and click listeners once the view is ready.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── Version number ────────────────────────────────────────────────
        // PackageInfo.versionName comes from the versionName field in build.gradle,
        // which is set dynamically from the latest git tag (e.g. "v1.2.0").
        // If the lookup fails for any reason we simply hide the version line.
        TextView tvVersion = view.findViewById(R.id.tvVersion);
        try {
            String version = requireContext()
                    .getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0)
                    .versionName;
            tvVersion.setText(getString(R.string.version_format, version));
        } catch (Exception e) {
            // Shouldn't happen in practice, but better than a crash.
            tvVersion.setVisibility(View.GONE);
        }

        // ── GitHub button ─────────────────────────────────────────────────
        // Fires an ACTION_VIEW Intent with the repository URL.
        // Android routes this to the installed browser (or a chooser if the
        // user has multiple browsers). No INTERNET permission is needed here
        // because the browser app handles the actual network request.
        Button btnGithub = view.findViewById(R.id.btnGithub);
        btnGithub.setOnClickListener(v -> {
            String url = getString(R.string.github_url);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
    }
}
