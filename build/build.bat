setlocal
if not defined BASEBUILDER_CVSROOT set BASEBUILDER_CVSROOT=:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse
if not defined BASEBUILDER_MODULE set BASEBUILDER_MODULE=org.eclipse.releng.basebuilder
if not defined BASEBUILDER_TAG set BASEBUILDER_TAG=R3_6_2
if not defined BASEBUILDER_DIR set BASEBUILDER_DIR=%~dp0\..\basebuilder

set BUILDER=%~dp0

set LAUNCHER_JAR=org.eclipse.equinox.launcher.jar

if exist %BASEBUILDER_DIR%\org.eclipse.releng.basebuilder\plugins\%LAUNCHER_JAR% goto buildOK

:fetchBaseBuilder
md %BASEBUILDER_DIR%
cd %BASEBUILDER_DIR%
cvs -d%BASEBUILDER_CVSROOT% co -r %BASEBUILDER_TAG% %BASEBUILDER_MODULE%
cd %~dp0

:buildOK
for /f "tokens=*" %%i in ('%~dp0bin\date.exe -u +%%Y%%m%%d-%%H%%M') do set TIMESTAMP=%%i
java -jar %BASEBUILDER_DIR%\org.eclipse.releng.basebuilder\plugins\%LAUNCHER_JAR% -application org.eclipse.ant.core.antRunner -buildfile %~dp0\build.xml -Dbuilder=%BUILDER% -Dtimestamp=%TIMESTAMP% %BUILDER_ARGUMENTS%

if not defined DONT_PAUSE pause
endlocal
