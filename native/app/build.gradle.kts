import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val gatewayProps = Properties().apply {
    val f = rootProject.file("gateway.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val gatewayUrl = gatewayProps.getProperty("url", "https://your-gateway.example")
val gatewayKey = gatewayProps.getProperty("key", "")

android {
    namespace = "com.example.flowclone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.flowclone"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.0.0"
        buildConfigField("String", "GATEWAY_URL", "\"$gatewayUrl\"")
        buildConfigField("String", "GATEWAY_KEY", "\"$gatewayKey\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // 发布签名：把你自己生成的 keystore 放到 native/your-release.jks，
    // 或用 RELEASE_KEYSTORE 指向它；不存在时 release 构建为未签名包。
    val releaseKeystore = System.getenv("RELEASE_KEYSTORE")?.let { file(it) }
        ?: rootProject.file("your-release.jks")
    signingConfigs {
        if (releaseKeystore.exists()) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "flowclone"
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.media3:media3-transformer:1.4.1")
    implementation("androidx.media3:media3-effect:1.4.1")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
