package com.aranai.crafty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

class HelperClient
{
    private Socket socket;
    private BufferedReader reader;
    private OutputStream out;
    public boolean isActive;
    public static final Object LOCK = new Object();

    public HelperClient() {
        isActive = false;
    }
    
    public void start() {
        try {
            socket = new Socket("localhost", 6789);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = socket.getOutputStream();
            isActive = true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public String sendCommand(byte cmd)
    {
        return sendCommand(cmd, "");
    }
    
    public String sendCommand(String params)
    {
        return sendCommand(HelperCommands.ECHO, params);
    }

    public String sendCommand(byte cmd, String params)
    {
        synchronized(LOCK)
        {
            if(isActive && socket.isConnected()) {
                try {
                    byte[] b = new byte[params.length()+2];
                    byte[] p = (params+"\n").getBytes();
                    
                    // Command byte first
                    b[0] = cmd;
                    
                    // And then the params
                    System.arraycopy(p, 0, b, 1, p.length);
                    
                    out.write(b);
                    out.flush();
                    
                    return reader.readLine();
                } catch (IOException e) {}
            }
        }
        
        return null;
    }
    
    public void stop() {
        try {
            isActive = false;
            out.close();
            reader.close();
            socket.close();
        } catch (IOException e) {}
    }
}