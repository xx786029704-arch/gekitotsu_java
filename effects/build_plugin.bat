@echo off
setlocal enabledelayedexpansion

:: Switch to script directory (drag-and-drop does not set working dir)
cd /d "%~dp0"
cd ..

echo ========================================
echo   GekitotsuKit - Effect Build ^& Package
echo ========================================
echo.

:: ========== 1. Locate classpath ==========
set "CP="
if exist "target\classes" (
    set "CP=target\classes"
    echo [OK] Found target\classes
) else if exist "gekitotsu_java-1.7.0.jar" (
    set "CP=gekitotsu_java-1.7.0.jar"
    echo [OK] Found gekitotsu_java-1.7.0.jar
) else if exist "dist\input\gekitotsu_java-1.7.0.jar" (
    set "CP=dist\input\gekitotsu_java-1.7.0.jar"
    echo [OK] Found dist\input\gekitotsu_java-1.7.0.jar
) else (
    echo [ERR] Cannot find classpath. Please run this script from the project root.
    pause
    exit /b 1
)

:: ========== 2. Locate javac ==========
set "JAVAC="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC=%JAVA_HOME%\bin\javac.exe"
        echo [OK] javac found via JAVA_HOME: %JAVA_HOME%
    )
)
if "%JAVAC%"=="" (
    where javac >nul 2>&1
    if !errorlevel! equ 0 (
        set "JAVAC=javac"
        echo [OK] javac found on PATH
    ) else (
        echo [ERR] javac not found. Please install JDK or set JAVA_HOME.
        pause
        exit /b 1
    )
)

:: ========== 3. Determine source file ==========
set "SRC_FILE=%1"
if "%SRC_FILE%"=="" (
    echo.
    echo Usage: drag a .java file onto this script, or run:
    echo   build_plugin.bat MyEffect.java
    echo.
    set /p "SRC_FILE=Enter .java file path: "
)
if "%SRC_FILE%"=="" (
    echo [ERR] No source file specified.
    pause
    exit /b 1
)
:: Remove surrounding quotes (drag-and-drop may add them)
set "SRC_FILE=%SRC_FILE:"=%"
if not exist "%SRC_FILE%" (
    echo [ERR] File not found: %SRC_FILE%
    pause
    exit /b 1
)
echo [OK] Source: %SRC_FILE%

:: ========== 4. Extract package and class name ==========
set "PACKAGE="
for /f "tokens=2 delims=; " %%a in ('findstr /r "^package" "%SRC_FILE%" 2^>nul') do set "PACKAGE=%%a"

set "CLASS_NAME=%~n1"
:: Remove quotes from class name too
set "CLASS_NAME=%CLASS_NAME:"=%"

if defined PACKAGE (
    set "FULL_CLASS=%PACKAGE%.%CLASS_NAME%"
) else (
    set "FULL_CLASS=%CLASS_NAME%"
)
echo        Full class: %FULL_CLASS%

:: ========== 5. Create temp build directory ==========
set "BUILD_DIR=effects\_build_temp"
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%"

:: ========== 6. Compile ==========
echo.
echo Compiling...
"%JAVAC%" -encoding UTF-8 -d "%BUILD_DIR%" -cp "%CP%" "%SRC_FILE%"
if !errorlevel! neq 0 (
    echo [ERR] Compilation failed! See error messages above.
    rmdir /s /q "%BUILD_DIR%"
    pause
    exit /b 1
)
echo [OK] Compilation successful

:: ========== 7. Generate META-INF/services ==========
mkdir "%BUILD_DIR%\META-INF\services"
echo %FULL_CLASS% > "%BUILD_DIR%\META-INF\services\org.example.GUI.Effect"
echo [OK] Service descriptor generated

:: ========== 8. Package JAR ==========
set "OUT_JAR=effects\%CLASS_NAME%.jar"
if not exist "effects" mkdir "effects"

pushd "%BUILD_DIR%"
jar cf "..\..\%OUT_JAR%" META-INF org
set "JAR_ERR=!errorlevel!"
popd
if !JAR_ERR! neq 0 (
    echo [ERR] JAR packaging failed
    rmdir /s /q "%BUILD_DIR%"
    pause
    exit /b 1
)
echo [OK] JAR created: %OUT_JAR%

:: Cleanup temp directory
rmdir /s /q "%BUILD_DIR%"

:: ========== 9. Done ==========
echo.
echo ========================================
echo   Plugin built: %OUT_JAR%
echo   Restart GekitotsuKit to see the new effect.
echo ========================================
echo.
pause
