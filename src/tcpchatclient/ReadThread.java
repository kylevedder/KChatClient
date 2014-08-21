/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpchatclient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kyle
 */
public class ReadThread extends Thread
{

    private volatile boolean isRunning = true;
    BufferedReader inFromServer = null;

    public ReadThread(BufferedReader inFromServer)
    {
        this.inFromServer = inFromServer;
    }

    @Override
    public void run()
    {
        if(Main.DEBUG)System.out.println("ReadThread Started");
        while (isRunning)
        {
            if(Main.DEBUG)System.out.println("check on input");
            String s = "";            
            try
            {
                if (inFromServer != null)
                {                    
                    s = inFromServer.readLine();
                    if(s == null)
                    {
                        Main.killConnection();
                        Main.showErrorBox("Server Connection Failed");
                    }
                    if(Main.DEBUG)System.out.println(s);
                }
            }
            catch (IOException ex)
            {
                Logger.getLogger(ReadThread.class.getName()).log(Level.SEVERE, null, ex);
                this.kill();
            }
            //if actual info
            if (s != null && !s.equals(""))
            {
                Main.textFIFO.add(s);
                Main.putIntoTextAreaLatch.countDown();//unlock Main.textFIFO for access
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
        
        if(Main.DEBUG) System.out.println("ReadThread stopped");
    }

    public void kill()
    {
        isRunning = false;
    }
}
