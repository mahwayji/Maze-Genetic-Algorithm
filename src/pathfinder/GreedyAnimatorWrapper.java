package pathfinder;

import maze.Maze;
import maze.Cell;
import maze.CellType;
import java.awt.Point;
import java.util.*;

public class GreedyAnimatorWrapper implements AnimatablePathfinder {
    
    private final Maze maze;
    private final PriorityQueue<Node> pq;
    private final boolean[][] visited;
    private final Point goal;
    
    private boolean finished = false;
    private List<Point> finalPath = Collections.emptyList();
    private List<Point> explored = new ArrayList<>(); 
    private int totalCost = 0;

    private final int[] dr = { -1, 1, 0, 0 };
    private final int[] dc = { 0, 0, -1, 1 };

    private static class Node implements Comparable<Node> {
        int r, c;
        int pTimes;
        int h;
        Node from;

        public Node(int r, int c, int pTimes, int h, Node from) {
            this.r = r;
            this.c = c;
            this.pTimes = pTimes;
            this.h = h;
            this.from = from;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.h, other.h);
        }
    }

    public GreedyAnimatorWrapper(Maze maze) {
        this.maze = maze;
        Point start = maze.getStart();
        this.goal = maze.getGoal();

        this.pq = new PriorityQueue<>();
        this.visited = new boolean[maze.height()][maze.width()];

        int hStart = heuristic(start, goal);
        Node startNode = new Node(start.x, start.y, 0, hStart, null); 
        
        pq.add(startNode);
        visited[start.x][start.y] = true;
    }

    private int heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private boolean isValid(Maze maze, int r, int c) {
        if (r >= 0 && r < maze.height() && c >= 0 && c < maze.width() && maze.get(r, c).type != CellType.WALL) {
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return "Greedy Best-First (Original)";
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
        return finalPath;
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
        return new HashSet<>(explored);
    }

    @Override
    public void nextStep() {
        if (finished || pq.isEmpty()) {
            finished = true;
            return;
        }

        Node current = pq.poll();
        
        explored.add(new Point(current.r, current.c));
        if (current.r == goal.x && current.c == goal.y) {
            finished = true;
            totalCost = current.pTimes;
            reconstructPath(current); 
            return;
        }
        for (int i = 0; i < 4; i++) {
            int nr = current.r + dr[i];
            int nc = current.c + dc[i];

            if (isValid(maze, nr, nc) && !visited[nr][nc]) {
                visited[nr][nc] = true;
                
                Cell nextCell = maze.get(nr, nc);
                int newPtimes = current.pTimes + nextCell.value;
                
                int h = heuristic(new Point(nr, nc), goal);
                
                Node neighbor = new Node(nr, nc, newPtimes, h, current);
                pq.add(neighbor);
            }
        }
    }

    private void reconstructPath(Node endNode) {
        List<Point> path = new ArrayList<>();
        Node curr = endNode;
        while (curr != null) {
            path.add(new Point(curr.r, curr.c));
            curr = curr.from;
        }
        Collections.reverse(path);
        this.finalPath = path;
    }
}