@echo off
echo ===================================================
echo Starting Personal Finance Management System...
echo ===================================================

if not exist target\classes\com\finance\Main.class (
    echo Compiled classes not found! Running compilation first...
    call compile.bat
)

REM Launch Java Application
java -cp target\classes com.finance.Main
pause
