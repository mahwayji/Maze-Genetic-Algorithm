package display;

import maze.Maze;
import pathfinder.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MazeAnimatorApp extends JFrame implements ActionListener {

    private Maze maze;
    private MazePanel mazePanel;
    private Timer timer;
    private AnimatablePathfinder currentSolver;

    private JButton startButton;
    private JButton resetButton;
    private JLabel statusLabel;
    private JComboBox<String> algoSelector;

    private int animationDelay;

    public MazeAnimatorApp(Maze maze) {
        this.maze = maze;
        setTitle("Maze Pathfinding Animator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        calculateSpeed();

        currentSolver = new DijkstraAnimatorWrapper(maze);

        setupGUI();

        timer = new Timer(animationDelay, this);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void calculateSpeed() {
        int totalCells = maze.width() * maze.height();

        if (totalCells <= 100) {
            animationDelay = 150;
        } else if (totalCells <= 900) {
            animationDelay = 50;
        } else if (totalCells <= 2500) {
            animationDelay = 10;
        } else {
            animationDelay = 1;
        }
    }

    private void setupGUI() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(230, 230, 230));
        topPanel.add(new JLabel("Select Algorithm: "));

        String[] algos = {
                "Dijkstra",
                "A* (A-Star)",
                "Greedy Best-First Search",
                "Genetic Algorithm"
        };

        algoSelector = new JComboBox<>(algos);
        algoSelector.setFocusable(false);
        algoSelector.addActionListener(e -> resetSolver());
        topPanel.add(algoSelector);

        contentPane.add(topPanel, BorderLayout.NORTH);

        mazePanel = new MazePanel(maze, currentSolver);
        contentPane.add(mazePanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(230, 230, 230));

        startButton = new JButton("Start");
        startButton.setFocusable(false);
        startButton.addActionListener(e -> toggleAnimation());

        resetButton = new JButton("Reset");
        resetButton.setFocusable(false);
        resetButton.addActionListener(e -> resetSolver());

        statusLabel = new JLabel("Status: Ready | Speed: " + animationDelay + "ms");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0)); // เว้นระยะนิดหน่อย

        controlPanel.add(startButton);
        controlPanel.add(resetButton);
        controlPanel.add(statusLabel);
        contentPane.add(controlPanel, BorderLayout.SOUTH);
    }

    private void resetSolver() {

        if (timer.isRunning())
            timer.stop();

        startButton.setText("Start");
        startButton.setEnabled(true);

        String selected = (String) algoSelector.getSelectedItem();

        if ("Dijkstra".equals(selected)) {
            currentSolver = new DijkstraAnimatorWrapper(maze);
        } else if ("A* (A-Star)".equals(selected)) {
            currentSolver = new AStarAnimatorWrapper(maze);
        } else if ("Greedy Best-First Search".equals(selected)) {
            currentSolver = new GreedyAnimatorWrapper(maze);
        } else if ("Genetic Algorithm".equals(selected)) {
            currentSolver = new GeneticAlgorithmVisualizer(maze);
        }

        mazePanel.setSolver(currentSolver);
        statusLabel.setText("Status: Ready (" + currentSolver.getName() + ")");
        mazePanel.repaint();
    }

    private void toggleAnimation() {
        if (timer.isRunning()) {
            timer.stop();
            startButton.setText("Resume");
            statusLabel.setText("Status: Paused");
        } else if (!currentSolver.isFinished()) {
            timer.start();
            startButton.setText("Pause");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!currentSolver.isFinished()) {
            currentSolver.nextStep();

            updateStatusLabel();

            mazePanel.repaint();
        } else {

            timer.stop();
            startButton.setText("Finished");
            startButton.setEnabled(false);

            statusLabel.setText("Done! Total: " + currentSolver.getTotalCost());
            statusLabel.setForeground(new Color(0, 128, 0));
            mazePanel.repaint();
        }
    }

    private void updateStatusLabel() {
        statusLabel.setForeground(Color.BLACK);

        if (currentSolver instanceof GeneticAlgorithmVisualizer) {
            statusLabel.setText(String.format("Running: %s | Best : %d",
                    currentSolver.getName(), currentSolver.getTotalCost()));
        } else {

            statusLabel.setText(String.format("Running: %s | OpenSet: %d | ClosedSet: %d",
                    currentSolver.getName(),
                    currentSolver.getOpenSet().size(),
                    currentSolver.getClosedSet().size()));
        }
    }
}