package buttondevteam.discordplugin.role;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.DiscordCommandBase;
import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;

import java.util.List;
import java.util.stream.Collectors;

public class RoleCommand extends DiscordCommandBase { //TODO: Use Command2's parser

	private GameRoleModule grm;

	RoleCommand(GameRoleModule grm) {
		this.grm = grm;
	}

    @Override
    public String getCommandName() {
        return "role";
    }

    @Override
    public boolean run(IMessage message, String args) {
        if (args.length() == 0)
            return false;
        String[] argsa = splitargs(args);
        if (argsa[0].equalsIgnoreCase("add")) {
	        final IRole role = checkAndGetRole(message, argsa, "This command adds a role to your account.");
            if (role == null)
                return true;
            try {
                DPUtils.perform(() -> message.getAuthor().addRole(role));
	            DiscordPlugin.sendMessageToChannel(message.getChannel(), "Added role.");
            } catch (Exception e) {
                TBMCCoreAPI.SendException("Error while adding role!", e);
                DiscordPlugin.sendMessageToChannel(message.getChannel(), "An error occured while adding the role.");
            }
        } else if (argsa[0].equalsIgnoreCase("remove")) {
	        final IRole role = checkAndGetRole(message, argsa, "This command removes a role from your account.");
            if (role == null)
                return true;
            try {
                DPUtils.perform(() -> message.getAuthor().removeRole(role));
	            DiscordPlugin.sendMessageToChannel(message.getChannel(), "Removed role.");
            } catch (Exception e) {
                TBMCCoreAPI.SendException("Error while removing role!", e);
                DiscordPlugin.sendMessageToChannel(message.getChannel(), "An error occured while removing the role.");
            }
        } else if (argsa[0].equalsIgnoreCase("list")) {
            listRoles(message);
        } else return false;
        return true;
    }

    private void listRoles(IMessage message) {
        DiscordPlugin.sendMessageToChannel(message.getChannel(),
	        "List of roles:\n" + grm.GameRoles.stream().sorted().collect(Collectors.joining("\n")));
    }

    private IRole checkAndGetRole(IMessage message, String[] argsa, String usage) {
        if (argsa.length < 2) {
            DiscordPlugin.sendMessageToChannel(message.getChannel(), usage + "\nUsage: " + argsa[0] + " <rolename>");
            return null;
        }
        StringBuilder rolename = new StringBuilder(argsa[1]);
        for (int i = 2; i < argsa.length; i++)
            rolename.append(" ").append(argsa[i]);
	    if (!grm.GameRoles.contains(rolename.toString())) {
	        DiscordPlugin.sendMessageToChannel(message.getChannel(), "That role cannot be found.");
            listRoles(message);
            return null;
        }
        final List<IRole> roles = DiscordPlugin.mainServer.getRolesByName(rolename.toString());
        if (roles.size() == 0) {
            DiscordPlugin.sendMessageToChannel(message.getChannel(),
                    "The specified role cannot be found on Discord! Removing from the list.");
	        grm.GameRoles.remove(rolename.toString());
            return null;
        }
        if (roles.size() > 1) {
            DiscordPlugin.sendMessageToChannel(message.getChannel(),
                    "There are more roles with this name. Why are there more roles with this name?");
            return null;
        }
        return roles.get(0);
    }

    @Override
    public String[] getHelpText() {
        return new String[]{ //
	        "Add or remove roles from yourself.", //
                "Usage: " + DiscordPlugin.getPrefix() + "role add|remove <name> or role list", //
        };
    }

}