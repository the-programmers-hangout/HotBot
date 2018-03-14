# Note 
These are out of date. New guide to come.

#### Overview
Hotbot is a single-server bot. It can operate on multiple servers but it is
not tested, and definitely not recommended. In the future, it may take into 
account the different guilds it is a part of but for now it operates under the
assumption of there being only one guild.

With that being said, the installation is fairly trivial on a linux machine.

##### Pre-requisites
Have the following installed and configured:

- JDK 8
- JRE 8
- Maven
- MySQL Server

##### 1.
Clone the repository.

 `git clone https://github.com/AberrantFox/hotbot.git`
 
##### 2.
Perform a single build.

`mvn clean package`

##### 3. 
Cd into the target directory

`cd target`

##### 4. 
Run the bot

`java -jar aegus.x.x.x-shaded.jar`

*Note*: The above command should not be copy-pasted. Run java -jar and then the third and final argument
should be the full name of the jar file, which changes with each release.

Running the bot will generate some default configurations. You'll also need
to drag help.json into this target directory (this step will be removed later)

##### 5.
After moving help.json into the directory (you may edit this to your own language) you should
then fill out the configuration file. At minimum, you need:

 - Bot token
 - Your owner id
 - The two channels (Welcome + Leave channels)
 - The credentials to the database.

#### 6.
Yes, the last step is correct, you need a MySQL server. 
The setup process for the mysql server is as follows.

  - Start mysql: `mysql -u root -p`
  - Login: Enter your root pass (or other user is cool too)
  - Create the database: `CREATE DATABASE hotbot;`
  - Exit: `exit;`

#### 7.
After filling in the entire config, and setting up MySQL, you can now
run the bot. `java -jar _shaded_jar_file_name_here`


#### 8.
If you find any bugs or have any suggestions, open an issue.
Use $help to get started, and if this prefix conflicts you may just use
$prefix !! or something. Have fun!  
