package model;

public class Point {

    /**
     * Direct access
     */
    public Integer x = 0;

    /**
     * Access via accessors
     */
    private Integer y = 0;

    private String label;

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
