// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // You might also declare the google-services plugin here with 'apply false'
    // if you want to manage its version centrally, e.g.:
    // alias(libs.plugins.google.gms.google.services) apply false
    // OR
    // id("com.google.gms.google-services") version "4.X.X" apply false
}
// REMOVE THIS LINE: apply(plugin = "com.google.gms.google-services")
