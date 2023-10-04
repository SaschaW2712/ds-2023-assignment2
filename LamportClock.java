
//Represents a Lamport Clock with a single time-value, used for logical ordering.
public class LamportClock {
    private int value = 0;
    
    //Increments the clock's value
    public synchronized void tick() {
        value++;
    }

    //Getter for the clock's value
    public synchronized int getValue() {
        return value;
    }

    //Given a new potential time, updates the clock's value if it is greater
    public synchronized void updateValue(int newValue) {
        value = Math.max(value, newValue);
    }
}
