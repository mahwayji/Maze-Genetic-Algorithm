package pathfinder;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import maze.Cell;
import maze.CellType;
import maze.Maze;
public class GeneticAlgorithm implements pathfinderStrategy{
    public int bestExit(Maze maze){
        //  constant
        int CHROM_LENGTH = (maze.width() + maze.height()) * 2;
        int MAX_POPULATION = 25000;
        double CROSSOVER_RATE = 0.90;
        double MUTATION_RATE = 0.05;
        
        
        List<int[]> population = generatePopulation(MAX_POPULATION, CHROM_LENGTH);
        Moves bestMoves = geneticAlgorithm(maze, population, CHROM_LENGTH, MAX_POPULATION, CROSSOVER_RATE, MUTATION_RATE);

        solve(maze, bestMoves.moves);
        return bestMoves.totalCost;
    }

    private void solve(Maze maze, int[] moves){
        Point pos = maze.getStart();
        List<Point> visited = new ArrayList<>();
        for(int move : moves){
            int newR = pos.x;
            int newC = pos.y;
            switch (move) {
                case 0: newR--; break; // up
                case 1: newR++; break; // down
                case 2: newC--; break; // left
                case 3: newC++; break; // right
            }
            
            visited.add(pos);

            // get next point
            pos = new Point(newR, newC);
            Cell nextCell = maze.get(pos.x, pos.y);

            if(nextCell.type == CellType.GOAL)
                break;
        }
        int count = 0;
        for(Point loc: visited){
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
        } while (stagnantCount < 20);

        return fitness_scores.get(0);
    }

    private int biasedStep(int prev) {
        Random r = new Random();

        if (prev == -1) return r.nextInt(4); // first move

        int[] forward;
        switch(prev){
            case 0 : forward = new int[]{0, 2, 3}; // up: up, left, right
            case 1 : forward = new int[]{1, 2, 3}; // down: down, left, right
            case 2 : forward = new int[]{2, 0, 1}; // left: left, up, down
            case 3 : forward = new int[]{3, 0, 1}; // right: right, up, down
            default : forward = new int[]{0, 1, 2, 3};// else up down left right
        }

        // strongly bias forward: 0=same direction
        int choice = r.nextInt(10);  // 0–9
        if (choice < 4) return forward[0];  
        if (choice < 7) return forward[1];  
        return forward[2];                 
    }

    // Generate population
    private int[] generateChromosomes(int length){
        int[] genes = new int[length];
        int prev = -1;
        for(int i = 0; i < length; i++){
            genes[i] = biasedStep(prev);
            prev = genes[i];
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
        int wallHits = 0;

        int rows = maze.height();
        int cols = maze.width();
        int prevDist = Integer.MAX_VALUE;
        int prev = -1; //previous move
        double fitness = 0;
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
                    fitness -= 100;
                }

            // out of bounds
            if(newR < 0 || newR >= rows || newC < 0 || newC >= cols){
                wallHits++;
                continue;
            }

            // get next cell
            Cell next = maze.get(newR, newC);

            // hit wall
            if(next.type == CellType.WALL){
                wallHits++;
                continue;
            }

            int newDist = Math.abs(newR - goal.x) + Math.abs(newC - goal.y);

            if(newDist < prevDist) fitness += 20;     // moving closer
            else if(newDist > prevDist) fitness -= 5; // moving away

            prevDist = newDist;

            // valid move 
            pos = new Point(newR, newC);
            fitness += 1;
            if(next.type == CellType.NUMBER)
                totalCost += next.value;
            if(next.type == CellType.GOAL)
                break;
        }

        int dist = Math.abs(pos.x - goal.x) +
                    Math.abs(pos.y - goal.y);
        int maxDist = maze.height() + maze.width();

        boolean goalReached = pos.equals(maze.getGoal());

        // fitness score
        if(goalReached)
            fitness += 3000;

        fitness -= wallHits * 2000;
        fitness += (maxDist - dist) * 10;
        fitness -= totalCost * 1;

        
        return new Moves(chromosome, fitness, totalCost, goalReached);
    }

    // Selection function
    private List<int[]> selection(ArrayList<Moves> fitness_scores){
        List<int[]> parents = new ArrayList<>();
        for(int i = 0; i < fitness_scores.size()/2; i++){
            Moves currMoves = fitness_scores.get(i);
            parents.add(currMoves.moves);
        }
        return parents;
    }

    // Crossover
    private int[][] crossover(int[] p1, int[] p2, double crossoverRate){
        Random rnd = new Random();
        int n = p1.length;
        int point = rnd.nextInt(n-2) + 1;

        if (rnd.nextDouble() > crossoverRate) {
            return new int[][]{
                    p1.clone(),
                    p2.clone()
            };
        }

        int[] c1 = new int[n];
        int[] c2 = new int[n];

        for(int i = 0; i < point; i ++){
            c1[i] = p1[i];
            c2[i] = p2[i];
        }

        for (int i = point; i < n; i++){
            c1[i] = p2[i];
            c2[i] = p1[i];
        }

        return new  int[][]{c1, c2};

    }
    //Mutation
    private void mutation(int[] chromosome, double mutationRate){
        Random rnd = new Random();

        for(int i = 0; i < chromosome.length; i++){
            if(rnd.nextDouble() < mutationRate){
                chromosome[i] = rnd.nextInt(4);
            }
        }
    }

    // Generate next generation
    private List<int[]> generateNextPopulation(List<int[]> parents, int popSize, double crossoverRate, double mutationRate){
        List<int[]> newPopulation = new ArrayList<>();
        Random rnd = new Random();

        // keep best parent
        newPopulation.add(parents.get(0).clone());

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

