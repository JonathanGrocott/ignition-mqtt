@echo off
REM #############################################################################
REM MQTT UNS Publisher Module - Complete Build Script (Windows)
REM #############################################################################
REM This script builds the complete module including:
REM 1. React/TypeScript frontend (web UI)
REM 2. Java backend (Gateway module)
REM 3. Packages everything into a .modl file
REM #############################################################################

setlocal enabledelayedexpansion

REM Project paths
set "PROJECT_ROOT=%~dp0"
set "WEB_UI_DIR=%PROJECT_ROOT%mqtt-gateway\web-ui"
set "MOUNTED_DIR=%PROJECT_ROOT%mqtt-gateway\src\main\resources\mounted"

echo ================================================================
echo    MQTT UNS Publisher Module - Complete Build
echo ================================================================
echo.

REM #############################################################################
REM Step 1: Check Prerequisites
REM #############################################################################
echo [1/4] Checking prerequisites...

REM Check for Node.js
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [X] Node.js is not installed. Please install Node.js 16+ and try again.
    exit /b 1
)
for /f "tokens=*" %%i in ('node --version') do set NODE_VERSION=%%i
echo [+] Node.js found: %NODE_VERSION%

REM Check for npm
where npm >nul 2>nul
if %errorlevel% neq 0 (
    echo [X] npm is not installed. Please install npm and try again.
    exit /b 1
)
for /f "tokens=*" %%i in ('npm --version') do set NPM_VERSION=%%i
echo [+] npm found: %NPM_VERSION%

REM Check for Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [X] Java is not installed. Please install Java 17+ and try again.
    exit /b 1
)
echo [+] Java found

REM Check for Gradle wrapper
if not exist "%PROJECT_ROOT%gradlew.bat" (
    echo [X] Gradle wrapper not found. Please ensure gradlew.bat exists in project root.
    exit /b 1
)
echo [+] Gradle wrapper found
echo.

REM #############################################################################
REM Step 2: Build Frontend (React/TypeScript)
REM #############################################################################
echo [2/4] Building frontend (React/TypeScript)...

cd /d "%WEB_UI_DIR%"

REM Check if node_modules exists, install if needed
if not exist "node_modules" (
    echo   -^> Installing npm dependencies...
    call npm install
    if %errorlevel% neq 0 (
        echo [X] npm install failed
        exit /b 1
    )
) else (
    echo   -^> npm dependencies already installed
)

REM Build React application
echo   -^> Building React application with webpack...
call npm run build
if %errorlevel% neq 0 (
    echo [X] Frontend build failed
    exit /b 1
)

REM Verify output file exists
if not exist "%MOUNTED_DIR%\mqtt-config.js" (
    echo [X] Frontend build failed: mqtt-config.js not found
    exit /b 1
)

echo [+] Frontend built successfully: mqtt-config.js
echo.

REM #############################################################################
REM Step 3: Build Backend (Java/Gradle)
REM #############################################################################
echo [3/4] Building backend (Java/Gradle)...

cd /d "%PROJECT_ROOT%"

REM Clean previous builds
echo   -^> Cleaning previous builds...
call gradlew.bat clean --quiet

REM Build the module
echo   -^> Compiling Java code and packaging module...
call gradlew.bat build
if %errorlevel% neq 0 (
    echo [X] Gradle build failed
    exit /b 1
)

echo [+] Backend built successfully
echo.

REM #############################################################################
REM Step 4: Locate and Display Output
REM #############################################################################
echo [4/4] Locating build artifacts...

REM Find the .modl file
for /r "%PROJECT_ROOT%build" %%f in (*.modl) do (
    set "MODL_FILE=%%f"
    goto :found_modl
)

echo [X] Could not find .modl file in build directory
exit /b 1

:found_modl
for %%f in ("%MODL_FILE%") do set "MODL_NAME=%%~nxf"
echo [+] Module file created: %MODL_NAME%
echo.

REM #############################################################################
REM Success Summary
REM #############################################################################
echo ================================================================
echo    Build Completed Successfully!
echo ================================================================
echo.
echo Module file location:
echo   %MODL_FILE%
echo.
echo Next steps:
echo   1. Navigate to your Ignition Gateway web interface
echo   2. Go to Config -^> System -^> Modules
echo   3. Click 'Install or Upgrade a Module'
echo   4. Upload: %MODL_NAME%
echo   5. After installation, navigate to Config -^> MQTT UNS Publisher
echo.
echo Web UI will be available at:
echo   http://your-gateway:8088/web/config/mqtt-uns-publisher
echo.
echo Happy publishing!
echo.

endlocal
