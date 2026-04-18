package be.uliege.montefiore.oop;

/*
 * A puzzle piece with exactly two adjacent flat sides.
 * Corner pieces belong in one of the four corners of the puzzle grid.
 */
public class CornerPiece extends Element {

    // Passes the four sides directly to the parent Element constructor.
    public CornerPiece(char[] sides) {
        super(sides);
    }
}
