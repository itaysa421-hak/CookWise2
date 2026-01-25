package com.cookwise2.utils;

import com.google.firebase.Timestamp;

import java.util.ArrayList;

public class RecipePost{
    private String title;
    private String description;
    private ArrayList<String> groceries;
    private String ownerUid;
    private String ownerNickname;
    private Timestamp createdAt;

    public RecipePost() {}

    public RecipePost(String title,String description,  ArrayList<String> groceries, String ownerUid, String ownerNickname,Timestamp createdAt) {
        this.title = title;
        this.groceries = groceries;
        this.description = description;
        this.ownerUid = ownerUid;
        this.ownerNickname = ownerNickname;
        this.createdAt = createdAt;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }
    public String getOwnerNickname() { return ownerNickname; }
    public void setOwnerNickname(String ownerNickname) { this.ownerNickname = ownerNickname; }
    public ArrayList<String> getGroceries(){return groceries;}
}