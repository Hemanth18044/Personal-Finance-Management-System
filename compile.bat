@echo off
echo ===================================================
echo Compiling Personal Finance Management System...
echo ===================================================

if not exist target\classes mkdir target\classes

REM Compile all java source files targeting Java 17 for maximum compatibility
javac --release 17 -d target\classes -sourcepath src\main\java src\main\java\com\finance\model\*.java src\main\java\com\finance\util\*.java src\main\java\com\finance\dao\*.java src\main\java\com\finance\service\*.java src\main\java\com\finance\view\*.java src\main\java\com\finance\Main.java

if %ERRORLEVEL% EQU 0 (
    echo Compilation Successful! Compatible with Java 17 to Java 26+.
    if exist src\main\resources\db.properties (
        copy /Y src\main\resources\db.properties target\classes\db.properties >nul
    )
) else (
    echo [ERROR] Compilation failed.
)
pause
