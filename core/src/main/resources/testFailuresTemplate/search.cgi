#! /usr/bin/bash
# This is a sample cgi file that can be modified for searching tests

urlDecode(){
  echo -e "$(sed 's/+/ /g;s/%\(..\)/\\x\1/g;')"
}

echo "Content-Type: application/json"
echo ""
decodedQuery=$(echo $QUERY_STRING | urlDecode | sed -r 's/&/ /g')

# Update jar path and config path to match local environment
java -jar /var/www/ctot/workflowTools.jar FindJobsContainingTest -c /var/www/ctot/config.json] --script-mode $decodedQuery 2>&1
