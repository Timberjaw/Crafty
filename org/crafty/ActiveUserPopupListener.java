package org.crafty;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class ActiveUserPopupListener extends MouseAdapter {
	private Crafty c;
	
	ActiveUserPopupListener(Crafty c)
	{
		this.c = c;
	}
	
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
        	int idx = c.activeUserList.locationToIndex(e.getPoint());
        	if(idx >= 0 && c.activeUserList.getCellBounds(idx, idx).contains(e.getPoint()))
        	{
        		// Select the list item
        		c.activeUserList.setSelectedIndex(idx);
        		c.activeUserPopup.show(e.getComponent(), e.getX(), e.getY());
        	}
        	else
        	{
        		c.activeUserList.clearSelection();
        	}
        }
    }
}