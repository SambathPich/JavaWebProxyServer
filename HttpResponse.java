package project2_sambathpich;

import java.io.*;

public class HttpResponse {

    final static String CRLF = "\r\n";
    final static int BUF_SIZE = 819200;
    /*
        Maximum size of objects that this proxy can handle.
        For the moment set to 20 MB. You can adjust this as needed.
        1,000,000 bytes = 1M
        //Cache works: 200000
    */
    final static int MAX_OBJECT_SIZE = 200000;

    /* Reply status and headers */
    String statusLine = "";
    String headers = "";
    static int responseSize = 0;
    static String responseStatusCode = null;
    static String responseStatus = null;

    /* Body of reply */
    byte[] body = new byte[MAX_OBJECT_SIZE];

    /* Read response from server */
    public HttpResponse(DataInputStream fromServer) {

        /* Length of the object */
        int length = -1;
        boolean gotStatusLine = false;

        /* First read status line and Response Headers */
        try {
            String line = fromServer.readLine(); /* Read inputStream from server */
            responseSize = 0;

            while (line.length() != 0) {
                if (!gotStatusLine) {
                    statusLine = line;
                    gotStatusLine = true;
                    responseStatus = statusLine;
                }
                else {
                    headers += line + CRLF;
                }

                /*
                    Get length of content as indicated by Content-Length header.
                    Unfortunately this is not present in every response.
                    Some servers return the header "Content-Length",
                    While others servers return "Content-length".
                    You need to check for both here.
                */
                //Get File Length
                if (line.startsWith("Content-Length:") || line.startsWith("Content-length:")) {
                    String[] tmp = line.split(" ");
                    length = Integer.parseInt(tmp[1]);

                    //responseSize = length; //Use to determine if file should be cached or not.
                }
                line = fromServer.readLine();
            }
        }
        catch (IOException e) {
            System.out.println("HttpResponse.java error: can't read response from server" + e);
        }

        /* Response Body */
        try {
            int bytesRead = 0;
            byte buf[] = new byte[BUF_SIZE];
            boolean loop = false;
	        /*
	            If we didn't get Content-Length header,
	            just loop until the connection is closed.
	        */
            if (length == -1) {
                loop = true;
            }

            /*
                Read the body in chunks of BUF_SIZE and copy the chunk into body.
                Usually replies come back in smaller chunks than BUF_SIZE.
                The while-loop ends when either we have read Content-Length bytes or
                when the connection is closed (when there is no Connection-Length in the response.
            */

            while (bytesRead < length || loop) {
                /* Read it in as binary data */
                int res = fromServer.read(buf, 0, BUF_SIZE); /* Reads binary data up to specify BUFF_SIZE */
                if (res == -1) {
                    break;
                }

                /* Copy the bytes into body. Make sure we don't exceed the maximum object size. */
                for (int i = 0; i < res && (i + bytesRead) < MAX_OBJECT_SIZE; i++) {
                    body[bytesRead + i] = buf[i]; /* copy bytes read to body */
                }
                bytesRead += res;
                responseSize = bytesRead;
            }
        }
        catch (IOException e) {
            return;
        }
    }

    /*
        Convert response into a string for easy re-sending.
        Only converts the response headers, body is not converted to a string.
    */
    public String toString() {

        /* Working with Response Header here */
        String res = statusLine + CRLF;
        res += headers;
        res += "Cache-Control: no-cache" + CRLF;
        responseStatusCode = statusLine.substring(9, statusLine.length()); //Get status code for log entry

        res += CRLF;
        return res;
    }
}