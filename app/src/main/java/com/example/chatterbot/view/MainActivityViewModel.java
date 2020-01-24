package com.example.chatterbot.view;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.chatterbot.repository.TranslatorRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;

public class MainActivityViewModel extends AndroidViewModel
{
    private TranslatorRepository translatorRepository;
    private OnTranslationResult onTranslationResultListener;
    private boolean waitingResponse;
    private boolean waitingBotTranslation;
    private String translateCountryCode = "es";

    private boolean tts;
    private Date currentDate;

    private RecyclerViewAdapter recyclerViewAdapter;

    private FirebaseUser user;
    private FirebaseDatabase database;
    private DatabaseReference reference;
    private boolean started;
    private boolean loading;

    public MainActivityViewModel(@NonNull Application application)
    {
        super(application);

        recyclerViewAdapter = new RecyclerViewAdapter();
        //recyclerViewAdapter.addMessage(new Message(false, "¡Hola!", getShortTime()));

        translatorRepository = new TranslatorRepository();
        translatorRepository.setOnTranslationResultListener(new TranslatorRepository.OnTranslationResult()
        {
            @Override
            public void OnTranslationResult(boolean ok, String text, String originalText, String countryCode)
            {
                if(onTranslationResultListener != null)
                    onTranslationResultListener.OnTranslationResult(ok, text, originalText, countryCode);
            }
        });

        tts = true;
        database = FirebaseDatabase.getInstance();
        try
        {
            database.setPersistenceEnabled(true);
        }
        catch(Exception e)
        {

        }
        started = false;
        loading = false;
    }

    public RecyclerViewAdapter getRecyclerViewAdapter()
    {
        return recyclerViewAdapter;
    }

    public String getTranslateCountryCode()
    {
        return translateCountryCode;
    }

    public void setTranslateCountryCode(String translateCountryCode)
    {
        this.translateCountryCode = translateCountryCode;
    }

    public void setOnTranslationResultListener(OnTranslationResult onTranslationResultListener)
    {
        this.onTranslationResultListener = onTranslationResultListener;
    }

    public void translate(String fromLang, String text, String to)
    {
        translatorRepository.translate(fromLang, text, to);
    }

    public boolean isWaitingResponse()
    {
        return waitingResponse;
    }

    public void setWaitingResponse(boolean waitingResponse)
    {
        this.waitingResponse = waitingResponse;
    }

    public interface OnTranslationResult
    {
        void OnTranslationResult(boolean ok, String text, String originalText, String countryCode);
    }

    public boolean isWaitingBotTranslation()
    {
        return waitingBotTranslation;
    }

    public void setWaitingBotTranslation(boolean waitingBotTranslation)
    {
        this.waitingBotTranslation = waitingBotTranslation;
    }

    public boolean isTts()
    {
        return tts;
    }

    public void setTts(boolean tts)
    {
        this.tts = tts;
    }

    public Date getCurrentDate()
    {
        return currentDate;
    }

    public void setCurrentDate(Date currentDate)
    {
        this.currentDate = currentDate;
    }

    public boolean isStarted()
    {
        return started;
    }

    public void setStarted(boolean started)
    {
        this.started = started;
    }

    public FirebaseUser getUser()
    {
        return user;
    }

    public void setUser(FirebaseUser user)
    {
        this.user = user;
    }

    public FirebaseDatabase getDatabase()
    {
        return database;
    }

    public DatabaseReference getReference()
    {
        return reference;
    }

    public void setReference(DatabaseReference reference)
    {
        this.reference = reference;
    }

    public boolean isLoading()
    {
        return loading;
    }

    public void setLoading(boolean loading)
    {
        this.loading = loading;
    }
}
