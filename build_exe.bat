@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

REM ============================================================
REM GekitotsuKit one-click build script
REM   Output:
REM     dist\激突Kit\激突Kit.exe          (jpackage image)
REM     dist\激突Kit v<version>.zip       (release zip)
REM ============================================================

REM === Path config (edit if needed) ===
set JDK_HOME=C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9
set M2=C:\Users\cain\.m2\repository
set FLATLAF=%M2%\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar
set JACKSON_DB=%M2%\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar
set JACKSON_CORE=%M2%\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar
set JACKSON_ANN=%M2%\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar

set SRC=src\main\java
set RES=src\main\resources
set OUT=target\classes
set STAGE=target\stage
set DIST=dist
set APP_NAME=激突Kit
set MAIN_CLASS=org.example.Main
set ICON=assets\icon.ico

set JAVAC=%JDK_HOME%\bin\javac.exe
set JAR=%JDK_HOME%\bin\jar.exe
set JPACKAGE=%JDK_HOME%\bin\jpackage.exe
set CP=%FLATLAF%;%JACKSON_DB%;%JACKSON_CORE%;%JACKSON_ANN%

REM === 必须在项目根目录运行 ===
if not exist pom.xml (
  echo [错误] 未找到 pom.xml，请在项目根目录运行本脚本
  goto :fail
)

REM === 解析版本号（pom.xml 第一个 <version>）===
set VERSION=
for /f "tokens=3 delims=<>" %%v in ('findstr /c:"<version>" pom.xml') do (
  set VERSION=%%v
  goto :got_ver
)
:got_ver
if "%VERSION%"=="" (
  echo [错误] 无法从 pom.xml 解析版本号
  goto :fail
)
echo [信息] 版本号: %VERSION%

REM === 从 pom.xml 生成 Version.java（版本号唯一权威源）===
set VERSION_FILE=%SRC%\org\example\Version.java
powershell -NoProfile -ExecutionPolicy Bypass -File _gen_version.ps1 -Version "%VERSION%" -VersionFile "%VERSION_FILE%"
if errorlevel 1 (
  echo [错误] 生成 Version.java 失败
  goto :fail
)

REM === 同步文档版本号（CLAUDE.md / 使用手册.md）===
powershell -NoProfile -ExecutionPolicy Bypass -File _sync_docs.ps1 -Version "%VERSION%"

REM === 校验 JDK 工具链 ===
if not exist "%JAVAC%"    ( echo [错误] 未找到 javac: %JAVAC%    & goto :fail )
if not exist "%JAR%"      ( echo [错误] 未找到 jar: %JAR%        & goto :fail )
if not exist "%JPACKAGE%" ( echo [错误] 未找到 jpackage: %JPACKAGE% & goto :fail )
if not exist "%ICON%"     ( echo [错误] 未找到图标: %ICON%       & goto :fail )

REM === 校验依赖 JAR ===
for %%f in ("%FLATLAF%" "%JACKSON_DB%" "%JACKSON_CORE%" "%JACKSON_ANN%") do (
  if not exist %%f (
    echo [错误] 缺少依赖: %%f
    goto :fail
  )
)

REM === 1. 编译源码 ===
echo [1/5] 编译源码...
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%" > nul 2>&1
dir /s /b "%SRC%\*.java" > sources.txt 2> nul
"%JAVAC%" -encoding UTF-8 -d "%OUT%" -cp "%CP%" -sourcepath "%SRC%" @sources.txt
if errorlevel 1 (
  echo [错误] 编译失败
  del sources.txt > nul 2>&1
  goto :fail
)
del sources.txt > nul 2>&1

REM 复制资源到 target/classes（javac 不复制资源；IDEA 运行时依赖 target/classes 作 classpath）
xcopy /E /I /Q /Y "%RES%" "%OUT%" > nul

REM === 2. 组装 fat JAR 暂存目录（classes + 依赖 + 资源）===
echo [2/5] 组装 fat JAR 暂存目录...
if exist "%STAGE%" rmdir /s /q "%STAGE%"
mkdir "%STAGE%" > nul 2>&1
xcopy /E /I /Q /Y "%OUT%" "%STAGE%" > nul
pushd "%STAGE%"
"%JAR%" xf "%FLATLAF%"
"%JAR%" xf "%JACKSON_DB%"
"%JAR%" xf "%JACKSON_CORE%"
"%JAR%" xf "%JACKSON_ANN%"
popd
REM 资源覆盖依赖中的同名文件（icon.ico / formula.json / unit_details.json 等）
xcopy /E /I /Q /Y "%RES%" "%STAGE%" > nul

REM === 3. 打 fat JAR ===
echo [3/5] 打包 fat JAR...
if not exist "%DIST%\input" mkdir "%DIST%\input"
del /q "%DIST%\input\gekitotsu_java-*.jar" 2> nul
set FAT_JAR=%DIST%\input\gekitotsu_java-%VERSION%.jar
"%JAR%" cfm "%FAT_JAR%" "%RES%\META-INF\MANIFEST.MF" -C "%STAGE%" .
if errorlevel 1 (
  echo [错误] 打包 JAR 失败
  goto :fail
)

REM === 4. jpackage 生成 EXE 镜像 ===
echo [4/5] 生成 EXE 镜像...

REM 缓存 runtime 以加速重复构建（首次构建后把 runtime 复制到 dist\runtime_cache）
if exist "%DIST%\%APP_NAME%\runtime" if not exist "%DIST%\runtime_cache" (
  echo [信息] 缓存 runtime 以加速后续构建...
  xcopy /E /I /Q /Y "%DIST%\%APP_NAME%\runtime" "%DIST%\runtime_cache" > nul
)

REM jpackage 要求目标目录不存在
if exist "%DIST%\%APP_NAME%" rmdir /s /q "%DIST%\%APP_NAME%"

set RUNTIME_FLAG=
if exist "%DIST%\runtime_cache" set RUNTIME_FLAG=--runtime-image "%DIST%\runtime_cache"

"%JPACKAGE%" --type app-image --name "%APP_NAME%" --app-version %VERSION% ^
  --input "%DIST%\input" --main-jar "gekitotsu_java-%VERSION%.jar" ^
  --main-class %MAIN_CLASS% --icon "%ICON%" --dest "%DIST%" %RUNTIME_FLAG%
if errorlevel 1 (
  echo [错误] jpackage 失败
  goto :fail
)

REM 首次构建时把生成的 runtime 复制到缓存，供下次复用
if not exist "%DIST%\runtime_cache" if exist "%DIST%\%APP_NAME%\runtime" (
  xcopy /E /I /Q /Y "%DIST%\%APP_NAME%\runtime" "%DIST%\runtime_cache" > nul
)

REM === 5. 复制 effects/ 插件目录 + 打包 zip ===
echo [5/5] 复制插件目录与打包 zip...
if exist "%DIST%\%APP_NAME%\effects" rmdir /s /q "%DIST%\%APP_NAME%\effects"
xcopy /E /I /Q /Y "effects" "%DIST%\%APP_NAME%\effects" > nul

set ZIP_PATH=%DIST%\激突Kit v%VERSION%.zip
if exist "%ZIP_PATH%" del "%ZIP_PATH%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%DIST%\%APP_NAME%\*' -DestinationPath '%ZIP_PATH%' -Force"
if errorlevel 1 (
  echo [警告] zip 打包失败，但 EXE 已生成
  goto :done_warn
)

:done_warn
echo.
echo ============================================================
echo 打包完成！
echo   EXE : %DIST%\%APP_NAME%\%APP_NAME%.exe
echo   JAR : %FAT_JAR%
echo   ZIP : %ZIP_PATH%
echo ============================================================
echo.
pause
exit /b 0

:fail
echo.
echo 打包失败，请检查上方错误信息
pause
exit /b 1
