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
	private static final String Version = "v0.5";
	
	private static Crafty instance;

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
	private String argString;
	private int consoleLineLimit;
	private int consoleLineCount;
	private ThemeManager tm;
	
	/*
	 * CraftBukkit Interception Fields
	 */
	protected PerformanceMonitor pf;
	protected OutputStream out;
	protected ByteArrayInputStream in;
	protected MinecraftServer ms;
	protected static Logger logger;
	private ThreadServerApplication tsa;

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
		this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/crafty/resources/icon.png")));
		this.setSize(850, 500);
		this.setMinimumSize(new Dimension(600,400));
		this.addWindowListener(new CraftyWindowListener(this));
		
		// Set up theme manager
		tm = new ThemeManager();
		
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
		button.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/crafty/resources/shutdown.png"))));
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
	    menuItem = new JMenuItem("Kick");
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/crafty/resources/kick.png"))));
	    menuItem.addActionListener(actionListener);
	    activeUserPopup.add(menuItem);
	    
	    // Ban
	    menuItem = new JMenuItem("Ban");
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/crafty/resources/ban.png"))));
	    menuItem.addActionListener(actionListener);
	    activeUserPopup.add(menuItem);
	    
	    // Get IP
	    menuItem = new JMenuItem("Get IP");
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/crafty/resources/getip.png"))));
	    menuItem.addActionListener(actionListener);
	    activeUserPopup.add(menuItem);
	    
	    // Op
	    menuItem = new JMenuItem("Op");
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/crafty/resources/op.png"))));
	    menuItem.addActionListener(actionListener);
	    activeUserPopup.add(menuItem);
	    
	    // DeOp
	    menuItem = new JMenuItem("DeOp");
	    menuItem.setFont(menuFont);
	    menuItem.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/crafty/resources/deop.png"))));
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
    	            Crafty.queueConsoleCommand(ms.server, consoleInput.getText());
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
	            tsa.start();
	            logger = Logger.getLogger("Minecraft");
	        } catch (Exception exception) {
	            MinecraftServer.a.log(Level.SEVERE, "Failed to start the minecraft server", exception);
	        }
	    }
	    
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

	private static List<String> asList(String... params) {
	    return Arrays.asList(params);
	}
	
	// Get instance
	static Crafty instance()
	{
		return Crafty.instance;
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
		// Get RAM usage
		String mem = Long.toString(PerformanceMonitor.memoryUsed()/1024/1024);
		String memMax = Long.toString(PerformanceMonitor.memoryAvailable()/1024/1024);
		
		// Get thread count
		int threads = PerformanceMonitor.threadsUsed();
		
		// Get CPU usage
		String cpu = new DecimalFormat("#.##").format(pf.getCpuUsage());
		
		// Get CraftBukkit build
		String cbVer = "Unknown";
		String cbName = "Unknown";
		if(ms != null && ms.server != null && ms.server.getWorlds() != null && ms.server.getWorlds().size() > 0)
		{
			cbVer = ms.server.getVersion();
			cbName = ms.server.getWorlds().get(0).getName();
		}
		
		statusMsg.setText("CraftBukkit Version: "+cbVer+" | World: "+cbName+" | Args: "+this.argString);
		
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
	
	public void close()
	{
		Crafty.queueConsoleCommand(ms.server, "stop");
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
	
	public void logMsg(String m)
	{
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timestamp = "";
		try {
			timestamp = df.format(new Date());
		} catch (Exception e) { /* No timestamp or bad format */ }
		this.prepAndPrintText(timestamp + " [CRAFTY] " + m + "\n");
	}
	
	public static void queueConsoleCommand(CraftServer server, String cmd) {
		// Intercept Crafty commands
		if(cmd.startsWith(".crafty"))
		{
			Crafty.instance().parseCraftyCommand(cmd);
			return;
		}
		
		// Credit: http://forums.bukkit.org/threads/send-commands-to-console.3241
        CraftServer cs = server;
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
	
	public void parseCraftyCommand(String cmd)
	{
		String[] cmdArgs = cmd.split(" ");
		if(cmdArgs.length > 1)
		{
			if(cmdArgs[1].equalsIgnoreCase("theme"))
			{
				// Theme actions
				if(cmdArgs.length > 2)
				{
					String action = cmdArgs[2];
					
					// Set theme
					if(action.equalsIgnoreCase("set"))
					{
						if(cmdArgs.length > 3)
						{
							String theme = cmdArgs[3];
							if(this.tm.themeAvailable(theme))
							{
								// Set theme
								this.setTheme(theme);
								this.logMsg("Theme changed.");
							}
							else
							{
								// Bad theme
								this.logMsg("Invalid theme specified.");
							}
						}
						else
						{
							// No theme specified
							this.logMsg("No theme specified.");
						}
					}
					
					// Current theme
					if(action.equalsIgnoreCase("current"))
					{
						String currentTheme = tm.getCurrentTheme().getName();
						this.logMsg("Current Theme: " + currentTheme);
					}
				}
				else
				{
					// No action specified
					this.logMsg("No action specified.");
				}
			}
		}
		else
		{
			// No command
			this.logMsg("No command entered.");
		}
	}
	
	public void prepAndPrintMultiLineText(String text)
	{
		String[] lines = text.split("\n");
		for(String s : lines)
		{
			this.prepAndPrintText(s+"\n");
		}
	}
	
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