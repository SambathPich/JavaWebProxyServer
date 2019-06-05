package project2_sambathpich;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProcessorHook extends Thread {

    @Override
    public void run() {
        int totalHttp = Main.countHttpRequest;
        int totalCacheHit = Main.countCacheHit;
        int totalCacheMissed = Main.countCacheMissed;

        float cacheHitRate;   //Cache hit ratio = [Cache Hits / (Cache Hits + Cache Misses)] x 100 %
        DecimalFormat df = new DecimalFormat("0.00");
        cacheHitRate = (float) totalCacheHit/totalHttp;
        cacheHitRate = cacheHitRate * 100;
        cacheHitRate = Float.parseFloat(df.format(cacheHitRate));
        Date date = new Date();

        writeLogFile("Date Terminated: " + new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss +-hhmm").format(date));
        writeLogFile("Total Http Request: " + totalHttp);
        writeLogFile("Total Cache Hit: " + totalCacheHit);
        writeLogFile("Total Cache Missed: " + totalCacheMissed);
        writeLogFile("Hit Rate: %" + cacheHitRate + "\n");
        System.out.println("Report Created");
    }


    private static void writeLogFile(String logEntry) {
        try {
            File file = new File("/Users/sambathpich/WebProxyServer/termination_report.txt");

            if (file.createNewFile()) {
                FileWriter writer = new FileWriter("/Users/sambathpich/WebProxyServer/termination_report.txt", true);
                writer.write(logEntry);
                writer.write("\n");
                writer.close();
            } else {
                FileWriter writer = new FileWriter("/Users/sambathpich/WebProxyServer/termination_report.txt", true);
                BufferedWriter bufferedWriter = new BufferedWriter(writer);
                writer.write(logEntry);
                bufferedWriter.newLine();
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
