# kotlinx.serialization — keep generated @Serializable companions so the runtime
# reflection-free descriptor lookup still works after R8.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.mediaplayer.android.**$$serializer { *; }
-keepclassmembers class com.mediaplayer.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.mediaplayer.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
