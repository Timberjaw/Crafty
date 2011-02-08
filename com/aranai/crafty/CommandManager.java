package com.aranai.crafty;

import java.util.Arrays;
import java.util.Hashtable;

public class CommandManager {
	private Crafty c;
	private String[] cmdArgs;
	private Hashtable<String,String> helpStrings;
	
	CommandManager(Crafty c)
	{
		this.c = c;
		this.prepHelpStrings();
	}
	
	public void prepHelpStrings()
	{
		this.helpStrings = new Hashtable<String,String>();
		
		// Help
		this.helpStrings.put("help", "Usage: .crafty help [command] | Description: This command.");
		
		// Theme
		this.helpStrings.put("theme", "Usage: .crafty theme [action] <value> | Description: Processes a theme action, such as 'set <value>' or 'current'.");
		this.helpStrings.put("theme set", "Usage: .crafty theme set <value> | Description: Sets the current theme to the specified theme. e.g. 'theme set Dark'.");
		this.helpStrings.put("theme current", "Usage: .crafty theme current | Description: Returns the name of the currently active theme.");
		
		// Version/About
		this.helpStrings.put("version", "Usage: .crafty version | Description: returns version and author information for Crafty. Alias of 'about'.");
		this.helpStrings.put("about", "Usage: .crafty about | Description: returns version and author information for Crafty. Alias of 'version'.");
		
		// Exit
		this.helpStrings.put("exit", "Usage: .crafty exit | Stops the server and closes Crafty.");
		
		// Stop
		this.helpStrings.put("stop", "Usage: .crafty stop | Description: Stops the server without closing Crafty.");
		
		// Restart
		this.helpStrings.put("restart", "Usage: .crafty restart | Description: stops and restarts the server without closing Crafty.");
	}
	
	public void parse(String cmd)
	{
		this.cmdArgs = cmd.split(" ");
		if(this.cmdArgs.length > 1)
		{
			if(this.cmdArgs[1].equalsIgnoreCase("help"))
			{
				this.help();
				return;
			}
			if(this.cmdArgs[1].equalsIgnoreCase("theme"))
			{
				this.theme();
				return;
			}
			if(this.cmdArgs[1].equalsIgnoreCase("version") || this.cmdArgs[1].equalsIgnoreCase("about"))
			{
				this.about();
				return;
			}
			if(this.cmdArgs[1].equalsIgnoreCase("exit"))
			{
				this.exit();
				return;
			}
			if(this.cmdArgs[1].equalsIgnoreCase("stop"))
			{
				this.stop();
				return;
			}
			if(this.cmdArgs[1].equalsIgnoreCase("restart"))
			{
				this.restart();
				return;
			}
			
			// Bad command
			c.logMsg("Unknown command.");
		}
		else
		{
			// No command
			c.logMsg("No command entered.");
		}
	}
	
	/*
	 * Help
	 */
	
	private void help()
	{
		if(this.cmdArgs.length > 2)
		{
			StringBuffer buf = new StringBuffer();
			for(String s : Arrays.copyOfRange(cmdArgs, 2, cmdArgs.length))
			{
				buf.append(" "+s);
			}
			String helpCmd = buf.toString().trim();
			
			if(this.helpStrings.containsKey(helpCmd)){
				c.logMsg(this.helpStrings.get(helpCmd));
			}
			else
			{
				c.logMsg("No info available on '"+helpCmd+"', sorry.");
			}
		}
		else
		{
			// Overview
			c.logMsg("Usage: .crafty <command> [args...]. All commands are prefixed with .crafty");
			c.logMsg("Use .crafty help <command> for more information about a particular command.");
			c.logMsg("Available commands:");
			for(String s : this.helpStrings.keySet())
			{
				c.logMsg(s);
			}
		}
	}
	
	/*
	 * One-liners
	 */
	
	private void about()
	{
		// Print out the current version info
		c.logMsg("Crafty Version " + Crafty.Version() + "\nAuthor: Steven \"Timberjaw\" Richards\nE-Mail: timberjaw@gmail.com");
	}
	
	private void exit()
	{
		// Stop the server and close the program
		this.c.close();
	}
	
	private void stop()
	{
		// Stop the server
	}
	
	private void restart()
	{
		// Restart the server
	}
	
	/*
	 * Themes
	 */
	
	private void theme()
	{
		// Theme actions
		if(this.cmdArgs.length > 2)
		{
			String action = cmdArgs[2];
			
			// Set theme
			if(action.equalsIgnoreCase("set"))
			{
				if(this.cmdArgs.length > 3)
				{
					String theme = cmdArgs[3];
					if(c.getThemeManager().themeAvailable(theme))
					{
						// Set theme
						c.setTheme(theme);
						c.logMsg("Theme changed.");
					}
					else
					{
						// Bad theme
						c.logMsg("Invalid theme specified.");
					}
				}
				else
				{
					// No theme specified
					c.logMsg("No theme specified.");
				}
			}
			
			// Current theme
			if(action.equalsIgnoreCase("current"))
			{
				String currentTheme = c.getThemeManager().getCurrentTheme().getName();
				c.logMsg("Current Theme: " + currentTheme);
			}
		}
		else
		{
			// No action specified
			c.logMsg("No action specified.");
		}
	}
}
