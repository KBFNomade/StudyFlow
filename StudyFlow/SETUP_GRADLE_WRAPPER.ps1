$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$wrapperDir = Join-Path $root "gradle\wrapper"
New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null
Invoke-WebRequest "https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradlew" -OutFile (Join-Path $root "gradlew")
Invoke-WebRequest "https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradlew.bat" -OutFile (Join-Path $root "gradlew.bat")
Invoke-WebRequest "https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar" -OutFile (Join-Path $wrapperDir "gradle-wrapper.jar")
Write-Host "Gradle Wrapper 8.9 preparado. Mantenha o Gradle JVM do Android Studio em JDK 17."
