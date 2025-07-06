import java.awt.*;

public class Node {
    String name;
    int x, y, r = 20;
    Node(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }
    boolean contains(Point p) {
        return p.distance(x, y) <= r;
    }
}