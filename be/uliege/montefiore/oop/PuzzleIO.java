package be.uliege.montefiore.oop;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

/*
 * Handles reading puzzle files from disk and generating random puzzles.
 *
 * Expected file format:
 *   Line 1 : "width height"   (e.g. "4 3")
 *   Line 2+: one piece per line, 4 characters — top right bottom left
 *            using F (flat), B (bump), or P (pit)
 */
public class PuzzleIO {

    // Utility class: no instances should be created.
    private PuzzleIO() {}

    // Reads a puzzle file and fills the given Puzzle object with its dimensions
    // and piece list. Returns true on success, false if the file is not found.
    public static boolean loadPuzzle(String filename, Puzzle puzzle) {

        ArrayList<Element> pieces = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            // Read dimensions from the first line.
            String[] sizeParts = reader.readLine().trim().split("\\s+");
            int width  = Integer.parseInt(sizeParts[0]);
            int height = Integer.parseInt(sizeParts[1]);

            puzzle.setWidth(width);
            puzzle.setHeight(height);

            // Read one piece per line, skipping blank lines.
            int expected = width * height;
            String line;
            while ((line = reader.readLine()) != null && pieces.size() < expected) {
                if (line.trim().isEmpty())
                    continue;
                char[] sides = { line.charAt(0), line.charAt(1), line.charAt(2), line.charAt(3) };
                // Element.of() picks the right subtype (CornerPiece, EdgePiece, InsidePiece).
                pieces.add(Element.of(sides));
            }

            puzzle.setPieces(pieces);

        } catch (FileNotFoundException e) {
            PuzzleError.fileNotFound(filename);
            return false;
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    // Generates a random puzzle of the given size and writes it to "puzzle_generated.txt".
    // Note: randomly generated puzzles are almost never solvable.
    public static boolean generatePuzzle(int width, int height) {

        char[] sides = { 'F', 'B', 'P' };
        Random random = new Random();

        try (PrintWriter writer = new PrintWriter("puzzle_generated.txt")) {
            writer.println(width + " " + height);
            for (int i = 0; i < width * height; i++) {
                StringBuilder line = new StringBuilder();
                for (int j = 0; j < 4; j++)
                    line.append(sides[random.nextInt(3)]);
                writer.println(line);
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}
