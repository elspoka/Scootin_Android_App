# scootin' — TODO list

## 🔧 In Progress / Bugs
- [ ] **Landscape layout** — buttons/timer κόβονται, ο top row (110dp fixed) δεν λειτουργεί σωστά σε όλα τα devices. Να δοκιμαστεί με ConstraintLayout αντί LinearLayout για το top section.

## 🚀 Next Features (v1.3.0)
- [ ] **Fragments** — προσθήκη Navigation Component με Fragments (ΟΧΙ obsolete, είναι το recommended Jetpack pattern)
- [ ] **About page** — Fragment με version info, credits
- [ ] **Settings page** — Fragment με options (π.χ. keep screen on toggle, theme)

## ✅ Done (v1.2.0)
- [x] LAP button (split + total time, newest lap πρώτο)
- [x] Lap list με alternating row colors + scrollbar
- [x] Fix timing: `uptimeMillis()` → `elapsedRealtime()` (σωστό και στο sleep)
- [x] Keep screen on (FLAG_KEEP_SCREEN_ON) ενώ τρέχει
- [x] Lap state survives screen rotation (onSaveInstanceState)
- [x] Fix rotation: αφαιρέθηκε android:configChanges
- [x] Landscape layout (portrait-style, compact) — laps scrollable κάτω
- [x] Scrollbar πάντα ορατό (fadeScrollbars=false)

## 📦 Release prep
- [ ] Commit + tag v1.2.0 (μόλις fix το landscape)
- [ ] Push to GitHub
- [ ] Signed release APK / AAB για Google Play
- [ ] Data Safety form στο Play Console
- [ ] Screenshots (portrait + landscape)

