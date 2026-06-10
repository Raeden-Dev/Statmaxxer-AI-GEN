package com.raeden.ors_to_do.dependencies.models;

import java.io.Serializable;

/** A named, fully isolated data profile. Each profile is backed by its own database file. */
public class Profile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;

    public Profile() { }

    public Profile(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() { return name; }
}
