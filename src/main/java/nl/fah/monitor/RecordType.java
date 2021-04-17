package nl.fah.monitor;

import java.util.List;

public class RecordType {
    byte[] data;
    List<Long>  tval;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public List<Long>  getTval() {
        return tval;
    }

    public void setTval(List<Long> tval) {
        this.tval = tval;
    }



}
