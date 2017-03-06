package com.oop.projectgroup10.studychatroom;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    Boolean loginOK = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button loginBtn = (Button) findViewById(R.id.loginBtn);
        Button registerBtn = (Button) findViewById(R.id.registerBtn);

        if (!hasInternetAccess()) {
            Toast.makeText(LoginActivity.this, "You have no internet Connection at the moment. Please try again later", Toast.LENGTH_LONG).show();

            loginBtn.setEnabled(false);
            registerBtn.setEnabled(false);
        } else {

            loginBtn.setEnabled(true);
            registerBtn.setEnabled(true);
        }
    }

    public void onBackPressed() {
        moveTaskToBack(true);
        System.exit(0);

    }

    public void register(View v) {
        Intent goToRegister = new Intent(v.getContext(), RegisterActivity.class);
        v.getContext().startActivity(goToRegister);
    }

    public void submitLogin(View v) {

        if (getUsername().isEmpty() || getPassword().isEmpty()) {
            Toast.makeText(this, "Please enter a valid username and password", Toast.LENGTH_LONG).show();
        } else {

            new SubmitLoginAndSignup(v.getContext(), this).execute("login", getUsername(), getPassword());
        }

    }

    public String getUsername() {
        EditText username = (EditText) findViewById(R.id.usernameL);

        return username.getText().toString();
    }

    public String getPassword() {
        EditText password = (EditText) findViewById(R.id.passwordL);
        return password.getText().toString();
    }

    //this function is to check if the user has internet access
    private boolean hasInternetAccess() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
