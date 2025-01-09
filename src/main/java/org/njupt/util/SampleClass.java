package org.njupt.util;

import java.util.List;
import java.util.ArrayList;

public class SampleClass {

    private List<String> list;

    public SampleClass() {
        list = new ArrayList<>();
    }

    public void addItem(String item) {
        list.add(item);
    }
}
