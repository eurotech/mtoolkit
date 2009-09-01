if not defined BASEBUILDER_CVSROOT set BASEBUILDER_CVSROOT=:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse
if not defined BASEBUILDER_MODULE set BASEBUILDER_MODULE=org.eclipse.releng.basebuilder
if not defined BASEBUILDER_TAG set BASEBUILDER_TAG=HEAD
if not defined BASEBUILDER_DIR set BASEBUILDER_DIR=%~dp0\..\basebuilder

set BUILDER=%~dp0

set LAUNCHER_JAR=org.eclipse.equinox.launcher.jar

if exist %BASEBUILDER_DIR%\plugins\%LAUNCHER_JAR% goto buildOK

:fetchBaseBuilder
cvs -d%BASEBUILDER_CVSROOT% export -r %BASEBUILDER_TAG% -d %BASEBUILDER_DIR% %BASEBUILDER_MODULE%

:buildOK

for /f "tokens=*" %%i in ('%~dp0bin\date.exe -u +%%Y%%m%%d-%%H00') do set TIMESTAMP=%%i
java -jar %BASEBUILDER_DIR%\plugins\%LAUNCHER_JAR% -application org.eclipse.ant.core.antRunner -buildfile ~dp0build.xml -Dbuilder=%BUILDER% -Dtimestamp=%TIMESTAMP%

if not defined DONT_PAUSE pause
