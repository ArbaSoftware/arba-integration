package nl.arba.integration.model;

import java.util.ArrayList;
import java.util.List;

public class Collection {
    private ArrayList<Object> items = new ArrayList<>();

    public void addItem(Object item) {
        items.add(item);
    }

    public List getItems() {
        return items;
    }
}
