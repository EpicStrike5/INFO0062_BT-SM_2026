package be.uliege.montefiore.oop;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class PuzzleIO {

    private PuzzleIO() {}

    public static boolean loadPuzzle(String filename, Puzzle puzzle) {

        ArrayList<Element> pieces = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            String[] sizeParts = reader.readLine().trim().split("\\s+");
            int width  = Integer.parseInt(sizeParts[0]);
            int height = Integer.parseInt(sizeParts[1]);

            puzzle.setWidth(width);
            puzzle.setHeight(height);

            int expected = width * height;
            String line;
            while ((line = reader.readLine()) != null && pieces.size() < expected) {
                if (line.trim().isEmpty())
                    continue;
                char[] sides = { line.charAt(0), line.charAt(1), line.charAt(2), line.charAt(3) };
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

    // random puzzles are almost never solvable but useful for manual testing
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
