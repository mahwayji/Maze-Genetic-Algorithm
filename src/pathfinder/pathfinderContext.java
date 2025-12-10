package pathfinder;

import maze.Maze;

public class pathfinderContext {
    private pathfinderStrategy strategy;

    public pathfinderContext(pathfinderStrategy strategy){
        this.strategy = strategy;
    }

    public void setStretegy(pathfinderStrategy strategy){
        this.strategy = strategy;
    }

    public int execute(Maze maze){
        return strategy.bestExit(maze);
    }
}
