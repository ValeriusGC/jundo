package serialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PointForSer implements Serializable {

    public static class Item implements Serializable{
        @Override
        public String toString() {
            return "Item{" +
                    "i=" + i +
                    '}';
        }

        public Item(int i) {
            this.i = i;
        }

        public int i;
    }

    List<Item> items = new ArrayList<>();

    public PointForSer() {
        items.add(new Item(1));
        items.add(new Item(2));
        items.add(new Item(3));
    }

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

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PointForSer{" +
                "y=" + y +
                ", label='" + label + '\'' +
                ", items=" + items +
                '}';
    }
}
