package be.uliege.montefiore.oop;

import java.util.ArrayList;
import java.util.List;

// two-phase backtracking: border first (clockwise), then interior (left-to-right top-to-bottom)
public class PuzzleSolver {

    private PuzzleSolver() {}

    // timeout for partialSolve, we stop after 3s and show the best state found
    private static final long PARTIAL_SOLVE_TIMEOUT_MS = 3000L;

    private static long partialSolveDeadline;

    public static boolean solve(Puzzle puzzle) {
        if (!checkFeasibility(puzzle))
            return false;

        List<int[]> borderCells = buildBorderCells(puzzle.getWidth(), puzzle.getHeight());
        if (!fillBorder(puzzle, 0, borderCells))
            return false;

        if (puzzle.getWidth() > 2 && puzzle.getHeight() > 2) {
            if (!fillInterior(puzzle, 1, 1))
                return false;
        }

        return true;
    }

    // rejects obviously unsolvable puzzles before spending time backtracking
    private static boolean checkFeasibility(Puzzle puzzle) {

        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        int totalF = 0, totalB = 0, totalP = 0;
        int corners = 0, edges = 0, insides = 0;

        for (Element e : puzzle.getPieces()) {
            for (char side : new char[]{ e.getTop(), e.getRight(), e.getBottom(), e.getLeft() }) {
                if (side == 'F') 
                    totalF++;
                else if (side == 'B') 
                    totalB++;
                else 
                    totalP++;
            }
            if (e instanceof CornerPiece)
                corners++;
            else if (e instanceof EdgePiece)
                edges++;
            else
                insides++;
        }

        int neededFlats = 2 * (w + h);
        if (totalF != neededFlats) {
            PuzzleError.wrongFlatCount(neededFlats, totalF);
            return false;
        }

        if (totalB != totalP) {
            PuzzleError.bumpsAndPitsDontMatch(totalB, totalP);
            return false;
        }

        // skip thin puzzles (1 row/col), piece-type counts don't apply there
        if (w >= 2 && h >= 2) {
            int neededCorners = 4;
            int neededEdges = 2 * (w - 2) + 2 * (h - 2);
            int neededInsides = (w - 2) * (h - 2);

            if (corners != neededCorners) {
                PuzzleError.wrongCornerCount(neededCorners, corners);
                return false;
            }
            if (edges != neededEdges) {
                PuzzleError.wrongEdgeCount(neededEdges, edges);
                return false;
            }
            if (insides != neededInsides) {
                PuzzleError.wrongInsideCount(neededInsides, insides);
                return false;
            }
        }

        return true;
    }

    private static List<int[]> buildBorderCells(int w, int h) {
        List<int[]> cells = new ArrayList<>();

        // thin puzzles have no "sides" to walk, just list cells linearly
        if (h == 1) {
            for (int c = 0; c < w; c++)
                cells.add(new int[]{0, c});
            return cells;
        }
        if (w == 1) {
            for (int r = 0; r < h; r++)
                cells.add(new int[]{r, 0});
            return cells;
        }

        for (int c = 0; c < w; c++) // top row, clockwise from top-left
            cells.add(new int[]{0, c});
        for (int r = 1; r < h; r++) // right column
            cells.add(new int[]{r, w - 1});
        for (int c = w - 2; c >= 0; c--) // bottom row
            cells.add(new int[]{h - 1, c});
        for (int r = h - 2; r >= 1; r--) // left column
            cells.add(new int[]{r, 0});

        return cells;
    }

    private static boolean fillBorder(Puzzle puzzle, int cellIndex, List<int[]> borderCells) {

        if (cellIndex == borderCells.size())
            return true;

        int row = borderCells.get(cellIndex)[0];
        int col = borderCells.get(cellIndex)[1];
        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        boolean isThinPuzzle = (w == 1 || h == 1);
        boolean isCorner = (row == 0 || row == h - 1) && (col == 0 || col == w - 1);

        for (int i = 0; i < puzzle.getPieces().size(); i++) {
            if (puzzle.isUsed(i))
                continue;
            Element piece = puzzle.getPieces().get(i);

            if (!isThinPuzzle) {
                if (isCorner && !(piece instanceof CornerPiece))
                    continue;
                if (!isCorner && !(piece instanceof EdgePiece))
                    continue;
            }

            for (int rot = 0; rot < 4; rot++) {
                Element rotated = piece.rotate(rot);
                if (!fitsPosition(rotated, row, col, w, h))
                    continue;
                if (!matchesNeighbours(rotated, row, col, puzzle))
                    continue;

                puzzle.setPlacement(new Placement(i, rot), row, col);
                puzzle.markUsed(i, true);

                if (fillBorder(puzzle, cellIndex + 1, borderCells))
                    return true;

                puzzle.setPlacement(null, row, col);
                puzzle.markUsed(i, false);
            }
        }

        return false;
    }

    private static boolean fillInterior(Puzzle puzzle, int row, int col) {

        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        if (row == h - 1)
            return true;

        // wrap to next row when we hit the right border column
        int nextCol = (col + 1 == w - 1) ? 1 : col + 1;
        int nextRow = (col + 1 == w - 1) ? row + 1 : row;

        for (int i = 0; i < puzzle.getPieces().size(); i++) {
            if (puzzle.isUsed(i))
                continue;
            Element piece = puzzle.getPieces().get(i);
            if (!(piece instanceof InsidePiece))
                continue;

            for (int rot = 0; rot < 4; rot++) {
                Element rotated = piece.rotate(rot);
                if (!fitsPosition(rotated, row, col, w, h))
                    continue;
                if (!matchesNeighbours(rotated, row, col, puzzle))
                    continue;

                puzzle.setPlacement(new Placement(i, rot), row, col);
                puzzle.markUsed(i, true);

                if (fillInterior(puzzle, nextRow, nextCol))
                    return true;

                puzzle.setPlacement(null, row, col);
                puzzle.markUsed(i, false);
            }
        }

        return false;
    }

    private static boolean fitsPosition(Element e, int row, int col, int width, int height) {
        if (row == 0 && e.getTop() != 'F')
            return false;
        if (row == height - 1 && e.getBottom() != 'F')
            return false;
        if (col == 0 && e.getLeft() != 'F')
            return false;
        if (col == width - 1 && e.getRight() != 'F')
            return false;
        if (row > 0 && e.getTop() == 'F')
            return false;
        if (row < height - 1 && e.getBottom() == 'F')
            return false;
        if (col > 0 && e.getLeft() == 'F')
            return false;
        if (col < width - 1 && e.getRight() == 'F')
            return false;
        return true;
    }

    private static boolean matchesNeighbours(Element e, int row, int col, Puzzle puzzle) {
        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        if (row > 0 && puzzle.getPlacement(row - 1, col) != null)
            if (!e.isCompatibleWith(effectivePiece(puzzle, row - 1, col), 0))
                return false;
        if (col > 0 && puzzle.getPlacement(row, col - 1) != null)
            if (!e.isCompatibleWith(effectivePiece(puzzle, row, col - 1), 3))
                return false;
        if (col < w - 1 && puzzle.getPlacement(row, col + 1) != null)
            if (!e.isCompatibleWith(effectivePiece(puzzle, row, col + 1), 1))
                return false;
        if (row < h - 1 && puzzle.getPlacement(row + 1, col) != null)
            if (!e.isCompatibleWith(effectivePiece(puzzle, row + 1, col), 2))
                return false;

        return true;
    }

    private static Element effectivePiece(Puzzle puzzle, int row, int col) {
        Placement p = puzzle.getPlacement(row, col);
        return puzzle.getPieces().get(p.getIndex()).rotate(p.getRotation());
    }

    public static void partialSolve(Puzzle puzzle) {
        puzzle.initialize();

        partialSolveDeadline = System.currentTimeMillis() + PARTIAL_SOLVE_TIMEOUT_MS;

        BestPartial best = new BestPartial(puzzle);
        List<int[]> borderCells = buildBorderCells(puzzle.getWidth(), puzzle.getHeight());

        partialFillBorder(puzzle, 0, borderCells, best);

        best.restore(puzzle);
    }

    // snapshots the deepest state reached so we can restore it after backtracking
    private static class BestPartial {

        private int bestCount = -1;
        private Placement[][] savedGrid;
        private boolean[] savedUsed;

        BestPartial(Puzzle puzzle) {
            savedGrid = new Placement[puzzle.getHeight()][puzzle.getWidth()];
            savedUsed = new boolean[puzzle.getPieces().size()];
        }

        void update(Puzzle puzzle, int filledCells) {
            if (filledCells <= bestCount)
                return;
            bestCount = filledCells;
            int h = puzzle.getHeight();
            int w = puzzle.getWidth();
            for (int r = 0; r < h; r++)
                for (int c = 0; c < w; c++)
                    savedGrid[r][c] = puzzle.getPlacement(r, c);
            int n = puzzle.getPieces().size();
            for (int i = 0; i < n; i++)
                savedUsed[i] = puzzle.isUsed(i);
        }

        void restore(Puzzle puzzle) {
            puzzle.initialize();
            int h = puzzle.getHeight();
            int w = puzzle.getWidth();
            for (int r = 0; r < h; r++)
                for (int c = 0; c < w; c++)
                    puzzle.setPlacement(savedGrid[r][c], r, c);
            int n = puzzle.getPieces().size();
            for (int i = 0; i < n; i++)
                puzzle.markUsed(i, savedUsed[i]);
        }
    }

    private static boolean partialFillBorder(Puzzle puzzle, int cellIndex,
                                             List<int[]> borderCells, BestPartial best) {
        if (System.currentTimeMillis() > partialSolveDeadline)
            return true;
        best.update(puzzle, cellIndex);

        if (cellIndex == borderCells.size()) {
            if (puzzle.getWidth() > 2 && puzzle.getHeight() > 2) {
                partialFillInterior(puzzle, 1, 1, borderCells.size(), best);
            }
            return true;
        }

        int row = borderCells.get(cellIndex)[0];
        int col = borderCells.get(cellIndex)[1];
        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        boolean isThinPuzzle = (w == 1 || h == 1);
        boolean isCorner = (row == 0 || row == h - 1) && (col == 0 || col == w - 1);

        for (int i = 0; i < puzzle.getPieces().size(); i++) {
            if (puzzle.isUsed(i))
                continue;
            Element piece = puzzle.getPieces().get(i);

            if (!isThinPuzzle) {
                if (isCorner && !(piece instanceof CornerPiece))
                    continue;
                if (!isCorner && !(piece instanceof EdgePiece))
                    continue;
            }

            for (int rot = 0; rot < 4; rot++) {
                Element rotated = piece.rotate(rot);
                if (!fitsPosition(rotated, row, col, w, h))
                    continue;
                if (!matchesNeighbours(rotated, row, col, puzzle))
                    continue;

                puzzle.setPlacement(new Placement(i, rot), row, col);
                puzzle.markUsed(i, true);

                if (partialFillBorder(puzzle, cellIndex + 1, borderCells, best))
                    return true;

                puzzle.setPlacement(null, row, col);
                puzzle.markUsed(i, false);
            }
        }
        return false;
    }

    private static void partialFillInterior(Puzzle puzzle, int row, int col,
                                            int borderSize, BestPartial best) {
        if (System.currentTimeMillis() > partialSolveDeadline)
            return;

        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        int interiorPlaced = (row - 1) * (w - 2) + (col - 1);
        best.update(puzzle, borderSize + interiorPlaced);

        if (row == h - 1)
            return;

        int nextCol = (col + 1 == w - 1) ? 1 : col + 1;
        int nextRow = (col + 1 == w - 1) ? row + 1 : row;

        for (int i = 0; i < puzzle.getPieces().size(); i++) {
            if (puzzle.isUsed(i))
                continue;
            Element piece = puzzle.getPieces().get(i);
            if (!(piece instanceof InsidePiece))
                continue;

            for (int rot = 0; rot < 4; rot++) {
                Element rotated = piece.rotate(rot);
                if (!fitsPosition(rotated, row, col, w, h))
                    continue;
                if (!matchesNeighbours(rotated, row, col, puzzle))
                    continue;

                puzzle.setPlacement(new Placement(i, rot), row, col);
                puzzle.markUsed(i, true);

                partialFillInterior(puzzle, nextRow, nextCol, borderSize, best);

                puzzle.setPlacement(null, row, col);
                puzzle.markUsed(i, false);
            }
        }
    }
}
