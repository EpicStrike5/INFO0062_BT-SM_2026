package be.uliege.montefiore.oop;

/*
 * A puzzle piece with no flat sides.
 * Inside pieces fill the interior of the puzzle grid, away from any border.
 */
public class InsidePiece extends Element {

    // Passes the four sides directly to the parent Element constructor.
    public InsidePiece(char[] sides) {
        super(sides);
    }
}
