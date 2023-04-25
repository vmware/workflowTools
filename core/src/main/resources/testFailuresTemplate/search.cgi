#! /usr/bin/bash
# This is a sample cgi file that can be modified for searching tests

if [ -z "${QUERY_STRING##*--log=*}" ] ;then
    echo "Content-Type: text/plain"
else
    echo "Content-Type: application/json"
fi

echo ""
# Update jar path and config path to match local environment
java -jar /var/www/ctot/workflowTools.jar FindJobsContainingTest -c /var/www/ctot/ctotsConfig.json --script-mode $QUERY_STRING 2>&1
