package be.uliege.montefiore.oop;

public abstract class Element {

    private final char top;
    private final char right;
    private final char bottom;
    private final char left;

    public Element(char[] sides) {
        this.top = sides[0];
        this.right = sides[1];
        this.bottom = sides[2];
        this.left = sides[3];
    }

    public char getTop() {
        return top;
    }

    public char getRight() {
        return right;
    }

    public char getBottom() {
        return bottom;
    }

    public char getLeft() {
        return left;
    }

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

    public Element rotate(int times) {
        char t = top, r = right, b = bottom, l = left;
        for (int i = 0; i < times; i++) {
            char temp = r;
            r = t;
            t = l;
            l = b;
            b = temp;
        }
        return Element.of(new char[]{ t, r, b, l });
    }

    // direction: 0=above, 1=right, 2=below, 3=left
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
