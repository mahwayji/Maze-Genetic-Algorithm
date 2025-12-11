package maze;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
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

    public int height() { 
        return grid.length; 
    }

    public int width() { 
        return grid[0].length; 
    }

    public Point getStart(){
        return start;
    }

    public Point getGoal(){
        return goal;
    }

    // show maze
    public void showMaze(List<Point> pathList){
        for(int r = 0;r < height();r++){
            for(int c = 0;c < width();c++){
                String cur = "";
                boolean isPath = false;
                for(Point p : pathList){
                    if(p.x == r && p.y == c){
                        isPath = true;
                        break;
                    }
                }
                Cell cell = grid[r][c];

                if(cell.type == CellType.START){
                    cur = "S";
                }
                else if(cell.type == CellType.GOAL){
                    cur = "G";
                }
                else if(isPath){
                    cur = "-1";
                }
                else if(cell.type == CellType.WALL){
                    cur = "#";
                }
                else{
                    cur = String.valueOf(cell.value);
                }
                System.out.printf("%-4s",cur);
            }
            System.out.println();
        }
}
    // helpers
    public int availableDirection(int y, int x){
        // we will mark the available direction as a bit map 4 bits
        // 0000 with mark as up down left right accordingly
        int available = 0;

        // up
        if(get(y-1, x).type != CellType.WALL)
            available += 8;

        // down
        if(get(y+1, x).type != CellType.WALL)
            available += 4;

        // left
        if(get(y, x-1).type != CellType.WALL)
            available += 2;

        // right
        if(get(y, x+1).type != CellType.WALL)
            available += 1;

        return available;
    }
}