package pathfinder;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import maze.Cell;
import maze.CellType;
import maze.Maze;

public class GeneticAlgorithm implements pathfinderStrategy {
    public int bestExit(Maze maze) {
        // constant
        int CHROM_LENGTH = (int) ((maze.width() + maze.height()) * 3 );
        int MAX_POPULATION = 25000;
        double CROSSOVER_RATE = 0.95;
        double MUTATION_RATE = 0.1;
        
        
        List<int[]> population = generatePopulation(MAX_POPULATION, CHROM_LENGTH);
        Moves bestMoves = geneticAlgorithm(maze, population, CHROM_LENGTH, MAX_POPULATION, CROSSOVER_RATE,
                MUTATION_RATE);

        solve(maze, bestMoves);
        return bestMoves.totalCost;
    }

    private Point getNextPosition(Point current, int move) {
        int newR = current.x;
        int newC = current.y;
        switch (move) {
            case 0: newR--; break; // up
            case 1: newR++; break; // down
            case 2: newC--; break; // left
            case 3: newC++; break; // right
        }
        return new Point(newR, newC);
    }

    private List<Integer> getValidMoves(Maze maze, int r, int c, int prevMove) {
        List<Integer> moves = new ArrayList<>();
        int available = maze.availableDirection(r, c);
        
        // 0000 -> Up, Down, Left, Right (8, 4, 2, 1)
        if ((available & 8) != 0 && prevMove != 1) moves.add(0); // Up
        if ((available & 4) != 0 && prevMove != 0) moves.add(1); // Down
        if ((available & 2) != 0 && prevMove != 3) moves.add(2); // Left
        if ((available & 1) != 0 && prevMove != 2) moves.add(3); // Right
        

        return moves;
    }

    private void solve(Maze maze, Moves best){
        Point pos = maze.getStart();
        List<Point> visited = new ArrayList<>();

        if (best.goalReached)
            System.out.println("Goal Reached");

        for(int move : best.moves){
            Point nextPos = getNextPosition(pos, move);
            visited.add(pos);

            if(nextPos.x < 0 || nextPos.x >= maze.height() || nextPos.y < 0 || nextPos.y >= maze.width() ){
                break;
            }
            // get next point
            pos = nextPos;
            Cell nextCell = maze.get(pos.x, pos.y);

            if (nextCell.type == CellType.GOAL)
                break;
        }
        int count = 0;
        for (Point loc : visited) {
            System.out.printf("%d. (%d, %d)\n", count++, loc.x, loc.y);
        }
        maze.showMaze(visited);
    }

    private Moves geneticAlgorithm(
            Maze maze,
            List<int[]> population,
            int CHROM_LENGTH,
            int MAX_POPULATION,
            double CROSSOVER_RATE,
            double MUTATION_RATE) {
        Moves bestMoves = new Moves(null, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
        int stagnantCount = 0;

        int generation = 0;
        double currentMutationRate = MUTATION_RATE;
        ArrayList<Moves> fitness_scores; 

        do{
            //hyper-mutation
            if(stagnantCount > 50)
                currentMutationRate = 0.25;
            else 
                currentMutationRate = MUTATION_RATE;
            fitness_scores = new ArrayList<>();
            for (int[] chromosome : population) {
                fitness_scores.add(fitnessCalculate(maze, chromosome));

            }

            Collections.sort(fitness_scores);
            // stop when no improvement found
            if (bestMoves.fitness + 1e-9 > fitness_scores.get(0).fitness)
                stagnantCount++;

            else if (bestMoves.fitness < fitness_scores.get(0).fitness) {
                bestMoves = fitness_scores.get(0);
                stagnantCount = 0;
            }

            System.out.println("Generation " + generation + " Best Fitness: " + bestMoves.fitness);
            generation++;

            List<Moves> selectedMoves = selection(fitness_scores, (int)(MAX_POPULATION/2));

            List<int[]> newGen = generateNextPopulation(maze, fitness_scores, selectedMoves, MAX_POPULATION, CROSSOVER_RATE, currentMutationRate);
            population.clear();
            population.addAll(newGen);
    
            
        } while (stagnantCount < 100);

        return bestMoves;
    }

    // Generate population
    private int[] generateChromosomes(int length) {
        int[] genes = new int[length];
        Random rnd = new Random();
        int prev = -1;
        int newMove;
        for (int i = 0; i < length; i++) {
            do {
                newMove = rnd.nextInt(4);
            } while ((prev == 0 && newMove == 1) ||
                    (prev == 1 && newMove == 0) ||
                    (prev == 2 && newMove == 3) ||
                    (prev == 3 && newMove == 2));

            prev = newMove;
            genes[i] = newMove;
        }
        return genes;
    }

    public List<int[]> generatePopulation(int populationSize, int chromLength) {
        List<int[]> population = new ArrayList<>();

        for (int i = 0; i < populationSize; i++) {
            population.add(generateChromosomes(chromLength));
        }

        return population;
    }

    // Fitness function
    private Moves fitnessCalculate(Maze maze, int[] chromosome) {
        Point pos = maze.getStart();

        int totalCost = 0;
        int stepsTaken = 0;
        int reverseCount = 0;

        int rows = maze.height();
        int cols = maze.width();

        int prev = -1; //previous move
        double fitness = 0;

        HashMap<Point, Integer> uniqueVisited = new HashMap<>();
        Deque<Point> recent = new ArrayDeque<>();

        boolean hitWall = false; 
        boolean outOfBound = false;  

        
        for(int move: chromosome){
            // loop move
            if((move == 0 && prev == 1) ||
                (move == 1 && prev == 0) ||
                (move == 2 && prev == 3) ||
                (move == 3 && prev == 2))
                {
                    reverseCount++;
                    fitness -= 200 * reverseCount;
                } else {
                    reverseCount = 0;
                }

            prev = move;
            pos = getNextPosition(pos, move);
            
            // if(move == 1 || move == 3) fitness += 50;
            // else fitness -= 20;
            // out of bounds
            if(pos.x < 0 || pos.x >= rows || pos.y < 0 || pos.y >= cols ){
                // Penalty based on how much of the chromosome was left unused
                fitness -= (chromosome.length - stepsTaken) * 100;
                //fitness -= (dist) * 1000;
                outOfBound = true;
                break;
            }

            // get next cell
            Cell next = maze.get(pos.x, pos.y);

            // hit wall
            if(next.type == CellType.WALL){
                fitness -= (chromosome.length - stepsTaken) * 100;
                //fitness -= (dist) * 1000;
                hitWall = true;
                break;
            }

            // Structural rewards

            // exploration reward
            if (uniqueVisited.get(pos) == null) {
                // fitness += 100; // Reward finding new floor tiles
                uniqueVisited.put(new Point(pos), 1);
            } else {
                int visited = uniqueVisited.get(pos);
                fitness -= (int)(50 * Math.pow(visited, 2)); // penalty for doing loop or going backward
                uniqueVisited.put(pos,visited++);
            }

            // Junction reward (branch discovery)
            int available = maze.availableDirection(pos.x, pos.y);
            int branches = Integer.bitCount(available);
                    if (branches >= 3) {
            fitness += branches * 200;
            }

            // Dead-end penalty
            if (branches == 1) {
                fitness -= 400;
            }

            // Loop detection (soft)
            if (recent.contains(pos)) {
                fitness -= 800;
            }

            recent.add(pos);
            if (recent.size() > 10) 
                recent.removeFirst();

            // cell type reward
            if (next.type == CellType.NUMBER)
                totalCost += next.value;

            if (next.type == CellType.GOAL)
                break;

            stepsTaken++;

        }

        
        boolean goalReached = maze.getGoal().equals(pos);

        // Global path quality

        if (goalReached) {
            fitness += 100000;
            fitness -= totalCost * 300;
            fitness -= stepsTaken * 100;

        } else {
            //fitness -= totalCost * 50;
        }

        // exploration reward
        fitness += Math.min(stepsTaken, uniqueVisited.size()) * 50; 
        
        // penalize costly paths
        fitness -= stepsTaken * 30;

        // fitness += (maxDist - dist) * 500;// Reward getting closer
        // if (dist <= 4) fitness += 1000*((4-dist));

        if (hitWall || outOfBound) {
            fitness -= 5000;
        }

        return new Moves(chromosome, fitness, totalCost, goalReached);
    }

    // Selection function
    // private List<Moves> selection(ArrayList<Moves> fitness_scores, int count) {
    // List<Moves> selected = new ArrayList<>();
    // Random rnd = new Random();
    // for (int i = 0; i < count; i++) {
    // // Pick two random competitors
    // Moves m1 = fitness_scores.get(rnd.nextInt(fitness_scores.size()));
    // Moves m2 = fitness_scores.get(rnd.nextInt(fitness_scores.size()));
    // // The better one wins
    // selected.add(m1.fitness > m2.fitness ? m1 : m2);
    // }

    // return selected;
    // }

    // Selection Ver 2
    private List<Moves> selection(ArrayList<Moves> fitness_scores, int count) {
        int competitionSize = 2;
        List<Moves> selected = new ArrayList<>();
        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            Moves bestM = fitness_scores.get(rnd.nextInt(fitness_scores.size()));

            for (int j = 1; j < competitionSize; j++) {
                Moves competM = fitness_scores.get(rnd.nextInt(fitness_scores.size()));
                if (competM.fitness > bestM.fitness) {
                    bestM = competM;
                }
            }
            selected.add(bestM);
        }

        return selected;
    }

    // private List<int[]> selection(ArrayList<Moves> fitness_scores){
    // List<int[]> parents = new ArrayList<>();
    // for(int i = 0; i < fitness_scores.size()/4; i++){
    // Moves currMoves = fitness_scores.get(i);
    // parents.add(currMoves.moves);
    // }
    // return parents;
    // }

    // Crossover
    private int[][] crossover(int[] p1, int[] p2, double crossoverRate) {
        Random rnd = new Random();
        int n = p1.length;

        if (rnd.nextDouble() > crossoverRate) {
            return new int[][] {
                    p1.clone(),
                    p2.clone()
            };
        }

        int a = rnd.nextInt(n);
        int b = rnd.nextInt(n);

        // Ensure a < b
        if (a > b) {
            int temp = a;
            a = b;
            b = temp;
        }

        int[] c1 = p1.clone();
        int[] c2 = p2.clone();

        for (int i = a; i < b; i++) {
            c1[i] = p2[i];
            c2[i] = p1[i];
        }

        return new int[][] { c1, c2 };

    }
    //Mutation
    private void mutation(Maze maze, int[] chromosome, double mutationRate){
        Random rnd = new Random();
        int prev = -1;
        Point pos = maze.getStart();

        int newMove;
        for(int i = 0; i < chromosome.length; i++){
            Point nextPos = getNextPosition(pos, chromosome[i]);
            if(rnd.nextDouble() < mutationRate){
                int r = pos.x;
                int c = pos.y;
                List<Integer> validMoves = getValidMoves(maze, r, c, prev);
                
                if(!validMoves.isEmpty()) {
                    newMove = validMoves.get(rnd.nextInt(validMoves.size()));
                //newMove = rnd.nextInt(4);    
                    chromosome[i] = newMove;

                nextPos = getNextPosition(pos, newMove);
                }
            }
            // only update if not our of bound
            if (nextPos.x >= 0 && nextPos.x < maze.height() &&
            nextPos.y >= 0 && nextPos.y < maze.width() &&
            maze.get(nextPos.x, nextPos.y).type != CellType.WALL) {
                pos = nextPos;
            } else {
                // Stop tracing the path if we hit a wall/boundary to prevent invalid coordinate tracing
                break;
            }
        }
    }

    
    // Generate next generation
    private List<int[]> generateNextPopulation(Maze maze, List<Moves> best, List<Moves> parent, int popSize, double crossoverRate, double mutationRate){
        List<int[]> newPopulation = new ArrayList<>();
        Random rnd = new Random();

        for (int i = 0; i < (popSize * 0.1); i++) {
            newPopulation.add(best.get(i).moves.clone());

        }

        while (newPopulation.size() < popSize) {
            // random parent
            int idx1 = rnd.nextInt(parent.size());
            int idx2 = rnd.nextInt(parent.size());

            int[] p1 = parent.get(idx1).moves;
            int[] p2 = parent.get(idx2).moves;

            int[][] children = crossover(p1, p2, crossoverRate);
            
            mutation(maze, children[0], mutationRate);
            mutation(maze, children[1], mutationRate);

            newPopulation.add(children[0]);

            if (newPopulation.size() < popSize)
                newPopulation.add(children[1]);
        }

        return newPopulation;
    }

}
