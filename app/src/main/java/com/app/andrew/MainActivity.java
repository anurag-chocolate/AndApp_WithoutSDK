package com.app.andrew;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.app.andrew.volley.AppController;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends AppCompatActivity {
    public final String TAG = "VOLLEY";
    String tag_json_obj = "json_obj_req";
    KProgressHUD progress_dialog;
    CallbackManager callbackManager;
    LoginButton loginButton;
    @InjectView(R.id.btn_login)
    TextView btnLogin;
    @InjectView(R.id.edt_email)
    EditText edtEmail;
    @InjectView(R.id.edt_password)
    EditText edtPassword;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.mingle.cash.vid",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:",
                        Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SharedPreferences mPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        boolean isLogin = mPrefs.getBoolean("isLogin", false);
        if (isLogin) {
            startActivity(new Intent(getApplicationContext(), DetailActivity.class));
            finish();
        }

        callbackManager = CallbackManager.Factory.create();

        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("email");

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                saveLoginSession("");
                Toast.makeText(getApplicationContext(), "Login success", Toast.LENGTH_LONG).show();
                startActivity(new Intent(getApplicationContext(), DetailActivity.class));
                finish();
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edtEmail.getText().toString().length() > 0 && edtPassword.getText().toString().length() > 0) {
                    login(edtEmail.getText().toString(), edtPassword.getText().toString());
                } else {
                    Toast.makeText(MainActivity.this, "Please give username and password", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveLoginSession(String key) {
        SharedPreferences mPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putBoolean("isLogin", true);
        prefsEditor.putString("key", key);
        prefsEditor.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void login(String username, String password) {
        String path  = "https://minglecash.com/api/v1/authenticate/";
        System.out.println(path);
        progress_dialog =   KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please wait")
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f)
                .show();
        progress_dialog.show();
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        params.put("password", password);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                path,  new JSONObject(params),
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        if (response.has("key")) {
                            String key = "";
                            int id = 0;
                            try {
                                key = response.getString("key");
                                id = response.getInt("id");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            saveLoginSession(key);
                            Toast.makeText(getApplicationContext(), "Login success", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(getApplicationContext(), DetailActivity.class));
                            finish();
                        } else {
                            Toast toast = Toast.makeText(MainActivity.this, "Invalid Credential", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                        progress_dialog.dismiss();

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(), "Invalid Credential", Toast.LENGTH_LONG).show();
                progress_dialog.dismiss();
            }
        }) {
            /**
             * Passing some request headers
             * */
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                return headers;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq, tag_json_obj);
    }
}
