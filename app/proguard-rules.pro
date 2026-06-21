# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.dutchapp.learn.data.model.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.dutchapp.learn.data.model.**$$serializer { *; }
