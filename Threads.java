package project2_sambathpich;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Threads implements Runnable
{

    private final Socket client;

    /* Constructor */
    public Threads(Socket client) {
        this.client = client;
    }

    /* Start thread here */
    public void run() {
        //Get Receiving Time
        long millisReceived=System.currentTimeMillis();
        java.util.Date dateReceived=new java.util.Date(millisReceived);

        Main.countHttpRequest ++;

        Socket mySocket = null;
        HttpRequest myHttpRequest;
        HttpResponse myHttpResponse;
        String isCacheMissed = "Missed";
        String replacementAlg = "N/A";
        boolean isValidated = false;

        /* ::::READ REQUEST FROM CLIENT */
        try {
            BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            myHttpRequest = new HttpRequest(fromClient);    /** 4. Read HttpRequest from client **/
        }
        catch (IOException e) {
            //System.out.println("Thread.java error: can't read request from client: " + e);
            return;
        }

        /* Send request to server */
        try {
	    /* Open socket and write request to socket */
            mySocket = new Socket(myHttpRequest.getHost(), myHttpRequest.getPort()); /* Create socket */
            DataOutputStream toServer = new DataOutputStream(mySocket.getOutputStream()); /* Create DataOutputStream */
            toServer.writeBytes(myHttpRequest.toString()); /* Write request */
            toServer.writeBytes("\r\n");

            //System.out.println("REQUEST HEADER: " + myHttpRequest.toString());

            toServer.flush();

        }
        catch (UnknownHostException e) {
            return;
        } catch (IOException e)
        {
            return;
        }
        catch (Exception e) {}

        /* ::::READ RESPONSE AND FORWARD IT TO CLIENT::::: */
        try {
            /* 5. Check cache if URI exists */
            byte[] getCache = Main.readFromCache(myHttpRequest.URI);

            /* NO CACHE, Create Caches */
            if (getCache.length == 0) {

                DataInputStream fromServer = new DataInputStream(mySocket.getInputStream()); /* Create DataInputStream */
                myHttpResponse = new HttpResponse(fromServer); /* 6. Create object with server response */
                DataOutputStream toClient = new DataOutputStream(client.getOutputStream());

                toClient.writeBytes(myHttpResponse.toString()); /* Create headers */
                toClient.write(myHttpResponse.body); /* Create body */

                /* Write response to client. First headers, then body */
                toClient.flush();

                //If bigger than Threshold AND cache is over the limit => No cache
                if( (HttpResponse.responseSize < Main.cacheThreshold) && (cacheOverLimited() == false)) {

                    if(myHttpResponse.responseStatus.toLowerCase().contains("304".toLowerCase())) {
                        replacementAlg = "Evicted";
                    }
                    else {

                        //Main.countCacheMissed++;    //Total cache missed
                        isCacheMissed = "Missed";

                        /** 7. Write Cache */
                        Main.createCache(myHttpRequest, myHttpResponse);
                    }
                }
                else {
                    replacementAlg = "Evicted";
                }
                isValidated = false;
            }

            /* CACHE IS FOUND */
            else {

                /*
                Explain:
                    - If Cache exist, return the number of bytes that have been placed into buffer.
                    - Then pass it to user, so no need to request to server again.

                    - With Consistency:
                        1. ETag: Compare Cache Etag and Server Etag
                            + If they are the same, return 304 header only
                            + If NOT:
                                - request that file again and return to user.
                                - remove old cache
                                - create new cache with the same URI
                */

                //Get ETAG from Cache File
                String cacheETAG = "";
                InputStream readBytes = null;
                BufferedReader bfReader = null;
                try {
                    readBytes = new ByteArrayInputStream(getCache);
                    bfReader = new BufferedReader(new InputStreamReader(readBytes));
                    String temp = null;

                    //Get only ETAG and then terminate
                    boolean gotEtag = false;
                    while((temp = bfReader.readLine()) != null){
                        if(gotEtag == true)
                        {
                            break;
                        }

                        //Last line of header
                        if (temp.startsWith("ETag:"))
                        {
                            cacheETAG = temp.substring(7,temp.length()-1);
                            gotEtag = true;
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if(readBytes != null) readBytes.close();
                    } catch (Exception ex){

                    }
                }

                //NO ETag, just paste file from MEMORY Cache
                if(cacheETAG == "") {
                    isValidated = false;

                    //READ RESPONSE FROM MEMORY to CLIENT
                    DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
                    toClient.write(getCache);
                    toClient.flush();


                }
                else {
                    isValidated = true;


                    /* Send request to server to get ETAG only */
                    try {
                        DataOutputStream toServer = new DataOutputStream(mySocket.getOutputStream()); /* Create DataOutputStream */
                        toServer.writeBytes(myHttpRequest.toString()); /* Write request */
                        toServer.writeBytes("If-None-Match: " + cacheETAG);
                        toServer.writeBytes("\r\n");
                        toServer.flush();

                    } catch (UnknownHostException e) {
                        return;
                    } catch (IOException e) {
                        return;
                    } catch (Exception e) {
                    }

                    /* Read Response */
                    DataInputStream fromServer = new DataInputStream(mySocket.getInputStream()); /* Create DataInputStream */
                    myHttpResponse = new HttpResponse(fromServer); /* Create object with server response */
                    int firstPos = myHttpResponse.toString().indexOf("\"");
                    int secondPos = myHttpResponse.toString().lastIndexOf("\"");

                    String serverETAG = myHttpResponse.toString().substring(firstPos + 1, secondPos);

                    //EQUAL ETag
                    if (cacheETAG.equals(serverETAG)) {

                        File cacheLocation = Main.getCacheLocation(myHttpRequest.URI);

                        try {
                            Path filePath = Paths.get(String.valueOf(cacheLocation));
                            String fileContent = new String(Files.readAllBytes(filePath));
                            fileContent = fileContent.replaceAll("200 OK", "304 Not Modified");
                            fileContent = fileContent.replaceAll("Cache-Control: no-cache", "");
                            Files.write(filePath, fileContent.getBytes());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        /* ::::: Send Response Message ::::: */
                        DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
                        toClient.write(getCache);
                        toClient.flush();

                    }
                    //NOT EQUAL ETag
                    else {
                        /*
                            - request that file again and return to user.
                            - remove old cache
                            - create new cache with the same URI
                        */
                        File cacheLocation = Main.getCacheLocation(myHttpRequest.URI);

                        try {

                            File file = new File(String.valueOf(cacheLocation));

                            if(file.delete()) {

                                /* Send request to server to get ETAG only */
                                try {
                                    DataOutputStream toServer = new DataOutputStream(mySocket.getOutputStream()); /* Create DataOutputStream */
                                    toServer.writeBytes(myHttpRequest.toString()); /* Write request */
                                    toServer.writeBytes("\r\n");
                                    toServer.flush();

                                } catch (UnknownHostException e) {
                                    //System.out.println("Thread.java error: Unknown host" + myHttpRequest.getHost());
                                    //System.out.println("Thread.java error: Unknown host");
                                    return;
                                } catch (IOException e) {
                                    //System.out.println("Threads.java error 1: " + e);
                                    return;
                                } catch (Exception e) {
                                    //System.out.println("Thread.java error 2: " + e);
                                }


                                //Read Response Body from Server
                                DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
                                //toClient.writeBytes(myHttpResponse.toString()); /* Create headers */
                                toClient.write(myHttpResponse.body); /* Create body */
                                toClient.flush();

                                Main.createCache(myHttpRequest, myHttpResponse);
                                //System.out.println("CACHE RECREATED");

                                isValidated = true;

                            }
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
                isCacheMissed = "Hit";
            }

        }
        catch (IOException e) {
        }
        catch (Exception e) {
        }

        /* ::::: Write Log Entry ::::: */
        try {

            if (HttpResponse.responseStatusCode.length() > 2) {

                long millisResponse = System.currentTimeMillis();
                java.util.Date dateResponse = new java.util.Date(millisResponse);

                long diff = dateResponse.getTime() - dateReceived.getTime();//as given
                long millisDiff = TimeUnit.MILLISECONDS.toMillis(diff);

                String logContent = "Receiving Time: " + dateReceived + "\n";   //Time Received
                logContent += "Duration: " + millisDiff + " ms\n";
                logContent += "Cache: " + isCacheMissed + "\n";
                logContent += "Replacement: " + replacementAlg + "\n";

                /* Cache Consistency */
                if (isValidated == true) {
                    logContent += "Consistency: Validated" + "\n";
                } else {
                    logContent += "Consistency: N/A" + "\n";
                }

                logContent += "Client: " + this.client.getInetAddress().getHostName() + "\n";    //Client address
                logContent += "URL: " + myHttpRequest.URI + "\n";  //URL requested
                logContent += "Status: " + HttpResponse.responseStatusCode + "\n";

                //logContent += HttpResponse.responseStatusCode;
                writeLogFile(logContent);

                if(isCacheMissed == "Missed") {
                    Main.countCacheMissed++;
                }

                if(isCacheMissed == "Hit") {
                    Main.countCacheHit++;
                }
            }
        }
        catch (Exception e) {

        }

        try {
            client.close();
        }
        catch (IOException e) {
            //e.printStackTrace();
        }
        try {
            mySocket.close();
        }
        catch (IOException e) {
            //e.printStackTrace();
        }
    }


    private static void writeLogFile(String logEntry) {
        try {
            File file = new File("/Users/sambathpich/WebProxyServer/log_entry.txt");

            if (file.createNewFile()) {
                //System.out.println("File is created!");
                FileWriter writer = new FileWriter("/Users/sambathpich/WebProxyServer/log_entry.txt", true);
                writer.write(logEntry + "\r\n");
                writer.close();
            } else {
                //System.out.println("File already exists.");
                FileWriter writer = new FileWriter("/Users/sambathpich/WebProxyServer/log_entry.txt", true);
                BufferedWriter bufferedWriter = new BufferedWriter(writer);
                writer.write("\r\n" + logEntry);
                bufferedWriter.newLine();
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean cacheOverLimited() {

        if(Main.reachCacheLimit == true) {
            return true;
        }
        else {
            return false;
        }
    }
}
