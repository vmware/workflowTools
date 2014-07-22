For getting started info run<br/>
<b>java -jar workflow.jar</b>

The internal config file can be viewed at<br/>
<b>core/src/main/resources/internalConfig.json</b>

You can add a project specific config file to your repository by adding a file called<br/>
<b>.workflow-config.properties</b><br/>
in your projects root folder

You can add a user specific config file by adding a file called<br/>
<b>.workflow-config.properties</b><br/>
in your home folder

You can also specify a custom config file via two methods.<br/>
Specify -c [config file path] as a command line parameter.<br/>
Add a git configuration value for workflow.configFile.<br/>
<b>git config workflow.configFile [path to your config file]</b><br/>
That config file will then be used for every operation within that repository.

Config file values override values in the internalConfig.json file.<br/>
Only the values you want to override need to be specified in the new config file.

A list of alias commands that are worth adding to your ~/.bash_profile can be found in
<b>sampleConfig/aliasCommands.txt</b><br/>
Change the first line to match the directory where the workflow.jar is.

<b>FOR DEVELOPERS</b>

If you want to build the project use the following command<br/>
<b>mvn clean package -DskipTests</b><br/>
The built jar should be in core/target.

Running the tests in restApis requires updating the following file<br/>
<b>restApis/stc/test/resources/test.properties</b><br/>
The jenkins, reviewboard and jira urls in there should be changed to match your actual servers.

If someone has interest in adding perforce support to match the existing git support, that would really be appreciated.<br/>
Email me at dbiggs at vmare.com if you have questions.
