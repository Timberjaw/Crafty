package com.aranai.crafty;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JMenuItem;

import com.aranai.crafty.Crafty.Location;

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
		
		// View on DynMap
        if(cmd.equals(Crafty.UserActions.MAP))
        {
            // Get coords
            Location l = c.getPlayerLocation(player);
            
            // Get DynMap URL
            String baseUrl = c.getDynMapUrl();
            
            // Build full URL
            String url = baseUrl+"/?worldname="+l.world+"&mapname=flat&zoom=3&x="+l.x+"&y="+l.y+"&z="+l.z;
            
            // Launch browser window
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(new URI(url));
                    }
                    catch(IOException ioe) {
                        ioe.printStackTrace();
                    }
                    catch(URISyntaxException use) {
                        use.printStackTrace();
                    }
                }
            }
            
            return;
        }
    }
}
