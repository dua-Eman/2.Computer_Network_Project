public class Node {
    String name;
    double x, y, r = 20;
    Node(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }
    boolean contains(double x, double y) {
        return Math.hypot(this.x - x, this.y - y) <= r;
    }
}