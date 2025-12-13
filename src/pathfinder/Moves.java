package pathfinder;

public class Moves implements Comparable<Moves>{
    public int[] moves;
    public double fitness;
    public int totalCost;
    public boolean goalReached;
    public Moves(int[] moves, double fitness, int totalCost, boolean goalReached){
        this.moves = moves;
        this.fitness = fitness;
        this.totalCost = totalCost;
        this.goalReached = goalReached;
    }

    @Override
    public int compareTo(Moves o) {
        return Double.compare(o.fitness, this.fitness);
    }
}
