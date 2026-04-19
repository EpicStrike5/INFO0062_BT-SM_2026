package be.uliege.montefiore.oop;

import java.util.ArrayList;
import java.util.List;

/*
 * Holds all data for a puzzle: its dimensions, the list of pieces, the solution
 * grid, and flags tracking which pieces have been placed.
 *
 * Also contains the program entry point (main).
 *
 * Typical usage:
 *   1. PuzzleIO.loadPuzzle(filename, puzzle)  — fills width, height, pieces
 *   2. puzzle.initialize()                    — allocates grid and used-flags
 *   3. PuzzleSolver.solve(puzzle)             — fills grid with the solution
 */
public class Puzzle {

    private int width;
    private int height;
    private ArrayList<Element> pieces;
    private Placement[][] grid;
    private boolean[] used;

    public Puzzle(int width, int height, ArrayList<Element> pieces) {
        this.width = width;
        this.height = height;
        this.pieces = pieces;
    }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public ArrayList<Element> getPieces() { return pieces; }
    public void setPieces(ArrayList<Element> p) { this.pieces = p; }

    public void markUsed(int i, boolean value) { used[i] = value; }
    public boolean isUsed(int i)               { return used[i]; }

    public void setPlacement(Placement p, int row, int col) { grid[row][col] = p; }
    public Placement getPlacement(int row, int col)         { return grid[row][col]; }

    // Allocates the solution grid and the used-piece flags. Call this before solving.
    public void initialize() {
        this.grid = new Placement[this.height][this.width];
        this.used = new boolean[this.pieces.size()];
    }

    /*
     * Program entry point.
     *
     * Usage:
     *   java Puzzle <file>            — solve and print the solution
     *   java Puzzle <file> --display  — also open a graphical window
     *   java Puzzle <file> --timer    — also print how long solving took
     *   java Puzzle <width> <height>  — generate a random puzzle and solve it
     *
     * Flags can be combined in any order:
     *   java Puzzle puzzle.txt --display --timer
     */
    public static void main(String[] args) {

        // We sort the args into named flags and positional arguments so that
        // their order on the command line doesn't matter.
        boolean showDisplay = false;
        boolean showTimer = false;
        List<String> positional = new ArrayList<String>();

        for (String arg : args) {
            if (arg.equals("--display"))
                showDisplay = true;
            else if (arg.equals("--timer"))
                showTimer = true;
            else if (arg.startsWith("--")) {
                PuzzleError.unknownFlag(arg);
                return;
            }
            else
                positional.add(arg);
        }

        // One positional arg means the user gave a file to solve.
        // Two positional args means the user wants a randomly generated puzzle.
        String filename = null;

        if (positional.size() == 1) {
            filename = positional.get(0);

        } else if (positional.size() == 2) {
            try {
                int w = Integer.parseInt(positional.get(0));
                int h = Integer.parseInt(positional.get(1));
                if (!PuzzleIO.generatePuzzle(w, h))
                    return;
                filename = "puzzle_generated.txt";
            } catch (NumberFormatException e) {
                PuzzleError.wrongUsage();
                return;
            }

        } else {
            PuzzleError.wrongUsage();
            return;
        }

        // We create a blank Puzzle object and let PuzzleIO fill in its fields.
        Puzzle puzzle = new Puzzle(0, 0, null);
        if (!PuzzleIO.loadPuzzle(filename, puzzle))
            return;

        puzzle.initialize();

        // Time the solver so we can optionally report it.
        long startTime = System.currentTimeMillis();
        boolean solved = PuzzleSolver.solve(puzzle);
        long elapsedMs = System.currentTimeMillis() - startTime;

        if (showTimer) {
            System.err.println("Solving took " + elapsedMs + " ms.");
        }

        if (solved) {
            // Print the solution: one line per cell, "pieceIndex rotation" (1-based index).
            for (int row = 0; row < puzzle.getHeight(); row++) {
                for (int col = 0; col < puzzle.getWidth(); col++) {
                    Placement p = puzzle.getPlacement(row, col);
                    System.out.println((p.getIndex() + 1) + " " + p.getRotation());
                }
            }
            if (showDisplay)
                PuzzleDisplay.show(puzzle);

        } else {
            PuzzleError.noSolution(filename);
            if (showDisplay) {
                // Fill the grid with the best partial arrangement we can find,
                // then open the window so the user can see how far we got.
                PuzzleSolver.partialSolve(puzzle);
                PuzzleDisplay.show(puzzle);
            }
        }
    }
}
