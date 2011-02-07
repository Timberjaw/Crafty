package org.crafty;

import java.awt.Color;
import java.awt.Font;

import javax.swing.text.MutableAttributeSet;

public class ThemeBuilder {
	private Theme t;
	
	ThemeBuilder(){ this.t = new Theme(); }
	
	public ThemeBuilder setName(String n) { t.setName(n); return this; }
	public ThemeBuilder setDescription(String d) { t.setDescription(d); return this; }
	
	public ThemeBuilder setAttributeSet(MutableAttributeSet m) { t.setBaseAttributeSet(m); return this; }
	public ThemeBuilder setBaseFont(Font f) { t.setBaseFont(f); return this; }
	
	public ThemeBuilder addColor(String n, String o) { t.addColor(n, o); return this; }
	public ThemeBuilder addFont(String n, String o) { t.addFont(n, o); return this; }
	public ThemeBuilder addColor(String n, Color c) { t.addColor(n, c); return this; }
	public ThemeBuilder addFont(String n, Font f) { t.addFont(n, f); return this; }
	
	public Theme getTheme() { return t; }
}