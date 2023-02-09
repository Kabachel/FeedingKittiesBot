package com.example.model;

import javax.persistence.*;

@Entity(name = "cats")
public class Cat {

    @Id
    @GeneratedValue
    private Long catId;

    @ManyToOne
    private User user;

    private String name;

    private int gramsPerDay;

    private int feedPerDay;

    private int currentFeed = 0;

    public Long getCatId() {
        return catId;
    }

    public void setCatId(Long catId) {
        this.catId = catId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGramsPerDay() {
        return gramsPerDay;
    }

    public void setGramsPerDay(int gramsPerDay) {
        this.gramsPerDay = gramsPerDay;
    }

    public int getFeedPerDay() {
        return feedPerDay;
    }

    public void setFeedPerDay(int feedPerDay) {
        this.feedPerDay = feedPerDay;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getCurrentFeed() {
        return currentFeed;
    }

    public void setCurrentFeed(int currentFeed) {
        this.currentFeed = currentFeed;
    }

    @Override
    public String toString() {
        return "Cat{" +
                "catId=" + catId +
                ", user=" + user +
                ", name='" + name + '\'' +
                ", gramsPerDay=" + gramsPerDay +
                ", feedPerDay=" + feedPerDay +
                '}';
    }
}
