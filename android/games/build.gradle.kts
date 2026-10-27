import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Lê a chave de assinatura de keystore.properties (arquivo local, fora do Git)
val ksProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "br.hy300.games"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.hy300.games"
        minSdk = 30
        targetSdk = 30
        versionCode = 4
        versionName = "0.2.0"

        // O projetor é de 32 bits
        ndk {
            abiFilters += listOf("armeabi-v7a")
        }
    }

    // Motor próprio (C++). Para compilar sem ele, ponha hy300.native=false em android/gradle.properties
    if (project.findProperty("hy300.native") != "false") {
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
            }
        }
    }

    // Extrai as bibliotecas nativas (o motor carrega o núcleo do Snes9x por caminho)
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    signingConfigs {
        create("release") {
            if (ksProps.isNotEmpty()) {
                storeFile = file(ksProps.getProperty("storeFile"))
                storePassword = ksProps.getProperty("storePassword")
                keyAlias = ksProps.getProperty("keyAlias")
                keyPassword = ksProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (ksProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Evita que o aviso de targetSdk antigo (30, de propósito: o aparelho é Android 11) barre o build de release
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}
