package some;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TextSample {

    // TODO: 16.01.18 Этот класс с "несколькими строками" будет примером для macros

    public final List<String> text = new ArrayList<>();

    /**
     * Добавление строки в текущую позицию
     *
     * @param s
     */
    public void add(String s) {
        String cur = text.remove(text.size() - 1);
        cur += s;
        text.add(cur);
    }

    /**
     * Удаление субстроки, начиная с текущей позиции.
     *
     * @param s
     */
    public void remove(String s) {
        String cur = text.remove(text.size() - 1);
        cur = cur.substring(0, cur.length() - s.length());
        text.add(cur);
    }

    /**
     * Добавление новой строки
     */
    public void addLine() {
        text.add("" + text.size() + ": ");
    }

    public void removeLine() {
        text.remove(text.size() - 1);
    }

    public void clear() {
        text.clear();
    }

    /**
     * Вывод "на печать"
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
        TextSample that = (TextSample) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }
}
