package pathfinder;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import maze.Cell;
import maze.CellType;
import maze.Maze;

public class GeneticAlgorithm implements pathfinderStrategy {
    public int bestExit(Maze maze) {
        // constant
        int CHROM_LENGTH = (int) ((maze.width() + maze.height()) * 2 );
        int MAX_POPULATION = 50000;
        double CROSSOVER_RATE = 0.95;
        double MUTATION_RATE = 0.08;
        
        
        List<int[]> population = generatePopulation(maze, MAX_POPULATION, CHROM_LENGTH);
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
            if(stagnantCount > 25)
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
    private int biasedStep(int prev) { 
        Random r = new Random(); 
        if (prev == -1) return r.nextInt(4); // first move 
        
        int[] forward; 
        switch(prev){ 
        case 0 : forward = new int[]{0, 3, 2}; // up: right, left, up 
        case 1 : forward = new int[]{1, 3, 2}; // down: down, right, left 
        case 2 : forward = new int[]{2, 1, 0}; // left: left, down, up 
        case 3 : forward = new int[]{3, 1, 0}; // right: right, up, down 
        default : forward = new int[]{1, 3, 2, 0};// else down, right, left, up 
        }

        int choice = r.nextInt(10); // 0–9 
        if (choice < 4) return forward[0]; 
        if (choice < 8) return forward[1]; 
        return forward[2];
    }
    // Generate population
    private int[] generateChromosomes(int length) {
        int[] genes = new int[length];
        int prev = -1;
        int newMove;
        for (int i = 0; i < length; i++) {
            newMove = biasedStep(prev);
            prev = newMove;
            genes[i] = newMove;
        }
        return genes;
    }

    private int[] guidedChromosomes(Maze maze, int length){
        int[] genes = new int[length];
        Point pos = maze.getStart();
        Random rnd =new Random();
        int prev = -1;
        int nextMove;
        Point nextPos;
        Set<Point> visited = new HashSet<>();
        visited.add(pos);
        
        Point goal = maze.getGoal();
        int bestMove = -1;
        double currentDist = Math.abs(pos.x - goal.x) + Math.abs(pos.y - goal.y);
        
        int count = 0;
        while(count < length/4){
            List<Integer> validMoves = getValidMoves(maze, pos.x, pos.y, prev);
            List<Integer> nonLoopMoves = new ArrayList<>();
            
            for(int move : validMoves) {
                Point potentialPos = getNextPosition(pos, move);
                double newDist = Math.abs(potentialPos.x - goal.x) + Math.abs(potentialPos.y - goal.y);
                if (!visited.contains(potentialPos)) {
                                    nonLoopMoves.add(move);
                }
                if (newDist < currentDist) {
                    bestMove = move;
                    currentDist = newDist;
                }
                
            }
            
            if(validMoves.isEmpty())
                break;

            if (bestMove != -1) {
                nextMove = bestMove;
            } else {
                // Fallback: use current random logic
                nextMove = nonLoopMoves.get(rnd.nextInt(nonLoopMoves.size()));
            }

            nextPos = getNextPosition(pos, nextMove);
            genes[count] = nextMove;
            prev = nextMove;
            count++;

            // If the chosen move leads to a loop, stop the guided part
            if (visited.contains(nextPos)) break;
            
            visited.add(nextPos);
            pos = nextPos;
            
            // No need to check for walls/bounds here as getValidMoves already ensures it's a valid path.
            // However, a goal cell check could be added if we wanted the guided part to stop at the goal.
            if (maze.get(pos.x, pos.y).type == CellType.GOAL) {
                break;
            }
        }

        for(int i = count; i < length; i++){
            nextMove = biasedStep(prev);
            prev = nextMove;
            genes[i] = nextMove;
        }

        return genes;
    }
    private List<int[]> generatePopulation(Maze maze, int populationSize, int chromLength) {
        List<int[]> population = new ArrayList<>();

        for (int i = 0; i < populationSize; i++) {
            if(i < (populationSize*0.5))
                population.add(guidedChromosomes(maze, chromLength));
            else
                population.add(generateChromosomes(chromLength));
        }

        return population;
    }

    // Fitness function
    private Moves fitnessCalculate(Maze maze, int[] chromosome) {
        Point start = maze.getStart();
        Point pos = start;
        int totalCost = 0;
        int stepsTaken = 0;

        int rows = maze.height();
        int cols = maze.width();

        double fitness = 0;

        HashMap<Point, Integer> uniqueVisited = new HashMap<>();
        Deque<Point> recent = new ArrayDeque<>();

        boolean hitWall = false; 
        boolean outOfBound = false;  

        double dist = Integer.MAX_VALUE;
        // int prev = -1;
        double prevDist = -1;
        int maxDist = maze.height() + maze.width();
        for(int move: chromosome){
            pos = getNextPosition(pos, move);
            

            // out of bounds
            if(pos.x < 0 || pos.x >= rows || pos.y < 0 || pos.y >= cols ){
                // Penalty based on how much of the chromosome was left unused
                fitness -= (chromosome.length - stepsTaken) * 500;
                // fitness -= (maxDist - dist) * 1000;
                outOfBound = true;
                break;
            }

            // get next cell
            Cell next = maze.get(pos.x, pos.y);

            // hit wall
            if(next.type == CellType.WALL){
                // // suicidal penalty
                // if(!getValidMoves(maze, pos.x, pos.y, move).isEmpty())
                //     fitness -= 20000;

                fitness -= (chromosome.length - stepsTaken) * 500;
                //fitness -= (maxDist - dist) * 1000;
                hitWall = true;
                break;
            }
            

            // exploration reward
            if (uniqueVisited.get(pos) == null) {
                fitness += 200; // Reward finding new floor tiles
                uniqueVisited.put(new Point(pos), 1);
            } else {
                // int visited = uniqueVisited.get(pos);
                // fitness -= (int)(100 * Math.pow(visited, 2)); // penalty for doing loop or going backward
                // uniqueVisited.put(pos,visited++);
            }

            prevDist = Math.pow(pos.x - start.x, 2) +
            Math.pow(pos.y - start.y, 2);
        
            prevDist = Math.sqrt(dist);

            if (dist > prevDist){
                fitness += 200;
            } else {
                fitness -= 100;
            }

            // Loop detection
            if (recent.contains(pos)) {
                fitness -= 4000;
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
            fitness += 1000000;
            fitness -= totalCost * 300;
            fitness -= stepsTaken * 250;

        } else {
            fitness -= totalCost * 25;
        }

        // exploration reward
        fitness += Math.min(stepsTaken, uniqueVisited.size()) * 100; 
        
        // penalize costly paths
        //fitness -= stepsTaken * 500;

        Point goal = maze.getGoal();

        dist = Math.pow(pos.x - goal.x, 2) +
                    Math.pow(pos.y - goal.y, 2);
        
        dist = Math.sqrt(dist);
        fitness += (maxDist - dist) * 500;// Reward getting farther from the starting point
        // if (dist <= 4) fitness += 1000*((4-dist));

        if (hitWall || outOfBound) {
            //fitness -= 10000;
        }
    
        // wasted run
        // if(stepsTaken == chromosome.length)
        //     fitness -= 100000;

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
        int competitionSize = 3;
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
            
                    chromosome[i] = newMove;
                    prev = newMove;
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
