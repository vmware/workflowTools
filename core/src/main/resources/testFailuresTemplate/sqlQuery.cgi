#! /usr/bin/bash
# This is a sample cgi file that can be modified for running a select SQL query

if [ -z "${QUERY_STRING##*--log=*}" ] ;then
  echo "Content-Type: text/plain"
  echo ""
  echo "Running java -jar /var/www/ctot/workflowTools.jar ExecuteSelectSqlStatementAsJson -c /var/www/ctot/ctotsConfig.json --script-mode --query-string=$QUERY_STRING 2>&1"
else
  echo "Content-Type: application/json"
  echo ""
fi



# Update jar path and config path to match local environment
java -jar /var/www/ctot/workflowTools.jar ExecuteSelectSqlStatementAsJson -c /var/www/ctot/ctotsConfig.json --script-mode --query-string=$QUERY_STRING 2>&1
