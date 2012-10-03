package com.aranai.crafty;

import net.minecraft.server.MinecraftServer;

public final class CraftyThreadServerApplication extends Thread {

    final MinecraftServer ms;

    public CraftyThreadServerApplication(String s, MinecraftServer minecraftserver) {
        super(s);
        this.ms = minecraftserver;
    }

    public void run() {
    	this.ms.run();
    }
    
    public void interrupt()
    {
    	ThreadGroup rootGroup = Thread.currentThread( ).getThreadGroup( );
    	System.out.println("Interrupting CTSA with "+rootGroup.activeCount()+" active threads.");
    	super.interrupt();
    }
}
