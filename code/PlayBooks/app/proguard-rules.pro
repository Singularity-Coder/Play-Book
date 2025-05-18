# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Missing classes detected while running R8. Please add the missing classes or apply additional keep rules that are generated in PlayBooks/app/build/outputs/mapping/debug/missing_rules.txt.

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn org.spongycastle.cert.X509CertificateHolder
-dontwarn org.spongycastle.cms.CMSEnvelopedData
-dontwarn org.spongycastle.cms.Recipient
-dontwarn org.spongycastle.cms.RecipientId
-dontwarn org.spongycastle.cms.RecipientInformation
-dontwarn org.spongycastle.cms.RecipientInformationStore
-dontwarn org.spongycastle.cms.jcajce.JceKeyTransEnvelopedRecipient
-dontwarn org.spongycastle.cms.jcajce.JceKeyTransRecipient
-dontwarn org.spongycastle.crypto.BlockCipher
-dontwarn org.spongycastle.crypto.CipherParameters
-dontwarn org.spongycastle.crypto.engines.AESFastEngine
-dontwarn org.spongycastle.crypto.modes.CBCBlockCipher
-dontwarn org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher
-dontwarn org.spongycastle.crypto.params.KeyParameter
-dontwarn org.spongycastle.crypto.params.ParametersWithIV


#  To keep a specific class:
# -keep class com.yourpackage.YourClass { *; }

# To keep everything in a package:
# -keep class com.yourpackage.** { *; }

# To avoid warnings
# -dontwarn org.xmlpull.**

# To keep classes that are used via reflection:
# -keepclassmembers class * {
#     @android.webkit.JavascriptInterface <methods>;
# }

-keepattributes Signature

# Keep all annotations
-keepattributes *Annotation*

-keep class com.singularitycoder.playbooks.** { *; }
-keep class com.google.gson.reflect.TypeToken


# To support retracing of your application's stack traces for debugging - necessary since R8 obfuscates code
# https://developer.android.com/build/shrink-code#retracing
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile