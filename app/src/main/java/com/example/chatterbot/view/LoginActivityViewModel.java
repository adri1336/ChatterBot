package com.example.chatterbot.view;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivityViewModel extends AndroidViewModel
{
    private boolean started;
    private FirebaseAuth firebaseAuth;

    public LoginActivityViewModel(@NonNull Application application)
    {
        super(application);
        started = false;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public boolean isStarted()
    {
        return started;
    }

    public void setStarted(boolean started)
    {
        this.started = started;
    }

    public FirebaseAuth getFirebaseAuth()
    {
        return firebaseAuth;
    }
}
