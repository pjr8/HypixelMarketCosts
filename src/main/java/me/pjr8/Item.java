package me.pjr8;

import lombok.Data;

@Data
public class Item {
    private String name;
    private int amount;

    public Item(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }
}
