package com.example.chatterbot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.chatterbot.apibot.ChatterBot;
import com.example.chatterbot.apibot.ChatterBotFactory;
import com.example.chatterbot.apibot.ChatterBotSession;
import com.example.chatterbot.apibot.ChatterBotType;
import com.example.chatterbot.data.Message;
import com.example.chatterbot.view.MainActivityViewModel;
import com.example.chatterbot.view.RecyclerViewAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_CODE_STT = 1;

    private MainActivityViewModel mainActivityViewModel;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private TextToSpeech mTts;

    private ProgressBar pbLoading;
    private ConstraintLayout clActivity;
    private EditText etText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        init();
    }

    private void init()
    {
        //View Models
        mainActivityViewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        //Views
        pbLoading = findViewById(R.id.pbLoading);
        clActivity = findViewById(R.id.clActivity);

        //TTS
        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {

            }
        });

        //Recycler
        recyclerView = findViewById(R.id.recyclerView);
        recyclerViewAdapter = mainActivityViewModel.getRecyclerViewAdapter();
        recyclerViewAdapter.setOnMessageClickListener(new RecyclerViewAdapter.OnMessageClickListener()
        {
            @Override
            public void onClick(final Message message, LinearLayout linearLayout)
            {
                PopupMenu popup = new PopupMenu(MainActivity.this, linearLayout);
                popup.getMenuInflater().inflate(R.menu.message, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem item)
                    {
                        switch(item.getItemId())
                        {
                            case R.id.message_info:
                            {
                                new MaterialAlertDialogBuilder(MainActivity.this)
                                        .setTitle(getString(R.string.message_info_title))
                                        .setMessage(getString(R.string.message_info_date) + " " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(message.getDate()))
                                        .setPositiveButton(getString(R.string.message_info_close), null)
                                        .show();
                                break;
                            }
                            case R.id.message_listen:
                            {
                                mTts.speak(message.getMessage(), TextToSpeech.QUEUE_FLUSH, null);
                                break;
                            }
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());

        //Boton enviar
        etText = findViewById(R.id.etText);
        Button btSend = findViewById(R.id.btSend);
        btSend.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String text = etText.getText().toString();
                if(text.length() > 0 && !mainActivityViewModel.isWaitingResponse())
                {
                    addMessage(true, text, null);
                    etText.setText("");

                    mainActivityViewModel.setWaitingResponse(true);
                    mainActivityViewModel.translate("auto-detect", text, "en");
                }
            }
        });

        //Cuando se traduce algo
        mainActivityViewModel.setOnTranslationResultListener(new MainActivityViewModel.OnTranslationResult()
        {
            @Override
            public void OnTranslationResult(boolean ok, String text, String originalText, String countryCode)
            {
                if(ok)
                {
                    if(mainActivityViewModel.isWaitingBotTranslation())
                    {
                        addMessage(false, text, originalText);
                        mainActivityViewModel.setWaitingResponse(false);
                        mainActivityViewModel.setWaitingBotTranslation(false);
                    }
                    else
                    {
                        mainActivityViewModel.setTranslateCountryCode(countryCode);
                        mTts.setLanguage(new Locale(countryCode));
                        new BotChat().execute(text);
                    }
                }
                else
                {
                    addMessage(false, "¡Error!", "");
                    mainActivityViewModel.setWaitingResponse(false);
                    mainActivityViewModel.setWaitingBotTranslation(false);
                }
            }
        });

        //Started
        if(!mainActivityViewModel.isStarted())
        {
            mainActivityViewModel.setUser((FirebaseUser) getIntent().getExtras().get("user"));
            mainActivityViewModel.setReference(mainActivityViewModel.getDatabase().getReference("user/" + mainActivityViewModel.getUser().getUid()));
            mainActivityViewModel.setStarted(true);
            setActivityLoading(true);
        }
        else setActivityLoading(mainActivityViewModel.isLoading());

        //Message DB Event
        mainActivityViewModel.getReference().addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(mainActivityViewModel.isLoading())
                {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
                    for(DataSnapshot dateKey : dataSnapshot.getChildren())
                    {
                        Date date = null;
                        try
                        {
                            date = simpleDateFormat.parse(dateKey.getKey());
                            mainActivityViewModel.setCurrentDate(date);
                            recyclerViewAdapter.addMessage(new Message(date));

                            for(DataSnapshot messageKey : dateKey.getChildren())
                            {
                                Message message = messageKey.getValue(Message.class);
                                recyclerViewAdapter.addMessage(message);
                            }
                        }
                        catch(ParseException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    setActivityLoading(false);
                    recyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
                Toast.makeText(MainActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                Log.v("ASD", "User: " + mainActivityViewModel.getUser().getEmail());
            }
        });
    }

    private void addMessage(boolean outcoming, String text, String textEn)
    {
        Date date = new Date();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        if(mainActivityViewModel.getCurrentDate() == null || !simpleDateFormat.format(date).equals(simpleDateFormat.format(mainActivityViewModel.getCurrentDate())))
        {
            mainActivityViewModel.setCurrentDate(date);
            recyclerViewAdapter.addMessage(new Message(date));
        }

        Message message;
        if(textEn == null) message = new Message(outcoming, text, date);
        else message = new Message(outcoming, text, textEn, date);

        recyclerViewAdapter.addMessage(message);
        recyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());

        if(!outcoming && mainActivityViewModel.isTts())
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);

        //DB
        Map<String, Object> map = new HashMap<>();
        String key = mainActivityViewModel.getReference().push().getKey();
        map.put(simpleDateFormat.format(message.getDate()) + "/" + key, message.toMap());
        mainActivityViewModel.getReference().updateChildren(map);
    }

    @Override
    protected void onDestroy()
    {
        if(mTts != null)
        {
            mTts.stop();
            mTts.shutdown();
        }
        mainActivityViewModel.getDatabase().goOffline();
        super.onDestroy();
    }

    private class BotChat extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... strings)
        {
            return chat(strings[0]);
        }

        @Override
        protected void onPostExecute(String s)
        {
            mainActivityViewModel.translate("en", s, mainActivityViewModel.getTranslateCountryCode());
            mainActivityViewModel.setWaitingBotTranslation(true);
        }
    }

    private String chat(String message)
    {
        String response = "";
        try
        {
            ChatterBotFactory factory = new ChatterBotFactory();
            ChatterBot bot = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
            ChatterBotSession botSession = bot.createSession();
            response = botSession.think(message);
        }
        catch(Exception e)
        {
            response = "Ha ocurrido un error ¿tienes conexión a internet?";
        }
        return response;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        switch(id)
        {
            case R.id.action_tts:
            {
                mainActivityViewModel.setTts(!mainActivityViewModel.isTts());
                if(mainActivityViewModel.isTts()) Toast.makeText(this, R.string.toastTts, Toast.LENGTH_SHORT).show();
                else Toast.makeText(this, R.string.toastTtsOff, Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.action_stt:
            {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

                if(intent.resolveActivity(getPackageManager()) != null) startActivityForResult(intent, REQUEST_CODE_STT);
                else Toast.makeText(this, R.string.toastNoStt, Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.action_disconnect:
            {
                SharedPreferences sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("remember");
                editor.remove("email");
                editor.remove("password");
                editor.apply();

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_STT && resultCode == RESULT_OK && data != null)
        {
            ArrayList<String> results;
            results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            etText.setText(results.get(0));
        }
    }

    private void setActivityLoading(boolean toggle)
    {
        mainActivityViewModel.setLoading(toggle);
        if(mainActivityViewModel.isLoading())
        {
            pbLoading.setVisibility(View.VISIBLE);
            clActivity.setVisibility(View.INVISIBLE);
        }
        else
        {
            pbLoading.setVisibility(View.INVISIBLE);
            clActivity.setVisibility(View.VISIBLE);
        }
    }
}
