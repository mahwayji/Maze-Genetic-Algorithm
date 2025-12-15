package pathfinder;

import maze.Maze;
import maze.CellType;
import java.awt.Point;
import java.util.*;

public class AStarAnimatorWrapper implements AnimatablePathfinder {
    private final Maze maze;
    private final PriorityQueue<Node> pq;
    private final int[][] path;
    private final Set<Point> closedSet;
    private final int rows, cols;
    private boolean finished = false;
    private List<Point> finalPath = Collections.emptyList();
    private int totalCost = -1;
    private final Point goal;

    // {up,down,left,right}
    private final int[] dRow = { -1, 1, 0, 0 };
    private final int[] dCol = { 0, 0, -1, 1 };
    private final int[] bits = { 8, 4, 2, 1 };

    public AStarAnimatorWrapper(Maze maze) {
        this.maze = maze;
        this.rows = maze.height();
        this.cols = maze.width();
        this.path = new int[rows][cols];
        this.closedSet = new HashSet<>();
        this.goal = maze.getGoal();

        for (int i = 0; i < rows; i++) {
            Arrays.fill(path[i], Integer.MAX_VALUE);
        }

        this.pq = new PriorityQueue<>();
        Point start = maze.getStart();
        path[start.x][start.y] = 0;
     
        int h = Math.abs(start.x - goal.x) + Math.abs(start.y - goal.y);
        pq.add(new Node(start.x, start.y, 0, h, null)); 
    }

    private int heuristic(int r, int c) {
        return Math.abs(r - goal.x) + Math.abs(c - goal.y);
    }

    @Override
    public String getName() {
        return "A* Algorithm";
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public int getTotalCost() {
        return totalCost;
    }

    @Override
    public List<Point> getPath() {
        return finished ? finalPath : Collections.emptyList();
    }

    @Override
    public Set<Point> getOpenSet() {
        Set<Point> openSet = new HashSet<>();
        for (Node node : pq) {
            openSet.add(new Point(node.r, node.c));
        }
        return openSet;
    }

    @Override
    public Set<Point> getClosedSet() {
        return closedSet;
    }

    @Override
    public void nextStep() {
        if (finished || pq.isEmpty()) {
            finished = true;
            return;
        }

        Node current = pq.poll();

        if (maze.get(current.r, current.c).type == CellType.GOAL) {
            finished = true;
            totalCost = current.pTimes;
            finalPath = new ArrayList<>();
            Node trace = current;
            while (trace != null) {
                finalPath.add(new Point(trace.r, trace.c));
                trace = trace.from;
            }
            Collections.reverse(finalPath);
            return;
        }

        if (current.pTimes > path[current.r][current.c]) {
            return;
        }

        closedSet.add(new Point(current.r, current.c));

        int available = maze.availableDirection(current.r, current.c);

        for (int i = 0; i < 4; i++) {
            if ((available & bits[i]) != 0) {
                int newR = current.r + dRow[i];
                int newC = current.c + dCol[i];

                if (newR < 0 || newR >= rows || newC < 0 || newC >= cols) {
                    continue;
                }

                int newpTimes = current.pTimes + maze.get(newR, newC).value;

                if (newpTimes < path[newR][newC]) {
                    path[newR][newC] = newpTimes;
                    int h = heuristic(newR, newC);
                    int f = newpTimes + h;
                    pq.add(new Node(newR, newC, newpTimes, f, current));
                }
            }
        }
    }
}