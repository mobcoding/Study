# Keep Gson JSON contract for retention config models.
-keepattributes Signature
-keepattributes *Annotation*

-keepclassmembers,allowobfuscation class com.study.retention.internal.Raw* {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep,allowobfuscation class com.study.retention.internal.Raw*
