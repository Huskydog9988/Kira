package com.github.maxopoly.kira.command.discord.admin;

import net.civmc.kira.Kira;
import com.github.maxopoly.kira.command.model.discord.ArgumentBasedCommand;
import com.github.maxopoly.kira.command.model.top.InputSupplier;
import com.github.maxopoly.kira.permission.KiraPermission;
import com.github.maxopoly.kira.permission.KiraRole;
import com.github.maxopoly.kira.permission.KiraRoleManager;
import com.github.maxopoly.kira.user.KiraUser;

public class ListPermissionsForUserCommand extends ArgumentBasedCommand {

	public ListPermissionsForUserCommand() {
		super("listperms", 1, "getperms");
	}

	@Override
	public String getFunctionality() {
		return "Lists all permissions a user has";
	}

	@Override
	public String getRequiredPermission() {
		return "admin";
	}

	@Override
	public String getUsage() {
		return "listperms [role]";
	}

	@Override
	public String handle(InputSupplier sender, String[] args) {
		StringBuilder sb = new StringBuilder();
		KiraRoleManager roleMan = Kira.Companion.getInstance().getKiraRoleManager();
		KiraUser user = Kira.Companion.getInstance().getUserManager().parseUser(args[0], sb);
		if (user == null) {
			sb.append("User not found");
			return sb.toString();
		}
		if (roleMan.getRoles(user).isEmpty()) {
			sb.append("This user has no rules\n");
		}
		for (KiraRole role : roleMan.getRoles(user)) {
			sb.append("From " + role.getName() + ":\n");
			for (KiraPermission perm : role.getAllPermissions()) {
				sb.append(" - "+perm.getName()+"\n");
			}
		}
		return sb.toString();
	}

}
