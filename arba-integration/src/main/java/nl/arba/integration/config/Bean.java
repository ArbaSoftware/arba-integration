package nl.arba.integration.config;

public class Bean {
    private String name;
    private String classname;

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getClassname() {
        return classname;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getName() {
        return name;
    }
}
