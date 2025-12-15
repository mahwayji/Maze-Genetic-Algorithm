package display;

import pathfinder.AnimatablePathfinder;
import maze.Maze;
import maze.CellType;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.List;

public class MazePanel extends JPanel {
    private final Maze maze;
    private AnimatablePathfinder solver;

    public MazePanel(Maze maze, AnimatablePathfinder solver) {
        this.maze = maze;
        this.solver = solver;
        this.setPreferredSize(new Dimension(800, 600)); 
        this.setBackground(Color.DARK_GRAY); 
    }

    public void setSolver(AnimatablePathfinder newSolver) {
        this.solver = newSolver;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int rows = maze.height();
        int cols = maze.width();

        int cellW = panelWidth / cols;
        int cellH = panelHeight / rows;
        int cellSize = Math.min(cellW, cellH); 
        
        cellSize = Math.max(2, cellSize);

        int totalMazeWidth = cols * cellSize;
        int totalMazeHeight = rows * cellSize;
        int startX = (panelWidth - totalMazeWidth) / 2;
        int startY = (panelHeight - totalMazeHeight) / 2;

        Set<Point> openSet = solver.getOpenSet();
        Set<Point> closedSet = solver.getClosedSet();
        List<Point> finalPath = solver.getPath();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = startX + (c * cellSize);
                int y = startY + (r * cellSize);
                Point p = new Point(r, c);
                
                CellType type = maze.get(r, c).type;
                int value = maze.get(r, c).value;

                if (type == CellType.WALL) {
                    g2d.setColor(Color.BLACK);
                } else if (p.equals(maze.getStart())) {
                    g2d.setColor(Color.GREEN);
                } else if (p.equals(maze.getGoal())) {
                    g2d.setColor(Color.RED);
                } else if (finalPath.contains(p)) {
                    g2d.setColor(Color.YELLOW); 
                } else if (closedSet.contains(p)) {
                    g2d.setColor(new Color(100, 100, 100)); 
                } else if (openSet.contains(p)) {
                    g2d.setColor(Color.CYAN); 
                } else {
                    g2d.setColor(Color.WHITE); 
                }
                g2d.fillRect(x, y, cellSize, cellSize);
                
                if (cellSize > 5) {
                    g2d.setColor(new Color(200, 200, 200));
                    g2d.drawRect(x, y, cellSize, cellSize);
                }

                if (cellSize > 15) {
                    if (type == CellType.NUMBER || type == CellType.START || type == CellType.GOAL) {
                        g2d.setColor(Color.BLACK);
                        int fontSize = Math.max(8, cellSize / 2); 
                        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
                        
                        String text = (type == CellType.START) ? "S" : 
                                      (type == CellType.GOAL) ? "G" : String.valueOf(value);
                        
                        FontMetrics fm = g2d.getFontMetrics();
                        int textX = x + (cellSize - fm.stringWidth(text)) / 2;
                        int textY = y + (cellSize - fm.getHeight()) / 2 + fm.getAscent();
                        g2d.drawString(text, textX, textY);
                    }
                }
            }
        }
    }
}