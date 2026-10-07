# Keep rules for release minification (R8). Verified with assembleRelease.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*

# Retrofit service interfaces + Gson models (fields accessed via reflection).
-keep interface com.hafiztraveltours.app.network.ApiService { *; }
-keep class com.hafiztraveltours.app.network.** { *; }
-keep class com.hafiztraveltours.app.models.** { *; }
-keepclassmembers class * implements java.io.Serializable { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ViewModels are instantiated reflectively by ViewModelProvider.
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# Enums used in switches (status/type mappings).
-keepclassmembers enum * { *; }

# Google Sign-In / Firebase Auth (SDK reflection + Play services).
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.**

# Glide generated API (harmless if unused).
-dontwarn com.bumptech.glide.**
