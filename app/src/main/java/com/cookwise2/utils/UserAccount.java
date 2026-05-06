package com.cookwise2.utils;

public class UserAccount {
    private String uid;
    private String nickname;

    public UserAccount() {} // חובה עבור Firebase

    public UserAccount(String uid, String nickname) {
        this.uid = uid;
        this.nickname = nickname;
    }

    public String getUid() { return uid; }
    public String getNickname() { return nickname; }

    public String getProfileImageUrl() {
        return "https://wkxapzreydqpqsthggzk.supabase.co/storage/v1/object/public/my-bucket/images/profile-pics/" + uid + ".jpg";
    }
}