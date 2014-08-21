/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpchatclient;

import com.gtranslate.Audio;
import com.gtranslate.Language;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javazoom.jl.decoder.JavaLayerException;

/**
 *
 * @author Kyle
 */
public class SpeakThread extends Thread
{

    private volatile boolean isRunning = true;

    @Override
    public void run()
    {
        if (Main.DEBUG)
        {
            System.out.println("SpeakThread Started");
        }
        Audio audio = Audio.getInstance();
        while (isRunning)
        {
            try
            {
                Main.putIntoSpeakLatch.await();//wait for items to be put into speak thread
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(ReadThread.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (isRunning)//still running.
            {
                if (!Main.speakFIFO.isEmpty())
                {
                    String message = Main.speakFIFO.get(0);
                    Main.speakFIFO.remove(0);
                    InputStream sound = null;
                    try
                    {                        
                        sound = audio.getAudio(message, Language.ENGLISH);
                        audio.play(sound);
                    }
                    catch (IOException ex)
                    {
                        Logger.getLogger(SpeakThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    catch (JavaLayerException ex)
                    {
                        Logger.getLogger(SpeakThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    finally
                    {
                        try
                        {
                            sound.close();
                            audio = Audio.getInstance();
                        }
                        catch (IOException ex)
                        {
                            Logger.getLogger(SpeakThread.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                else
                {
                    Main.putIntoSpeakLatch = new CountDownLatch(1);//after empty FIFO, relock
                    if(Main.DEBUG)System.out.println("putIntoSpeakLatch locked");
                }
            }
        }
    }

    public void kill()
    {
        Main.putIntoSpeakLatch.countDown();//break latch, to quit immeditly        
        isRunning = false;
    }
}
