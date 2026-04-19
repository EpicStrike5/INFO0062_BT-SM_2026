package be.uliege.montefiore.oop;

/*
 * Represents what occupies one cell in the solution grid:
 * which piece (by 0-based index into the puzzle's piece list) and
 * how many clockwise quarter-turns it has been rotated (0 = original, 1–3 = rotated).
 *
 * Placement objects are immutable — once created they never change.
 */
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
