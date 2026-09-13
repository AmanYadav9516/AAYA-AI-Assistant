# Proguard rules for AAYA Assistant
-keep class com.aaya.assistant.data.model.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
