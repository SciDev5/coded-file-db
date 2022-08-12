echo " :: enter arguments [ex. '--open', '--init', or '--help', etc...]:"
read -r P
echo " "
# shellcheck disable=SC2086
java -jar "{{JAR_PATH}}" $P -holdOpenOnFinish