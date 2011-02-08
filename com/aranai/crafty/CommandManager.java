package com.aranai.crafty;

public class CommandManager {
	private Crafty c;
	private String[] cmdArgs;
	
	CommandManager(Crafty c)
	{
		this.c = c;
	}
	
	public void parse(String cmd)
	{
		this.cmdArgs = cmd.split(" ");
		if(this.cmdArgs.length > 1)
		{
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
