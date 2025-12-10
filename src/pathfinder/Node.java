package pathfinder;

public class Node implements Comparable<Node>{
    int r,c;
    int times;
    Node from;
    public Node(int r,int c,int times,Node from){
        this.r = r;
        this.c = c;
        this.times = times;
        this.from = from;
    }

    @Override
    public int compareTo(Node other){
        return Integer.compare(this.times, other.times);
    }
}
