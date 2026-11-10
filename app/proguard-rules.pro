-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
-keep class com.rawedit.app.data.** { *; }
-keep class com.rawedit.app.domain.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-dontwarn kotlinx.serialization.**
