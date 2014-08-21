/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpchatclient;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kyle
 */
public class TextFeederThread extends Thread
{

    private volatile boolean isRunning = true;

    @Override
    public void run()
    {
        while (isRunning)
        {
            try
            {
                Main.putIntoTextAreaLatch.await();//wait for unlock
                if(Main.DEBUG)System.out.println("putIntoTextAreaLatch unlocked");
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(TextFeederThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (isRunning)
            {
                if (!Main.textFIFO.isEmpty())
                {                    
                    String s = Main.textFIFO.remove(0);
                    if(Main.DEBUG)System.out.println("FEED: " + s);
                    if (s.startsWith("/n"))//has an author
                    {
                        s = s.substring(2);//parse out the /n
                        int index = s.indexOf("/t");
                        String name = s.substring(0, index);
                        String message = s.substring(index + 2);
                        //unescape the message
                        message = message.replace("//t", "/t");
                        message = message.replace("//n", "/n");
                        
                        //gets and removes the element fromt the FIFO
                        Main.ui.addText("[" + name + "] " + message);
                        
                        Main.speakFIFO.add(name + "says " + message);
                        Main.putIntoSpeakLatch.countDown();//unlock read thread so it will read
                    }
                    else if(s.startsWith("/c"))//connection info update
                    {
                        s = s.substring(2);//parse out the /c
                        Main.ui.addText(s);
                        Main.speakFIFO.add(s);
                        Main.putIntoSpeakLatch.countDown();//unlock read thread so it will read
                    }
                }
                else
                {
                    Main.putIntoTextAreaLatch = new CountDownLatch(1);//after empty FIFO, relock
                    if(Main.DEBUG)System.out.println("putIntoTextAreaLatch locked");
                }
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

    public void kill()
    {
        Main.putIntoTextAreaLatch.countDown();//break latch to quit
        this.isRunning = false;
    }
}
