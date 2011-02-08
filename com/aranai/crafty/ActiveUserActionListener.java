package com.aranai.crafty;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.bukkit.entity.Player;

public class ActiveUserActionListener implements ActionListener {
	private Crafty c;
	
	ActiveUserActionListener(Crafty c)
	{		
		this.c = c;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		String player = "";
		String cmd = ((JMenuItem)e.getSource()).getText();
		
		if(c.activeUserList.getSelectedIndex() >= 0)
		{
			// Get selected player name
			player = (String)c.activeUserList.getSelectedValue();
		}
		
		// Kick user
		if(cmd.equals(Crafty.UserActions.KICK))
		{
			Crafty.queueConsoleCommand(c.ms.server, "kick "+player);
			return;
		}
		
		// Ban user by name
		if(cmd.equals(Crafty.UserActions.BAN))
		{
			Crafty.queueConsoleCommand(c.ms.server, "ban "+player);
			return;
		}
		
		// Ban user by name
		if(cmd.equals(Crafty.UserActions.BANIP))
		{
			Crafty.queueConsoleCommand(c.ms.server, "ban-ip "+player);
			return;
		}
		
		// Get IP address (display in console)
		if(cmd.equals(Crafty.UserActions.GETIP))
		{
			Player p = c.ms.server.getPlayer(player);
			if(p != null)
			{
				Crafty.logger.info("[Crafty] Player "+player+" has IP: "+p.getAddress().toString());
			}
			else
			{
				Crafty.logger.info("[Crafty] Could not find player "+player);
			}
			return;
		}
		
		// Op user
		if(cmd.equals(Crafty.UserActions.OP))
		{
			Crafty.queueConsoleCommand(c.ms.server, "op "+player);
			return;
		}
		
		// DeOp user
		if(cmd.equals(Crafty.UserActions.DEOP))
		{
			Crafty.queueConsoleCommand(c.ms.server, "deop "+player);
			return;
		}
    }
}
