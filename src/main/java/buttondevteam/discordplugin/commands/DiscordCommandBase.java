package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.handle.obj.IMessage;

import java.util.HashMap;

public abstract class DiscordCommandBase {
	public abstract String getCommandName();

	public abstract void run(IMessage message, String args);

	public abstract String[] getHelpText();

	static final HashMap<String, DiscordCommandBase> commands = new HashMap<String, DiscordCommandBase>();

	static {
		commands.put("connect", new ConnectCommand()); // TODO: API for adding commands?
		commands.put("userinfo", new UserinfoCommand());
		commands.put("help", new HelpCommand());
		commands.put("role", new RoleCommand());
		commands.put("mcchat", new MCChatCommand());
	}

	public static void runCommand(String cmd, String args, IMessage message) {
		DiscordCommandBase command = commands.get(cmd);
		if (command == null) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"Unknown command: " + cmd + " with args: " + args + "\nDo '"
							+ (message.getChannel().isPrivate() ? "" : message.getClient().getOurUser().mention() + " ")
							+ "help' for help");
			return;
		}
		try {
			command.run(message, args);
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while executing command " + cmd + "!", e);
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"An internal error occured while executing this command. For more technical details see the server-issues channel on the dev Discord.");
		}
	}
}
