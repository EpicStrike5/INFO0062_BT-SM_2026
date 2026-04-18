package be.uliege.montefiore.oop;

/*
 * Central code of all error messages for the puzzle program.
 * Each error is a static method so call sites stay clean and all
 * wording lives in one place.
 */
public class PuzzleError {

    // Utility class: no instances should be created.
    private PuzzleError() {}

    // File errors
    public static void fileNotFound(String filename) {
        System.err.println("Error: file not found: " + filename);
    }

    // Feasibility errors — checked before backtracking starts
    public static void wrongFlatCount(int needed, int found) {
        System.err.println("Error: wrong number of flat sides — need "
                + needed + " (the perimeter), found " + found + ".");
    }


    public static void bumpsAndPitsDontMatch(int bumps, int pits) {
        System.err.println("Error: bumps and pits don't balance — "
                + bumps + " bump(s) vs " + pits + " pit(s).");
    }

    public static void wrongCornerCount(int needed, int found) {
        System.err.println("Error: wrong number of corner pieces — need "
                + needed + ", found " + found + ".");
    }

    public static void wrongEdgeCount(int needed, int found) {
        System.err.println("Error: wrong number of edge pieces — need "
                + needed + ", found " + found + ".");
    }

    public static void wrongInsideCount(int needed, int found) {
        System.err.println("Error: wrong number of inside pieces — need "
                + needed + ", found " + found + ".");
    }

    // Solver and CLI errors
    public static void noSolution(String filename) {
        System.err.println("Error: no solution found for: " + filename);
    }

    public static void unknownFlag(String flag) {
        System.err.println("Error: unknown flag: " + flag);
    }

    public static void wrongUsage() {
        System.err.println("Usage: java Puzzle <file> [--display] [--timer]");
        System.err.println("       java Puzzle <width> <height>");
    }
}
