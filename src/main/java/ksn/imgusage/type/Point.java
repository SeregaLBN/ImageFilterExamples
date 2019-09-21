package ksn.imgusage.type;

public class Point {

    public int x;
    public int y;

    public Point() {}

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "{ x=" + x + ", y=" + y + " }";
    }

}
