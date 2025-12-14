package pathfinder;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import maze.Cell;
import maze.CellType;
import maze.Maze;

public class GeneticAlgorithm implements pathfinderStrategy {
    private final HashMap<Point, double[]> deadEndMemory = new HashMap<>();
    private final HashMap<Point, Double> pointScore = new HashMap<>();
    private static final double PATH_HARD_BLOCK = 40.0;

    private final int MAX_POPULATION = 5000;
    private final double CROSSOVER_RATE = 0.95;
    private final double MUTATION_RATE = 0.1;
    private final double GUIDED_RATIO = 0.25;
    private Random rnd = new Random();

    public int bestExit(Maze maze) {
        // constant
        int CHROM_LENGTH = (int) ((maze.width() + maze.height()) * 3 );
        
        List<int[]> population = generatePopulation(maze, CHROM_LENGTH);
        Moves bestMoves = geneticAlgorithm(maze, population, CHROM_LENGTH);

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

    private List<Integer> getValidMoves(Maze maze, Point pos, int prevMove) {
        List<Integer> moves = new ArrayList<>();
        int available = maze.availableDirection(pos.x, pos.y);
        
        // 0000 -> Up, Down, Left, Right (8, 4, 2, 1)
        if ((available & 8) != 0 && prevMove != 1) moves.add(0); // Up
        if ((available & 4) != 0 && prevMove != 0) moves.add(1); // Down
        if ((available & 2) != 0 && prevMove != 3) moves.add(2); // Left
        if ((available & 1) != 0 && prevMove != 2) moves.add(3); // Right
        
        // Hive-mind pruning
        moves.removeIf(m -> {
        Point next = getNextPosition(pos, m);
        return getPenalty(pos, m) > PATH_HARD_BLOCK;
    });
    return moves;
    }

    private List<Integer> getAllValidMovesIgnoringMemory(Maze maze, Point pos) {
        List<Integer> moves = new ArrayList<>();
        int available = maze.availableDirection(pos.x, pos.y);

        if ((available & 8) != 0) moves.add(0);
        if ((available & 4) != 0) moves.add(1);
        if ((available & 2) != 0) moves.add(2);
        if ((available & 1) != 0) moves.add(3);

        return moves;
    }

    private void solve(Maze maze, Moves best){
        Point pos = maze.getStart();
        List<Point> visited = new ArrayList<>();

        if (best.goalReached)
            System.out.println("Goal Reached");

        for(int move: best.moves){
            System.out.printf("%d", move);
        }

        System.out.println();
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
            int CHROM_LENGTH
        ) {
        Moves bestMoves = new Moves(null, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
        int stagnantCount = 0;
        int annihilationCount = 0;
        
        int generation = 0;
        ArrayList<Moves> fitness_scores; 

        do{
            //hyper-mutation
            // if(stagnantCount > 25)
            //     currentMutationRate = 0.10;
            // else 
            //     currentMutationRate = MUTATION_RATE;
            fitness_scores = new ArrayList<>();
            for (int[] chromosome : population) {
                fitness_scores.add(fitnessCalculate(maze, chromosome));

            }

            Collections.sort(fitness_scores);
            // stop when no improvement found
            if (bestMoves.fitness + 1e-9 > fitness_scores.get(0).fitness){
                annihilationCount++;
                stagnantCount++;
            }

            else if (bestMoves.fitness < fitness_scores.get(0).fitness) {
                bestMoves = fitness_scores.get(0);
                stagnantCount = 0;
                annihilationCount = 0;
            }

            System.out.println("Generation " + generation + " Best Fitness: " + bestMoves.fitness);
            generation++;

            List<Moves> selectedMoves = selection(fitness_scores, (int)(MAX_POPULATION/2));
            List<int[]> newGen;
            if(annihilationCount < 25 ) 
                newGen = generateNextPopulation(maze, fitness_scores, selectedMoves);
            else {
                annihilationCount = 0;
                newGen = localOptimaEscaped(maze, fitness_scores);
            }
            population.clear();
            population.addAll(newGen);
            //decayHiveMind();
            
        } while (stagnantCount < 200);

        return bestMoves;
    }

    private int biasedStep(int prev) { 
        if (prev == -1) return rnd.nextInt(4); // first move 
        
        int[] forward; 
        switch(prev){ 
        case 0 : forward = new int[]{0, 3, 2}; // up: right, left, up 
        case 1 : forward = new int[]{1, 3, 2}; // down: down, right, left 
        case 2 : forward = new int[]{2, 1, 0}; // left: left, down, up 
        case 3 : forward = new int[]{3, 1, 0}; // right: right, up, down 
        default : forward = new int[]{1, 3, 2, 0};// else down, right, left, up 
        }

        int choice = rnd.nextInt(10); // 0–9 
        if (choice < 6) return forward[0]; //50%
        if (choice < 9) return forward[1]; //30%
        return forward[2];                 //20%
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
        int prev = -1;
        int nextMove;
        Point nextPos;
        Set<Point> visited = new HashSet<>();
        visited.add(pos);
                
        int count = 0;
        while(count < length*GUIDED_RATIO){
            List<Integer> validMoves = getValidMoves(maze, pos, prev);
            List<Integer> nonLoopMoves = new ArrayList<>();
            
            for(int move : validMoves) {
                Point potentialPos = getNextPosition(pos, move);
                if (!visited.contains(potentialPos)) {
                    nonLoopMoves.add(move);
                }
                
            }
            
            if(nonLoopMoves.isEmpty())
                break;
            else {
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

    private List<int[]> generatePopulation(Maze maze, int chromLength) {
        List<int[]> population = new ArrayList<>();

        for (int i = 0; i < MAX_POPULATION; i++) {
            if(i < (MAX_POPULATION*0.5))
                population.add(guidedChromosomes(maze, chromLength));
            else
                population.add(generateChromosomes(chromLength));
        }

        return population;
    }

    // Hive mind helper for fitness function
    private double getPenalty(Point p, int move){
        double[] arr = deadEndMemory.get(p);
        return arr == null ? 0.0 : arr[move];
    }

    private void addPenalty(Point p, int move, double value){
        deadEndMemory
            .computeIfAbsent(new Point(p), k -> new double[4])[move] += value;
    }
    
    private double getScore(Point p){
        return pointScore.getOrDefault(p, 0.0);
    }

    private void addScore(Point p, double value){
        pointScore.put(p, pointScore.getOrDefault(p, 0.0) + value);
    }

    // Fitness function
    private Moves fitnessCalculate(Maze maze, int[] chromosome) {
        Point start = maze.getStart();
        Point goal = maze.getGoal();
        Point pos = start, prev = start;
        int totalCost = 0;
        int stepsTaken = 0;

        int rows = maze.height();
        int cols = maze.width();

        double fitness = 0;

        HashMap<Point, Integer> uniqueVisited = new HashMap<>();
        ArrayList<Point> recent = new ArrayList<>();

        double dist = Integer.MAX_VALUE;
        // int prev = -1;
        int maxDist = maze.height() + maze.width();
        for(int move: chromosome){
            pos = getNextPosition(pos, move);
            

            // out of bounds
            if(pos.x < 0 || pos.x >= rows || pos.y < 0 || pos.y >= cols ){
                // Penalty based on how much of the chromosome was left unused
                fitness -= (chromosome.length - stepsTaken) * 500;
                // fitness -= (maxDist - dist) * 1000;
                addPenalty(prev, move, 10.0);
                break;
            }

            // get next cell
            Cell next = maze.get(pos.x, pos.y);

            // hit wall
            if(next.type == CellType.WALL){
                fitness -= (chromosome.length - stepsTaken) * 500;
                addPenalty(prev, move, 10.0);
                //fitness -= (maxDist - dist) * 1000;
                break;
            }
            
            // Dead End detection
            List<Integer> exits = getAllValidMovesIgnoringMemory(maze, pos);

            // exploration reward
            if (uniqueVisited.get(pos) == null) {
                fitness += 250; // Reward finding new floor tiles
                uniqueVisited.put(new Point(pos), 1);
            } else {
                int visited = uniqueVisited.get(pos);
                fitness -= 300 * visited * visited;                
                uniqueVisited.put(new Point(pos), visited + 1);
                if (visited >= 4) {
                    fitness -= 1_000_000;
                    addPenalty(prev, move, 20.0);
                }
            }

            double currDist = Math.hypot(pos.x - goal.x, pos.y - goal.y);

            if (currDist < dist) {
                fitness += 300;
            } else if (currDist > dist + 2){
                fitness -= 50;
            }

            dist = currDist;

            // Loop detection
            if (recent.contains(pos)) {
                fitness -= 4000;
            }

            recent.add(pos);
            if (recent.size() > 10) 
                recent.remove(0);

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
            fitness += 1_000_000;
            fitness -= totalCost * 300;
            fitness -= stepsTaken * 250;

        } else {
            fitness -= totalCost * 25;
        }

        // eliminate looper
        if (!goalReached && uniqueVisited.size() < stepsTaken * 0.4) {
            fitness = -500_000;
        }

        // exploration reward
        //fitness += Math.min(stepsTaken, uniqueVisited.size()) * 150; 
        //fitness += stepsTaken * 500;
        // penalize costly paths
        //fitness -= stepsTaken * 500;

        dist = Math.pow(pos.x - goal.x, 2) +
                    Math.pow(pos.y - goal.y, 2);
        
        dist = Math.sqrt(dist);
        fitness += (maxDist - dist) * 500;// Reward getting farther from the starting point
        // if (dist <= 5) fitness += 1000*((5-dist));

    
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
        int competitionSize = 5;
        List<Moves> selected = new ArrayList<>();
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
    private int[][] crossover(int[] p1, int[] p2) {
        int n = p1.length;

        if (rnd.nextDouble() > CROSSOVER_RATE) {
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
    private void mutation(Maze maze, int[] chromosome) {
        int prev = -1;
        Point pos = maze.getStart();

        for (int i = 0; i < chromosome.length; i++) {

            Point nextPos = getNextPosition(pos, chromosome[i]);

            if (rnd.nextDouble() < MUTATION_RATE) {

                List<Integer> validMoves = getValidMoves(
                    maze, pos, prev
                );

                if (!validMoves.isEmpty()) {
                    int bestMove = -1;
                    double bestScore = Double.NEGATIVE_INFINITY;

                    for (int m : validMoves) {
                        if (getPenalty(pos, m) > PATH_HARD_BLOCK) continue;

                        Point next = getNextPosition(pos, m);

                        double score = rnd.nextDouble();
                        if (score > bestScore) {
                            bestScore = score;
                            bestMove = m;
                        }
                    }

                    if (bestMove >= 0) {
                        chromosome[i] = bestMove;
                        nextPos = getNextPosition(pos, bestMove);
                    }
                }
            }

            // Advance position safely
            if (nextPos.x >= 0 && nextPos.x < maze.height()
                    && nextPos.y >= 0 && nextPos.y < maze.width()
                    && maze.get(nextPos.x, nextPos.y).type != CellType.WALL) {

                pos = nextPos;
                prev = chromosome[i]; 

            } else {
                break;
            }
        }
    }
    
    // Generate next generation
    private List<int[]> generateNextPopulation(Maze maze, List<Moves> best, List<Moves> parent){
        List<int[]> newPopulation = new ArrayList<>();

        for (int i = 0; i < (MAX_POPULATION * 0.05); i++) {
            newPopulation.add(best.get(i).moves.clone());

        }

        while (newPopulation.size() < MAX_POPULATION) {
            // random parent
            int idx1 = rnd.nextInt(parent.size());
            int idx2 = rnd.nextInt(parent.size());

            int[] p1 = parent.get(idx1).moves;
            int[] p2 = parent.get(idx2).moves;

            int[][] children = crossover(p1, p2);
            
            mutation(maze, children[0]);
            mutation(maze, children[1]);

            newPopulation.add(children[0]);

            if (newPopulation.size() < MAX_POPULATION)
                newPopulation.add(children[1]);
        }

        return newPopulation;
    }

    private List<int[]> localOptimaEscaped(Maze maze, List<Moves> best){
        List<int[]> population = new ArrayList<>();
        // select first 10% best from current generation
        for(int i = 0; i < MAX_POPULATION*0.05; i++){
            population.add(best.get(i).moves);
        }

        int length = best.get(0).moves.length;
        // Generate new guided genes for 50%
        for(int j = 0; j < MAX_POPULATION * 0.55 ; j++){
            population.add(guidedChromosomes(maze, length));
        }

        // Generate a random genes for  the 40% left
        while(population.size() < MAX_POPULATION){
            population.add(generateChromosomes(length));
        }
        return population;
    }

}
