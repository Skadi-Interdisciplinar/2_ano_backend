@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo ==============================================
echo  Iniciando Backend Skadi (Spring Boot Java 21)
echo ==============================================
call .\mvnw.cmd spring-boot:run
pause
