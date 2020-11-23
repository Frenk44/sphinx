package nl.fah.monitor.data;

import java.sql.Timestamp;

public class DataStore {
    Timestamp timestamp;
    int sequence;
    int write_index;
    int read_index;
    String buf0;
    String buf1;

    public void DataStore()
    {
        write_index = 0;
        read_index = 1;
        sequence = 0;
    }

    public void write(String input)
    {
        sequence++;
        timestamp = new Timestamp(System.currentTimeMillis());
        if(write_index==0)
        {
            buf0 = input;
            write_index = 1;
            read_index = 0;
        }
        else
        {
            buf1 = input;
            write_index = 0;
            read_index = 1;
        }

    }

    public String read()
    {
        if(read_index==0)
        {
            return buf0;
        }
        else
        {
            return buf1;
        }
    }


}
