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
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.security.Permission;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.swing.text.StyledDocument;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ThreadServerApplication;

public class Crafty extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String Version = "v0.6";
	
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
	private boolean serverOn;
	private boolean loading;
	private int consoleLineLimit;
	private int consoleLineCount;
	private ThemeManager tm;
	private CommandManager cm;
	
	/*
	 * CraftBukkit Interception Fields
	 */
	protected PerformanceMonitor pf;
	protected OutputStream out;
	protected ByteArrayInputStream in;
	protected MinecraftServer ms;
	protected static Logger logger;
	private ThreadServerApplication tsa;
	private CraftyExceptionHandler eh;

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
		
		// Set common variables
		this.args = args;
		this.serverOn = false;
		this.loading = true;
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
		eh = new CraftyExceptionHandler();
		
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
		serverOutput.setContentType("text/html");
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
    				close();
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
		
		// Capture system.out and system.err
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
	
	public static class ExitTrappedException extends SecurityException { private static final long serialVersionUID = 1L; }

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

	// asList converts a series of strings to a List
	private static List<String> asList(String... params) {
	    return Arrays.asList(params);
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
						int eol = fullText.indexOf("\n");
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
		if(!this.serverOn) { activeUserListModel.clear(); return; }
		
		if(ms == null || ms.server == null) { return; }
		Player[] players = ms.server.getOnlinePlayers();
		String[] playerNames = new String[players.length];
		if(players.length == 0)
		{
			activeUserListModel.clear();
		}
		else
		{
			// Add new players
			int i = 0;
			for(Player p : players)
			{
				if(!activeUserListModel.contains(p.getName()))
				{
					activeUserListModel.addElement(p.getName());
				}
				playerNames[i] = p.getName();
				i++;
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
	
	// Update the performance monitor
	private void updatePerfMon()
	{
		// Return if server is not on
		if(!this.serverOn) {
			if(ms != null && ms.server != null && ms.server.getWorlds() != null && ms.server.getWorlds().size() > 0)
			{
				this.serverOn = true;
				this.loading = false;
			}
		}
		
		// Get RAM usage
		String mem = Long.toString(PerformanceMonitor.memoryUsed()/1024/1024);
		String memMax = Long.toString(PerformanceMonitor.memoryAvailable()/1024/1024);
		
		// Get thread count
		int threads = PerformanceMonitor.threadsUsed();
		
		// Get CPU usage
		String cpu = new DecimalFormat("#.##").format(pf.getCpuUsage());
		
		if(this.serverOn) {
			// Get CraftBukkit build
			String cbVer = "Unknown";
			String cbName = "Unknown";
			cbVer = ms.server.getVersion();
			cbName = ms.server.getWorlds().get(0).getName();
			
			statusMsg.setText("CraftBukkit Version: "+cbVer+" | World: "+cbName+" | Args: "+this.argString);
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
	  in = new ByteArrayInputStream(buf);
	  
		System.setOut(new PrintStream(out, true));  
		System.setErr(new PrintStream(out, true));
		System.setIn(in);
	}
	
	/*
	 * startServer() attempts to start the server
	 */
	
	public void startServer()
	{
		String[] args = this.args;
		
		// Parse options
		// Option parsing code taken from: org/bukkit/craftbukkit/Main.java
		OptionParser parser = new OptionParser() {
	        {
	            acceptsAll(asList("?", "help"), "Show the help");

	            acceptsAll(asList("c", "config"), "Properties file to use")
	                    .withRequiredArg()
	                    .ofType(File.class)
	                    .defaultsTo(new File("server.properties"))
	                    .describedAs("Properties file");

	            acceptsAll(asList("P", "plugins"), "Plugin directory to use")
	                    .withRequiredArg()
	                    .ofType(File.class)
	                    .defaultsTo(new File("plugins"))
	                    .describedAs("Plugin directory");

	            acceptsAll(asList("h", "host", "server-ip"), "Host to listen on")
	                    .withRequiredArg()
	                    .ofType(String.class)
	                    .describedAs("Hostname or IP");

	            acceptsAll(asList("w", "world", "level-name"), "World directory")
	                    .withRequiredArg()
	                    .ofType(String.class)
	                    .describedAs("World dir");

	            acceptsAll(asList("p", "port", "server-port"), "Port to listen on")
	                    .withRequiredArg()
	                    .ofType(Integer.class)
	                    .describedAs("Port");

	            acceptsAll(asList("o", "online-mode"), "Whether to use online authentication")
	                    .withRequiredArg()
	                    .ofType(Boolean.class)
	                    .describedAs("Authentication");

	            acceptsAll(asList("s", "size", "max-players"), "Maximum amount of players")
	                    .withRequiredArg()
	                    .ofType(Integer.class)
	                    .describedAs("Server size");
	        }
	    };

	    OptionSet options = null;

	    try {
	        options = parser.parse(args);
	    } catch (joptsimple.OptionException ex) {
	        Logger.getLogger(Crafty.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
	    }

	    if ((options == null) || (options.has("?"))) {
	        try {
	            parser.printHelpOn(System.out);
	        } catch (IOException ex) {
	            Logger.getLogger(Crafty.class.getName()).log(Level.SEVERE, null, ex);
	        }
	    } else {
			// Start the server
			try {
	            ms = new MinecraftServer(options);
	            tsa = new ThreadServerApplication("Server thread", ms);
	            tsa.setUncaughtExceptionHandler(eh);
	            tsa.start();
	            logger = Logger.getLogger("Minecraft");
	        } catch (Exception exception) {
	            MinecraftServer.a.log(Level.SEVERE, "Failed to start the minecraft server", exception);
	        }
	    }
	}
	
	/*
	 * stopServer attempts to stop the server without exiting Crafty
	 */
	
	public void stopServer()
	{
		if(this.serverOn)
		{
			this.logMsg("Stopping server...");
			
			new Timer().schedule( 
			        new java.util.TimerTask() {
			            @Override
			            public void run() {
			
							// Disable exit calls
							Crafty.forbidSystemExitCall();
							
							// Stop the server
							Crafty.queueConsoleCommand("stop");
							
							try {
								if(tsa.isAlive())
								{
									tsa.join();
								}	
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
							// Re-enable exit calls
							Crafty.enableSystemExitCall();
							
							// Cancel self
							this.cancel();
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
	
	public void stopServerDone()
	{
		// Server has stopped
		this.ms = null;
		this.serverOn = false;
		this.logMsg("Server has stopped!");
		this.logMsg("Running Garbage Collector...");
		System.gc();
		this.logMsg("Garbage Collector Complete.");
	}
	
	/*
	 * restartServer attempts to restart the server without exiting Crafty
	 */
	
	public void restartServer()
	{
		this.logMsg("Restarting server.");
		
		if(this.serverOn)
		{
			this.logMsg("Server is on");
			this.stopServer();
		}
		
		this.startServer();
	}
	
	/*
	 * close() stops the server and exits Crafty
	 */
	public void close()
	{
		// Disallow close while loading
		if(this.loading) { return; }
		
		if(this.serverOn)
		{
			this.logMsg("Exiting! Waiting for server to stop...");
			
			Crafty.queueConsoleCommand("stop");
			try {
				if(tsa.isAlive())
				{
					tsa.join();
				}
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
		this.prepAndPrintText(timestamp + " [CRAFTY] " + m + "\n");
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
		
		// Credit: http://forums.bukkit.org/threads/send-commands-to-console.3241
		// Note: this is likely to break on Minecraft server updates
        CraftServer cs = Crafty.instance().ms.server;
        Field f;
        try {
            f = CraftServer.class.getDeclaredField("console");
        } catch (NoSuchFieldException ex) {
            logger.info("NoSuchFieldException");
            return;
        } catch (SecurityException ex) {
            logger.info("SecurityException");
            return;
        }
        MinecraftServer ms;
        try {
            f.setAccessible(true);
            ms = (MinecraftServer) f.get(cs);
        } catch (IllegalArgumentException ex) {
            logger.info("IllegalArgumentException");
            return;
        } catch (IllegalAccessException ex) {
            logger.info("IllegalAccessException");
            return;
        }
        if ((!ms.g) && (MinecraftServer.a(ms))) {
            ms.a(cmd, ms);
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
		// Parse timestamp
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
		
		text = timestamp+" "+text;
		
		try {
			StyledDocument doc = serverOutput.getStyledDocument();
			serverOutput.setBackground(tm.getCurrentTheme().getColor(Theme.BG_BASE));
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
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
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
}