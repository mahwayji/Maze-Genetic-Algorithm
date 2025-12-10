import java.awt.Point;
import java.util.ArrayList;

public class Maze {
    private Cell[][] grid;
    private Point start;
    private Point goal;

    public Maze(ArrayList<String> lines){
        ArrayList<ArrayList<Cell>> parsed = new ArrayList<>();

        for(String line: lines){
            parsed.add(parseLine(line));
        }

        int rows = parsed.size();
        int cols = parsed.get(0).size();

        grid = new Cell[rows][cols];

        for(int r = 0; r < rows; r++){
            for(int c = 0; c < cols; c++){
                Cell cell = parsed.get(r).get(c);
                grid[r][c] = cell;

                if(cell.type == CellType.START)
                    start = new Point(r, c);
                
                if (cell.type == CellType.GOAL)
                    goal = new Point(r, c);
            }
        }
    }

    // parse input function
    private ArrayList<Cell> parseLine(String line){
        ArrayList<Cell> list = new ArrayList<>();
        char[] lineChar = line.toCharArray();
        String temp = "";
        boolean inBracket = false;
        for(char c: lineChar){
            // start/ end of number cell
            if(c == '"'){
                // end of number
                if(inBracket){
                    list.add(new Cell(CellType.NUMBER, Integer.parseInt(temp)));
                    temp = "";
                    
                }
                // trigger from start and end
                inBracket = !inBracket;
            }
            // other
            else if(inBracket)
                temp += c;
            else if (c == '#')
                list.add(new Cell(CellType.WALL));
            else if (c == 'S')
                list.add(new Cell(CellType.START));
            else if (c == 'G')
                list.add(new Cell(CellType.GOAL));
        }

        return list;
    }

    // get values function
    public Cell get(int r, int c) { 
        return grid[r][c]; 
    }

    public int rows() { 
        return grid.length; 
    }

    public int cols() { 
        return grid[0].length; 
    }

    public Point getStart(){
        return start;
    }

    public Point getGoal(){
        return goal;
    }

    // show maze
    public void showMaze(){
        for(Cell[] cells: grid){
            for(Cell cell: cells){
                String cur = "";
                if(cell.type == CellType.GOAL)
                    cur = "G";
                if(cell.type == CellType.START)
                    cur = "S";
                if(cell.type == CellType.WALL)
                    cur = "#";
                if(cell.type == CellType.NUMBER)
                    cur = String.valueOf(cell.value);

                System.out.printf("%s ", cur);
            }
            System.out.println();
        }
    }
}