package project2_sambathpich;

import java.io.*;
import java.net.*;
import java.util.*;

public class Main {

    private static int port;    /* Get Port assigned later in the program */
    private static ServerSocket socket;

    /* Create the ProxyCache object and the socket */
    private static Map<String, String> cache = new Hashtable<String, String>();
    static int countCacheMissed = 0;
    static int countCacheHit = 0;
    static int countHttpRequest = 0;
    static int cacheThreshold = 20000;
    static boolean reachCacheLimit = false;

    static String[] maxWebPages;

    /* Read command line arguments and start proxy **/
    //Args: PORT THRESHOLD MAX-PAGES
    public static void main(String args[]) {

        int myPort = 9999;
        Runtime.getRuntime().addShutdownHook(new ProcessorHook());  //If program terminates

        /* Create folder to store Cache files */
        File cacheDir = new File("/Users/sambathpich/WebProxyServer/cache/");
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }

        /* No Argument */
        if(args.length == 0) {
            maxWebPages = new String[20];

        }
        else {
            //Set CacheThreshold Argument
            myPort = Integer.parseInt(args[0]);
            cacheThreshold = Integer.parseInt(args[1]);
            maxWebPages = new String[Integer.parseInt(args[2])];
        }

        init(myPort);   /* 2. Create Socket from myPort */

        /*
            Main loop.
            Listen for incoming connections and spawn a new thread for handling them
         */
        Socket client;

        System.out.println("+++++ WEB PROXY SERVER STARTED WITH PORT +++++");
        System.out.println("=> Port: " + myPort);
        System.out.println("=> Cache Threshold: " + cacheThreshold);
        System.out.println("=> Maximum Cached Webpages: " + maxWebPages.length);

        while (true) {
            try {
                client = socket.accept();
                (new Thread(new Threads(client))).start();  /** 3. Create Threads **/
            }
            catch (IOException e) {
                continue;
            }
        }
    }

    public static void init(int portNumber) {
        port = portNumber;
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            System.exit(-1);
        }
    }


    public synchronized static void createCache(HttpRequest myHttpRequest, HttpResponse myHttpResponse) throws IOException {
        File myFile;
        DataOutputStream outputStream;

        myFile = new File("/Users/sambathpich/WebProxyServer/cache/","cached_" + System.currentTimeMillis());

        outputStream = new DataOutputStream( new FileOutputStream(myFile));
        outputStream.writeBytes(myHttpResponse.toString()); /* Receive header from HttpResponse */
        outputStream.write(myHttpResponse.body); /* Receive body from HttpResponse */
        outputStream.close();

        cache.put(myHttpRequest.URI, myFile.getAbsolutePath()); //cache.put(pathName, textFile);
    }

    public synchronized static File getCacheLocation(String URI) throws IOException {
        File myFile;
        String hashFile;

        /* Read cache from URI and put into hashFile */
        /* cache.get(pathName); */
        hashFile = cache.get(URI);

        if(hashFile != null)
        {
            myFile = new File(hashFile);
            return myFile;
        }
        else
        {
            return null;
        }
    }

    public synchronized static byte[] readFromCache(String URI) throws IOException {

        File myFile;
        FileInputStream inputStream;
        String hashFile;
        byte[] bytesCached;

        /* Read cache from URI and put into hashFile */
        /* cache.get(pathName); */
        hashFile = cache.get(URI);

        if(hashFile != null) {
            myFile = new File(hashFile);    //paste cache to myFile
            inputStream = new FileInputStream(myFile);
            bytesCached = new byte[(int)myFile.length()];   //get length in bytes from myFile
            inputStream.read(bytesCached);

            return bytesCached;
        }
        else {
            byte[] bytesZero = new byte[0];  //assign 0 bytes and return it back.
            return bytesZero;
        }
    }
}