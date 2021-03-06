package com.project.quora20;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.project.quora20.dto.logindtos.AccessTokenLoginResponse;
import com.project.quora20.dto.logindtos.CoAuthLoginRequest;
import com.project.quora20.dto.logindtos.FCMTokenRequest;
import com.project.quora20.dto.logindtos.FCMTokenResponse;
import com.project.quora20.dto.logindtos.JWTGetDetailsRequest;
import com.project.quora20.dto.logindtos.JWTGetDetailsResponse;
import com.project.quora20.dto.logindtos.UserDTO;
import com.project.quora20.retrofit.QuoraRetrofitService;
import com.project.quora20.retrofit.RetrofitLoginService;
import com.project.quora20.retrofit.RetrofitUsersInstance;

import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginMain extends AppCompatActivity {
    private SharedPreferences sp;
    private CoAuthLoginRequest coAuthLoginRequest = new CoAuthLoginRequest();
    private AccessTokenLoginResponse accessTokenLoginResponse = new AccessTokenLoginResponse();
    private JWTGetDetailsRequest jwtGetDetailsRequest = new JWTGetDetailsRequest();
    private JWTGetDetailsResponse jwtGetDetailsResponse = new JWTGetDetailsResponse();
    private UserDTO userDTO = new UserDTO();
    private String FCMToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_main);

        Button register = findViewById(R.id.login_register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent = new Intent(LoginMain.this, Register.class);
                startActivity(registerIntent);
            }
        });

        Button loginButton = findViewById(R.id.login_loginButton);
        sp = getSharedPreferences("LoginData", MODE_PRIVATE);
        String check = sp.getString("LoginCheck", "false");
        if (check.equals("false")) {
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    EditText user = findViewById(R.id.login_userName);
                    EditText pass = findViewById(R.id.login_password);
                    final String user1 = String.valueOf(user.getText());
                    final String pw = String.valueOf(pass.getText());
                    if (user1.length() == 0 || pw.length() == 0) {
                        Toast.makeText(getApplicationContext(), "Enter Login Details", Toast.LENGTH_SHORT).show();
                    } else {
                        InputMethodManager inputManager = (InputMethodManager)
                                getSystemService(Context.INPUT_METHOD_SERVICE);

                        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                                InputMethodManager.HIDE_NOT_ALWAYS);
                        coAuthLoginRequest.setEmail(user1);
                        coAuthLoginRequest.setPassword(pw);
                        QuoraRetrofitService quoraRetrofitService = RetrofitLoginService.getRetrofitInstance().create(QuoraRetrofitService.class);
                        Call<AccessTokenLoginResponse> call = quoraRetrofitService.loginUser(coAuthLoginRequest);
                        call.enqueue(new Callback<AccessTokenLoginResponse>() {
                            @Override
                            public void onResponse(Call<AccessTokenLoginResponse> call, Response<AccessTokenLoginResponse> response) {
                                if (response.code() == 200) {
                                    accessTokenLoginResponse = response.body();
                                    if (accessTokenLoginResponse != null) {
                                        sp = getSharedPreferences("LoginData", MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sp.edit();
                                        editor.putString("LoginCheck", "true");
                                        editor.putString("AccessToken", accessTokenLoginResponse.getAccessToken()).apply();
                                        editor.putString("TokenType", accessTokenLoginResponse.getTokenType()).apply();
                                        editor.commit();
                                        System.out.println("ACCESS TOKEN:" + accessTokenLoginResponse.getAccessToken());
                                        String accessToken = "Bearer " + accessTokenLoginResponse.getAccessToken();
                                        GetCoAuthUserDetailsCall(accessToken);
                                    }
                                } else {
                                    Snackbar snackbar = Snackbar.make(findViewById(R.id.login_layout), "Invalid Login Details", Snackbar.LENGTH_LONG);
                                    snackbar.show();
                                    System.out.println("OnResponse CUSTOM LOGIN PW MISMATCH");
                                }
                            }

                            @Override
                            public void onFailure(Call<AccessTokenLoginResponse> call, Throwable t) {
                                System.out.println("OnFailure Login" + t.getMessage());
                                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });
        } else {
            Intent LoggedIn = new Intent(LoginMain.this, MainActivity.class);
            startActivity(LoggedIn);
            finish();
        }

        //SKIP LOGIN
        Button skipSignIn = findViewById(R.id.skip);
        skipSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent skipSignInIntent = new Intent(LoginMain.this, MainActivity.class);
                int max = Integer.MAX_VALUE;
                int min = 0;
                Random random = new Random();
                int randomNumber = random.nextInt(max - min) + min;
                String guestUserId = String.valueOf(randomNumber);
                System.out.println(guestUserId + "LOGIN GUEST USERID");
                sp = getSharedPreferences("LoginData", MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("UserId", guestUserId).apply();
                editor.putString("LoginCheck", "false").apply();
                editor.commit();
                startActivity(skipSignInIntent);
            }
        });
    }

    private void GetCoAuthUserDetailsCall(String accessToken) {
        jwtGetDetailsRequest.setProvider(2);

        final QuoraRetrofitService quoraRetrofitService = RetrofitLoginService.getRetrofitInstance().create(QuoraRetrofitService.class);
        Call<JWTGetDetailsResponse> call = quoraRetrofitService.getUserDetails(accessToken, jwtGetDetailsRequest);
        call.enqueue(new Callback<JWTGetDetailsResponse>() {
            @Override
            public void onResponse(Call<JWTGetDetailsResponse> call, Response<JWTGetDetailsResponse> response) {
                System.out.println("OnResponse JWT GetUserDetails");
                System.out.println(response.code() + "CODE");
                if (response.body() != null) {
                    jwtGetDetailsResponse = response.body();
                    sp = getSharedPreferences("LoginData", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("UserId", String.valueOf(jwtGetDetailsResponse.getId())).apply();
                    editor.putString("Name", jwtGetDetailsResponse.getName()).apply();
                    editor.putString("Email", jwtGetDetailsResponse.getEmail()).apply();
                    editor.commit();
                    System.out.println(jwtGetDetailsResponse.getId() + "ROLE ID");
                    userDTO.setUserId(String.valueOf(jwtGetDetailsResponse.getId()));
                    System.out.println(jwtGetDetailsResponse.getEmail() + "ROLE EMAIL");
                    userDTO.setUserEmail(jwtGetDetailsResponse.getEmail());
                    System.out.println(jwtGetDetailsResponse.getName() + "ROLE NAME");
                    userDTO.setUserName(jwtGetDetailsResponse.getName());
                    System.out.println(jwtGetDetailsResponse.getName()+"JWTNAME:");
                    System.out.println(jwtGetDetailsResponse.getRole() + "ROLE");

                    if (jwtGetDetailsResponse.getRole() == null) {
                        QuoraRetrofitService quoraRetrofitService1 = RetrofitUsersInstance.getRetrofitInstance().create(QuoraRetrofitService.class);
                        Call<String> call1 = quoraRetrofitService1.registerOnQuora(userDTO);
                        call1.enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                if (response.body() != null) {
                                    System.out.println("OnResponse RegisterQuora");
                                    Intent registerQuora = new Intent(LoginMain.this, QuoraRegister.class);
                                    fcmTokenGeneration();
                                    startActivity(registerQuora);
                                    finish();
                                }
                            }

                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                System.out.println("OnFailure RegisterQuora:" + t.getMessage());
                                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Intent loginNow = new Intent(LoginMain.this, MainActivity.class);
                        startActivity(loginNow);
                        finish();
                    }
                } else {
                    System.out.println("RECEVING NULL FROM JWT");
                }
            }

            @Override
            public void onFailure(Call<JWTGetDetailsResponse> call, Throwable t) {
                System.out.println("OnFailure JWT GetUserDetails" + t.getMessage());
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fcmTokenGeneration() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("NOTIF", "getInstanceId failed", task.getException());
                            return;
                        }
                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        FCMToken = token;
                        // Log and toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Toast.makeText(LoginMain.this, "Notifications Enabled", Toast.LENGTH_SHORT).show();
                        FCMApiCall(FCMToken);
                    }
                });
    }

    public void FCMApiCall(String Token) {
        FCMTokenRequest fcmTokenRequest = new FCMTokenRequest();
        System.out.println("TOKEN" + Token);
        fcmTokenRequest.setFcmtoken(Token);

        String token = "Bearer " + sp.getString("AccessToken", "");
        System.out.println("JWT TOKEN" + token);
        QuoraRetrofitService quoraRetrofitService1 = RetrofitLoginService.getRetrofitInstance().create(QuoraRetrofitService.class);
        Call<FCMTokenResponse> fcmTokenResponseCall = quoraRetrofitService1.sendFCM(token, fcmTokenRequest);
        fcmTokenResponseCall.enqueue(new Callback<FCMTokenResponse>() {
            @Override
            public void onResponse(Call<FCMTokenResponse> call, Response<FCMTokenResponse> response) {
                System.out.println("OnResponse FCMToken");
            }

            @Override
            public void onFailure(Call<FCMTokenResponse> call, Throwable t) {
                System.out.println("OnFailure FCMToken" + t.getMessage());
            }
        });
    }
}
