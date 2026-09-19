// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

// Automatically ensure debug.keystore exists before build execution
val debugKeystoreFile = file("debug.keystore")
val debugKeystoreBase64File = file("debug.keystore.base64")
if (!debugKeystoreFile.exists()) {
  if (debugKeystoreBase64File.exists()) {
    val decoded = java.util.Base64.getDecoder().decode(debugKeystoreBase64File.readText().trim())
    debugKeystoreFile.writeBytes(decoded)
  } else {
    val keytoolCmd = arrayOf(
      "keytool", "-genkeypair", "-v",
      "-keystore", debugKeystoreFile.absolutePath,
      "-alias", "androiddebugkey",
      "-keypass", "android",
      "-storepass", "android",
      "-keyalg", "RSA",
      "-keysize", "2048",
      "-validity", "10000",
      "-dname", "CN=Android Debug,O=Android,C=US"
    )
    ProcessBuilder(*keytoolCmd).redirectErrorStream(true).start().waitFor()
  }
}

