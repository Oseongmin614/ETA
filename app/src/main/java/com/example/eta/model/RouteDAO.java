package com.example.eta.model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RouteDAO {
    private DatabaseReference mDatabase;
    private static RouteDAO instance;
    private RouteDAO() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (instance == null) {
            instance = this;
        }
    }
    public static RouteDAO getInstance() {
        if (instance == null) {
            return null;
        }
   return instance;
    }

    public void addPoint(){

    }
}
