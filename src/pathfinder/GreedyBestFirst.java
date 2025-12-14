package pathfinder;

import java.util.*;
import maze.*;
import java.awt.Point;

public class GreedyBestFirst implements pathfinderStrategy {
    @Override
    public int bestExit(Maze maze) {
        Point start = maze.getStart();
        Point goal = maze.getGoal();

        PriorityQueue<Node> pq = new PriorityQueue<>();
        boolean[][] visited = new boolean[maze.height()][maze.width()];
        int hStart = heuristic(start, goal);
        Node startNode = new Node(start.x, start.y, 0, hStart, null);
        pq.add(startNode);
        visited[start.x][start.y] = true;

        List<Point> explored = new ArrayList<>();
        while (!pq.isEmpty()) {
            Node current = pq.poll();
            explored.add(new Point(current.r, current.c));

            // find exits
            if (current.r == goal.getX() && current.c == goal.y) {
                return reconstuctPath(maze, current);
            }
            int[] dr = { -1, 1, 0, 0 };
            int[] dc = { 0, 0, -1, 1 };

            for (int i = 0; i < 4; i++) {
                int nr = current.r + dr[i];
                int nc = current.c + dc[i];

                if (isValid(maze, nr, nc) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    Cell nexCell = maze.get(nr, nc);
                    int newPtimes = current.pTimes + nexCell.value;
                    int h = heuristic(new Point(nr, nc), goal);
                    Node neighbor = new Node(nr, nc, newPtimes, h, current);
                    pq.add(neighbor);
                }
            }
        }
        return -1;
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

    private int reconstuctPath(Maze maze, Node endNode) {
        List<Point> path = new ArrayList<>();
        Node curr = endNode;
        while (curr != null) {
            path.add(new Point(curr.r, curr.c));
            curr = curr.from;
        }
        Collections.reverse(path);
        maze.showMaze(path);
        return endNode.pTimes;
    }
}
