package nl.fah.common;

import nl.fah.logger.DataLogger;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

public class Utils {

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
                e.printStackTrace();
            }
        }
        dumper.close();
    }

}
