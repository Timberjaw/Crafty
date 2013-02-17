package com.aranai.crafty;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

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
			Crafty.queueConsoleCommand("kick "+player);
			return;
		}
		
		// Ban user by name
		if(cmd.equals(Crafty.UserActions.BAN))
		{
			Crafty.queueConsoleCommand("ban "+player);
			return;
		}
		
		// Ban user by name
		if(cmd.equals(Crafty.UserActions.BANIP))
		{
			Crafty.queueConsoleCommand("ban-ip "+player);
			return;
		}
		
		// Get IP address (display in console)
		if(cmd.equals(Crafty.UserActions.GETIP))
		{
		    c.logMsg(c.helper.sendCommand(HelperCommands.GETIP, player));
			return;
		}
		
		// Op user
		if(cmd.equals(Crafty.UserActions.OP))
		{
			Crafty.queueConsoleCommand("op "+player);
			return;
		}
		
		// DeOp user
		if(cmd.equals(Crafty.UserActions.DEOP))
		{
			Crafty.queueConsoleCommand("deop "+player);
			return;
		}
    }
}
