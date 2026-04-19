package be.uliege.montefiore.oop;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Draws the puzzle solution (or best partial fill) in a Swing window.
 *
 * Each piece is drawn as a jigsaw outline — flat sides become straight edges,
 * bump (B) and pit (P) sides become outward or inward Bézier curves.
 * Unplaced pieces are shown in a section below the main grid.
 *
 * The cell size is computed at construction time so the grid always fits within
 * 85% of the screen. A JScrollPane is added as a fallback for very large puzzles.
 *
 * -- API references --
 *
 *   JPanel / JFrame / JScrollPane / SwingUtilities
 *     https://docs.oracle.com/javase/8/docs/api/javax/swing/JPanel.html
 *     https://docs.oracle.com/javase/8/docs/api/javax/swing/SwingUtilities.html
 *
 *   Toolkit.getDefaultToolkit().getScreenSize()
 *     Returns the screen resolution so we can scale the window to fit.
 *     https://docs.oracle.com/javase/8/docs/api/java/awt/Toolkit.html
 *
 *   Graphics2D
 *     https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html
 *
 *   Path2D / curveTo  (cubic Bézier curves for bumps and pits)
 *     https://docs.oracle.com/javase/8/docs/api/java/awt/geom/Path2D.html
 *
 *   BasicStroke  (line width, dash pattern)
 *     https://docs.oracle.com/javase/8/docs/api/java/awt/BasicStroke.html
 *
 *   RenderingHints  (antialiasing)
 *     https://docs.oracle.com/javase/8/docs/api/java/awt/RenderingHints.html
 *
 *   FontMetrics  (text measurement for centred labels)
 *     https://docs.oracle.com/javase/8/docs/api/java/awt/FontMetrics.html
 */
public class PuzzleDisplay extends JPanel {

    // 10-colour palette — pieces are coloured by (index % 10) so neighbouring
    // pieces are visually distinct.
    private static final Color[] PALETTE = {
        new Color(255, 213, 79),
        new Color(77, 182, 172),
        new Color(255, 138, 91),
        new Color(149, 117, 205),
        new Color(240, 128, 128),
        new Color(79, 195, 247),
        new Color(174, 213, 129),
        new Color(240, 157, 181),
        new Color(255, 241, 118),
        new Color(128, 203, 196),
    };

    private final Puzzle puzzle;

    // These are computed in the constructor based on screen size and puzzle dimensions,
    // so large puzzles automatically scale down to fit on screen.
    private final int CELL_SIZE;
    private final int PADDING;
    private final int SMALL_SIZE;
    private final int GAP;

    public PuzzleDisplay(Puzzle puzzle) {
        this.puzzle = puzzle;

        // We aim for 85% of the screen so the window decorations and taskbar still fit.
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int availW = (int)(screen.width * 0.85);
        int availH = (int)(screen.height * 0.85);

        // The cell size is limited by whichever dimension is tighter, capped at 100
        // (the default) and floored at 20 so labels are still legible on large grids.
        int cellByWidth = availW / puzzle.getWidth();
        int cellByHeight = availH / puzzle.getHeight();
        CELL_SIZE = Math.max(20, Math.min(100, Math.min(cellByWidth, cellByHeight)));

        // The other size constants scale proportionally with CELL_SIZE.
        PADDING = Math.max(10, CELL_SIZE / 2);
        SMALL_SIZE = Math.max(15, CELL_SIZE * 65 / 100);
        GAP = Math.max(8, CELL_SIZE * 40 / 100);

        setBackground(new Color(240, 240, 240));
    }

    // Tells Swing how large this panel wants to be: grid area + optional unplaced section.
    @Override
    public Dimension getPreferredSize() {
        int winW = puzzle.getWidth() * CELL_SIZE + 2 * PADDING;
        int winH = puzzle.getHeight() * CELL_SIZE + 2 * PADDING + unplacedAreaHeight();
        return new Dimension(winW, winH);
    }

    // Main drawing method. Swing calls this whenever the window needs to be redrawn.
    // We draw in four passes so that elements layer correctly on top of each other.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Turn on antialiasing and high-quality rendering so curved edges look smooth.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        // Pass 1a: draw grey rectangles for empty cells first, so piece fills
        // (which can protrude into adjacent cells) always appear on top of the grey.
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                if (puzzle.getPlacement(row, col) == null) {
                    g2.setColor(new Color(210, 210, 210));
                    g2.fillRect((int)pixelX(col), (int)pixelY(row), CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // Pass 1b: fill placed pieces. We draw a slightly offset darker shadow first,
        // then the actual piece on top, to give a subtle depth effect.
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                Placement p = puzzle.getPlacement(row, col);
                if (p == null)
                    continue;

                Element e = getEffective(p);
                Path2D path = buildPath(e, pixelX(col), pixelY(row), CELL_SIZE);
                Color base = PALETTE[p.getIndex() % PALETTE.length];

                g2.setColor(base.darker());
                g2.translate(3, 3);
                g2.fill(path);
                g2.translate(-3, -3);

                g2.setColor(base);
                g2.fill(path);
            }
        }

        // Pass 2: draw outlines. Empty cells get a dashed grey border;
        // placed pieces get a solid dark border.
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                Placement p = puzzle.getPlacement(row, col);

                if (p == null) {
                    g2.setColor(new Color(170, 170, 170));
                    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            1f, new float[]{6, 4}, 0));
                    g2.drawRect((int)pixelX(col), (int)pixelY(row), CELL_SIZE, CELL_SIZE);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    continue;
                }

                g2.setColor(new Color(40, 40, 40));
                Path2D path = buildPath(getEffective(p), pixelX(col), pixelY(row), CELL_SIZE);
                g2.draw(path);
            }
        }

        // Pass 3: draw piece-index labels. We render a white "halo" behind each
        // label by drawing it in white with a 1-pixel offset in all eight directions,
        // then draw the black label on top. This keeps the text readable on any colour.
        int fontSize = Math.max(9, CELL_SIZE / 7);
        g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                Placement p = puzzle.getPlacement(row, col);
                if (p == null)
                    continue;

                String label = String.valueOf(p.getIndex() + 1);
                int tx = (int)(pixelX(col) + CELL_SIZE / 2f - fm.stringWidth(label) / 2f);
                int ty = (int)(pixelY(row) + CELL_SIZE / 2f + fm.getAscent() / 2f - 3);

                g2.setColor(Color.WHITE);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        if (dx != 0 || dy != 0)
                            g2.drawString(label, tx + dx, ty + dy);

                g2.setColor(Color.BLACK);
                g2.drawString(label, tx, ty);
            }
        }

        // Pass 4: draw unplaced pieces below the grid (only relevant for unsolvable puzzles).
        drawUnplacedPieces(g2);
    }

    // Renders unplaced pieces in a tiled section below the main grid.
    // Each piece is shown in its original orientation (rotation 0) since it was never placed.
    private void drawUnplacedPieces(Graphics2D g2) {
        List<Integer> unplaced = getUnplacedIndices();
        if (unplaced.isEmpty())
            return;

        int mainW = puzzle.getWidth() * CELL_SIZE + 2 * PADDING;
        int startY = puzzle.getHeight() * CELL_SIZE + 2 * PADDING;

        // Draw a dashed separator between the grid and the unplaced section.
        g2.setColor(new Color(160, 160, 160));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                1f, new float[]{6, 4}, 0));
        g2.drawLine(PADDING, startY, mainW - PADDING, startY);

        startY += PADDING / 2 + 4;
        g2.setFont(new Font("SansSerif", Font.ITALIC, Math.max(10, CELL_SIZE / 8)));
        g2.setColor(new Color(100, 100, 100));
        g2.drawString("Unplaced pieces (" + unplaced.size() + "):", PADDING, startY + 12);
        startY += 30;

        // Tile the pieces across the available width.
        int availW = mainW - 2 * PADDING;
        int cols = Math.max(1, availW / (SMALL_SIZE + GAP));

        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int k = 0; k < unplaced.size(); k++) {
            int idx = unplaced.get(k);
            float px = PADDING + (k % cols) * (SMALL_SIZE + GAP);
            float py = startY + (k / cols) * (SMALL_SIZE + GAP);

            Element e = puzzle.getPieces().get(idx);
            Path2D path = buildPath(e, px, py, SMALL_SIZE);
            Color base = PALETTE[idx % PALETTE.length];

            g2.setColor(base.darker());
            g2.translate(2, 2);
            g2.fill(path);
            g2.translate(-2, -2);

            g2.setColor(base);
            g2.fill(path);

            g2.setColor(new Color(40, 40, 40));
            g2.draw(path);

            // Centre the piece-index label inside the small piece.
            String label = String.valueOf(idx + 1);
            int labelSize = Math.max(8, SMALL_SIZE / 7);
            g2.setFont(new Font("SansSerif", Font.BOLD, labelSize));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (int)(px + SMALL_SIZE / 2f - fm.stringWidth(label) / 2f);
            int ty = (int)(py + SMALL_SIZE / 2f + fm.getAscent() / 2f - 2);

            g2.setColor(Color.WHITE);
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    if (dx != 0 || dy != 0)
                        g2.drawString(label, tx + dx, ty + dy);

            g2.setColor(Color.BLACK);
            g2.drawString(label, tx, ty);
        }
    }

    // Builds the closed jigsaw outline for one piece, traced clockwise from the top-left corner.
    // We call drawSide() once per side, appending each segment to the same path.
    private Path2D buildPath(Element e, float x, float y, int size) {
        float s = size;
        Path2D.Float path = new Path2D.Float();
        path.moveTo(x, y);

        // normalX/Y is the outward unit normal for each side — used by drawSide to
        // know which direction a bump protrudes or a pit recedes.
        drawSide(path, e.getTop(), x, y, x + s, y, 0, -1, size);
        drawSide(path, e.getRight(), x + s, y, x + s, y + s, 1, 0, size);
        drawSide(path, e.getBottom(), x + s, y + s, x, y + s, 0, 1, size);
        drawSide(path, e.getLeft(), x, y + s, x, y, -1, 0, size);

        path.closePath();
        return path;
    }

    // Appends one side to the path.
    // For a flat side (F) this is just a straight line.
    // For a bump (B) or pit (P) this is two back-to-back cubic Bézier curves that
    // form the rounded tab shape. The dir variable flips the normal outward (bump)
    // or inward (pit). The tab is centred on the midpoint of the side.
    private void drawSide(Path2D.Float path, char type,
            float fromX, float fromY,
            float toX, float toY,
            float normalX, float normalY,
            int size) {

        if (type == 'F') {
            path.lineTo(toX, toY);
            return;
        }

        float dir = (type == 'B') ? 1f : -1f;
        float t = size * 0.28f; // bump/pit height scales with cell size
        float neckW = t * 0.40f;
        float headW = t * 0.52f;

        // nx/ny is the outward displacement vector scaled by the tab height.
        float nx = normalX * dir * t;
        float ny = normalY * dir * t;

        float midX = (fromX + toX) / 2f;
        float midY = (fromY + toY) / 2f;

        // tux/tuy is the unit vector along the side (tangent direction).
        float dx = toX - fromX;
        float dy = toY - fromY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float tux = dx / len;
        float tuy = dy / len;

        // The tab narrows at the base (neck) before widening into the rounded head.
        float nsX = midX - tux * neckW; // neck entry point
        float nsY = midY - tuy * neckW;
        float neX = midX + tux * neckW; // neck exit point
        float neY = midY + tuy * neckW;

        path.lineTo(nsX, nsY);

        // First curve: from neck entry to the tip of the tab.
        path.curveTo(
            nsX + normalX * dir * t * 0.72f, nsY + normalY * dir * t * 0.72f,
            midX + nx - tux * headW, midY + ny - tuy * headW,
            midX + nx, midY + ny
        );

        // Second curve: mirror image from the tip back to the neck exit.
        path.curveTo(
            midX + nx + tux * headW, midY + ny + tuy * headW,
            neX + normalX * dir * t * 0.72f, neY + normalY * dir * t * 0.72f,
            neX, neY
        );

        path.lineTo(toX, toY);
    }

    // Returns the piece at this placement with its rotation applied.
    private Element getEffective(Placement p) {
        return puzzle.getPieces().get(p.getIndex()).rotate(p.getRotation());
    }

    // Collects the indices of all pieces that were not placed in the grid.
    private List<Integer> getUnplacedIndices() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < puzzle.getPieces().size(); i++)
            if (!puzzle.isUsed(i))
                result.add(i);
        return result;
    }

    // Computes the pixel height of the unplaced-pieces section.
    // Returns 0 if all pieces were placed (normal solved puzzle).
    private int unplacedAreaHeight() {
        List<Integer> unplaced = getUnplacedIndices();
        if (unplaced.isEmpty())
            return 0;
        int mainW = puzzle.getWidth() * CELL_SIZE + 2 * PADDING;
        int availW = mainW - 2 * PADDING;
        int cols = Math.max(1, availW / (SMALL_SIZE + GAP));
        int rows = (unplaced.size() + cols - 1) / cols;
        return PADDING / 2 + 4 + 30 + rows * (SMALL_SIZE + GAP) + PADDING / 2;
    }

    private float pixelX(int col) { return PADDING + col * CELL_SIZE; }
    private float pixelY(int row) { return PADDING + row * CELL_SIZE; }

    // Opens the display window on the Swing Event Dispatch Thread.
    // Swing requires all UI work to happen on the EDT — invokeLater() schedules it.
    public static void show(final Puzzle puzzle) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Check if any piece is unplaced so we can mark the title "unsolvable".
                boolean partial = false;
                for (int i = 0; i < puzzle.getPieces().size(); i++)
                    if (!puzzle.isUsed(i)) {
                        partial = true;
                        break;
                    }

                String title = "Puzzle  " + puzzle.getWidth() + " \u00d7 " + puzzle.getHeight()
                    + "  (" + puzzle.getPieces().size() + " pieces)"
                    + (partial ? "  \u2014  unsolvable" : "");

                // Wrap in a scroll pane so very large puzzles remain navigable
                // even after the auto-scaling has already shrunk the cells.
                PuzzleDisplay display = new PuzzleDisplay(puzzle);
                JScrollPane scroll = new JScrollPane(display);
                scroll.setBorder(null);

                // The window is sized to the panel's preferred size, but capped at
                // 90% of the screen so it always fits with room for the OS chrome.
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                Dimension preferred = display.getPreferredSize();
                int frameW = Math.min(preferred.width + 20, (int)(screen.width * 0.90));
                int frameH = Math.min(preferred.height + 20, (int)(screen.height * 0.90));
                scroll.setPreferredSize(new Dimension(frameW, frameH));

                JFrame frame = new JFrame(title);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.getContentPane().add(scroll);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setResizable(true);
                frame.setVisible(true);
            }
        });
    }
}
