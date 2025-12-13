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
        int CHROM_LENGTH = (int) ((maze.width() + maze.height()) * 2);
        int MAX_POPULATION = 40000;
        double CROSSOVER_RATE = 0.95;
        double MUTATION_RATE = 0.05;

        List<int[]> population = generatePopulation(MAX_POPULATION, CHROM_LENGTH);
        Moves bestMoves = geneticAlgorithm(maze, population, CHROM_LENGTH, MAX_POPULATION, CROSSOVER_RATE,
                MUTATION_RATE);

        solve(maze, bestMoves);
        return bestMoves.totalCost;
    }

    private void solve(Maze maze, Moves best) {
        Point pos = maze.getStart();
        List<Point> visited = new ArrayList<>();

        if (best.goalReached)
            System.out.println("Goal Reached");

        for (int move : best.moves) {
            int newR = pos.x;
            int newC = pos.y;
            switch (move) {
                case 0:
                    newR--;
                    break; // up
                case 1:
                    newR++;
                    break; // down
                case 2:
                    newC--;
                    break; // left
                case 3:
                    newC++;
                    break; // right
            }

            visited.add(pos);

            if (newR < 0 || newR >= maze.height() || newC < 0 || newC >= maze.width()) {
                break;
            }
            // get next point
            pos = new Point(newR, newC);
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
        ArrayList<Moves> fitness_scores;
        do {

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

            List<Moves> selectedMoves = selection(fitness_scores, (int) (MAX_POPULATION / 2));

            List<int[]> newGen = generateNextPopulation(fitness_scores, selectedMoves, MAX_POPULATION, CROSSOVER_RATE,
                    MUTATION_RATE);
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
        Point goal = maze.getGoal();
        int totalCost = 0;
        int stepsTaken = 0;
        int reverseCount = 0;

        int rows = maze.height();
        int cols = maze.width();
        int prev = -1; // previous move
        double fitness = 0;
        HashMap<Point, Integer> uniqueVisited = new HashMap<>();
        Deque<Point> last10 = new ArrayDeque<>();

        int dist = Integer.MAX_VALUE;
        int maxDist = maze.height() + maze.width();
        boolean hitWall = false;
        boolean outOfBound = false;

        for (int move : chromosome) {
            int newR = pos.x;
            int newC = pos.y;

            switch (move) {
                case 0:
                    newR--;
                    break; // up
                case 1:
                    newR++;
                    break; // down
                case 2:
                    newC--;
                    break; // left
                case 3:
                    newC++;
                    break; // right
            }

            // loop move
            if ((move == 0 && prev == 1) ||
                    (move == 1 && prev == 0) ||
                    (move == 2 && prev == 3) ||
                    (move == 3 && prev == 2)) {
                reverseCount++;
                fitness -= 200 * reverseCount;
            } else {
                reverseCount = 0;
            }
            prev = move;
            pos = new Point(newR, newC);

            // out of bounds
            if (newR < 0 || newR >= rows || newC < 0 || newC >= cols) {
                // Penalty based on how much of the chromosome was left unused
                // fitness -= (chromosome.length - stepsTaken) * 100;
                fitness -= (dist) * 1000;

                outOfBound = true;
                break;
            }

            // get next cell
            Cell next = maze.get(newR, newC);

            // hit wall
            if (next.type == CellType.WALL) {
                // fitness -= (chromosome.length - stepsTaken) * 100;
                fitness -= (dist) * 1000;
                hitWall = true;
                break;
            }

            // valid move
            // if (move == 0 && dy < 0) fitness += 15; // up
            // if (move == 1 && dy > 0) fitness += 15; // down
            // if (move == 2 && dx < 0) fitness += 15; // left
            // if (move == 3 && dx > 0) fitness += 15; // right

            // exploration reward
            if (uniqueVisited.get(pos) == null) {
                // fitness += 100; // Reward finding new floor tiles
                uniqueVisited.put(new Point(pos), 1);
            } else {
                int visited = uniqueVisited.get(pos);
                fitness -= (int) (100 * Math.pow(3, visited)); // penalty for doing loop or going backward
                uniqueVisited.put(pos, visited++);
            }

            // penalty for cycles
            if (last10.contains(pos)) {
                fitness -= 4000;
            }
            last10.add(pos);
            if (last10.size() > 10)
                last10.removeFirst();

            // cell type reward
            if (next.type == CellType.NUMBER)
                totalCost += next.value;

            if (next.type == CellType.GOAL)
                break;

            stepsTaken++;
        }

        boolean goalReached = pos.equals(goal);
        // fitness score

        if (goalReached) {
            fitness += 100000;
            fitness += (chromosome.length - stepsTaken) * 1; // Bonus for speed
            fitness -= totalCost * 100;
        } else {
            fitness -= totalCost * 5;
        }

        dist = Math.abs(pos.x - goal.x) +
                Math.abs(pos.y - goal.y);

        fitness += stepsTaken * 50;
        fitness += (maxDist - dist) * 300; // Reward getting closer
        if (dist <= 4)
            fitness += 1000 * ((4 - dist));

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
        int competitonsize = 5;
        List<Moves> selected = new ArrayList<>();
        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            Moves bestM = fitness_scores.get(rnd.nextInt(fitness_scores.size()));

            for (int j = 1; j < competitonsize; j++) {
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

    // Mutation
    private void mutation(int[] chromosome, double mutationRate) {
        Random rnd = new Random();
        int prev = -1;
        int newMove;
        for (int i = 0; i < chromosome.length; i++) {
            if (rnd.nextDouble() < mutationRate) {
                do {
                    newMove = rnd.nextInt(4);
                } while ((prev == 0 && newMove == 1) ||
                        (prev == 1 && newMove == 0) ||
                        (prev == 2 && newMove == 3) ||
                        (prev == 3 && newMove == 2));

                prev = newMove;
                chromosome[i] = newMove;
            }
        }
    }

    // Generate next generation
    private List<int[]> generateNextPopulation(List<Moves> best, List<Moves> parent, int popSize, double crossoverRate,
            double mutationRate) {
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

            mutation(children[0], mutationRate);
            mutation(children[1], mutationRate);

            newPopulation.add(children[0]);

            if (newPopulation.size() < popSize)
                newPopulation.add(children[1]);
        }

        return newPopulation;
    }

}
