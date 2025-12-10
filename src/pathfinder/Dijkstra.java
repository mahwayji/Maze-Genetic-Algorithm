package pathfinder;

import maze.Maze;
import maze.CellType;
import java.awt.Point;
import java.util.*;

public class Dijkstra implements pathfinderStrategy {
    @Override
    public int bestExit(Maze maze) {
        maze.showMaze(new ArrayList<>());
        System.out.println();
        int rows = maze.height();
        int cols = maze.width();
        int[][] path = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                path[i][j] = Integer.MAX_VALUE;
            }
        }
        PriorityQueue<Node> pq = new PriorityQueue<>();
        Point start = maze.getStart();
        path[start.x][start.y] = 0;
        pq.add(new Node(start.x, start.y, 0, null));
        // {up,down,left,right}
        int[] dRow = { -1, 1, 0, 0 };
        int[] dCol = { 0, 0, -1, 1 };
        int[] bits = { 8, 4, 2, 1 };

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (maze.get(current.r, current.c).type == CellType.GOAL) {
                List<Point> pathList = new ArrayList<>();
                ArrayList<Integer> AddtimesList = new ArrayList<>();
                ArrayList<Integer> timesPathList = new ArrayList<>();
                Node trace = current;
                while (trace != null) {
                    pathList.add(new Point(trace.r, trace.c));
                    int getTimes = maze.get(trace.r,trace.c).value;
                    timesPathList.add(getTimes);
                    AddtimesList.add(trace.times);
                    trace = trace.from;
                }

                maze.showMaze(pathList);
                // timePathList start ไป end นะจ้ะ
                Collections.reverse(timesPathList);
                System.out.println(timesPathList);
                // adding timeList start ไป end นะจ้ะ
                Collections.reverse(AddtimesList);
                System.out.println(AddtimesList);
                return current.times;
            }
            if (current.times > path[current.r][current.c]) {
                continue;
            }
            int available = maze.availableDirection(current.r, current.c);

            for (int i = 0; i < 4; i++) {
                if ((available & bits[i]) != 0) {
                    int newR = current.r + dRow[i];
                    int newC = current.c + dCol[i];
                    if (newR < 0 || newR >= rows || newC < 0 || newC >= cols) {
                        continue;
                    }
                    int newCost = current.times + maze.get(newR, newC).value;

                    if (newCost < path[newR][newC]) {
                        path[newR][newC] = newCost;
                        pq.add(new Node(newR, newC, newCost, current));
                    }
                }
            }
        }
        return -1;

    }
}
