package com.github.maxopoly.kira.command.discord.admin;

import net.civmc.kira.Kira;
import com.github.maxopoly.kira.command.model.discord.Command;
import com.github.maxopoly.kira.command.model.top.InputSupplier;
import com.github.maxopoly.kira.permission.KiraRole;
import com.github.maxopoly.kira.permission.KiraRoleManager;

public class GiveDefaultPermission extends Command {

	public GiveDefaultPermission() {
		super("givedefaultperms");
	}

	@Override
	public String getFunctionality() {
		return "Gives the default permission to all accounts without permissions";
	}

	@Override
	public String getRequiredPermission() {
		return "admin";
	}

	@Override
	public String getUsage() {
		return "givedefaultperms";
	}

	@Override
	public String handleInternal(String argument, InputSupplier sender) {
		KiraRoleManager roleMan = Kira.Companion.getInstance().getKiraRoleManager();
		KiraRole defaultRole = roleMan.getDefaultRole();
		StringBuilder sb = new StringBuilder();
		Kira.Companion.getInstance().getUserManager().getAllUsers().forEach(u -> {
			if (roleMan.getRoles(u).isEmpty()) {
				roleMan.giveRoleToUser(u, defaultRole);
				sb.append("Giving default role to " + u.toString() + "\n");
			}

		});
		KiraRole authRole = roleMan.getRole("auth");
		Kira.Companion.getInstance().getUserManager().getAllUsers().stream().filter(u -> u.hasIngameAccount()).forEach(u ->{
			if(!roleMan.getRoles(u).contains(authRole)) {
				roleMan.giveRoleToUser(u, authRole);
				sb.append("Giving auth role to " + u.toString() + "\n");
			}

		});
		return sb.toString();
	}

}