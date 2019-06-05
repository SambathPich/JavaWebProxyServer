package project2_sambathpich;

import java.io.*;

public class HttpRequest {

    final static String CRLF = "\r\n";
    final static int HTTP_PORT = 80;
    static String tempURI;

    /* Store the request parameters */
    String method;
    String URI;
    String version;
    String headers = "";

    /* Server and Port */
    private String host;
    private int port;

    /* Create HttpRequest by reading it from the client socket */
    public HttpRequest(BufferedReader fromClient) {

        String firstLine = "";

        firstLine = fromClient.readLine(); /* Read firstLine only of the request */
       
        try {
            /* Breakdown firstLine requests => GET http://www.google.com HTTP/1.0 */
            String[] tmp = firstLine.split(" ");
            method = tmp[0];    /* method GET */
            URI =  tmp[1];      /* URI */
            version =  tmp[2];  /* HTTP version */

            tempURI = URI;  //Get fileName to check if it should be cached or not

            /* Read all lines request  */
            String line = fromClient.readLine();

            while (line.length() != 0) {
                headers += line + CRLF;

                /*
                    We need to find host header to know which server to contact
                    in case the request URI is not complete.
                */
                if (line.startsWith("Host:")) {

                    tmp = line.split(" ");
                    if (tmp[1].indexOf(':') > 0) {
                        String[] tmp2 = tmp[1].split(":");
                        host = tmp2[0];
                        port = Integer.parseInt(tmp2[1]);
                    }
                    else {
                        host = tmp[1];
                        port = HTTP_PORT;
                    }
                }

                if (line.startsWith("Referer:")) {
                    if (line.toLowerCase().contains("amazon".toLowerCase()) ||
                            line.toLowerCase().contains("google".toLowerCase()) ||
                            line.toLowerCase().contains("widgets".toLowerCase())) {}
                    else {

                        boolean isDuplicated = false;
                        int startIndex = 0;
                        int stopIndex = 0;
                        int plusTimes = 0;
                        String refererURI;

                        if (line.toLowerCase().contains("www.".toLowerCase())) {
                            startIndex = line.indexOf("http://www.");
                            if (line.toLowerCase().contains(".com".toLowerCase())) {
                                stopIndex = line.indexOf(".com");
                                plusTimes = 4;
                            } else if (line.toLowerCase().contains(".co.uk".toLowerCase())) {
                                stopIndex = line.indexOf(".co.uk");
                                plusTimes = 6;
                            } else if (line.toLowerCase().contains(".edu".toLowerCase())) {
                                stopIndex = line.indexOf(".edu");
                                plusTimes = 4;
                            }
                        } else {
                            startIndex = line.indexOf("http://");
                            if (line.toLowerCase().contains(".com".toLowerCase())) {
                                stopIndex = line.indexOf(".com");
                                plusTimes = 4;
                            } else if (line.toLowerCase().contains(".co.uk".toLowerCase())) {
                                stopIndex = line.indexOf(".co.uk");
                                plusTimes = 6;
                            } else if (line.toLowerCase().contains(".edu".toLowerCase())) {
                                stopIndex = line.indexOf(".edu");
                                plusTimes = 4;
                            } else if (line.toLowerCase().contains(".nku.edu".toLowerCase())) {
                                stopIndex = line.indexOf(".nku.edu");
                                plusTimes = 8;
                            }

                        }

                        refererURI = line.substring(startIndex, stopIndex + plusTimes);
                        //System.out.println("REFER:" + refererURI);

                        //If Last Index is NOT assigned yet
                        if (Main.reachCacheLimit == false) {

                            //Loop in Array and check for existing one
                            for (int i = 0; i < Main.maxWebPages.length; i++) {
                                if (refererURI.equals(Main.maxWebPages[i])) {
                                    isDuplicated = true;
                                    break;
                                }

                            }

                            if (isDuplicated != true) {
                                int getAvailableIndex = 0;
                                for (int i = 0; i < Main.maxWebPages.length; i++) {
                                    if (Main.maxWebPages[i] == null) {
                                        getAvailableIndex = i;
                                        break;
                                    }
                                }
                                Main.maxWebPages[getAvailableIndex] = refererURI;
                            }
                            //Main.maxWebPages[0] = line.substring(startIndex, stopIndex + 4);

                            if (Main.maxWebPages[Main.maxWebPages.length - 1] != null) {
                                Main.reachCacheLimit = true;
                            }

                        } else {
                            System.out.println("\nCache Limit IS full:");
                        }
                    }
                }
                line = fromClient.readLine();
            }
        }
        catch (IOException e) {
            return;
        }
    }

    /* Return host for which this request is intended */
    public String getHost() {
        return host;
    }

    /* Return port for server */
    public int getPort() {
        return port;
    }


    /* Convert request into a string */
    public String toString() {
        String req = method + " " + URI + " " + version + CRLF;
        req += headers;
        return req;
    }
}