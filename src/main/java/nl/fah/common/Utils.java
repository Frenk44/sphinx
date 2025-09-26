package nl.fah.common;


import nl.fah.monitor.message.MessageMonitor;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Utils {
    static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static boolean validIP(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        ip = ip.trim();
        if ((ip.length() < 6) & (ip.length() > 15)) return false;

        try {
            Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    public static boolean validMulticast(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        ip = ip.trim();
        if ((ip.length() < 6) & (ip.length() > 15)) return false;

        try {
            Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }
    // convert InputStream to String
    public static String getStringFromInputStream(InputStream iss) throws IOException {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(iss));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            throw e;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    throw e;
                }
            }
        }

        return sb.toString();
    }
    
    public static void dumpPCAP(Packet[] packets, String fileName) throws PcapNativeException, NotOpenException, IOException {
        PcapHandle handle = null;
        PcapDumper dumper;

        dumper = handle.dumpOpen(fileName);

        for (Packet packet:packets)
        {
            try {
                dumper.dump(packet, handle.getTimestamp());
            } catch (NotOpenException e) {
                logger.error(e.getMessage());
            }
        }
        dumper.close();
    }

}
