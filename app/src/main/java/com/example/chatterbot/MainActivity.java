package com.example.chatterbot;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chatterbot.apibot.ChatterBot;
import com.example.chatterbot.apibot.ChatterBotFactory;
import com.example.chatterbot.apibot.ChatterBotSession;
import com.example.chatterbot.apibot.ChatterBotType;
import com.example.chatterbot.data.Message;
import com.example.chatterbot.view.MainActivityViewModel;
import com.example.chatterbot.view.RecyclerViewAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_CODE_STT = 1;

    private MainActivityViewModel mainActivityViewModel;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private TextToSpeech mTts;

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
        recyclerView = findViewById(R.id.recyclerView);

        recyclerViewAdapter = mainActivityViewModel.getRecyclerViewAdapter();

        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());

        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {

            }
        });

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
                    addMessage(true, text);
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
            public void OnTranslationResult(boolean ok, String text, String countryCode)
            {
                if(ok)
                {
                    if(mainActivityViewModel.isWaitingBotTranslation())
                    {
                        addMessage(false, text);
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
                    addMessage(false, "¡Error!");
                    mainActivityViewModel.setWaitingResponse(false);
                    mainActivityViewModel.setWaitingBotTranslation(false);
                }
            }
        });
    }

    private void addMessage(boolean outcoming, String text)
    {
        recyclerViewAdapter.addMessage(new Message(outcoming, text, mainActivityViewModel.getShortTime()));
        recyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());

        if(!outcoming && mainActivityViewModel.isTts())
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    protected void onDestroy()
    {
        if(mTts != null)
        {
            mTts.stop();
            mTts.shutdown();
        }
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
}
