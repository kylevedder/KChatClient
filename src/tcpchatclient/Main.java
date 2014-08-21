/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpchatclient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Kyle
 */
public class Main
{

    static final boolean DEBUG = false;
    
    static ChatUI ui;
    static Socket clientSocket = null;
    static BufferedReader inFromServer = null;
    static DataOutputStream outToServer = null;
    static String address = null;
    static String port = null;
    static ReadThread rt = null;
    static WriteThread wt = null;
    static TextFeederThread tf = null;
    static SpeakThread st = null;

    static volatile CountDownLatch outToServerLatch = new CountDownLatch(1);
    static volatile CountDownLatch putIntoTextAreaLatch = new CountDownLatch(1);
    static volatile CountDownLatch putIntoSpeakLatch = new CountDownLatch(1);

    static volatile LinkedList<String> outToServerFIFO = new LinkedList<String>();
    static volatile List<String> textFIFO = Collections.synchronizedList(new LinkedList<String>());
    static volatile List<String> speakFIFO = Collections.synchronizedList(new LinkedList<String>());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        ui = new ChatUI();
        ui.setVisible(true);
    }

    public static void startConnection(String address, int port, String nickname, boolean speak)
    {
        if (getPortFromServer(address, port))//if sockets started succesfully
        {
            outToServerFIFO.clear();
            textFIFO.clear();
            speakFIFO.clear();
            if(Main.DEBUG)System.out.println("Starting threads");
            outToServerLatch = new CountDownLatch(1);
            putIntoTextAreaLatch = new CountDownLatch(1);
            rt = new ReadThread(inFromServer);
            rt.start();
            wt = new WriteThread(outToServer, nickname);
            wt.start();
            tf = new TextFeederThread();
            tf.start();
            if(speak)
            {
                st = new SpeakThread();
                st.start();
            }
            if(Main.DEBUG)System.out.println("threads started");
        }
    }

    public static void killConnection()
    {
        if(Main.DEBUG)System.out.println("HALTING CONNECTIONS");
        if (wt != null)
        {
            wt.printLogoff();
        }
        if (rt != null)
        {
            rt.kill();
        }
        if (wt != null)
        {
            wt.kill();
        }
        if (tf != null)
        {
            tf.kill();
        }

        try
        {
            if (inFromServer != null)
            {
                inFromServer.close();
            }
            if (outToServer != null)
            {
                outToServer.close();
            }
            if (clientSocket != null)
            {
                clientSocket.close();
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        ui.resetUI();
        if(Main.DEBUG)System.out.println("CONNECTIONS HALTED");
    }

    public synchronized static void addStringToSubmit(String s)
    {
        outToServerFIFO.add(s);
        outToServerLatch.countDown();//unlock outToServerLatch for access

    }

    public synchronized static String getStringToSubmit()
    {
        if (!outToServerFIFO.isEmpty())
        {
            return outToServerFIFO.removeFirst();
        }
        else
        {
            outToServerLatch = new CountDownLatch(1);//reset outToServerLatch
            return "";
        }
    }

    private static boolean getPortFromServer(String address, int port)
    {
        boolean socketSucceed = true;
        try
        {
            if(Main.DEBUG)System.out.println("Starting client connection");
            try
            {
                clientSocket = new Socket(address, port);
            }
            catch (java.net.ConnectException ex)
            {
                if(Main.DEBUG)System.out.println("Connection failed, killing connection");
                Main.killConnection();
                ui.resetUI();
                socketSucceed = false;
                showErrorBox("Connection Failed!");
            }

            if (clientSocket != null && socketSucceed)
            {
                if(Main.DEBUG)System.out.println("Client connection started");
                inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                if(Main.DEBUG)System.out.println("In and out setup");
            }

        }
        catch (IOException ex)
        {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return socketSucceed;
    }

    /**
     * Creates an error message with a custom body.
     *
     * @param errorMessage String holding error message body
     */
    public static void showErrorBox(String errorMessage)
    {
        JOptionPane.showMessageDialog(null, errorMessage, "Error!", JOptionPane.ERROR_MESSAGE);
    }

    //<editor-fold defaultstate="collapsed" desc="Basic Test Client">
    private static void basicTestClient()
    {
        try
        {
            String sentence;
            String modifiedSentence = "";
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

            if(Main.DEBUG)System.out.println("start");
            Socket clientSocket = null;
            clientSocket = new Socket("localhost", 6789);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            while (true)
            {
                sentence = inFromUser.readLine();
                try
                {
                    outToServer.writeBytes(sentence + '\n');
                    modifiedSentence = inFromServer.readLine();
                }
                catch (java.net.SocketException ex)
                {
                    System.err.println("Connection to server lost");
                    modifiedSentence = "";
                }

                if(Main.DEBUG)System.out.println("FROM SERVER: " + modifiedSentence);
            }
//            clientSocket.close();
        }
        catch (IOException ex)
        {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
//</editor-fold>
}
