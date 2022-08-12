echo " :: enter arguments [ex. '--open', '--init', or '--help', etc...]:"
read -r P
echo " "
# shellcheck disable=SC2086
java -jar "{{JAR_PATH}}" $P

printf "\n :: ended, press enter to close ::"
# shellcheck disable=SC2162
read