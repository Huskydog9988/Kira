package com.github.maxopoly.kira.command.discord.admin;

import net.civmc.kira.Kira;
import com.github.maxopoly.kira.command.model.discord.ArgumentBasedCommand;
import com.github.maxopoly.kira.command.model.top.InputSupplier;
import com.github.maxopoly.kira.permission.KiraRole;
import com.github.maxopoly.kira.permission.KiraRoleManager;
import com.github.maxopoly.kira.user.KiraUser;

public class GiveRoleCommand extends ArgumentBasedCommand {

	public GiveRoleCommand() {
		super("giverole", 2, "addrole");
	}

	@Override
	public String getFunctionality() {
		return "Gives a role to a user";
	}

	@Override
	public String getRequiredPermission() {
		return "admin";
	}

	@Override
	public String getUsage() {
		return "giverole [role] [user]";
	}

	@Override
	public String handle(InputSupplier sender, String[] args) {
		StringBuilder sb = new StringBuilder();
		KiraRoleManager roleMan = Kira.Companion.getInstance().getKiraRoleManager();
		KiraRole role = roleMan.getRole(args[0]);
		if (role == null) {
			sb.append("Role " + args[0] + " not found");
			return sb.toString();
		}
		KiraUser user = Kira.Companion.getInstance().getUserManager().parseUser(args[1], sb);
		if (user == null) {
			sb.append("User not found");
			return sb.toString();
		}
		if (roleMan.getRoles(user).contains(role)) {
			sb.append(user.toString() + " already has role " + role.getName());
		} else {
			roleMan.giveRoleToUser(user, role);
			sb.append("Giving role " + role.getName() + " to " + user.toString());
		}
		return sb.toString();
	}

}
