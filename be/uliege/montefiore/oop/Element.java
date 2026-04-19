package be.uliege.montefiore.oop;

/*
 * Abstract base class for all puzzle pieces.
 *
 * Every piece has four sides in clockwise order: top, right, bottom, left.
 * Each side is one of:
 *   F = Flat  (sits on the outer border of the puzzle)
 *   B = Bump  (protrudes outward, fits into a Pit)
 *   P = Pit   (recessed inward, accepts a Bump)
 *
 * Pieces are immutable after creation. Use the factory method Element.of(sides)
 * instead of calling a constructor directly, it picks the right subtype based on the number of flat sides.
 */
public abstract class Element {

    private final char top;
    private final char right;
    private final char bottom;
    private final char left;

    // Called by each subclass constructor via super(sides).
    public Element(char[] sides) {
        this.top = sides[0];
        this.right = sides[1];
        this.bottom = sides[2];
        this.left = sides[3];
    }

    public char getTop()    { return top; }
    public char getRight()  { return right; }
    public char getBottom() { return bottom; }
    public char getLeft()   { return left; }

    /*
     * Factory method — call this instead of using constructors directly.
     * It inspects the flat sides to decide which subtype to create:
     *
     *   Two adjacent flat sides  →  CornerPiece  (goes in a grid corner)
     *   Exactly one flat side    →  EdgePiece    (goes along a grid border)
     *   No flat sides            →  InsidePiece  (goes in the interior)
     *
     * We check adjacent flats because that's the stricter condition —
     * a corner piece also has at least one flat, so order matters here.
     */
    public static Element of(char[] sides) {
        boolean adjacentFlats =
            (sides[0] == 'F' && sides[1] == 'F') ||
            (sides[1] == 'F' && sides[2] == 'F') ||
            (sides[2] == 'F' && sides[3] == 'F') ||
            (sides[3] == 'F' && sides[0] == 'F');

        if (adjacentFlats)
            return new CornerPiece(sides);

        boolean anyFlat = sides[0] == 'F' || sides[1] == 'F' ||
                          sides[2] == 'F' || sides[3] == 'F';

        if (anyFlat)
            return new EdgePiece(sides);

        return new InsidePiece(sides);
    }

    /*
     * Returns a new piece rotated clockwise by the given number of quarter-turns.
     * The original piece is never modified, we compute new side values in local
     * variables and pass them to Element.of() at the end.
     *
     * One clockwise quarter-turn shifts the sides like this:
     *   new top    = old left
     *   new right  = old top
     *   new bottom = old right
     *   new left   = old bottom
     */
    public Element rotate(int times) {
        // We copy the four sides into local variables so we can shuffle them
        // without touching the final fields.
        char t = top, r = right, b = bottom, l = left;
        for (int i = 0; i < times; i++) {
            // We use a temporary variable to do a four-way circular shift in place.
            char temp = r;
            r = t;
            t = l;
            l = b;
            b = temp;
        }
        return Element.of(new char[]{ t, r, b, l });
    }

    /*
     * Returns true if this piece is compatible with a neighbour in the given direction.
     *   0 = neighbour is above  (this.top    must match neighbour.bottom)
     *   1 = neighbour is right  (this.right  must match neighbour.left)
     *   2 = neighbour is below  (this.bottom must match neighbour.top)
     *   3 = neighbour is left   (this.left   must match neighbour.right)
     *
     * Valid pairings: F–F, B–P, P–B.
     */
    public boolean isCompatibleWith(Element neighbour, int direction) {
        switch (direction) {
            case 0: return sidesMatch(this.top, neighbour.bottom);
            case 1: return sidesMatch(this.right, neighbour.left);
            case 2: return sidesMatch(this.bottom, neighbour.top);
            case 3: return sidesMatch(this.left, neighbour.right);
            default: return false;
        }
    }

    private boolean sidesMatch(char a, char b) {
        return (a == 'F' && b == 'F') ||
               (a == 'B' && b == 'P') ||
               (a == 'P' && b == 'B');
    }
}
