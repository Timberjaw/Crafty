package com.aranai.crafty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.security.Permission;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Crafty extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String Version = "v0.8.0";
	
	private static Crafty instance;
	
	/*
	 * Constants
	 */
	
	public static final class UserActions {
		public static final String KICK = "Kick";
		public static final String BAN = "Ban";
		public static final String BANIP = "Ban IP";
		public static final String GETIP = "Get IP";
		public static final String OP = "Op";
		public static final String DEOP = "DeOp";
	}

	/*
	 * GUI Fields
	 */
	protected JPanel window;
	protected JPanel p;
	protected JPanel commandPanel;
	protected JPanel consoleInputPanel;
	
	protected JTextPane serverOutput;
	protected JScrollPane serverOutputScroller;
	
	protected JTextField consoleInput;
	protected JLabel consoleInputLabel;
	
	protected StatusBar statusBar;
	protected JLabel statusMsg;
	
	protected JLabel activeUserLabel;
	protected JList activeUserList;
	protected DefaultListModel activeUserListModel;
	protected JScrollPane activeUserScroller;
	protected JPopupMenu activeUserPopup;
	
	protected JLabel perfMonLabel;
	protected JTextArea perfMonText;
	
	/*
	 * Common fields
	 */
	private String[] args;
	private String argString;
	public static boolean useJline = false;
	protected volatile boolean serverOn;
	protected volatile boolean isLoading;
	protected volatile boolean isRestarting;
	private int consoleLineLimit;
	private int consoleLineCount;
	private ThemeManager tm;
	private CommandManager cm;
	
	/*
	 * Logger
	 */
	private Logger cLog;
	private FileHandler cLogFile;
	
	/*
	 * CraftBukkit Interception Fields
	 */
	protected PerformanceMonitor pf;
	protected OutputStream out;
	protected ByteArrayInputStream in;
	protected static Logger logger;
	//private CraftyExceptionHandler eh;
	
	protected Process bukkit;
	private InputStream is;
	private OutputStream os;
	private BufferedWriter writer;
	
	protected HelperClient helper;

	public static void main(String args[]) {
		try {
		    // Set System L&F
	        UIManager.setLookAndFeel(
	            UIManager.getSystemLookAndFeelClassName());
		} 
	    catch (Exception e) {
	       // handle exception
	    }
		
		new Crafty(args);
	}
	
	Crafty(String args[]) {
		// Set instance
		Crafty.instance = this;
		
		// Get logger
		this.cLog = Logger.getLogger("CraftyLog");
		try {
			this.cLogFile = new FileHandler("Crafty.log", true);
			this.cLogFile.setFormatter(new SimpleFormatter());
			this.cLog.setLevel(Level.ALL);
			this.cLog.addHandler(cLogFile);
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Set common variables
		this.args = args;
		this.serverOn = false;
		this.isLoading = true;
		this.consoleLineLimit = 1000;
		this.consoleLineCount = 0;
		
		// Get command line options and args
		argString = "";
		RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
		StringBuilder sb = new StringBuilder();
		Object[] tmpOptsObj = RuntimemxBean.getInputArguments().toArray();
		String[] tmpOpts = Arrays.copyOf(tmpOptsObj, tmpOptsObj.length, String[].class);
		for(String s : tmpOpts) { sb.append(s+", "); }
		for(String s : args) { sb.append(s+", "); }
		if(sb.length() >= 2)
		{
			argString = sb.substring(0, sb.length()-2);
		}
		
		// Load helper
        helper = new HelperClient();
		
		// Set frame options
		this.setTitle("Crafty "+Crafty.Version);
		this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/aranai/crafty/resources/icon.png")));
		this.setSize(850, 500);
		this.setMinimumSize(new Dimension(600,480));
		this.addWindowListener(new CraftyWindowListener(this));
		
		// Set up theme manager
		tm = new ThemeManager();
		
		// Set up command manager (for .crafty commands)
		cm = new CommandManager(this);
		
		// Set up exception handler
		//eh = new CraftyExceptionHandler();
		
		// Create panels
		window = new JPanel(new BorderLayout());
		p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(5,5,0,0));
		commandPanel = new JPanel();
		commandPanel.setPreferredSize(new Dimension(200,100));
		consoleInputPanel = new JPanel(new BorderLayout());
		consoleInputPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		// Add output window
		serverOutput = new JTextPane();
		serverOutput.setEditable(false);
		serverOutput.setMargin(new Insets(5,5,5,5));
		serverOutput.setContentType("text/html; charset=UTF-8");
		
		DefaultCaret caret = (DefaultCaret) serverOutput.getCaret();
	    caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		
		serverOutputScroller = new JScrollPane(serverOutput);
		p.add(serverOutputScroller, BorderLayout.CENTER);
		
		// Build themes
		this.buildThemes();
		
		// Add status bar
		statusBar = new StatusBar();
		statusBar.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		
		// Add status message
		statusMsg = new JLabel();
		statusMsg.setText("Loading...");
		statusBar.add(statusMsg);
		
		// Add status bar to window
		window.add(statusBar, BorderLayout.SOUTH);
		
		// Add stop button
		JButton button = new JButton("Stop & Exit");
		button.setIconTextGap(10);
		button.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/aranai/crafty/resources/shutdown.png"))));
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setHorizontalTextPosition(SwingConstants.RIGHT);
		button.setMargin(new Insets(5,5,5,5));
		button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
		button.setPreferredSize(new Dimension(190, 30));
        button.addActionListener(
    		new ActionListener() {
    			public void actionPerformed(ActionEvent e)
    			{
    				Crafty.queueConsoleCommand(".crafty exit");
    			}
    		}
        );
        commandPanel.add(button);
        
        // Add active user list
        activeUserLabel = new JLabel("Active Users: ");
        activeUserLabel.setPreferredSize(new Dimension(190, 24));
        commandPanel.add(activeUserLabel);
        
        activeUserListModel = new DefaultListModel();
		activeUserList = new JList(activeUserListModel);
		activeUserListModel.addElement("Nobody");
		activeUserList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		activeUserList.setLayoutOrientation(JList.VERTICAL);
		activeUserList.setVisibleRowCount(-1);
		activeUserScroller = new JScrollPane(activeUserList);
		activeUserScroller.setPreferredSize(new Dimension(190, 200));
		commandPanel.add(activeUserScroller);
		
		ActiveUserActionListener actionListener = new ActiveUserActionListener(this);
	    activeUserPopup = new JPopupMenu();
	    JMenuItem menuItem = null;
	    
	    Font menuFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	    
	    // Kick
	    menuItem = new JMenuItem(Crafty.UserActions.KICK);
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/aranai/crafty/resources/kick.png"))));
	    menuItem.addActionListener(actionListener);
	    activeUserPopup.add(menuItem);
	    
	    // Ban
	    menuItem = new JMenuItem(Crafty.UserActions.BAN);
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/aranai/crafty/resources/ban.png"))));
	    menuItem.addActionListener(actionListener);
	    activeUserPopup.add(menuItem);
	    
	    // Ban IP
	    menuItem = new JMenuItem(Crafty.UserActions.BANIP);
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/aranai/crafty/resources/ban.png"))));
	    menuItem.addActionListener(actionListener);
	    activeUserPopup.add(menuItem);
	    
	    // Get IP
	    menuItem = new JMenuItem(Crafty.UserActions.GETIP);
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/aranai/crafty/resources/getip.png"))));
	    menuItem.addActionListener(actionListener);
	    activeUserPopup.add(menuItem);
	    
	    // Op
	    menuItem = new JMenuItem(Crafty.UserActions.OP);
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/aranai/crafty/resources/op.png"))));
	    menuItem.addActionListener(actionListener);
	    activeUserPopup.add(menuItem);
	    
	    // DeOp
	    menuItem = new JMenuItem(Crafty.UserActions.DEOP);
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/aranai/crafty/resources/deop.png"))));
	    menuItem.addActionListener(actionListener);
	    activeUserPopup.add(menuItem);
	    
	    MouseListener popupListener = new ActiveUserPopupListener(this);
	    activeUserList.addMouseListener(popupListener);
		
		// Add performance monitor
		perfMonLabel = new JLabel("Performance:");
		perfMonLabel.setPreferredSize(new Dimension(190, 24));
        commandPanel.add(perfMonLabel);
        
        perfMonText = new JTextArea();
        perfMonText.setEditable(false);
		perfMonText.setMargin(new Insets(5,5,5,5));
		perfMonText.setPreferredSize(new Dimension(190, 70));
		commandPanel.add(perfMonText);
        
        // Add command panel to main panel
        p.add(commandPanel, BorderLayout.EAST);
        
        // Add console input label
        consoleInputLabel = new JLabel("Command:   ");
        consoleInputPanel.add(consoleInputLabel, BorderLayout.WEST);
        
        // Add console input text field
        Font f = new Font("sansserif", Font.BOLD, 14);
        consoleInput = new JTextField();
        consoleInput.setFont(f);
        consoleInput.setMargin(new Insets(5,5,5,5));
        consoleInput.addActionListener(
    	    new ActionListener() {
    	        public void actionPerformed(ActionEvent e) {
    	            Crafty.queueConsoleCommand(consoleInput.getText());
    	            consoleInput.setText("");
    	        }
    	    }
    	);
        consoleInputPanel.add(consoleInput, BorderLayout.CENTER);
        p.add(consoleInputPanel, BorderLayout.SOUTH);
        
        // Add panel to frame
        window.add(p, BorderLayout.CENTER);
        add(window);
		
        // Show window
		setVisible(true);
		
		// Capture system.in, system.out and system.err
		this.redirectSystemStreams();
		
		// Get performance monitor
		pf = new PerformanceMonitor();
		
		// Start the server
		this.startServer();
	    
	    // Set player list update timer
	    new Timer().schedule( 
	        new java.util.TimerTask() {
	            @Override
	            public void run() {
	                updatePlayerList();
	            }
	        }, 
	        0,
	        3000
		);
	    
	    // Set performance monitor update timer
	    new Timer().schedule( 
	        new java.util.TimerTask() {
	            @Override
	            public void run() {
	                updatePerfMon();
	            }
	        }, 
	        5000,
	        3000
		);
	}
	
	/*
	 * Methods for capturing system exit calls
	 * Used to prevent the server from killing Crafty when the server stops
	 * Code from: http://jroller.com/ethdsy/entry/disabling_system_exit
	 */
	
	private static void forbidSystemExitCall() {
	    final SecurityManager securityManager = new SecurityManager() {
	    	public void checkPermission( Permission permission ) {
	    	  	if(permission.getName().contains("exitVM")) {
	        		throw new ExitTrappedException() ;
	        	}
	      	}
		} ;
		System.setSecurityManager( securityManager ) ;
	}

	private static void enableSystemExitCall() {
		System.setSecurityManager( null ) ;
	}
	
	// Get instance
	public static Crafty instance()
	{
		return Crafty.instance;
	}
	
	// Get version
	public static String Version()
	{
		return Crafty.Version;
	}
	
	// Update the output window
	private void updateTextArea(final String text) {  
		SwingUtilities.invokeLater(new Runnable() {  
			public void run() {
				prepAndPrintText(text);
				consoleLineCount++;
				
				// Check line limit
				if(consoleLineCount > consoleLineLimit)
				{
					String fullText;
					try {
						StyledDocument doc = serverOutput.getStyledDocument();
						fullText = doc.getText(0,doc.getLength());
						int eol = fullText.indexOf("\n", 0);
						doc.remove(0, eol+1);
						consoleLineCount--;
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}
		});  
	}
	
	// Update the player list
	private void updatePlayerList()
	{
		// Clear the player list and return if the server is inactive
		if(!this.serverOn || !helper.isActive) { activeUserListModel.clear(); return; }
		
		String response = helper.sendCommand(HelperCommands.GETPLAYERLIST);
		
		if(response != null)
		{
    		String[] playerNames = response.split(":");
    		if(playerNames.length == 0)
    		{
    			activeUserListModel.clear();
    		}
    		else
    		{
    			// Add new players
    			for(int i = 0; i < playerNames.length; i++)
    			{
    				if(!activeUserListModel.contains(playerNames[i]))
    				{
    					activeUserListModel.addElement(playerNames[i]);
    				}
    			}
    			
    			// Remove disconnected players
    			List<String> pList = Arrays.asList(playerNames);
    			for(Object p : activeUserListModel.toArray())
    			{
    				if(!pList.contains((String)p))
    				{
    					activeUserListModel.removeElement((String)p);
    				}
    			}
    		}
		}
	}
	
	// Update the performance monitor
	private void updatePerfMon()
	{
		// Return if server is not on
		if(!this.serverOn) {
			if(!this.isRestarting && bukkit != null)// && Bukkit.getServer() != null)
			{
				this.serverOn = true;
				this.isLoading = false;
			}
		}
		
		// Get RAM usage
		String mem = Long.toString(PerformanceMonitor.memoryUsed()/1024/1024);
		String memMax = Long.toString(PerformanceMonitor.memoryAvailable()/1024/1024);
		
		// Get thread count
		int threads = PerformanceMonitor.threadsUsed();
		
		// Get CPU usage
		String cpu = new DecimalFormat("#.##").format(pf.getCpuUsage());
		
		if(serverOn && helper.isActive) {
			// Get CraftBukkit build
			statusMsg.setText(helper.sendCommand(HelperCommands.GETVERSION));
		}
		
		perfMonText.setText(
			"Memory Used: " + mem + "/" + memMax + "mb\n"
			+"CPU: " + cpu + "%\n"
			+"Threads: " + threads + "\n"
		);
	}
	
	// Capture system.out and system.err
	private void redirectSystemStreams() {
	    
		out = new OutputStream() {
			@Override  
			public void write(int b) throws IOException {  
				updateTextArea(String.valueOf((char) b));  
			}  
			
			@Override  
			public void write(byte[] b, int off, int len) throws IOException {  
				updateTextArea(new String(b, off, len));  
			}  
			
			@Override  
			public void write(byte[] b) throws IOException {  
				write(b, 0, b.length);  
			}  
		};
  
		byte[] buf = "".getBytes();
		in = new ByteArrayInputStream(buf){
			@Override
			public int read()
			{
				return -1;
			}
		};
		//System.setOut(new PrintStream(out, true));
		
		System.setErr(new PrintStream(out, true)); // Wat.
		System.setIn(in);
	}
	
	/*
	 * startServer() attempts to start the server
	 */
	
	public void startServer()
	{
		String[] args = this.args;
		
		final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        File currentJar = new File("craftbukkit.jar");

        /* is it a jar file? */
        if(!currentJar.getName().endsWith(".jar"))
        {
            this.logMsg("ERROR: Current application is not a .jar, cannot restart.");
            return;
        }

        /* Build command: java -jar application.jar */
        final ArrayList<String> command = new ArrayList<String>();
        command.add(javaBin);
        
        // Add java args
        RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
        Object[] tmpOptsObj = RuntimemxBean.getInputArguments().toArray();
        for(Object s : tmpOptsObj)
        {
            command.add(s.toString());
        }
        
        // Add jar
        command.add("-jar");
        command.add(currentJar.getAbsolutePath());
        command.add("nogui");
        
        // Add Crafty / CraftBukkit / Minecraft_Server args
        for(String s : this.args)
        {
            command.add(s);
        }
		
		final ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		this.prepAndPrintText(command.toString());
        try {
            // Launch the new process
            bukkit = builder.start();
            is = bukkit.getInputStream();
            os = bukkit.getOutputStream();
            
            StreamReader lsr = new StreamReader(is);
            Thread is_thread = new Thread(lsr, "StreamReader");
            is_thread.start();
            
            this.writer = new BufferedWriter(new OutputStreamWriter(os));
            
            //StreamWriter lsw = new StreamWriter(os);
            //Thread os_thread = new Thread(lsw, "StreamWriter");
            //os_thread.start();
        } catch (IOException e) {
            e.printStackTrace();
            this.logMsg("ERROR: Failed to start!");
        }
        
        logger = Logger.getLogger("Minecraft");
	}
	
	/*
	 * stopServer attempts to stop the server without exiting Crafty
	 */
	
	public void stopServer()
	{
		if(this.serverOn)
		{
			this.logMsg("Stopping server...");
			
			// Schedule shutdown task
			new Timer().schedule( 
			        new java.util.TimerTask() {
			            @Override
			            public void run() {
			
							// Disable exit calls
							Crafty.forbidSystemExitCall();
							
							// Stop the server
							Crafty.queueConsoleCommand("stop");
							
							// Wait for process to terminate
							try { bukkit.waitFor(); } catch (InterruptedException e) { e.printStackTrace(); }
							
							// Confirm server has stopped
							Crafty.instance().stopServerDone();
							
							// Cancel self
							this.cancel();
							
							// Restart if requested
							if(isRestarting)
							{
								Crafty.instance().restartApplication();
							}
			            }
			        }, 
			        1000,
			        1000
				);
		}
		else
		{
			this.logMsg("Server is off.");
		}
	}
	
	/*
	 * stopServerDone does some cleanup after server stop, and notifies the user that the operation has completed
	 */
	
	public void stopServerDone()
	{
		// Server has stopped
		this.serverOn = false;
		
		this.logMsg("Server has stopped!");
		
		// Run the GC
		this.logMsg("Running Garbage Collector...");
		System.gc();
		this.logMsg("Garbage Collector Complete.");
	}
	
	/*
	 * restartServer attempts to restart the server
	 */
	
	public void restartServer()
	{
		this.logMsg("Restarting server.");
		this.isRestarting = true;
		
		// If server is on, stop it
		if(this.serverOn)
		{
			this.logMsg("Server is on.");
			this.stopServer();
		}
		else
		{
			this.restartApplication();
		}
	}
	
	/*
	 * close() stops the server and exits Crafty
	 */
	public void close()
	{
		// Disallow close while loading
		if(this.isLoading) { return; }
		
		if(this.serverOn)
		{
			this.logMsg("Exiting! Waiting for server to stop...");
			
			// Prevent server from killing application
			Crafty.forbidSystemExitCall();
			
			Crafty.queueConsoleCommand("stop");
			try {
			    bukkit.waitFor();
				
				// Re-enable system exit call and exit
				Crafty.enableSystemExitCall();
				
				this.logMsg("Server has stopped. Exiting.");
				System.exit(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		else
		{
			this.logMsg("Exiting! Server is off.");
			System.exit(0);
		}
	}
	
	/*
	 * logMsg writes a timestamped Crafty-stamped log message to the console
	 */
	public void logMsg(String m)
	{
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timestamp = "";
		try {
			timestamp = df.format(new Date());
		} catch (Exception e) { /* No timestamp or bad format */ }
		
		// Build final output string
		String output = timestamp + " [CRAFTY] " + m + "\n";
		
		// Print to screen
		this.prepAndPrintText(output);
	}
	
	/*
	 * queueConsoleCommand first parses a console command to see if it's a Crafty command
	 * If so, it is sent to the CommandManager
	 * If not, it is queued in the server  
	 */
	public static void queueConsoleCommand(String cmd) {
		// Intercept Crafty commands
		if(cmd.startsWith(".crafty"))
		{
			Crafty.instance().parseCraftyCommand(cmd);
			return;
		}
		
		if(!Crafty.instance().serverOn) { return; }
		
		try {
		    Crafty.instance().writer.write(cmd+"\n");
		    Crafty.instance().writer.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
	
	/*
	 * parseCraftyCommand simply provides a local method for sending commands to the CommandManager
	 */
	public void parseCraftyCommand(String cmd)
	{
		// Send command to command manager
		this.cm.parse(cmd);
	}
	
	/*
	 * prepAndPrintMultiLineText breaks a string into lines and prints each individually
	 * Primarily needed when changing the console theme, as each line must be reprocessed
	 */
	public void prepAndPrintMultiLineText(String text)
	{
		String[] lines = text.split("\n");
		for(String s : lines)
		{
			this.prepAndPrintText(s+"\n");
		}
	}
	
	/*
	 * prepAndPrintString handles syntax highlighting for Crafty's fancy console output
	 * Processes timestamps and standard bracketed values
	 */
	public void prepAndPrintText(String text)
	{
		// Save to log
		this.cLog.log(Level.ALL, text);
		
		// Check for helper load
		if(!helper.isActive && text.contains("[CraftyHelper]"))
		{
		    // Start helper
		    helper.start();
		    this.logMsg(helper.sendCommand("Ding"));
		}
		
		// Parse timestamp
		// TODO: Get DateFormat from server.properties
		String timestamp = "";
		if(text.length() >= 20)
		{
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				Date today = df.parse(text.substring(0, 20));
				timestamp = df.format(today);
				text = text.substring(20);
			} catch (Exception e) { /* No timestamp or bad format */ }
		}
		
		// Parse color codes
		char esc = '\033';
		int find = text.indexOf(esc);
		int end = 0;
		//int color = -1;
		while(find >= 0)
		{
			StringBuffer sb = new StringBuffer(text);
			end = text.indexOf('m', find);
			if(end < 0) { break; }
			sb.replace(find, end+1, "");
			
			/*
			 * Parse out color code if we want to use it for something
			try
			{
				color = Integer.parseInt(sb.substring(find+2, end));
			}catch(NumberFormatException e) { System.out.println("Err: " + sb.substring(find+2, end)); break; }
			
			sb.replace(find, end+1, "COLOR{"+color+"}");
			*/
			
			text = sb.toString();
			find = text.indexOf(esc);
		}
		
		// Add timestamp back
		text = timestamp+" "+text;
		
		try {
			StyledDocument doc = serverOutput.getStyledDocument();
			serverOutput.setBackground(tm.getCurrentTheme().getColor(Theme.BG_BASE));
			
			// Decide whether to scroll after this
			int origScroll = serverOutputScroller.getVerticalScrollBar().getValue();
			boolean doScroll = ((origScroll+serverOutputScroller.getVerticalScrollBar().getVisibleAmount()) == serverOutputScroller.getVerticalScrollBar().getMaximum());
			
			int len = doc.getLength();
			doc.insertString(len, text, tm.getCurrentTheme().getAttributeSet(Theme.TEXT_BASE));
			
			// Style bracketed log indicators
			int at = text.indexOf("[INFO]");
			if(at >= 0) { doc.setCharacterAttributes(len+at, 6, tm.getCurrentTheme().getAttributeSet(Theme.SYNTAX_INFO), false); }
			at = text.indexOf("[WARNING]");
			if(at >= 0) { doc.setCharacterAttributes(len+at, 9, tm.getCurrentTheme().getAttributeSet(Theme.SYNTAX_WARNING), false); }
			at = text.indexOf("[SEVERE]");
			if(at >= 0) { doc.setCharacterAttributes(len+at, 8, tm.getCurrentTheme().getAttributeSet(Theme.SYNTAX_SEVERE), false); }
			at = text.indexOf("[CRAFTY]");
			if(at >= 0) { doc.setCharacterAttributes(len+at, 8, tm.getCurrentTheme().getAttributeSet(Theme.SYNTAX_CRAFTY), false); }
			
			// Style timestamp
			if(timestamp.length() > 0)
			{
				doc.setCharacterAttributes(len, 20, tm.getCurrentTheme().getAttributeSet(Theme.SYNTAX_TIMESTAMP), false);
			}
			
			// Scroll to end
			if(doScroll)
			{
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						serverOutputScroller.getVerticalScrollBar().setValue(serverOutputScroller.getVerticalScrollBar().getMaximum());
					}
				});
			}
		} catch (BadLocationException e) {
			// TODO: Handle server output failure
			// Not a critical failure, but GUI output is likely broken until
			// restart and we need a way to alert the user
			e.printStackTrace();
		}
	}
	
	/*
	 * getCommandManager returns the CommandManager instance
	 */
	public CommandManager getCommandManager()
	{
		return this.cm;
	}
	
	/*
	 * getThemeManager returns the ThemeManager instance
	 */
	public ThemeManager getThemeManager()
	{
		return this.tm;
	}
	
	/*
	 * setTheme sets the visual theme for the console window and rebuilds the output.
	 * The operation is expensive, but is unlikely to be used frequently.
	 */
	public void setTheme(String name)
	{
		tm.setCurrentTheme(name);
		this.refreshTheme();
	}
	
	/*
	 * refreshTheme handles the visual refresh when switching themes or changing font size
	 */
	public void refreshTheme()
	{
		StyledDocument doc = serverOutput.getStyledDocument();
		try {
			// Get current document text
			String cur = doc.getText(0, doc.getLength());
			
			// Clear document text
			doc.remove(0, doc.getLength());
			
			// Prep and print each line
			this.prepAndPrintMultiLineText(cur);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * buildThemes creates the default themes and adds them to the Theme Manager
	 */
	private void buildThemes()
	{
		Theme defaultTheme = new ThemeBuilder()
		.setName("Default")
		.setDescription("Default theme")
		.setAttributeSet(serverOutput.getInputAttributes())
		.getTheme();
		
		tm.addTheme(defaultTheme);
		
		Theme altTheme = new ThemeBuilder()
		.setName("Horrible")
		.setDescription("Alternate theme")
		.setAttributeSet(serverOutput.getInputAttributes())
		.setBaseFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14))
		.addColor(Theme.BG_BASE, new Color(50,100,200))
		.addColor(Theme.TEXT_BASE, Color.WHITE)
		.addColor(Theme.SYNTAX_TIMESTAMP, Color.PINK)
		.addColor(Theme.SYNTAX_WARNING, Color.GREEN)
		.addFont(Theme.SYNTAX_TIMESTAMP, new Font(Font.SANS_SERIF, Font.BOLD, 12))
		.getTheme();
		
		tm.addTheme(altTheme);
		
		altTheme = new ThemeBuilder()
		.setName("Dark")
		.setDescription("Dark theme")
		.setAttributeSet(serverOutput.getInputAttributes())
		.setBaseFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14))
		.addColor(Theme.BG_BASE, new Color(0,0,0))
		.addColor(Theme.TEXT_BASE, Color.WHITE)
		.getTheme();
		
		tm.addTheme(altTheme);
		
		tm.setCurrentTheme("Default");
	}
	
	/*
	 * restartApplication closes and restarts Crafty
	 * Code from: http://stackoverflow.com/questions/4159802/restart-a-java-application/4194224#4194224
	 */
	public void restartApplication()
	{
		final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "javaw";
		File currentJar;
		try {
			currentJar = new File(Crafty.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			return;
		}

		/* is it a jar file? */
		if(!currentJar.getName().endsWith(".jar"))
		{
			this.logMsg("ERROR: Current application is not a .jar, cannot restart.");
			return;
		}

		/* Build command: java -jar application.jar */
		final ArrayList<String> command = new ArrayList<String>();
		command.add(javaBin);
		
		// Add java args
		RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
		Object[] tmpOptsObj = RuntimemxBean.getInputArguments().toArray();
		for(Object s : tmpOptsObj)
		{
			command.add(s.toString());
		}
		
		// Add jar
		command.add("-jar");
		command.add(currentJar.getPath());
		
		// Add Crafty / CraftBukkit / Minecraft_Server args
		for(String s : this.args)
		{
			command.add(s);
		}

		final ProcessBuilder builder = new ProcessBuilder(command);
		try {
			// Launch the new process
			bukkit = builder.start();
			is = bukkit.getInputStream();
			os = bukkit.getOutputStream();
			
			// Exit
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			this.logMsg("ERROR: Failed to restart!");
		}
	}
}