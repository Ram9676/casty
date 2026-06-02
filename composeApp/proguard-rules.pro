#-dontshrink
-dontobfuscate
#-dontoptimize
#-repackageclasses 'defpackage'

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile



-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep classes with @JsonAdapter annotation
-keep,allowobfuscation,allowoptimization @com.google.gson.annotations.JsonAdapter class *

##---------------End: proguard configuration for Gson  ----------

## Rules for NewPipeExtractor
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.JavaToJSONConverters
-dontwarn org.mozilla.javascript.tools.**

# Hilt - Critical for release builds (prevents immediate crash on launch)
-keep class dagger.hilt.** { *; }
-keep class **_HiltModules.* { *; }
-keep class * extends androidx.hilt.work.HiltWorkerFactory { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.* *;
}

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# Keep our custom components
-keep class com.casty.music.** { *; }

# Additional strong rules for release stability
-keep class com.google.android.exoplayer2.** { *; }
-keep class androidx.media3.** { *; }

# Keep all Compose related classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Better crash reporting - keep line numbers
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-renamesourcefileattribute SourceFile

# Prevent obfuscation of important classes for debugging crashes
-keep,allowobfuscation,allowshrinking class com.casty.music.app.CastyApplication { *; }
