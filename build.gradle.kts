buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.5.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.7")
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.3.0" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.google.firebase.crashlytics") version "3.0.7" apply false
}