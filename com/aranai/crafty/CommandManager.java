package com.aranai.crafty;

public class CommandManager {
	private Crafty c;
	
	CommandManager(Crafty c)
	{
		this.c = c;
	}
	
	public void parse(String cmd)
	{
		String[] cmdArgs = cmd.split(" ");
		if(cmdArgs.length > 1)
		{
			if(cmdArgs[1].equalsIgnoreCase("theme"))
			{
				this.theme(cmdArgs);
			}
		}
		else
		{
			// No command
			c.logMsg("No command entered.");
		}
	}
	
	/*
	 * Theme commands
	 */
	
	private void theme(String[] cmdArgs)
	{
		// Theme actions
		if(cmdArgs.length > 2)
		{
			String action = cmdArgs[2];
			
			// Set theme
			if(action.equalsIgnoreCase("set"))
			{
				if(cmdArgs.length > 3)
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
