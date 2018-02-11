package some;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sample class for macros example.
 */
public class DocSample {

    public final String id;
    public final List<String> text = new ArrayList<>();

    public DocSample(String id) {
        this.id = id;
    }

    /**
     * Adds substring to current position.
     *
     * @param s
     */
    public void add(String s) {
        String cur = text.remove(text.size() - 1);
        cur += s;
        text.add(cur);
    }

    /**
     * Removes substring from current position
     *
     * @param s
     */
    public void remove(String s) {
        String cur = text.remove(text.size() - 1);
        cur = cur.substring(0, cur.length() - s.length());
        text.add(cur);
    }

    /**
     * Adds new line
     */
    public void addLine() {
        text.add("");
    }

    public void removeLine() {
        text.remove(text.size() - 1);
    }

    public void clear() {
        text.clear();
    }

    public void reset(List<String> value) {
        text.clear();
        text.addAll(value);
    }

    /**
     * Prints.
     *
     * @return
     */
    public String print() {
        String out = "";
        for (String s :
                text) {
            out += s + "\n";
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocSample that = (DocSample) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }
}
