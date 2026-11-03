# Project-specific R8 rules go here.

# Text recognition: Tesseract's native code calls back into these classes by name.
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.googlecode.leptonica.android.** { *; }
