package com.cookwise2.utils;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.ArrayList;

public class RecipePost implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private String description;
    private ArrayList<String> groceries;
    private String ownerUid;
    private String ownerNickname;
    private long createdAtMillis;
    private String postId;
    public RecipePost() {}

    public RecipePost(String postId,String title,String description,  ArrayList<String> groceries, String ownerUid, String ownerNickname,Timestamp createdAt) {
        this.title = title;
        this.groceries = groceries;
        this.description = description;
        this.ownerUid = ownerUid;
        this.ownerNickname = ownerNickname;
        this.createdAtMillis = createdAt.toDate().getTime();
        this.postId = postId;
    }


    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Timestamp getCreatedAt() { return new Timestamp(new java.util.Date(createdAtMillis)); }
    public void setCreatedAt(Timestamp createdAt) { this.createdAtMillis = createdAt.toDate().getTime(); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }
    public String getOwnerNickname() { return ownerNickname; }
    public void setOwnerNickname(String ownerNickname) { this.ownerNickname = ownerNickname; }
    public ArrayList<String> getGroceries(){return groceries;}
    public String getPostId(){return postId;}
}