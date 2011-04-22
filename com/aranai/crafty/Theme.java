package com.aranai.crafty;

import java.awt.Color;
import java.awt.Font;
import java.util.Hashtable;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class Theme {
	public static final String BG_BASE = "Background.Base";
	public static final String TEXT_BASE = "Text.Base";
	public static final String SYNTAX_TIMESTAMP = "Syntax.Timestamp";
	public static final String SYNTAX_INFO = "Syntax.Info";
	public static final String SYNTAX_WARNING = "Syntax.Warning";
	public static final String SYNTAX_SEVERE = "Syntax.Severe";
	public static final String SYNTAX_CRAFTY = "Syntax.Crafty";
	
	public static final String[] fonts = { TEXT_BASE, SYNTAX_TIMESTAMP, SYNTAX_INFO, SYNTAX_WARNING, SYNTAX_SEVERE, SYNTAX_CRAFTY };
	
	private String name;
	private String desc;
	private MutableAttributeSet m;
	
	private Hashtable<String,Color> themeColors;
	private Hashtable<String,Font> themeFonts;
	
	Theme()
	{
		this("Untitled Theme");
	}
	
	Theme(String setName)
	{
		this(setName, "Untitled theme");
	}
	
	Theme(String setName, String setDesc)
	{
		this(setName, setDesc, new Hashtable<String,Color>(), new Hashtable<String,Font>());
	}
	
	Theme(String setName, String setDesc, Hashtable<String,Color> colors, Hashtable<String,Font> fonts)
	{
		this.setName(setName);
		this.setDescription(setDesc);
		
		this.themeColors = new Hashtable<String,Color>();
		this.themeFonts = new Hashtable<String,Font>();
		
		// Set default colors & fonts
		this.setDefaultColors();
		this.setDefaultFonts();
		
		// Import specified colors and fonts
		this.themeColors.putAll(colors);
		this.themeFonts.putAll(fonts);
		
		this.m = null;
	}
	
	public void setName(String setName)
	{
		this.name = setName;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public void setDescription(String setDesc)
	{
		this.desc = setDesc;
	}
	
	public String getDescription()
	{
		return this.desc;
	}
	
	public void setBaseAttributeSet(MutableAttributeSet setM)
	{
		this.m = setM;
	}
	
	public void addColor(String newColor, String oldColor)
	{
		// Add an alias of an existing color
		Color c = this.getColor(oldColor);
		if(c != null)
		{
			this.themeColors.put(newColor, c);
		}
	}
	
	public void addColor(String colorName, Color color)
	{
		this.themeColors.put(colorName, color);
	}
	
	public Color getColor(String colorName)
	{
		if(this.themeColors.containsKey(colorName))
		{
			return this.themeColors.get(colorName);
		}
		
		return null;
	}
	
	public void addFont(String newFont, String oldFont)
	{
		// Add an alias of an existing font
		Font f = this.getFont(oldFont);
		if(f != null)
		{
			this.themeFonts.put(newFont, f);
		}
	}
	
	public void addFont(String fontName, Font font)
	{
		this.themeFonts.put(fontName, font);
	}
	
	public Font getFont(String fontName)
	{
		if(this.themeFonts.containsKey(fontName))
		{
			return this.themeFonts.get(fontName);
		}
		
		return null;
	}
	
	private void setDefaultColors()
	{
		this.addColor(Theme.BG_BASE, Color.WHITE);
		this.addColor(Theme.TEXT_BASE, Color.BLACK);
		this.addColor(Theme.SYNTAX_TIMESTAMP, Color.DARK_GRAY);
		this.addColor(Theme.SYNTAX_INFO, Color.BLUE);
		this.addColor(Theme.SYNTAX_WARNING, Color.ORANGE);
		this.addColor(Theme.SYNTAX_SEVERE, Color.RED);
		this.addColor(Theme.SYNTAX_CRAFTY, Color.GREEN);
	}
	
	private void setDefaultFonts()
	{
		Font baseFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
		this.setBaseFont(baseFont);
	}
	
	public void setBaseFont(Font f)
	{
		this.addFont(Theme.TEXT_BASE, f);
		this.addFont(Theme.SYNTAX_TIMESTAMP, f);
		this.addFont(Theme.SYNTAX_INFO, f);
		this.addFont(Theme.SYNTAX_WARNING, f);
		this.addFont(Theme.SYNTAX_SEVERE, f);
		this.addFont(Theme.SYNTAX_CRAFTY, f);
	}
	
	public void adjustFontSizeByFactor(float factor)
	{
		// Adjust the size for each font by multiplying its size by the specified factor
		for(String s : Theme.fonts)
		{
			Font f = this.getFont(s);
			float value = Math.round(f.getSize() * factor);
			
			Font f2 = f.deriveFont(value);
			this.addFont(s, f2);
		}
	}
	
	public void adjustFontSizeByAmount(float amount)
	{
		// Adjust the size for each font by multiplying its size by the specified factor
		for(String s : Theme.fonts)
		{
			Font f = this.getFont(s);
			float value = f.getSize() + amount;
			
			Font f2 = f.deriveFont(value);
			this.addFont(s, f2);
		}
	}
	
	public void setFontSize(float size)
	{
		// Set the size for each font to a fixed value
		for(String s : Theme.fonts)
		{
			Font f = this.getFont(s);
			Font f2 = f.deriveFont(size);
			this.addFont(s, f2);
		}
	}
	
	public SimpleAttributeSet getAttributeSet(String name)
	{
		SimpleAttributeSet s = new SimpleAttributeSet(m);
		
		// Get color
		Color c = this.getColor(name);
		
		// Get font
		Font f = this.getFont(name);
		
		s.addAttribute(StyleConstants.FontFamily, f.getFamily());
		if(f.isBold()) { s.addAttribute(StyleConstants.Bold, true); }
		if(f.isItalic()) { s.addAttribute(StyleConstants.Italic, true); }
		s.addAttribute(StyleConstants.FontSize, f.getSize());
		s.addAttribute(StyleConstants.Foreground, c);
		
		return s;
	}
}