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

set CLASSPATH=%APP_HOME%\lib\Icicle-1.0-SNAPSHOT.jar;%APP_HOME%\lib\discord4j-core-3.2.0-RC2.jar;%APP_HOME%\lib\configurate-yaml-4.1.1.jar;%APP_HOME%\lib\activejdbc-gradle-plugin-2.2.jar;%APP_HOME%\lib\activejdbc-3.0-SNAPSHOT.jar;%APP_HOME%\lib\app-config-3.0-SNAPSHOT.jar;%APP_HOME%\lib\javalite-common-3.0-SNAPSHOT.jar;%APP_HOME%\lib\log4j-slf4j-impl-2.13.3.jar;%APP_HOME%\lib\log4j-core-2.14.1.jar;%APP_HOME%\lib\log4j-api-2.14.1.jar;%APP_HOME%\lib\mysql-connector-java-8.0.25.jar;%APP_HOME%\lib\discord4j-rest-3.2.0-RC2.jar;%APP_HOME%\lib\discord4j-gateway-3.2.0-RC2.jar;%APP_HOME%\lib\discord4j-voice-3.2.0-RC2.jar;%APP_HOME%\lib\discord4j-common-3.2.0-RC2.jar;%APP_HOME%\lib\discord-json-1.6.7.jar;%APP_HOME%\lib\discord-json-api-1.6.7.jar;%APP_HOME%\lib\jackson-annotations-2.12.4.jar;%APP_HOME%\lib\jackson-dataformat-yaml-2.12.4.jar;%APP_HOME%\lib\jackson-datatype-jdk8-2.12.4.jar;%APP_HOME%\lib\stores-jdk-3.2.1.jar;%APP_HOME%\lib\stores-api-3.2.1.jar;%APP_HOME%\lib\jackson-databind-2.12.4.jar;%APP_HOME%\lib\jackson-core-2.12.4.jar;%APP_HOME%\lib\reflections-0.9.12.jar;%APP_HOME%\lib\snakeyaml-1.28.jar;%APP_HOME%\lib\configurate-core-4.1.1.jar;%APP_HOME%\lib\slf4j-api-1.7.30.jar;%APP_HOME%\lib\protobuf-java-3.11.4.jar;%APP_HOME%\lib\activejdbc-instrumentation-2.2.jar;%APP_HOME%\lib\javassist-3.26.0-GA.jar;%APP_HOME%\lib\reactor-extra-3.4.3.jar;%APP_HOME%\lib\reactor-netty-http-1.0.9.jar;%APP_HOME%\lib\reactor-netty-core-1.0.9.jar;%APP_HOME%\lib\reactor-core-3.4.8.jar;%APP_HOME%\lib\reactive-streams-1.0.3.jar;%APP_HOME%\lib\simple-fsm-1.0.1.jar;%APP_HOME%\lib\geantyref-1.3.11.jar;%APP_HOME%\lib\dom4j-2.1.3.jar;%APP_HOME%\lib\caffeine-2.8.8.jar;%APP_HOME%\lib\Servicer-1.0.3.jar;%APP_HOME%\lib\stax-api-1.0-2.jar;%APP_HOME%\lib\xsdlib-2013.6.1.jar;%APP_HOME%\lib\jaxb-api-2.2.12.jar;%APP_HOME%\lib\pull-parser-2.jar;%APP_HOME%\lib\xpp3-1.1.4c.jar;%APP_HOME%\lib\jaxen-1.2.0.jar;%APP_HOME%\lib\checker-qual-3.8.0.jar;%APP_HOME%\lib\error_prone_annotations-2.4.0.jar;%APP_HOME%\lib\netty-codec-http2-4.1.65.Final.jar;%APP_HOME%\lib\netty-handler-proxy-4.1.65.Final.jar;%APP_HOME%\lib\netty-codec-http-4.1.65.Final.jar;%APP_HOME%\lib\netty-resolver-dns-native-macos-4.1.65.Final-osx-x86_64.jar;%APP_HOME%\lib\netty-resolver-dns-4.1.65.Final.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.1.65.Final-linux-x86_64.jar;%APP_HOME%\lib\relaxngDatatype-20020414.jar;%APP_HOME%\lib\netty-handler-4.1.65.Final.jar;%APP_HOME%\lib\netty-codec-dns-4.1.65.Final.jar;%APP_HOME%\lib\netty-codec-socks-4.1.65.Final.jar;%APP_HOME%\lib\netty-codec-4.1.65.Final.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.65.Final.jar;%APP_HOME%\lib\netty-transport-4.1.65.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.65.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.65.Final.jar;%APP_HOME%\lib\netty-common-4.1.65.Final.jar


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
