package pathfinder;

public class Node implements Comparable<Node>{
    public int r,c;
    public int pTimes;
    public int fTimes;
    public Node from;
    public Node(int r,int c,int pTimes,int fTimes,Node from){
        this.r = r;
        this.c = c;
        this.pTimes = pTimes;
        this.fTimes = fTimes;
        this.from = from;
    }

    @Override
    public int compareTo(Node other){
        return Integer.compare(this.fTimes, other.fTimes);
    }
}
