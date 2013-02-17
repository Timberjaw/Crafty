package com.aranai.crafty;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class StreamWriter implements Runnable {

    BufferedWriter writer;

    public StreamWriter(OutputStream os) {
        this.writer = new BufferedWriter(new OutputStreamWriter(os));
    }

    public void run() {
        try {
            //writer.write(input);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}