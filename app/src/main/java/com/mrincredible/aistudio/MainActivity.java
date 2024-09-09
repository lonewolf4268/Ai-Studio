package com.mrincredible.aistudio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private LinearLayout outputContainer;
    private TextInputEditText inputEditText;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private TextRecognizer textRecognizer;

    private static final int REQUEST_CODE_SELECT_PHOTO = 100;
    private List<ChatMessage> chatHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        outputContainer = findViewById(R.id.outputContainer);
        inputEditText = findViewById(R.id.inputEditText);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                result -> {
                    if (result != null) {
                        processImage(result);
                    }
                }
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.uploadButton).setOnClickListener(this::uploadImage);
        findViewById(R.id.sendButton).setOnClickListener(this::sendMessage);

        askGoogleAi("Hi");
    }

    private void processImage(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            textRecognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text visionText) {
                            String recognizedText = visionText.getText();
                            askGoogleAi(recognizedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void uploadImage(View view) {
        if (isPhotoPickerPermissionGranted()) {
            launchPhotoPicker();
        } else {
            requestPhotoPickerPermission();
        }
    }

    public void sendMessage(View view) {
        String message = inputEditText.getText().toString();
        chatHistory.add(new ChatMessage("You", message));
        displayOutput(message, "You");
        askGoogleAi(buildContextualPrompt());
        inputEditText.setText("");
    }

    private String buildContextualPrompt() {
        StringBuilder promptBuilder = new StringBuilder();
        for (ChatMessage message : chatHistory) {
            promptBuilder.append(message.getSender()).append(": ")
                    .append(message.getMessage()).append("\n");
        }
        return promptBuilder.toString();
    }

    private void displayOutput(Object message, String sender) {
        TextView textView = new TextView(getApplicationContext());

        if (message instanceof Spanned) {
            textView.setText((Spanned) message);
        } else {
            textView.setText(sender + ":\t" + message);
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = 16;
        int padding = 12;
        layoutParams.setMargins(sender.equals("You") ? margin * 2 : margin, margin,
                sender.equals("You") ? margin : margin * 2, margin);
        textView.setLayoutParams(layoutParams);
        textView.setPadding(padding, padding, padding, padding);

        if (sender.equals("You")) {
            textView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            textView.setBackgroundResource(R.drawable.user_message_background);
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            textView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            textView.setBackgroundResource(R.drawable.ai_message_background);
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }

        outputContainer.addView(textView);
    }

    private boolean isPhotoPickerPermissionGranted() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPhotoPickerPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED},
                    REQUEST_CODE_SELECT_PHOTO);
        } else {
            // For Android versions before Tiramisu, no permission is needed
            launchPhotoPicker();
        }
    }

    private void launchPhotoPicker() {
        imagePickerLauncher.launch("image/*");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_SELECT_PHOTO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchPhotoPicker();
            } else {
                Toast.makeText(this, "Permission denied to access photos.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void askGoogleAi(String text) {
        // **IMPORTANT!** Replace with your actual API key
        String apiKey = "AIzaSyCYXYFBj0MdMIvTtHAe1CR0bUZClUnSyxE";
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
        // MODELS: gemini-1.5-pro, gemini-1.5-flash, gemini-1.0-pro, aqa
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content content = new Content.Builder()
                .addText(text)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                chatHistory.add(new ChatMessage("Ai", resultText));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        succesfull(resultText);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayOutput(t.getMessage(), "App");
                    }
                });
            }
        }, executor);
    }

    private void succesfull(String resultText) {
        Spanned formattedText = Formmatter.fromMarkdown(resultText);
        displayOutput(formattedText, "Ai");
    }

    class ChatMessage {
        private String sender;
        private String message;

        public ChatMessage(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        public String getSender() {
            return sender;
        }

        public String getMessage() {
            return message;
        }
    }
}