package org.crafty;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class CraftyWindowListener implements WindowListener {
	
	private Crafty c;
	
	CraftyWindowListener(Crafty c)
	{
		this.c = c;
	}

	public void windowClosing(WindowEvent arg0) {
		c.close();
	}
	
	public void windowOpened(WindowEvent arg0) {
	}
	
	public void windowClosed(WindowEvent arg0) {
	}
	
	public void windowIconified(WindowEvent arg0) {
	}
	
	public void windowDeiconified(WindowEvent arg0) {
	}
	
	public void windowActivated(WindowEvent arg0) {
	}
	
	public void windowDeactivated(WindowEvent arg0) {
	}

}