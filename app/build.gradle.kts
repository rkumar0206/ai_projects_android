import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("androidx.room")
}

// Function to read a property from local.properties
fun getLocalProperty(propertyName: String, project: Project): String {
    val propFile = project.rootProject.file("local.properties")
    val properties = Properties()
    if (propFile.exists()) {
        FileInputStream(propFile).use { properties.load(it) }
    }
    return properties.getProperty(propertyName, "")
        ?: "" // Return empty string if null or not found
}

android {
    namespace = "com.rtb.ai.projects"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rtb.ai.projects"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val geminiApiKey = getLocalProperty("GEMINI_API_KEY", project)
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }


    packagingOptions {
        // Option 1: Pick the first one encountered
        resources.pickFirsts.add("META-INF/INDEX.LIST")

        // Option 2: Exclude the file entirely
        // resources.excludes.add("META-INF/INDEX.LIST")

        // You might encounter other META-INF conflicts, you can add them here too:
        // resources.pickFirsts.add("META-INF/LICENSE")
        // resources.pickFirsts.add("META-INF/LICENSE.txt")
        // resources.pickFirsts.add("META-INF/NOTICE")
        // resources.pickFirsts.add("META-INF/NOTICE.txt")
        // resources.pickFirsts.add("META-INF/ASL2.0")
        resources.pickFirsts.add("META-INF/DEPENDENCIES")
        // resources.pickFirsts.add("META-INF/LGPL2.1")
        // resources.excludes.add("META-INF/kotlin-tooling-metadata.json") // Example
    }

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains:annotations:23.0.0")
        }
        exclude(group = "org.jetbrains", module = "annotations-java5") // Add this
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.google.genai)
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx) // Use the same version as the plugin
    ksp(libs.hilt.compiler)         // Use the same version as the plugin

    // Markwon for markdown support
    implementation(libs.markwon.core) // Check for the latest version
    // For specific features, add corresponding plugins:
    implementation(libs.markwon.html) // If you need to render HTML within Markdown
    //implementation("io.noties.markwon:image:4.6.2") // For image loading
    implementation(libs.markwon.syntax.highlight) // For code syntax highlighting
    // implementation("io.noties.markwon:ext-tables:4.6.2") // For tables
    // implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    // implementation("io.noties.markwon:ext-tasklist:4.6.2")

    //Glide
    implementation(libs.glide)
    ksp(libs.glide.compiler)

    //Room db
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}