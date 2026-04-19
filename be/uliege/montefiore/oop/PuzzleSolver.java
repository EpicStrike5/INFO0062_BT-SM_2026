package be.uliege.montefiore.oop;

import java.util.ArrayList;
import java.util.List;

/*
 * Solves a jigsaw puzzle using a two-phase backtracking algorithm.
 *
 * Phase 1 — Border: fills corner and edge cells clockwise starting from (0,0).
 * Phase 2 — Interior: fills inside cells left-to-right, top-to-bottom.
 *
 * Before any backtracking, a quick feasibility check is run to immediately reject
 * puzzles that can never be solved (wrong flat count, unbalanced bumps/pits, wrong
 * piece-type counts), instead of wasting time searching.
 */
public class PuzzleSolver {

    // Utility class: no instances should be created.
    private PuzzleSolver() {}

    // Timeout for the partial-solve interior search (display mode only).
    // If backtracking hasn't finished within this limit, we stop and show
    // whatever best state was found so far.
    // Helps in reducing long computation at the price of accuracy.
    private static final long PARTIAL_SOLVE_TIMEOUT_MS = 3000L;

    // Deadline set once at the start of each partialSolve() call.
    private static long partialSolveDeadline;

    // Tries to solve the puzzle. Returns true and stores the solution inside the
    // puzzle object if successful, false otherwise.
    public static boolean solve(Puzzle puzzle) {
        if (!checkFeasibility(puzzle))
            return false;

        List<int[]> borderCells = buildBorderCells(puzzle.getWidth(), puzzle.getHeight());
        if (!fillBorder(puzzle, 0, borderCells))
            return false;

        // Only puzzles wider and taller than 2 have interior cells to fill.
        if (puzzle.getWidth() > 2 && puzzle.getHeight() > 2) {
            if (!fillInterior(puzzle, 1, 1))
                return false;
        }

        return true;
    }

    // Quickly checks whether a puzzle can possibly be solved, without backtracking.
    // This saves a lot of time — if the piece set is fundamentally broken, there is
    // no point running the full search.
    private static boolean checkFeasibility(Puzzle puzzle) {

        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        // We look at every side of every piece to build up three totals:
        // flat sides, bumps, and pits.
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

        // A valid puzzle's border has exactly 2*(width + height) outer-facing edges,
        // so we need the same number of flat sides across all pieces.
        int neededFlats = 2 * (w + h);
        if (totalF != neededFlats) {
            PuzzleError.wrongFlatCount(neededFlats, totalF);
            return false;
        }

        // Every internal edge pairs one bump with one pit, so the two counts must match.
        if (totalB != totalP) {
            PuzzleError.bumpsAndPitsDontMatch(totalB, totalP);
            return false;
        }

        // For a normal (non-thin) grid, the number of each piece type is fixed by
        // the dimensions: exactly 4 corners, 2*(w-2)+2*(h-2) edges, (w-2)*(h-2) insides.
        // Thin puzzles (w=1 or h=1) are skipped because the classification breaks down.
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

    // Returns all border cells in clockwise visit order (no duplicates).
    // Clockwise means: top row left→right, right column top→bottom,
    // bottom row right→left, left column bottom→top.
    private static List<int[]> buildBorderCells(int w, int h) {
        List<int[]> cells = new ArrayList<>();

        // A thin puzzle (one row or one column) has no distinct sides,
        // so we just list cells linearly to avoid duplicates.
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

        // General case: clockwise from top-left corner.
        for (int c = 0; c < w; c++) // top row
            cells.add(new int[]{0, c});
        for (int r = 1; r < h; r++) // right column
            cells.add(new int[]{r, w - 1});
        for (int c = w - 2; c >= 0; c--) // bottom row
            cells.add(new int[]{h - 1, c});
        for (int r = h - 2; r >= 1; r--) // left column
            cells.add(new int[]{r, 0});

        return cells;
    }

    // Fills the border cells using backtracking. Places a CornerPiece at corner
    // positions and an EdgePiece everywhere else. For thin puzzles the type
    // filter is skipped and fitsPosition takes care of orientation constraints.
    private static boolean fillBorder(Puzzle puzzle, int cellIndex, List<int[]> borderCells) {

        // Base case: all border cells are filled, the border is complete.
        if (cellIndex == borderCells.size())
            return true;

        int row = borderCells.get(cellIndex)[0];
        int col = borderCells.get(cellIndex)[1];
        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        boolean isThinPuzzle = (w == 1 || h == 1);
        boolean isCorner = (row == 0 || row == h - 1) && (col == 0 || col == w - 1);

        // Classic backtracking loop: try every unused piece in every rotation.
        // If the piece fits the position and matches its already-placed neighbours,
        // place it and recurse. If the recursion fails, undo and try the next option.
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

                // This rotation/piece didn't lead to a solution — undo and keep trying.
                puzzle.setPlacement(null, row, col);
                puzzle.markUsed(i, false);
            }
        }

        return false;
    }

    // Fills the interior cells (all non-border cells) using backtracking.
    // Interior cells are visited left-to-right, top-to-bottom.
    private static boolean fillInterior(Puzzle puzzle, int row, int col) {

        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        // Base case: we've stepped past the last interior row — done.
        if (row == h - 1)
            return true;

        // Advance to the next interior cell. When we reach the right border,
        // we wrap to the first interior column of the next row.
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

                // Backtrack.
                puzzle.setPlacement(null, row, col);
                puzzle.markUsed(i, false);
            }
        }

        return false;
    }

    // Checks that the piece's sides are legal for the given grid position.
    // The first block checks outer-facing sides — they must be flat.
    // The second block checks inner-facing sides — they must not be flat
    // because they will connect to a neighbour.
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

    // Checks that the piece is compatible with every already-placed neighbour.
    // We only look at cells that have a piece — unplaced neighbours don't constrain
    // the current cell yet.
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

    // Returns the piece at (row, col) with its stored rotation already applied.
    private static Element effectivePiece(Puzzle puzzle, int row, int col) {
        Placement p = puzzle.getPlacement(row, col);
        return puzzle.getPieces().get(p.getIndex()).rotate(p.getRotation());
    }

    // Fills the grid with the best partial solution found by backtracking.
    // Called for display purposes when the puzzle has no complete solution.
    // Every placed piece is guaranteed to be compatible with its neighbours
    // because we restore the deepest valid backtracking state.
    // The search stops after PARTIAL_SOLVE_TIMEOUT_MS milliseconds.
    public static void partialSolve(Puzzle puzzle) {
        puzzle.initialize();

        partialSolveDeadline = System.currentTimeMillis() + PARTIAL_SOLVE_TIMEOUT_MS;

        BestPartial best = new BestPartial(puzzle);
        List<int[]> borderCells = buildBorderCells(puzzle.getWidth(), puzzle.getHeight());

        partialFillBorder(puzzle, 0, borderCells, best);

        // Restore whatever the deepest valid state was.
        best.restore(puzzle);
    }

    // Tracks the deepest state reached during backtracking.
    // Every time we beat our depth record, we copy the entire grid and used-flags
    // into savedGrid/savedUsed. When backtracking finishes, restore() puts that
    // snapshot back into the puzzle so the display shows the most progress made.
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
            // Copy the grid and used-flags into our snapshot arrays.
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
            // Wipe the puzzle clean first, then write the saved snapshot back in.
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

    // Border fill variant used by partialSolve. Works like fillBorder, but also
    // tracks the deepest state in 'best' and, when the full border is placed,
    // immediately dives into the interior so we can snapshot mid-interior states too.
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

    // Interior fill variant used by partialSolve. Unlike the regular fillInterior
    // (which stops at the first solution), this method is void because we want to
    // keep exploring and record the deepest state, not just the first valid one.
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
