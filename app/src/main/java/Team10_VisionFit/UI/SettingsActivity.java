package Team10_VisionFit.UI;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.teamten.visionfit.R;

import Team10_VisionFit.Backend.firebaseAuthentication.Login;
import Team10_VisionFit.MainActivity;
import Team10_VisionFit.PoseDetector.LivePreviewActivity;
import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {
    FirebaseAuth auth;
    SwitchCompat switchMode;
    boolean nightMode;
    private CircleImageView settingsImageView;
    EditText emailBox, editTextPwdCurr, editTextPwdNew, editTextPwdConfirmNew;
    SharedPreferences sharedPreferences;
    TextView displayNameTextView;
    SharedPreferences.Editor editor;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        displayNameTextView = findViewById(R.id.username_settings);
        settingsImageView = findViewById(R.id.userImage);
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            String email=user.getEmail();

            DocumentReference userRef = firestore.collection("users").document(userId);

            // Fetch user data from Firestore
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        // Get profile picture URL and display name
                        String profilePicUrl = document.getString("photo_url");
                        String displayName = document.getString("username");

                        displayNameTextView.setText(displayName);

                        if (profilePicUrl != null) {
                            // Load profile picture using Picasso with error handling
                            Picasso.get().load(profilePicUrl).into(settingsImageView, new Callback() {
                                @Override
                                public void onSuccess() {
                                    // Image loaded successfully, do nothing
                                }

                                @Override
                                public void onError(Exception e) {
                                    // Error loading image, set default image
                                    settingsImageView.setImageResource(R.drawable.icon_profile);
                                }
                            });
                        } else {
                            // User doesn't have a profile picture, set default image
                            settingsImageView.setImageResource(R.drawable.icon_profile);
                        }
                    }
                }
            });
        }

        switchMode = findViewById(R.id.switchMode);

        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        nightMode = sharedPreferences.getBoolean("nightMode", false);

        if (nightMode) {
            switchMode.setChecked(true);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        switchMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nightMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    editor = sharedPreferences.edit();
                    editor.putBoolean("nightMode", false);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    editor = sharedPreferences.edit();
                    editor.putBoolean("nightMode", true);
                }
                editor.apply();
            }
        });

        SwitchCompat notificationSwitch = findViewById(R.id.switchNotifications);

        // Get the last switch state from SharedPreferences
        boolean isNotificationEnabled = getNotificationSwitchState();

        // Set the switch state
        notificationSwitch.setChecked(isNotificationEnabled);

        // Set a listener to handle the switch state changes
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update the switch state in SharedPreferences
            setNotificationSwitchState(isChecked);
        });

        final ImageView settingsToProfile = (ImageView) findViewById(R.id.settingGoProfile);
        settingsToProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        final ImageView settingstoChangePassword = (ImageView) findViewById(R.id.settingGoResetPassword);
        settingstoChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
                builder.setView(dialogView);
                AlertDialog dialog = builder.create();

                emailBox = dialogView.findViewById(R.id.emailBox);
                editTextPwdCurr = dialogView.findViewById(R.id.oldPasswordBox);
                editTextPwdNew = dialogView.findViewById(R.id.newPasswordBox);
                editTextPwdConfirmNew = dialogView.findViewById(R.id.confirmNewPasswordBox);
                auth = FirebaseAuth.getInstance();
                FirebaseUser firebaseUser = auth.getCurrentUser();
                dialogView.findViewById(R.id.btnChangePassword).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String userEmail = emailBox.getText().toString();
                        String oldPassword = editTextPwdCurr.getText().toString();
                        String newPassword = editTextPwdNew.getText().toString();
                        String confirmNewPassword = editTextPwdConfirmNew.getText().toString();

                        if (TextUtils.isEmpty(userEmail) || !Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                            emailBox.setError("Valid email required");
                            return;
                        }

                        if (TextUtils.isEmpty(oldPassword)) {
                            editTextPwdCurr.setError("Old password required");
                            return;
                        }

                        if (TextUtils.isEmpty(newPassword)) {
                            editTextPwdNew.setError("New password required");
                            return;
                        }

                        if (newPassword.equals(oldPassword)) {
                            editTextPwdNew.setError("New password should be different from the old one");
                            return;
                        }

                        if (!newPassword.equals(confirmNewPassword)) {
                            editTextPwdConfirmNew.setError("Passwords don't match");
                            return;
                        }

                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        AuthCredential credential = EmailAuthProvider.getCredential(userEmail, oldPassword);
                        firebaseUser.reauthenticate(credential)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        firebaseUser.updatePassword(newPassword)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(SettingsActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                                        dialog.dismiss();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(SettingsActivity.this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(SettingsActivity.this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
                dialogView.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                }
                dialog.show();
            }
        });

        final ImageView settingsToAboutUs = (ImageView) findViewById(R.id.settingsGoAboutUs);
        settingsToAboutUs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), AboutUsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        final ImageView settingsToPrivacy = (ImageView) findViewById(R.id.settingsGoPrivacy);
        settingsToPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), PrivacyPolicyActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });


        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.bottom_settings);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.bottom_home:
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    Log.d("Button Check", "Home Button Clicked");
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                    return true;
                case R.id.bottom_settings:
                    startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                    Log.d("Button Check", "Settings Button Clicked");
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                    return true;
                case R.id.cameraButton:
                    startActivity(new Intent(getApplicationContext(), LivePreviewActivity.class));
                    Log.d("Button Check", "Camera Button Clicked");
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                    return true;
                case R.id.bottom_userProfile:
                    startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                    Log.d("Button Check", "Profile Button Clicked");
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                    return true;
                case R.id.bottom_logout:
                    Log.d("Button Check", "Logout Button Clicked");
                    customExitDialog();
                    return true;
            }
            return false;
        });
    }

    public  void customExitDialog()
    {
        // creating custom dialog
        final Dialog dialog = new Dialog(SettingsActivity.this);

        // setting content view to dialog
        dialog.setContentView(R.layout.logout_dialog_box);

        // getting reference of TextView
        TextView dialogButtonYes = (TextView) dialog.findViewById(R.id.textViewYes);
        TextView dialogButtonNo = (TextView) dialog.findViewById(R.id.textViewNo);

        // click listener for No
        dialogButtonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // dismiss the dialog
                dialog.dismiss();

            }
        });
        // click listener for Yes
        dialogButtonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // dismiss the dialog and exit the exit
                dialog.dismiss();

                Toast.makeText(getApplicationContext(), "Logout Successful", Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                SharedPreferences sharedPreferences = getSharedPreferences("loginref", MODE_PRIVATE);
                sharedPreferences.edit().clear().commit();
                finish();

            }
        });

        // show the exit dialog
        dialog.show();
    }

    private void setNotificationSwitchState(boolean isChecked) {
        // Save the switch state to SharedPreferences
        // Replace "notificationSwitchState" with your preference key
        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("notificationSwitchState", isChecked)
                .apply();
    }

    private boolean getNotificationSwitchState() {
        // Retrieve the last switch state from SharedPreferences
        // Replace "notificationSwitchState" with your preference key
        return getSharedPreferences("MyPrefs", MODE_PRIVATE)
                .getBoolean("notificationSwitchState", true); // Default value is true
    }

    @Override
    public void onBackPressed() {
        // Do nothing (disable back button functionality)
    }
}