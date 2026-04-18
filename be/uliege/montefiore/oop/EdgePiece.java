package be.uliege.montefiore.oop;

/*
 * A puzzle piece with exactly one flat side.
 * Edge pieces sit along one of the four sides of the puzzle grid, but not in a corner.
 */
public class EdgePiece extends Element {

    // Passes the four sides directly to the parent Element constructor.
    public EdgePiece(char[] sides) {
        super(sides);
    }
}
