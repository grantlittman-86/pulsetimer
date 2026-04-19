# ── Gson serialization ──
# Keep all data model classes used with Gson (field names must survive for reflection)
-keep class com.grantlittman.wearapp.data.model.** { *; }
-keep class com.grantlittman.wearapp.timer.TimerState { *; }

# Keep Gson internals
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# ── Compose ──
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── Wear OS services ──
# Keep services declared in manifest (foreground service + complication)
-keep class com.grantlittman.wearapp.timer.TimerService { *; }
-keep class com.grantlittman.wearapp.complication.PulseTimerComplicationService { *; }

# Keep enum values (used by Gson for HapticType, AudioType)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
