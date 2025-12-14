package pathfinder;

import maze.Maze;

public class PathfinderContext {
    private PathfinderStrategy strategy;

    public PathfinderContext(PathfinderStrategy strategy){
        this.strategy = strategy;
    }

    public void setStretegy(PathfinderStrategy strategy){
        this.strategy = strategy;
    }

    public int execute(Maze maze){
        return strategy.bestExit(maze);
    }
}
