# DiscordRolesBot
Uses [JDA](https://github.com/DV8FromTheWorld/JDA)

### Prerequisites
- Basic command line ability (this README assumes a *nix environment)
- A server or always-on computer with
- Java 11+ installed
- Administrator access on a Discord server

### To set up a new instance of the bot:
1. Follow [these instructions](https://github.com/DV8FromTheWorld/JDA/wiki/3%29-Getting-Started) to create a new API token and add your bot to your server.
2. Get and compile the source code: 
    1. `$ git clone https://github.com/lpreams/DiscordRolesBot.git`
    2. `$ cd DiscordRolesBot`
    3. `$ ./gradlew clean build`
    4. File `./build/libs/rolesbot.jar` should now exist
3. Run the bot
    1. `$ java -jar ./build/libs/rolesbot.jar $DISCORD_TOKEN`
    2. but replace `$DISCORD_TOKEN` in the above command with the token you got in step 1.
4. Create a Role for your bot on your server. It should have permission to send/delete/edit its own messages, and add/remove any/all reactions from its own messages. 

### To use your new bot
1. As a Discord administrator, type `?addrole $ROLE`, replacing `$ROLE` with the name of an existing role on your server 
    - ***BE CAREFUL*** - the bot will allow you to add any Role here, and will attempt to assign any available Role to any requesting user. So if the bot has admin permissions (it probably shouldn't) and you add a Role with admin permissions, the bot will happily grant that role to anyone who asks for it. **Only add roles that you are comfortable giving out to any and all users!**
2. The bot will respond with a message telling you what to do. 
    - If `$ROLE` does not exist, you will need to edit server Roles or `$ROLE`
    - If `$ROLE` may refer to multiple Roles, you will need to give correct capitalization, or edit Roles on your server
    - If `$ROLE` does exist, the bot will ask that you react to its message. It will use the given reaction in the roles menu later
        - Any administrator may react to this message, not just the one who triggered it
3. Assuming the bot found the requested Role, react to its message to set the reaction for that Role.
4. Once you have added your desired roles, type `?rolesmenu` in any channel to have the bot set up a menu in that channel
    - It is recommended that you use a channel in which normal users are not allowed to send messages, so that the bot's menu message does not get burried
    - The bot will only allow a single menu to exist on each server, although the bot will happily exist on multiple servers without issue
        - Requesting another `?rolesmenu` in another channel will cause the bot to delete the original menu
5. If a roles menu already exists, using `?addrole` will cause the bot to edit the existing menu in real time.

