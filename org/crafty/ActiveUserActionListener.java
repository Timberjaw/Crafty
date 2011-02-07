package org.crafty;

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
		if(c.activeUserList.getSelectedIndex() >= 0)
		{
			// Get selected player name
			player = (String)c.activeUserList.getSelectedValue();
		}
		
		if(((JMenuItem)e.getSource()).getText().equals("Kick"))
		{
			Crafty.queueConsoleCommand(c.ms.server, "kick "+player);
			return;
		}
		
		if(((JMenuItem)e.getSource()).getText().equals("Ban"))
		{
			Crafty.queueConsoleCommand(c.ms.server, "ban "+player);
			return;
		}
		
		if(((JMenuItem)e.getSource()).getText().equals("Get IP"))
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
		
		if(((JMenuItem)e.getSource()).getText().equals("Op"))
		{
			Crafty.queueConsoleCommand(c.ms.server, "op "+player);
			return;
		}
		
		if(((JMenuItem)e.getSource()).getText().equals("DeOp"))
		{
			Crafty.queueConsoleCommand(c.ms.server, "deop "+player);
			return;
		}
    }
}
