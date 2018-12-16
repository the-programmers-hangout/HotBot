# How to setup Hotbot

Note: this setup is based on Windows. Linux users should follow the same steps, though certain minor aspects (binaries, wording, etc) may differ.

In your preferred directory, `git clone` the source code from https://gitlab.com/Aberrantfox/hotbot or visit the site and download the source as a .zip archive.

## Installation

#### Install Java

Download the most recent release from the [Java Downloads](http://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html) page and proceed through the installer.

#### Install IntelliJ IDEA

Download your preferred edition of [IntelliJ](https://www.jetbrains.com/idea/download/#section=windows) and proceed through the installer.
For users who wish to customize their installation, only the Maven plugin is required. This plugin is included in the default installation.

#### Install Apache Maven 

Note: this is not necessary if the IntelliJ maven plugin was installed.

Download [Maven](https://maven.apache.org/download.cgi) and follow the [instructions](https://maven.apache.org/install.html) if needed.
It is required that Maven is added to PATH.

#### Install and setup MySQL 

Install [MySQL](https://dev.mysql.com/downloads/windows/installer/5.7.html) and follow the instructions below.

**Important Note**:  This is a somewhat dated installation of MySQL, but your test bot may not work with newer versions of MySQL.

1. If you want to install everything, it is fine to ignore the rest of the instructions in this section. 
   However, if you'd rather only install the components necessary for Hotbot, please continue reading.

3. When prompted, under "custom" install the following by selecting the components to install:
   - MySQL server
   - MySQL Workbench
   - MySQL Notifier
   - MySQL Utilities
   - MySQL Shell 
   - Connector/J
 
4. When prompted, select the following for each tab given:
	- Under "Group Replication", select "Standalone MySQL Server / Classic MySQL Replication"
	- Under "Type and Networking", click next
	- Under "Authentication", select the legacy option.
	- Under "Account and Roles", make and record the root password (you will need this later)
		- You don't need to make a user account
	- Under "Windows Service", it is your personal choice whether to start the service at startup or not
		- Keep everything else the same and finish the installation.

## Hotbot Setup

#### Create a Test Server
Create a Discord server for the bot to run on. Follow the [guide](https://support.discordapp.com/hc/en-us/articles/204849977-How-do-I-create-a-server-) if needed.

#### Create a Bot Account
Create one here: https://discordapp.com/developers/applications/me

- Under "General Information" 
	- Give an app icon and a name.
	- Note that the name is not changeable, so make sure you like it!
	- Record the client ID for future use.
- Under "Bot"
	- Create a bot.
	- Give it a username, app icon, and record the token for future use.
		- Note: This is a secret token, don't reveal it!
	- Uncheck the option to let it be a public bot so that only you can add it to servers.
- Save changes

#### Add the Bot to your Server 
Go to this link: https://discordapi.com/permissions.html

- Grant Administrator permissions to the bot by selecting the options.
- Under "OAth URL Generator" enter the bot's client ID and click the link to add it to your server.
- Make sure that the bot's role is the highest role (under Owner, of course.)
	
#### Create a database for Hotbot
1. Open up MySQL Workbench 
	- Click on the localhost - if it prompts you for a password, it is the root password that you saved earlier.
2. Make sure that the server status is running by clicking "server status" on the left hand side toolbar 
3. Click the button in the toolbar at the top that has the description "Create a new schema in the connected server"
4. Give the schema a name (which you will need to record for future use) and click apply a couple of times.


## Configure Hotbot
1. Under the main directory of hotbot's source code, go to the `config` folder.
2. Open "Config.json" with your favorite text editor (if you want to, you can open this in IDEA as well).
3. Under "serverInformation", put your bot's token with double quotes around them, making sure to preserve the JSON file structure.
4. Put your ID under the "ownerID field" 
	- to get your ID, enable "Developer Options" under "Appearance" in Discord settings, right click your name on a post you've made in the test server, and copy the ID
5. Choose a prefix that you would like the bot to respond to.
6. Copy-paste the ID of the test server for "guildID".
7. For each channel under "messageChannels" and "logChannels", create a channel for it in the test server and copy-paste the channel IDs as needed.
8. Under "databaseCredentials",
	- let the username be "root",
	- enter the root password for "password",
	- keep the hostname for "localhost",
	- and for "database" use the name you gave the database when in the MySQL Workbench.
9. Save the config file.


## Run Hotbot
1. Open a command prompt
2. CD into the main directory where the source was pulled from
3. Run "mvn clean package" in the main directory, making sure that Maven is in your PATH.
4. Run "java -jar \<targetJar\>.jar" on the jar with hotbot's dependencies.
	- This may look something like "java -jar target/hotbot-6.0.2-jar-with-dependencies.jar" 	

As an alternative, you can run Hotbot by opening the project in IntelliJ IDEA and clicking the green arrow directly left of the main function's definition in MainApp.kt (you may not see this if you're in Power Saving Mode)

Thanks for contributing to Hotbot!