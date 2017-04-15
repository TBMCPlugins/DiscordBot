package buttondevteam.discordplugin.mccommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.discordplugin.listeners.MCChatListener;
import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.TBMCPlayer;
import buttondevteam.lib.player.TBMCPlayerBase;

public class AcceptMCCommand extends DiscordMCCommandBase {

	@Override
	public String GetDiscordCommandPath() {
		return "accept";
	}

	@Override
	public String[] GetHelpText(String alias) {
		return new String[] { //
				"§6---- Accept Discord connection ----", //
				"Accept a pending connection between your Discord and Minecraft account.", //
				"To start the connection process, do §b@ChromaBot connect <MCname>§r in the #bot channel on Discord", //
				"Usage: /" + alias + " accept" //
		};
	}

	@Override
	public boolean GetModOnly() {
		return false;
	}

	@Override
	public boolean GetPlayerOnly() {
		return true;
	}

	@Override
	public boolean OnCommand(CommandSender sender, String alias, String[] args) {
		String did = ConnectCommand.WaitingToConnect.get(sender.getName());
		if (did == null) {
			sender.sendMessage("§cYou don't have a pending connection to Discord.");
			return true;
		}
		DiscordPlayer dp = ChromaGamerBase.getUser(did, DiscordPlayer.class);
		TBMCPlayer mcp = TBMCPlayerBase.getPlayer(((Player) sender).getUniqueId(), TBMCPlayer.class);
		dp.connectWith(mcp);
		dp.save();
		mcp.save();
		ConnectCommand.WaitingToConnect.remove(sender.getName());
		MCChatListener.UnconnectedSenders.remove(did);
		sender.sendMessage("§bAccounts connected.");
		return true;
	}

}
