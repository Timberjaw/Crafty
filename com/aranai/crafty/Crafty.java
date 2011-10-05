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
import java.net.ServerSocket;
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

import jline.ConsoleReader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.Main;
import org.bukkit.entity.Player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PropertyManager;
import net.minecraft.server.StatisticList;
import net.minecraft.server.ThreadServerApplication;

public class Crafty extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String Version = "v0.7.6";
	
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
			if(!this.isRestarting && ms != null && ms.server != null && ms.server.getWorlds() != null && ms.server.getWorlds().size() > 0)
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

                acceptsAll(asList("d", "date-format"), "Format of the date to display in the console (for log entries)")
                        .withRequiredArg()
                        .ofType(SimpleDateFormat.class)
                        .describedAs("Log date format");
                
                acceptsAll(asList("log-pattern"), "Specfies the log filename pattern")
		                .withRequiredArg()
		                .ofType(String.class)
		                .defaultsTo("server.log")
		                .describedAs("Log filename");

		        acceptsAll(asList("log-limit"), "Limits the maximum size of the log file (0 = unlimited)")
		                .withRequiredArg()
		                .ofType(Integer.class)
		                .defaultsTo(0)
		                .describedAs("Max log size");
		
		        acceptsAll(asList("log-count"), "Specified how many log files to cycle through")
		                .withRequiredArg()
		                .ofType(Integer.class)
		                .defaultsTo(1)
		                .describedAs("Log count");
		
		        acceptsAll(asList("log-append"), "Whether to append to the log file")
		                .withRequiredArg()
		                .ofType(Boolean.class)
		                .defaultsTo(true)
		                .describedAs("Log append");

                acceptsAll(asList("b", "bukkit-settings"), "File for bukkit settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("bukkit.yml"))
                        .describedAs("Yml file");
                
                acceptsAll(asList("nojline"), "Disables jline and emulates the vanilla console");
            }
        };

	    OptionSet options = null;

	    try {
	        options = parser.parse(args);
	        for(String s : options.nonOptionArguments())
	        {
	        	this.cLog.log(Level.ALL, "Arg: "+s);
	        }
	    } catch (joptsimple.OptionException ex) {
	        this.cLog.log(Level.SEVERE, ex.getLocalizedMessage());
	    }

	    if ((options == null) || (options.has("?"))) {
	        try {
	            parser.printHelpOn(System.out);
	        } catch (IOException ex) {
	            this.cLog.log(Level.SEVERE, null, ex);
	        }
	    } else {
			// Start the server
			try {
				// Check port availability
				// If we're restarting, it may not have been freed yet
				boolean portTaken = true;
				int port = 25565;
				if(options.has("server-port")) {
					port = (Integer)options.valueOf("server-port"); 
				}
				else
				{
					// Parse properties file and check for specified port
					PropertyManager pm = new PropertyManager(options);
					port = pm.getInt("server-port", 25565); // WARNING: Internals Access
				}
				int portWaitTime = 0;
				while(portTaken == true && portWaitTime < 5000)
				{
					ServerSocket socket = null;
					try {
					    socket = new ServerSocket(port);
					    portTaken = false;
					} catch (IOException e) {
						this.logMsg("Port "+port+" is unavailable, waiting...");
						synchronized(this)
						{
							this.wait(1000);
						}
						portWaitTime += 1000;
					} finally { 
					    // Clean up
					    if (socket != null) socket.close();
					}
				}
				
				// Make sure port was available
				if(portTaken)
				{
					this.logMsg("Could not start server because port " + port + " is still unavailable. You can try restarting again with .crafty start or check for other applications using this port.");
					return;
				}
				
				// Handle jLine option
				useJline = !"jline.UnsupportedTerminal".equals(System.getProperty("jline.terminal"));
                
                if (options.has("nojline")) {
                    System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
                    System.setProperty("user.language", "en");
                    useJline = false;
                    Main.useJline = useJline;
                }
                
                this.cLog.log(Level.ALL, "Using JLine: " + ((useJline) ? "YES" : "NO"));
				
                // Initialize the StatisticsList; I don't know what it is or does, and this will break on new versions because Notchfuscation :(
                StatisticList.a();
                
				// Create the Minecraft server
	            ms = new MinecraftServer(options);
	            
	            // Override the default ConsoleReader with our own bizarro version
	            // Ours sits around for a bit and then returns null
	            // Take that, Minecraft!
	            ms.reader = new ConsoleReader(){
	            	@Override
					public String readLine() throws IOException
	            	{
	            		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
	            		return null;
	            	}
	            };
	            
	            // Create our own ThreadServerApplication
	            tsa = new ThreadServerApplication("Server thread", ms);
	            
	            // Catch exceptions from the TSA ourselves (mainly used to catch system.exit() events)
	            tsa.setUncaughtExceptionHandler(eh);
	            
	            // Start the TSA
	            tsa.start();
	            
	            logger = Logger.getLogger("Minecraft");
	        } catch (Exception exception) {
	            MinecraftServer.log.log(Level.SEVERE, "Failed to start the minecraft server", exception);
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
			
			// Schedule shutdown task
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
				if(tsa.isAlive())
				{
					tsa.join();
				}
				
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
		
		// Handle 'stop' command
		if(cmd.startsWith("stop"))
		{
			Crafty.instance().kickAll("Shutting down.");
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
        
        if ((!ms.isStopped) && (MinecraftServer.isRunning(ms))) {
        	ms.issueCommand(cmd, ms);
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
			builder.start();
			
			// Exit
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			this.logMsg("ERROR: Failed to restart!");
		}
	}
	
	private void kickAll(String reason)
	{
		for(Player p : this.ms.server.getOnlinePlayers())
		{
			p.kickPlayer(reason);
		}
	}
}