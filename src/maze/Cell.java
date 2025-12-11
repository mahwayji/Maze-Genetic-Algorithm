package maze;

public class Cell {
    public static final CellType CellType = null;
    public CellType type;
    public int value;

    public Cell(CellType type) {
        this.type = type;
    }

    public Cell(CellType type, int value) {
        this.type = type;
        this.value = value;
    }
}