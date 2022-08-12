echo :: enter arguments [ex. '--open', '--init', or '--help', etc...]:
@echo off
set /p "P= "
@echo on
java -jar "{{JAR_PATH}}" %P% -holdOpenOnFinish