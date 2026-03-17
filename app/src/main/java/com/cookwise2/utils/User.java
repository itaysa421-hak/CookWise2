package com.cookwise2.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private String uid;
    private String nickname;
    private List<String> savedPosts; // רשימה של ID של פוסטים

    public User() {} // חובה עבור Firestore

    public User(String uid, String nickname) {
        this.uid = uid;
        this.nickname = nickname;
        this.savedPosts = new ArrayList<>();
    }

    // Getters & Setters
    public String getUid() { return uid; }
    public String getNickname() { return nickname; }
    public List<String> getSavedPosts() { return savedPosts; }
    public void setSavedPosts(List<String> savedPosts) { this.savedPosts = savedPosts; }
}