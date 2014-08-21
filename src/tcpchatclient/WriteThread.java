/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpchatclient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kyle
 */
public class WriteThread extends Thread
{

    private volatile boolean isRunning = true;
    DataOutputStream outToServer = null;
    String nickname;

    public WriteThread(DataOutputStream outToServer, String nickname)
    {
        this.outToServer = outToServer;
        this.nickname = nickname;

    }

    @Override
    public void run()
    {
        if(Main.DEBUG)System.out.println("WriteThread Started");

        if (nickname == null)
        {
            nickname = "Guest";
        }
        if(Main.DEBUG)System.out.println("printing logon");
        printLogon();
        if(Main.DEBUG)System.out.println("logon printed");
        while (isRunning)
        {
            if(Main.DEBUG)System.out.println("check on string to submit");
            //blocks untill outToServerLatch opens for use
            try
            {
                Main.outToServerLatch.await();
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(ReadThread.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (isRunning)
            {
                String s = Main.getStringToSubmit();

                if (!s.equals(""))
                {
                    try
                    {
                        //escape the message
                        s = s.replace("/n", "//n");
                        s = s.replace("/t", "//t");
                        if(Main.DEBUG)System.out.println("Writing to server: " + s);
                        outToServer.writeBytes("/n" + nickname + "/t" + s + '\n');
                    }
                    catch (IOException ex)
                    {
                        Logger.getLogger(WriteThread.class.getName()).log(Level.SEVERE, null, ex);
                        this.kill();
                    }
                    //<editor-fold defaultstate="collapsed" desc="Sleep 200 milis">
                    try
                    {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException ex)
                    {
                        Logger.getLogger(WriteThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //</editor-fold>
                }
            }
        }
        if(Main.DEBUG)System.out.println("WriteThread stopped");
    }

    public void printLogon()
    {
        try
        {
            outToServer.writeBytes("/c" + nickname + " has logged on " + '\n');
        }
        catch (IOException ex)
        {
            Logger.getLogger(WriteThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void printLogoff()
    {
        try
        {
            outToServer.writeBytes("/c" + nickname + " has logged off " + '\n');
        }
        catch (IOException ex)
        {
            Logger.getLogger(WriteThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void kill()
    {
        Main.outToServerLatch.countDown();//break latch
        if (outToServer != null)
        {
            try
            {
                outToServer.writeBytes("/kill" + '\n');
            }
            catch (IOException ex)
            {
                Logger.getLogger(WriteThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        isRunning = false;
    }
}
