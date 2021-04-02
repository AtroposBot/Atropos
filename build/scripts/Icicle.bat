@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Icicle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and ICICLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\Icicle-1.0-SNAPSHOT.jar;%APP_HOME%\lib\JDA-4.2.0_247.jar;%APP_HOME%\lib\configurate-yaml-4.0.0.jar;%APP_HOME%\lib\activejdbc-gradle-plugin-2.2.jar;%APP_HOME%\lib\activejdbc-2.5-j8.jar;%APP_HOME%\lib\app-config-2.5-j8.jar;%APP_HOME%\lib\javalite-common-2.5-j8.jar;%APP_HOME%\lib\log4j-slf4j-impl-2.13.3.jar;%APP_HOME%\lib\log4j-core-2.14.1.jar;%APP_HOME%\lib\log4j-api-2.14.1.jar;%APP_HOME%\lib\cloud-jda-1.4.0.jar;%APP_HOME%\lib\cloud-core-1.4.0.jar;%APP_HOME%\lib\mysql-connector-java-8.0.23.jar;%APP_HOME%\lib\jackson-dataformat-yaml-2.10.3.jar;%APP_HOME%\lib\jackson-databind-2.11.3.jar;%APP_HOME%\lib\jackson-core-2.11.3.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\annotations-16.0.1.jar;%APP_HOME%\lib\slf4j-api-1.7.30.jar;%APP_HOME%\lib\nv-websocket-client-2.14.jar;%APP_HOME%\lib\okhttp-3.13.0.jar;%APP_HOME%\lib\opus-java-1.0.4.pom;%APP_HOME%\lib\commons-collections4-4.1.jar;%APP_HOME%\lib\trove4j-3.0.3.jar;%APP_HOME%\lib\snakeyaml-1.27.jar;%APP_HOME%\lib\configurate-core-4.0.0.jar;%APP_HOME%\lib\cloud-services-1.4.0.jar;%APP_HOME%\lib\geantyref-1.3.11.jar;%APP_HOME%\lib\protobuf-java-3.11.4.jar;%APP_HOME%\lib\jackson-annotations-2.11.3.jar;%APP_HOME%\lib\activejdbc-instrumentation-2.2.jar;%APP_HOME%\lib\okio-1.17.2.jar;%APP_HOME%\lib\opus-java-api-1.0.4.jar;%APP_HOME%\lib\opus-java-natives-1.0.4.jar;%APP_HOME%\lib\dom4j-2.1.3.jar;%APP_HOME%\lib\jna-4.4.0.jar;%APP_HOME%\lib\stax-api-1.0-2.jar;%APP_HOME%\lib\xsdlib-2013.6.1.jar;%APP_HOME%\lib\jaxb-api-2.2.12.jar;%APP_HOME%\lib\pull-parser-2.jar;%APP_HOME%\lib\xpp3-1.1.4c.jar;%APP_HOME%\lib\jaxen-1.2.0.jar;%APP_HOME%\lib\relaxngDatatype-20020414.jar


@rem Execute Icicle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %ICICLE_OPTS%  -classpath "%CLASSPATH%" dev.laarryy.Icicle.Icicle %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable ICICLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%ICICLE_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
