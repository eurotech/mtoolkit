set BASEBUILDER=%~dp0\..\basebuilder
set LAUNCHER_NAME=org.eclipse.equinox.launcher_1.0.200.v20090128-1500.jar
set PDEBUILD_NAME=org.eclipse.pde.build_3.5.0.v20090129
set TARGET=%~dp0\..\target
set BUILDER=%~dp0
for /f "tokens=*" %%i in ('%~dp0bin\date.exe -u +%%Y%%m%%d-%%H00') do set TIMESTAMP=%%i

java -jar %BASEBUILDER%\plugins\%LAUNCHER_NAME% -application org.eclipse.ant.core.antRunner -buildfile %BASEBUILDER%\plugins\%PDEBUILD_NAME%\scripts\build.xml -Dbuilder=%BUILDER% -Dtimestamp=%TIMESTAMP%

pause
