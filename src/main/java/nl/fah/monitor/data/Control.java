package nl.fah.monitor.data;

class Control {
    public volatile DataStore[] received;

    public Control(int size){
        received = new DataStore[size];

        for(int i=0;i<size;i++)
        {
            received[i] = new DataStore();
        }

    }

}
