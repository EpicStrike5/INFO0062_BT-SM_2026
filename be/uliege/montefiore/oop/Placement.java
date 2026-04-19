package be.uliege.montefiore.oop;

public class Placement {

    private final int index;
    private final int rotation;

    public Placement(int index, int rotation) {
        this.index = index;
        this.rotation = rotation;
    }

    public int getIndex() {
        return index;
    }

    public int getRotation() {
        return rotation;
    }
}
