package com.example.chatterbot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.chatterbot.view.LoginActivityViewModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity
{
    //Vistas
    private ProgressBar pbLoading;
    private LinearLayout formLinearLayout;
    private TextInputEditText tietEmail, tietPassword;
    private CheckBox cbRememberLogin;
    private Button btLogin, btRegister;

    //Vars
    private LoginActivityViewModel loginActivityViewModel;
    private String email, password;
    private boolean remember, activityLoading;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setActivityViews();
        setActivityVars(savedInstanceState);
        setActivityLoading(activityLoading);
        init();
        assignEvents();
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState)
    {
        tietEmail.setText(savedInstanceState.getString("email"));
        tietPassword.setText(savedInstanceState.getString("password"));
        cbRememberLogin.setChecked(savedInstanceState.getBoolean("remember"));
        setActivityLoading(savedInstanceState.getBoolean("activityLoading"));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState)
    {
        outPersistentState.putString("email", tietEmail.getText().toString());
        outPersistentState.putString("password", tietPassword.getText().toString());
        outPersistentState.putBoolean("remember", cbRememberLogin.isChecked());
        outPersistentState.putBoolean("activityLoading", activityLoading);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    private void setActivityVars(Bundle bundle)
    {
        loginActivityViewModel = ViewModelProviders.of(this).get(LoginActivityViewModel.class);
        if(bundle != null)
        {
            email = bundle.getString("email");
            password = bundle.getString("password");
            remember = bundle.getBoolean("remember");
            activityLoading = bundle.getBoolean("activityLoading");
        }
        else
        {
            email = "";
            password = "";
            remember = false;
            activityLoading = false;
        }
    }

    private void setActivityViews()
    {
        pbLoading = findViewById(R.id.pbLoading);
        formLinearLayout = findViewById(R.id.formLinearLayout);
        tietEmail = findViewById(R.id.tietEmail);
        tietPassword = findViewById(R.id.tietPassword);
        cbRememberLogin = findViewById(R.id.cbRememberLogin);
        btLogin = findViewById(R.id.btLogin);
        btRegister = findViewById(R.id.btRegister);
    }

    private void setActivityLoading(boolean toggle)
    {
        activityLoading = toggle;
        if(activityLoading)
        {
            pbLoading.setVisibility(View.VISIBLE);
            formLinearLayout.setVisibility(View.GONE);
        }
        else
        {
            pbLoading.setVisibility(View.GONE);
            formLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    private void init()
    {
        if(!loginActivityViewModel.isStarted())
        {
            loginActivityViewModel.setStarted(true);

            SharedPreferences sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
            remember = sharedPreferences.getBoolean("remember", false);
            if(remember)
            {
                email = sharedPreferences.getString("email", "");
                password = sharedPreferences.getString("password", "");
                tryLogin();
            }
        }
    }

    private void assignEvents()
    {
        btRegister.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                email = tietEmail.getText().toString();
                password = tietPassword.getText().toString();
                remember = cbRememberLogin.isChecked();
                if((email == null || email.isEmpty() || email.length() <= 0) || (password == null || password.isEmpty() || password.length() <= 0)) Toast.makeText(LoginActivity.this, LoginActivity.this.getText(R.string.toastInvalidData), Toast.LENGTH_LONG).show();
                else
                {
                    setActivityLoading(true);
                    loginActivityViewModel.getFirebaseAuth().createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task)
                        {
                            if(task.isSuccessful())
                            {
                                FirebaseUser user = loginActivityViewModel.getFirebaseAuth().getCurrentUser();
                                if(remember)
                                {
                                    SharedPreferences sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putBoolean("remember", remember);
                                    editor.putString("email", email);
                                    editor.putString("password", password);
                                    editor.apply();
                                }
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("user", user);
                                startActivity(intent);
                                finish();
                            }
                            else
                            {
                                setActivityLoading(false);
                                Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });

        btLogin.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                email = tietEmail.getText().toString();
                password = tietPassword.getText().toString();
                remember = cbRememberLogin.isChecked();
                tryLogin();
            }
        });
    }

    private void tryLogin()
    {
        if((email == null || email.isEmpty() || email.length() <= 0) || (password == null || password.isEmpty() || password.length() <= 0)) Toast.makeText(LoginActivity.this, LoginActivity.this.getText(R.string.toastInvalidData), Toast.LENGTH_LONG).show();
        else
        {
            setActivityLoading(true);
            loginActivityViewModel.getFirebaseAuth().signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
            {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task)
                {
                    if(task.isSuccessful())
                    {
                        FirebaseUser user = loginActivityViewModel.getFirebaseAuth().getCurrentUser();
                        if(remember)
                        {
                            SharedPreferences sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("remember", remember);
                            editor.putString("email", email);
                            editor.putString("password", password);
                            editor.apply();
                        }
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("user", user);
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        setActivityLoading(false);
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
}
