package pathfinder;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import maze.Cell;
import maze.CellType;
import maze.Maze;
public class GeneticAlgorithm implements pathfinderStrategy{
    public int bestExit(Maze maze){
        //  constant
        int CHROM_LENGTH = (maze.width() + maze.height()) * 2;
        int MAX_POPULATION = 25000;
        double CROSSOVER_RATE = 0.9;
        double MUTATION_RATE = 0.08;
        
        
        List<int[]> population = generatePopulation(MAX_POPULATION, CHROM_LENGTH);
        Moves bestMoves = geneticAlgorithm(maze, population, CHROM_LENGTH, MAX_POPULATION, CROSSOVER_RATE, MUTATION_RATE);

        solve(maze, bestMoves);
        return bestMoves.totalCost;
    }

    private void solve(Maze maze, Moves best){
        Point pos = maze.getStart();
        List<Point> visited = new ArrayList<>();

        if(best.goalReached)
            System.out.println("Goal Reached");

        for(int move : best.moves){
            int newR = pos.x;
            int newC = pos.y;
            switch (move) {
                case 0: newR--; break; // up
                case 1: newR++; break; // down
                case 2: newC--; break; // left
                case 3: newC++; break; // right
            }
            
            visited.add(pos);

            if(newR < 0 || newR >= maze.height() || newC < 0 || newC >= maze.width() ){
                break;
            }
            // get next point
            pos = new Point(newR, newC);
            Cell nextCell = maze.get(pos.x, pos.y);

            if(nextCell.type == CellType.GOAL)
                break;
        }
        // int count = 0;
        // for(Point loc: visited){
        //     System.out.printf("%d. (%d, %d)\n", count++, loc.x, loc.y);
        // }
        maze.showMaze(visited);
    }

    private Moves geneticAlgorithm(
                                Maze maze,
                                List<int[]> population, 
                                int CHROM_LENGTH,
                                int MAX_POPULATION,
                                double CROSSOVER_RATE,
                                double MUTATION_RATE       
    ){
        Moves bestMoves = new Moves(null, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
        int stagnantCount = 0;

        int generation = 0;
        ArrayList<Moves> fitness_scores; 
        do{
            fitness_scores = new ArrayList<>();
            for(int[] chromosome: population){
                fitness_scores.add(fitnessCalculate(maze, chromosome));
                
            }

            Collections.sort(fitness_scores);
            // stop when no improvement found
            if(bestMoves.fitness + 1e-9 > fitness_scores.get(0).fitness)
                stagnantCount++;

            else if(bestMoves.fitness < fitness_scores.get(0).fitness){
                    bestMoves = fitness_scores.get(0);
                    stagnantCount = 0;
                }
            
            System.out.println("Generation "+ generation + " Best Fitness: " + bestMoves.fitness);
            generation++;

            List<int[]> newGen = generateNextPopulation(selection(fitness_scores), MAX_POPULATION, CROSSOVER_RATE, MUTATION_RATE);
            population.clear();
            population.addAll(newGen);
        } while (stagnantCount < 50);

        return bestMoves;
    }

    // Generate population
    private int[] generateChromosomes(int length){
        int[] genes = new int[length];
        Random rnd = new Random();
        for(int i = 0; i < length; i++){
            genes[i] = rnd.nextInt(4);
        }
        return genes;
    }

    public List<int[]> generatePopulation(int populationSize, int chromLength){
        List<int[]> population = new ArrayList<>();

        for(int i = 0; i < populationSize; i++){
            population.add(generateChromosomes(chromLength));
        }

        return population;
    }
    // Fitness function
    private Moves fitnessCalculate(Maze maze, int[] chromosome){
        Point pos = maze.getStart();
        Point goal = maze.getGoal();
        int totalCost = 0;
        int stepsTaken = 0;

        int rows = maze.height();
        int cols = maze.width();
        int prev = -1; //previous move
        double fitness = 0;
        Set<Point> uniqueVisited = new HashSet<>();

        for(int move: chromosome){
            int newR = pos.x;
            int newC = pos.y;

            switch (move) {
                case 0: newR--; break; // up
                case 1: newR++; break; // down
                case 2: newC--; break; // left
                case 3: newC++; break; // right
            }

            // loop move
            if((move == 0 && prev == 1) ||
                (move == 1 && prev == 0) ||
                (move == 2 && prev == 3) ||
                (move == 3 && prev == 2))
                {
                    fitness -= 500;
                }
            prev = move;

            // out of bounds
            if(newR < 0 || newR >= rows || newC < 0 || newC >= cols ){
                // Penalty based on how much of the chromosome was left unused
                fitness -= (chromosome.length - stepsTaken) * 500;
                break;
            }

            // get next cell
            Cell next = maze.get(newR, newC);

            // hit wall
            if(next.type == CellType.WALL){
                fitness -= (chromosome.length - stepsTaken) * 100;
                break;
            }

            // valid move 
            pos = new Point(newR, newC);

            if (!uniqueVisited.contains(pos)) {
                fitness += 100; // Reward finding new floor tiles
                uniqueVisited.add(new Point(pos));
            }

            if(next.type == CellType.NUMBER)
                totalCost += next.value;

            if(next.type == CellType.GOAL)
                break;

            stepsTaken++;
        }

        int dist = Math.abs(pos.x - goal.x) +
                    Math.abs(pos.y - goal.y);
        int maxDist = maze.height() + maze.width();

        boolean goalReached = pos.equals(maze.getGoal());

        // fitness score
        fitness += (maxDist - dist) * 10; // Reward getting closer

        if (goalReached) {
            fitness += 10000;
            fitness += (chromosome.length - stepsTaken) * 50; // Bonus for speed
        }
        fitness -= totalCost * 5;

        
        return new Moves(chromosome, fitness, totalCost, goalReached);
    }

    // Selection function
    private List<int[]> selection(ArrayList<Moves> fitness_scores, int count) {
        List<int[]> selected = new ArrayList<>();
        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            // Pick two random competitors
            Moves m1 = fitness_scores.get(rnd.nextInt(fitness_scores.size()));
            Moves m2 = fitness_scores.get(rnd.nextInt(fitness_scores.size()));
            // The better one wins
            selected.add(m1.fitness > m2.fitness ? m1.moves : m2.moves);
        }
        return selected;
    }

    private List<int[]> selection(ArrayList<Moves> fitness_scores){
        List<int[]> parents = new ArrayList<>();
        for(int i = 0; i < fitness_scores.size()/4; i++){
            Moves currMoves = fitness_scores.get(i);
            parents.add(currMoves.moves);
        }
        return parents;
    }
    // Crossover
    private int[][] crossover(int[] p1, int[] p2, double crossoverRate){
        Random rnd = new Random();
        int n = p1.length;

        if (rnd.nextDouble() > crossoverRate) {
            return new int[][]{
                    p1.clone(),
                    p2.clone()
            };
        }

        int a = rnd.nextInt(n);
        int b = rnd.nextInt(n);
        if(a > b){ int tmp = a; a = b; b = tmp; }

        int[] c1 = p1.clone();
        int[] c2 = p2.clone();

        for(int i = a; i < b; i++){
            c1[i] = p2[i];
            c2[i] = p1[i];
        }

        return new  int[][]{c1, c2};

    }
    //Mutation
    private void mutation(int[] chromosome, double mutationRate){
        Random rnd = new Random();

        for(int i = 1; i < chromosome.length; i++){
            if(rnd.nextDouble() < mutationRate){
                chromosome[i] = rnd.nextInt(4);
            }
        }
    }

    // Generate next generation
    private List<int[]> generateNextPopulation(List<int[]> parents, int popSize, double crossoverRate, double mutationRate){
        List<int[]> newPopulation = new ArrayList<>();
        Random rnd = new Random();

        while(newPopulation.size() < popSize){
            // random parent
            int idx1 = rnd.nextInt(parents.size());
            int idx2 = rnd.nextInt(parents.size());

            int[] p1 = parents.get(idx1);
            int[] p2 = parents.get(idx2);

            int[][] children = crossover(p1, p2, crossoverRate);
            
            mutation(children[0], mutationRate);
            mutation(children[1], mutationRate);

            newPopulation.add(children[0]);

            if(newPopulation.size() < popSize)
                newPopulation.add(children[1]);
        }

        return newPopulation;
    }

}

