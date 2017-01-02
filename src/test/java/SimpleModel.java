/**
 * Created by jiangkun on 17/1/2.
 */
public class SimpleModel {
    String name;
    int id;

    public SimpleModel() {
    }

    public SimpleModel(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
