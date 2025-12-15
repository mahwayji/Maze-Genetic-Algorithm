package pathfinder;

import maze.Maze;
import maze.Cell;
import maze.CellType;
import java.awt.Point;
import java.util.*;

public class GeneticAlgorithmVisualizer implements AnimatablePathfinder {

    private final Maze maze;
    private final HashMap<Point, double[]> deadEndMemory = new HashMap<>();
    private static final double PATH_HARD_BLOCK = 40.0;

    private final int MAX_POPULATION = 5000;
    private final double CROSSOVER_RATE = 0.9;
    private final double MUTATION_RATE = 0.1;
    private final double GUIDED_RATIO = 0.25;
    private Random rnd = new Random();

    private final int CHROM_LENGTH;
    private final int MAX_STAGNANT;

    private List<int[]> population;
    private Moves bestMoves;

    private int generation = 0;
    private int stagnantCount = 0;
    private int annihilationCount = 0;
    private boolean finished = false;

    private List<Point> currentPath = new ArrayList<>();

    public GeneticAlgorithmVisualizer(Maze maze) {
        this.maze = maze;

        this.CHROM_LENGTH = (int) ((maze.width() + maze.height()) * 2);
        this.MAX_STAGNANT = Math.max(maze.width(), maze.height()) * 3;

        this.population = generatePopulation(maze, CHROM_LENGTH);
        this.bestMoves = new Moves(null, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
    }

    @Override
    public String getName() {
        return "GA HiveMind (Gen: " + generation + ")";
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public int getTotalCost() {
        return bestMoves.totalCost;
    }

    @Override
    public void nextStep() {
        if (finished)
            return;

        ArrayList<Moves> fitness_scores = new ArrayList<>();
        for (int[] chromosome : population) {
            fitness_scores.add(fitnessCalculate(maze, chromosome));
        }

        Collections.sort(fitness_scores);

        if (bestMoves.fitness + 1e-9 > fitness_scores.get(0).fitness) {
            annihilationCount++;
            stagnantCount++;
        }

        else if (bestMoves.fitness < fitness_scores.get(0).fitness) {
            bestMoves = fitness_scores.get(0);
            stagnantCount = 0;
            annihilationCount = 0;

            updateDisplayPath(bestMoves);
        }

        System.out.println("Generation " + generation + " Best Fitness: " + bestMoves.fitness);
        generation++;

        if (stagnantCount >= MAX_STAGNANT) {
            finished = true;
            if (bestMoves.goalReached)
                System.out.println("Goal Reached!");
            else
                System.out.println("Given up (Stagnant)");
            return;
        }

        List<Moves> selectedMoves = selection(fitness_scores, (int) (MAX_POPULATION / 2));
        List<int[]> newGen;

        if (annihilationCount < MAX_STAGNANT / 5) {
            newGen = generateNextPopulation(maze, fitness_scores, selectedMoves);
        } else {
            System.out.println("--- TRIGGER ANNIHILATION (Escaping Local Optima) ---");
            annihilationCount = 0;
            newGen = localOptimaEscaped(maze, fitness_scores);
        }

        population.clear();
        population.addAll(newGen);
    }

    private void updateDisplayPath(Moves best) {
        currentPath.clear();
        Point pos = maze.getStart();
        currentPath.add(pos);

        for (int move : best.moves) {
            Point nextPos = getNextPosition(pos, move);

            if (nextPos.x < 0 || nextPos.x >= maze.height() || nextPos.y < 0 || nextPos.y >= maze.width()) {
                break;
            }

            pos = nextPos;
            currentPath.add(pos);

            Cell nextCell = maze.get(pos.x, pos.y);
            if (nextCell.type == CellType.WALL)
                break;
            if (nextCell.type == CellType.GOAL)
                break;
        }
    }

    @Override
    public List<Point> getPath() {
        return currentPath;
    }

    @Override
    public Set<Point> getOpenSet() {
        return Collections.emptySet();
    }

    @Override
    public Set<Point> getClosedSet() {
        return deadEndMemory.keySet();
    }

    private Point getNextPosition(Point current, int move) {
        int newR = current.x;
        int newC = current.y;
        switch (move) {
            case 0:
                newR--; // up 
                break;
            case 1:
                newR++; // down 
                break;
            case 2:
                newC--; // left
                break;
            case 3:
                newC++; // right
                break;
        }
        return new Point(newR, newC);
    }

    private List<Integer> getValidMoves(Maze maze, Point pos, int prevMove) {
        List<Integer> moves = new ArrayList<>();
        int available = maze.availableDirection(pos.x, pos.y);

        if ((available & 8) != 0 && prevMove != 1) // up
            moves.add(0);
        if ((available & 4) != 0 && prevMove != 0) // down 
            moves.add(1);
        if ((available & 2) != 0 && prevMove != 3) // left
            moves.add(2);
        if ((available & 1) != 0 && prevMove != 2) // right
            moves.add(3);

        // Hive-mind pruning
        moves.removeIf(m -> {
            return getPenalty(pos, m) > PATH_HARD_BLOCK;
        });
        return moves;
    }

    private int biasedStep(int prev) {
        if (prev == -1)
            return rnd.nextInt(4);

        int[] forward;
        switch (prev) {
            case 0:
                forward = new int[] { 0, 3, 2 }; // up: right, left, up 
            case 1:
                forward = new int[] { 1, 3, 2 }; // down: down, right, left 
            case 2:
                forward = new int[] { 2, 1, 0 }; // left: left, down, up 
            case 3:
                forward = new int[] { 3, 1, 0 }; // right: right, up, down 
            default:
                forward = new int[] { 1, 3, 2, 0 };
        }

        int choice = rnd.nextInt(10);
        if (choice < 6) // 60%
            return forward[0];
        if (choice < 9) // 20%
            return forward[1];
        return forward[2]; // 10%
    }

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

    private int[] guidedChromosomes(Maze maze, int length) {
        int[] genes = new int[length];
        Point pos = maze.getStart();
        int prev = -1;
        int nextMove;
        Point nextPos;
        Set<Point> visited = new HashSet<>();
        visited.add(pos);

        int count = 0;
        while (count < length * GUIDED_RATIO) {
            List<Integer> validMoves = getValidMoves(maze, pos, prev);
            List<Integer> nonLoopMoves = new ArrayList<>();

            for (int move : validMoves) {
                Point potentialPos = getNextPosition(pos, move);
                if (!visited.contains(potentialPos)) {
                    nonLoopMoves.add(move);
                }
            }

            if (nonLoopMoves.isEmpty())
                break;
            else {
                nextMove = nonLoopMoves.get(rnd.nextInt(nonLoopMoves.size()));
            }

            nextPos = getNextPosition(pos, nextMove);
            genes[count] = nextMove;
            prev = nextMove;
            count++;

            // If the chosen move leads to a loop, stop the guided part
            if (visited.contains(nextPos))
                break;

            visited.add(nextPos);
            pos = nextPos;

            if (maze.get(pos.x, pos.y).type == CellType.GOAL) {
                break;
            }
        }

        for (int i = count; i < length; i++) {
            nextMove = biasedStep(prev);
            prev = nextMove;
            genes[i] = nextMove;
        }

        return genes;
    }

    private List<int[]> generatePopulation(Maze maze, int chromLength) {
        List<int[]> population = new ArrayList<>();

        for (int i = 0; i < MAX_POPULATION; i++) {
            // 50% Guided population and 50% random
            if (i < (MAX_POPULATION * 0.5))
                population.add(guidedChromosomes(maze, chromLength));
            else
                population.add(generateChromosomes(chromLength));
        }
        return population;
    }

    // Hive mind helper for fitness function
    private double getPenalty(Point p, int move) {
        double[] arr = deadEndMemory.get(p);
        return arr == null ? 0.0 : arr[move];
    }

    private void addPenalty(Point p, int move, double value) {
        deadEndMemory
                .computeIfAbsent(new Point(p), k -> new double[4])[move] += value;
    }

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
        int maxDist = maze.height() + maze.width();

        for (int move : chromosome) {
            pos = getNextPosition(pos, move);

            // out of bounds
            if (pos.x < 0 || pos.x >= rows || pos.y < 0 || pos.y >= cols) {
                fitness -= (chromosome.length - stepsTaken) * 500;
                fitness -= getPenalty(prev, move);
                addPenalty(prev, move, 20.0);
                break;
            }

            // get next cell
            Cell next = maze.get(pos.x, pos.y);

            // hit wall
            if (next.type == CellType.WALL) {
                addPenalty(prev, move, 20.0);
                fitness -= getPenalty(prev, move);
                fitness -= (chromosome.length - stepsTaken) * 500;
                break;
            }

            // exploration reward
            if (uniqueVisited.get(pos) == null) {
                fitness += 250;
                uniqueVisited.put(new Point(pos), 1);
            } else {
                // revisited old place
                int visited = uniqueVisited.get(pos);
                fitness -= 300 * Math.pow(visited, 2);
                uniqueVisited.put(new Point(pos), visited + 1);
                if (visited >= 4) {
                    fitness -= 1_000_000;
                    addPenalty(prev, move, 20.0);
                }
            }

            double currDist = Math.hypot(pos.x - goal.x, pos.y - goal.y);

            if (currDist < dist) {
                fitness += 300;
            } else if (currDist > dist + 2) {
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

            double penalty = getPenalty(prev, move);
            if (penalty > 0.0) {
                // Heavily penalize moving into known bad areas
                fitness -= penalty * 50;
            }

            prev = pos;
            stepsTaken++;
        }

        // Global path quality
        boolean goalReached = maze.getGoal().equals(pos);
        if (goalReached) {
            fitness += 1_000_000;
            fitness -= totalCost * 300;
            fitness -= stepsTaken * 250;
        } else {
            fitness -= totalCost * 25;
        }

        // eliminate loop move
        if (!goalReached && uniqueVisited.size() < stepsTaken * 0.4) {
            fitness = -500_000;
        }

        dist = Math.pow(pos.x - goal.x, 2) + Math.pow(pos.y - goal.y, 2);
        dist = Math.sqrt(dist);
        // Reward getting closer to goal
        fitness += (maxDist - dist) * 500;

        return new Moves(chromosome, fitness, totalCost, goalReached);
    }

    // Selection
    private List<Moves> selection(ArrayList<Moves> fitness_scores, int count) {
        int competitionSize = 4;
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

    // Crossover
    private int[][] crossover(int[] p1, int[] p2) {
        int n = p1.length;

        if (rnd.nextDouble() > CROSSOVER_RATE) {
            return new int[][] { p1.clone(), p2.clone() };
        }

        int a = rnd.nextInt(n);
        int b = rnd.nextInt(n);

        // ensure a < b
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

    private void mutation(Maze maze, int[] chromosome) {
        int prev = -1;
        Point pos = maze.getStart();
        for (int i = 0; i < chromosome.length; i++) {

            Point nextPos = getNextPosition(pos, chromosome[i]);

            if (rnd.nextDouble() < MUTATION_RATE) {
                // get all possible move from current point
                List<Integer> validMoves = getValidMoves(maze, pos, prev);

                // mutate the the best move based on current deadEndMemory score
                if (!validMoves.isEmpty()) {
                    int bestMove = -1;
                    double bestScore = Double.NEGATIVE_INFINITY;

                    for (int m : validMoves) {
                        // if the score are exceed the PATH_HARD_BLOCK we will considered
                        // it as a block path
                        if (getPenalty(pos, m) > PATH_HARD_BLOCK)
                            continue;

                        nextPos = getNextPosition(pos, m);
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
    private List<int[]> generateNextPopulation(Maze maze, ArrayList<Moves> allFitnessScores,
            List<Moves> selectedParents) {
        List<int[]> newPopulation = new ArrayList<>();

        // elitism t%
        for (int i = 0; i < (MAX_POPULATION * 0.05); i++) {
            newPopulation.add(allFitnessScores.get(i).moves.clone());
        }

        while (newPopulation.size() < MAX_POPULATION) {
            // random parent
            int idx1 = rnd.nextInt(selectedParents.size());
            int idx2 = rnd.nextInt(selectedParents.size());

            int[] p1 = selectedParents.get(idx1).moves;
            int[] p2 = selectedParents.get(idx2).moves;

            int[][] children = crossover(p1, p2);

            mutation(maze, children[0]);
            mutation(maze, children[1]);
            newPopulation.add(children[0]);

            if (newPopulation.size() < MAX_POPULATION)
                newPopulation.add(children[1]);
        }
        return newPopulation;
    }

    private List<int[]> localOptimaEscaped(Maze maze, ArrayList<Moves> best) {
        deadEndMemory.clear();
        List<int[]> newPop = new ArrayList<>();

        // elitism 5%
        for (int i = 0; i < MAX_POPULATION * 0.05; i++) {
            newPop.add(best.get(i).moves);
        }

        int length = best.get(0).moves.length;
        // Generate new guided genes for 55%
        for (int j = 0; j < MAX_POPULATION * 0.55; j++) {
            newPop.add(guidedChromosomes(maze, length));
        }

        // Generate a random genes for  the 40% left
        while (newPop.size() < MAX_POPULATION) {
            newPop.add(generateChromosomes(length));
        }
        return newPop;
    }
}