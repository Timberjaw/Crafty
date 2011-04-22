package com.aranai.crafty;

import java.lang.Thread.UncaughtExceptionHandler;


public class CraftyExceptionHandler implements UncaughtExceptionHandler {

	@Override
	public void uncaughtException(Thread arg0, Throwable e) {
		if(e instanceof ExitTrappedException)
		{
			Crafty.instance().stopServerDone();
		}
		else
		{
			System.out.print(e.getStackTrace());
		}
	}

}
