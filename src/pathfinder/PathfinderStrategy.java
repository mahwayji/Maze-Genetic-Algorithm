package pathfinder;

import maze.Maze;

public interface PathfinderStrategy {
    public int bestExit(Maze maze);
}
