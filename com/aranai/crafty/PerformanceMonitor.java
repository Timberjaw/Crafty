package com.aranai.crafty;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

// Code adapted from: http://stackoverflow.com/questions/25552/using-java-to-get-os-level-system-information/61727#61727

public class PerformanceMonitor { 
    private int  availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    private long lastSystemTime      = 0;
    private long lastProcessCpuTime  = 0;
    
    static final Runtime runtime = Runtime.getRuntime ();

    @SuppressWarnings("restriction")
	public synchronized double getCpuUsage()
    {
        if ( lastSystemTime == 0 )
        {
            baselineCounters();
            return 0.0;
        }

        double cpuUsage = 0.0;
	    long systemTime     = System.nanoTime();
	    long processCpuTime = 0;
	    
	    try
	    {
		    if ( ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean )
		    {
		        processCpuTime = ( (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean() ).getProcessCpuTime();
		    }
		
		    cpuUsage = (double) ( processCpuTime - lastProcessCpuTime ) / ( systemTime - lastSystemTime );
		
		    lastSystemTime     = systemTime;
		    lastProcessCpuTime = processCpuTime;
	    }
	    catch(ClassCastException e)
	    {
	    	// Non-Sun JVM?
	    }
	
	    return (cpuUsage / availableProcessors) * 100.0;
	}
	
	@SuppressWarnings("restriction")
	private void baselineCounters()
	{
	    lastSystemTime = System.nanoTime();
	    
	    try
	    {
		    if ( ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean )
		    {
		        lastProcessCpuTime = ( (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean() ).getProcessCpuTime();
		    }
	    }
	    catch(ClassCastException e)
	    {
	    	// Non-Sun JVM?
	    }
	}
	
	static long memoryUsed ()
    {
        return runtime.totalMemory () - runtime.freeMemory ();
    }
	
	static long memoryAvailable()
	{
		return runtime.totalMemory();
	}
	
	static int threadsUsed()
	{
		ThreadMXBean tb = ManagementFactory.getThreadMXBean();
		return tb.getThreadCount();  
	}
}