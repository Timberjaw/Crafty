package org.crafty;

import java.util.Hashtable;

public class ThemeManager {
	private Hashtable<String,Theme> themes;
	private Theme currentTheme;
	
	ThemeManager()
	{
		this.themes = new Hashtable<String,Theme>();
		this.currentTheme = null;
	}
	
	public void addTheme(Theme t)
	{
		this.themes.put(t.getName(), t);
	}
	
	public boolean themeAvailable(String name)
	{
		return this.themes.containsKey(name);
	}
	
	public Theme getTheme(String name)
	{
		if(this.themeAvailable(name))
		{
			return this.themes.get(name);
		}
		
		return null;
	}
	
	public void setCurrentTheme(String setTheme)
	{
		this.currentTheme = this.getTheme(setTheme);
	}
	
	public Theme getCurrentTheme()
	{
		return this.currentTheme;
	}
}