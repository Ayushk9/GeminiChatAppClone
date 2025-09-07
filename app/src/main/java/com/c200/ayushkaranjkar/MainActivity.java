package com.c200.ayushkaranjkar;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private EditText promptEditText;
    private TextView responseTextView;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(adapter);

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
//        responseTextView = findViewById(R.id.displayTextView);
        progressBar = findViewById(R.id.progressBar);
        String apiKey = com.c200.ayushkaranjkar.BuildConfig.GEMINI_API_KEY;

        // Create GenerativeModel
        GenerativeModel generativeModel = new GenerativeModel("gemini-1.5-flash",
                apiKey);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        ImageButton micButton = findViewById(R.id.micButton);

        micButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
            } catch (Exception e) {
                Toast.makeText(this, "Speech input is not supported on this device", Toast.LENGTH_SHORT).show();
            }
        });

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                String string = getString(R.string.aistring);
                responseTextView.setText(TextFormatter.getBoldSpannableText(string));
                return;
            }
            messageList.add(new Message(prompt, true));
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);

            promptEditText.setText("");
            progressBar.setVisibility(VISIBLE);
            generativeModel.generateContent(prompt, new Continuation<GenerateContentResponse>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object result) {
                    runOnUiThread(() -> progressBar.setVisibility(GONE));

                    try {
                        if (result instanceof kotlin.Result.Failure) {
                            Throwable error = ((kotlin.Result.Failure) result).exception;
                            runOnUiThread(() -> {
                                messageList.add(new Message("⚠️ API Error: " + error.getMessage(), false));
                                adapter.notifyItemInserted(messageList.size() - 1);
                                recyclerView.scrollToPosition(messageList.size() - 1);
                            });
                            return;
                        }

                        // Success case
                        GenerateContentResponse response = (GenerateContentResponse) result;
                        String responseString = response.getText();
                        Log.d("GeminiAPI", "Response object: " + result.toString());

                        runOnUiThread(() -> {
                            String safeResponse = (responseString == null || responseString.trim().isEmpty())
                                    ? "⚠️ No text received from Gemini"
                                    : responseString;
                            messageList.add(new Message(safeResponse, false));
                            adapter.notifyItemInserted(messageList.size() - 1);
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            messageList.add(new Message("⚠️ Exception: " + e.getMessage(), false));
                            adapter.notifyItemInserted(messageList.size() - 1);
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        });
                    }
                }
            });
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                promptEditText.setText(result.get(0));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Mic permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Mic permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}