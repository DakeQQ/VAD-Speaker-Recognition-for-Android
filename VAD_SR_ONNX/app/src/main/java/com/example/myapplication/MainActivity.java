package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 48000;  // The sampling rate.
    private static final int FRAME_BUFFER_SIZE_MONO_48k = 2868;  //  2868 means 60ms per loop at 48kHz. Please edit the 'loop_time', which in the project.h, at the same time. The recommends range is 60 ~ 100ms, due to the model was exported with (1, 15, 560) static shape.
    private static final int FRAME_BUFFER_SIZE_MONO_16k = FRAME_BUFFER_SIZE_MONO_48k / 3;
    private static final int FRAME_BUFFER_SIZE_STEREO = FRAME_BUFFER_SIZE_MONO_48k * 2;
    private static final int vad_input_shape = 400;  // The same variable in the project.h, please modify at the same time.
    private static final int amount_of_mic = 1;  // The same variable in the project.h, please modify at the same time.///////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final int amount_of_mic_channel = amount_of_mic * 1;  // number of channels per mic, The same variable in the project.h, please modify at the same time.///////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final int all_record_size = amount_of_mic_channel * FRAME_BUFFER_SIZE_MONO_48k;
    private static final int all_record_size_16k = all_record_size / 3;  // 48k to 16k
    private static final int continue_threshold = 6;  // Set a continuous speaking threshold to avoid capturing undesired speech.
    private static final int vad_temp_save_limit = 100;
    private static final int print_threshold = 3;  // The waiting loop counts before sending the ASR results. Sometimes, the VAD is too sensitive/insensitive and cuts off speech prematurely.
    private static final int model_hidden_size_Res2Net = 512;
    private static int temp_stop = -1;
    private static int speaker_id = -1;
    private static int amount_of_speakers = 0; // The speakers who have permission are stored in the speaker_features.txt file.
    static final int font_size = 16;
    private static final float threshold_Speaker_Confirm = 0.42f;  //  You can print the max_score, which was calculated in the Compare_Similarity, to assign a appropriate value.
    private static final float threshold_Speaker_unknown = 0.9f;
    private static final int[] continue_active = new int[amount_of_mic_channel];
    private static final int[] print_count = new int[amount_of_mic_channel];
    private static final float[] negMean_vad = new float[vad_input_shape];
    private static final float[] invStd_vad = new float[vad_input_shape];
    private static final float[] score_pre_calculate_Speaker = new float[100];  // Edit it if you need.
    private static final long[] speaker_frequency = new long[score_pre_calculate_Speaker.length];
    private static final float[][] score_data_Speaker = new float[score_pre_calculate_Speaker.length][model_hidden_size_Res2Net];  // Pre-allocate the storage space for speaker confirm.
    private static final String file_name_negMean_vad = "negMean_vad.txt";
    private static final String file_name_invStd_vad = "invStd_vad.txt";
    private static final String file_name_speakers = "speaker_features.txt";
    private static final String file_name_speakers_names = "speaker_names.txt";
    private static final String file_name_speakers_frequency = "speaker_frequency.txt";
    @SuppressLint("SdCardPath")
    private static final String cache_path = "/data/user/0/com.example.myapplication/cache/";
    private static final String sr_turn_on = "启动说话人识别 \nSpeaker Recognition Starting";
    private static final String cleared = "已清除 Cleared";
    private static final String restart_success = "已重新启动 Restarted";
    private static final String restart_failed = "重新启动失败, 请退出此应用程序并手动重新启动。\nRestart Failed. Please exit this App and manually restart.";
    private static final String rename_id = "请用符号将ID和名称分开。例如：0.John 或 0/John。\nPlease use a sign to separate the ID and name. For example: 0.John or 0/John.";
    private static final String no_id = "此ID不存在。\nThis ID doesn't exist.";
    private static final String add_full = "因为记录已满，因此自动替换不常使用的说话人ID。\nDue to the record being full, automatically replace less frequently used speakers ID.";
    private static final String add_new_one = "添加新说话人\nAdding a new speaker ID: ";
    private static final String[] mic_owners = {"主驾驶-Master", "副驾驶-Co_Pilot", "左后座-Lefter", "右后座-Righter"};  // The corresponding name of the mic. This size must >= amount_of_mic.
    private static final String[] speaker_name = new String[score_pre_calculate_Speaker.length];
    private static final List<List<Integer>> speaker_history = new ArrayList<>();
    private static final List<List<float[]>> feature_history = new ArrayList<>();
    private static boolean recording = false;
    Button clearButton;
    Button startButton;
    @SuppressLint("StaticFieldLeak")
    static Button renameButton;
    Button restartButton;
    static TextInputEditText input_box;
    ImageView set_photo;
    @SuppressLint("StaticFieldLeak")
    static RecyclerView answerView;
    private static ChatAdapter chatAdapter;
    private static List<ChatMessage> messages;
    private static SR_Thread sr_Thread;
    private static MultiMicRecorder multiMicRecorder;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);

    static {
        System.loadLibrary("myapplication");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.example.myapplication.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        AssetManager mgr = getAssets();
        executorService.execute(()->{
            Load_Models_A(mgr,false,false,false,false,false,false);
            Load_Models_B(mgr,false,false,false,false,false,false);
        });
        executorService.execute(()->{
            Read_Assets(file_name_negMean_vad, mgr);
            Read_Assets(file_name_invStd_vad, mgr);
            Read_Assets(file_name_speakers_frequency, mgr);
            Pre_Process(negMean_vad, invStd_vad);
            for (float[] row : score_data_Speaker) {
                row[0] = -999.f;
            }
            Read_Assets(file_name_speakers_names, mgr);
            Read_Assets(file_name_speakers, mgr);
            for (int i = 0; i < score_pre_calculate_Speaker.length; i++) {
                if (score_data_Speaker[i][0] != -999.f) {
                    score_pre_calculate_Speaker[i] = (float) Math.sqrt(Dot(score_data_Speaker[i], score_data_Speaker[i]));
                    amount_of_speakers += 1;
                } else {
                    score_pre_calculate_Speaker[i] = 1.f;
                }
            }
        });
        input_box = findViewById(R.id.inputBox);
        set_photo = findViewById(R.id.role_image);
        clearButton = findViewById(R.id.clear);
        startButton = findViewById(R.id.start);
        renameButton = findViewById(R.id.rename);
        restartButton = findViewById(R.id.restart);
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        answerView = findViewById(R.id.result_text);
        answerView.setLayoutManager(new LinearLayoutManager(this));
        answerView.setAdapter(chatAdapter);
        set_photo.setImageResource(R.drawable.psyduck);
        for (int i = 0; i < amount_of_mic_channel; i++) {
            speaker_history.add(new ArrayList<>());
            feature_history.add(new ArrayList<>());
            print_count[i] = 0;
        }
        input_box.setText("");
        clearButton.setOnClickListener(v -> clearHistory());
        restartButton.setOnClickListener(v -> Restart());
        Rename();
        startButton.setOnClickListener(v -> {
            if (recording) {
                stop_SR();
                startButton.setText(R.string.start);
            } else {
                start_SR();
                startButton.setText(R.string.stop);
            }
        });
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        showToast(sr_turn_on, false);
    }
    private class SR_Thread extends Thread {
        @Override
        public void run() {
            while (recording) {
                float[] recordedData = multiMicRecorder.Read_PCM_Data();
                executorService.execute(()->{
                    long start_time = System.currentTimeMillis();
                    float[] result = Run_VAD_SR(FRAME_BUFFER_SIZE_MONO_16k, recordedData, temp_stop);
                    System.out.println("SR_Time_Cost: " + (System.currentTimeMillis() - start_time) + "ms");
                    int index_i = 0;
                    for (int i = 0; i < amount_of_mic_channel; i++) {
                        if (result[index_i] != -999.f) {
                            continue_active[i] += 1;
                            print_count[i] = 0;
                            int finalI = i;
                            if (amount_of_mic_channel > 1) {
                                float[] temp_array = new float[model_hidden_size_Res2Net];
                                System.arraycopy(result,0, temp_array, index_i,index_i + model_hidden_size_Res2Net);
                                int speaker = Compare_Similarity(temp_array);
                                runOnUiThread(() -> addHistory(ChatMessage.TYPE_USER,"说话中 Now talking: " + continue_active[finalI] + "\n来自 From_Mic: " + mic_owners[finalI]));
                                speaker_history.get(i).add(speaker);
                                feature_history.get(i).add(temp_array);
                            } else {
                                int speaker = Compare_Similarity(result);
                                runOnUiThread(() -> addHistory(ChatMessage.TYPE_USER,"说话中 Now talking: " + continue_active[finalI] + "\n来自 From_Mic: " + mic_owners[finalI]));
                                speaker_history.get(i).add(speaker);
                                feature_history.get(i).add(result);
                            }
                            if (speaker_history.get(i).size() > vad_temp_save_limit) {
                                speaker_history.get(i).remove(0);
                                feature_history.get(i).remove(0);
                            }
                        } else {
                            temp_stop = -1;
                            if (print_count[i] <= print_threshold) {
                                print_count[i] += 1;
                            } else {
                                print_count[i] = print_threshold;
                                if (!speaker_history.isEmpty()) {
                                    speaker_history.get(i).clear();
                                    feature_history.get(i).clear();
                                }
                            }
                        }
                        index_i += model_hidden_size_Res2Net;
                    }
                    for (int k = 0; k < amount_of_mic_channel; k++) {
                        if (print_count[k] >= print_threshold) {
                            if (continue_active[k] >= continue_threshold) {
                                int finalK = k;
                                if (amount_of_speakers > 0) {
                                    runOnUiThread(() -> continue_active[finalK] = 0);
                                    for (int i = continue_threshold / 2; i > 0; i--) {
                                        if (speaker_history.get(k).size() > i) {
                                            speaker_history.get(k).subList(0, i).clear();
                                            feature_history.get(k).subList(0, i).clear();
                                            break;
                                        }
                                    }
                                    for (int i = continue_threshold / 2; i > 0; i--) {
                                        if (speaker_history.get(k).size() > i) {
                                            speaker_history.get(k).subList(speaker_history.get(k).size() - i, speaker_history.get(k).size()).clear();
                                            feature_history.get(k).subList(feature_history.get(k).size() - i, feature_history.get(k).size()).clear();
                                            break;
                                        }
                                    }
                                    int[] count = new int[score_pre_calculate_Speaker.length + 1];
                                    for (int i = 0; i < speaker_history.get(k).size(); i++) {
                                        if (speaker_history.get(k).get(i) != -1) {
                                            count[speaker_history.get(k).get(i)] += 1;
                                        } else {
                                            count[score_pre_calculate_Speaker.length] += 1; // Put the unknown_id into the last index.
                                        }
                                    }
                                    int max_count = count[0];
                                    speaker_id = 0;
                                    for (int i = 1; i < score_pre_calculate_Speaker.length; i++) {
                                        if (count[i] > max_count) {
                                            max_count = count[i];
                                            speaker_id = i;
                                        }
                                    }
                                    if (count[score_pre_calculate_Speaker.length] > max_count) {  // Comparing whether the unknown_id frequency is larger than the known_id or not.
                                        if ((float) count[score_pre_calculate_Speaker.length] > threshold_Speaker_unknown * speaker_history.get(k).size()) {  // Sometimes, it is not really unknown. Therefore, use a ratio to confirm it again.
                                            speaker_id = -1;
                                        }
                                    }
                                } else {
                                    speaker_id = -1;
                                }
                                if (speaker_id != -1) {
                                    if (Objects.equals(speaker_name[speaker_id], "") | speaker_name[speaker_id] == null) {
                                        runOnUiThread(() -> addHistory(ChatMessage.TYPE_SERVER,"说话者 Speaker: " + speaker_id + "\nMic: " + mic_owners[finalK]));
                                    } else {
                                        runOnUiThread(() -> addHistory(ChatMessage.TYPE_SERVER,"说话者 Speaker: " + speaker_name[speaker_id] + "\nMic: " + mic_owners[finalK]));
                                    }
                                    speaker_frequency[speaker_id] += 1;
                                    float[] avg_features = new float[model_hidden_size_Res2Net];
                                    float count = 0.f;
                                    for (int i = 0; i < speaker_history.get(k).size(); i++) {
                                        if (speaker_history.get(k).get(i) == speaker_id) {
                                            for (int j = 0; j < model_hidden_size_Res2Net; j++) {
                                                avg_features[j] += feature_history.get(k).get(i)[j];
                                            }
                                            count += 1.f;
                                        }
                                    }
                                    if (count != 0.f) {
                                        count = 1.f / count;
                                        for (int i = 0; i < model_hidden_size_Res2Net; i++) {  // Update the recently features.
                                            score_data_Speaker[speaker_id][i] = (score_data_Speaker[speaker_id][i] + avg_features[i] * count) * 0.5f;
                                        }
                                        score_pre_calculate_Speaker[speaker_id] = (float) Math.sqrt(Dot(score_data_Speaker[speaker_id], score_data_Speaker[speaker_id]));
                                    }
                                } else {
                                    boolean first_unknown = true;
                                    for (int i = speaker_history.get(k).size() - 1; i > -1; i--) {
                                        if (speaker_history.get(k).get(i) == -1) {
                                            if (first_unknown) {
                                                first_unknown = false;
                                                if (amount_of_speakers < score_pre_calculate_Speaker.length) {
                                                    for (int j = 0; j < score_pre_calculate_Speaker.length; j++) {
                                                        if (score_data_Speaker[j][0] == -999.f) {
                                                            score_data_Speaker[j] = feature_history.get(k).get(i);
                                                            score_pre_calculate_Speaker[j] = (float) Math.sqrt(Dot(score_data_Speaker[j], score_data_Speaker[j]));
                                                            amount_of_speakers += 1;
                                                            speaker_frequency[j] = 1;
                                                            final int jj = j;
                                                            runOnUiThread(() -> addHistory(ChatMessage.TYPE_SYSTEM,add_new_one + jj));
                                                            if (amount_of_speakers == 1) {  // Handle the specific cast.
                                                                i = -1;
                                                            }
                                                            break;
                                                        }
                                                    }
                                                } else {
                                                    long min_value = speaker_frequency[score_pre_calculate_Speaker.length - 1];
                                                    int min_position = score_pre_calculate_Speaker.length - 1;
                                                    for (int j = score_pre_calculate_Speaker.length - 2; j > -1; j--) {  // Find the min and older one.
                                                        if (speaker_frequency[j] < min_value) {
                                                            min_value = speaker_frequency[j];
                                                            min_position = j;
                                                        }
                                                    }
                                                    speaker_frequency[min_position] = 1;
                                                    score_data_Speaker[min_position] = feature_history.get(k).get(i);
                                                    score_pre_calculate_Speaker[min_position] = (float) Math.sqrt(Dot(score_data_Speaker[min_position], score_data_Speaker[min_position]));
                                                    amount_of_speakers += 1;
                                                    final int m = min_position;
                                                    runOnUiThread(()-> {
                                                        if (Objects.equals(speaker_name[m], "") | speaker_name[m] == null) {
                                                            addHistory(ChatMessage.TYPE_SYSTEM, add_full + m + "\n\n" + add_new_one + m);
                                                        } else {
                                                            addHistory(ChatMessage.TYPE_SYSTEM, add_full + speaker_name[m] + "\n\n" + add_new_one + m);
                                                        }
                                                    });
                                                }
                                            } else {
                                                int speaker = Compare_Similarity(feature_history.get(k).get(i));
                                                if (speaker != -1) {
                                                    for (int j = 0; j < model_hidden_size_Res2Net; j++) {
                                                        score_data_Speaker[speaker][j] = (score_data_Speaker[speaker][j] + feature_history.get(k).get(i)[j]) * 0.5f;
                                                    }
                                                    score_pre_calculate_Speaker[speaker] = (float) Math.sqrt(Dot(score_data_Speaker[speaker], score_data_Speaker[speaker]));
                                                } else {
                                                    if (amount_of_speakers < score_pre_calculate_Speaker.length) {
                                                        for (int j = 0; j < score_pre_calculate_Speaker.length; j++) {
                                                            if (score_data_Speaker[j][0] == -999.f) {
                                                                score_data_Speaker[j] = feature_history.get(k).get(i);
                                                                score_pre_calculate_Speaker[j] = (float) Math.sqrt(Dot(score_data_Speaker[j], score_data_Speaker[j]));
                                                                amount_of_speakers += 1;
                                                                speaker_frequency[j] = 1;
                                                                final int jj = j;
                                                                runOnUiThread(() -> addHistory(ChatMessage.TYPE_SYSTEM,add_new_one + jj));
                                                                break;
                                                            }
                                                        }
                                                    } else {
                                                        long min_value = speaker_frequency[score_pre_calculate_Speaker.length - 1];
                                                        int min_position = score_pre_calculate_Speaker.length - 1;
                                                        for (int j = score_pre_calculate_Speaker.length - 2; j > -1; j--) {  // Find the min and older one.
                                                            if (speaker_frequency[j] < min_value) {
                                                                min_value = speaker_frequency[j];
                                                                min_position = j;
                                                            }
                                                        }
                                                        speaker_frequency[min_position] = 1;
                                                        score_data_Speaker[min_position] = feature_history.get(k).get(i);
                                                        score_pre_calculate_Speaker[min_position] = (float) Math.sqrt(Dot(score_data_Speaker[min_position], score_data_Speaker[min_position]));
                                                        amount_of_speakers += 1;
                                                        final int m = min_position;
                                                        runOnUiThread(()-> {
                                                            if (Objects.equals(speaker_name[m], "") | speaker_name[m] == null) {
                                                                addHistory(ChatMessage.TYPE_SYSTEM, add_full + m + "\n\n" + add_new_one + m);
                                                            } else {
                                                                addHistory(ChatMessage.TYPE_SYSTEM, add_full + speaker_name[m] + "\n\n" + add_new_one + m);
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (k == amount_of_mic - 1) {
                                    runOnUiThread(() -> {
                                        saveToFile(score_data_Speaker,cache_path + file_name_speakers);
                                        saveToFile(speaker_frequency,cache_path + file_name_speakers_frequency);
                                    });
                                }
                            }
                            continue_active[k] = 0;
                            speaker_history.get(k).clear();
                            feature_history.get(k).clear();
                        }
                    }
                });
            }
        }
        private void stop_SR() {
            recording = false;
            multiMicRecorder.stopRecording();
            for (int k = 0; k < amount_of_mic_channel; k++) {
                speaker_history.get(k).clear();
                feature_history.get(k).clear();
                print_count[k] = 0;
            }
        }
    }
    private class MultiMicRecorder {
        private final AudioRecord[] audioRecords;
        private final short[][] bufferArrays;
        private final Thread[] recordThreads;
        private MultiMicRecorder() {
            audioRecords = new AudioRecord[amount_of_mic];
            bufferArrays = new short[amount_of_mic][FRAME_BUFFER_SIZE_STEREO];
            recordThreads = new Thread[amount_of_mic];
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{android.Manifest.permission.RECORD_AUDIO},1);
            } else {
                for (int i = 0; i < amount_of_mic; i++) {
                    audioRecords[i] = new AudioRecord(MediaRecorder.AudioSource.VOICE_PERFORMANCE, SAMPLE_RATE,
                            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, FRAME_BUFFER_SIZE_STEREO);  // One microphone usually has two channels: left & right. You can use CHANNEL_IN_STEREO instead of CHANNEL_IN_MONO, and then separate the stereo recording results into odd & even indices to obtain the left & right PCM data, respectively.
                    final int micIndex = i;
                    recordThreads[i] = new Thread(() -> {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                        audioRecords[micIndex].startRecording();
                    });
                }
            }
        }
        private void startRecording() {
            for (Thread thread : recordThreads) {
                thread.start();
            }
        }
        private void stopRecording() {
            for (AudioRecord audioRecord : audioRecords) {
                audioRecord.stop();
                audioRecord.release();
            }
        }
        private float[] Read_PCM_Data() {
            short[] resultArray = new short[all_record_size];
            Thread[] readThreads = new Thread[amount_of_mic];
            for (int i = 0; i < amount_of_mic; i++) {
                final int micIndex = i;
                readThreads[i] = new Thread(() -> {
                    int bytesRead = audioRecords[micIndex].read(bufferArrays[micIndex], 0, FRAME_BUFFER_SIZE_MONO_48k);  // If use STEREO, FRAME_BUFFER_SIZE_STEREO instead.
                    System.arraycopy(bufferArrays[micIndex], 0, resultArray, micIndex * FRAME_BUFFER_SIZE_MONO_48k, bytesRead); // If use STEREO, FRAME_BUFFER_SIZE_STEREO instead.
                });
            }
            for (Thread thread : readThreads) {
                thread.start();
            }
            try {
                for (Thread thread : readThreads) {
                    thread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
////    If you would like using STEREO recording, please open the following code.
////    Remember to edit the "amount_of_mic_channel" both in project.h & Java. Also the "AudioFormat.CHANNEL_IN_MONO" -> "AudioFormat.CHANNEL_IN_STEREO", "bytesRead: FRAME_BUFFER_SIZE_MONO_48k -> FRAME_BUFFER_SIZE_STEREO"
//            {
//                short[] odd = new short[FRAME_BUFFER_SIZE_MONO_48k * amount_of_mic];
//                short[] even = new short[odd.length];
//                int micIndex = 0;
//                for (int i = 0; i < amount_of_mic; i++) {
//                    int count = 0;
//                    for (int j = 0; j < FRAME_BUFFER_SIZE_STEREO; j += 2) {
//                        even[count] = resultArray[micIndex + j];
//                        odd[count] = resultArray[micIndex + j + 1];
//                        count += 1;
//                    }
//                    System.arraycopy(even, 0, resultArray, micIndex, FRAME_BUFFER_SIZE_MONO_48k);
//                    System.arraycopy(odd, 0, resultArray, micIndex + FRAME_BUFFER_SIZE_MONO_48k, FRAME_BUFFER_SIZE_MONO_48k);
//                    micIndex += FRAME_BUFFER_SIZE_STEREO;
//                }
//            }
            float[] all_mic_record = new float[all_record_size_16k];  // Down sampling from 48kHz to 16kHz to achieve greater accuracy. Yor can use 16kHz float32 PCM directly without the following process.
            int index = 0;
            for (int i = 1; i < all_record_size - 1; i+=3) {
                all_mic_record[index] = (resultArray[i - 1] + resultArray[i] + resultArray[i] + resultArray[i + 1]) * 0.25f;  // Central weight average.
                index += 1;
            }
            return all_mic_record;
        }
    }
    @SuppressLint("SetTextI18n")
    private void start_SR() {
        recording = true;
        multiMicRecorder = new MultiMicRecorder();
        multiMicRecorder.startRecording();
        sr_Thread = new SR_Thread();
        sr_Thread.start();
    }
    @SuppressLint("SetTextI18n")
    private void stop_SR() {sr_Thread.stop_SR();}
    @SuppressLint("NotifyDataSetChanged")
    private static void addHistory(int messageType, String result) {
        int lastMessageIndex = messages.size() - 1;
        if (messageType == ChatMessage.TYPE_SYSTEM) {
            messages.add(new ChatMessage(messageType, result));
        } else if (lastMessageIndex >= 0 && messages.get(lastMessageIndex).type() == messageType) {
            if (messageType == ChatMessage.TYPE_USER ) {
                messages.set(lastMessageIndex, new ChatMessage(messageType, result));
            } else {
                messages.add(new ChatMessage(messageType, result));
            }
        } else {
            messages.add(new ChatMessage(messageType, result));
        }
        chatAdapter.notifyDataSetChanged();
        answerView.smoothScrollToPosition(messages.size() - 1);
    }
    @SuppressLint("NotifyDataSetChanged")
    private void clearHistory(){
        messages.clear();
        chatAdapter.notifyDataSetChanged();
        answerView.smoothScrollToPosition(0);
        for (int k = 0; k < amount_of_mic_channel; k++) {
            speaker_history.get(k).clear();
            feature_history.get(k).clear();
            print_count[k] = 0;
        }
        input_box.setText("");
        showToast( cleared,false);
    }
    @SuppressLint("NotifyDataSetChanged")
    private void Restart(){
        try {
            stop_SR();
            start_SR();
            clearHistory();
            showToast(restart_success,false);
        } catch (Exception e) {
            showToast(restart_failed,false);
        }
    }
    private void showToast(final String content, boolean display_long){
        if (display_long) {
            Toast.makeText(this, content, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
        }
    }
    private static float Dot(float[] vector1, float[] vector2) {
        float sum = 0.f;
        for (int i = 0; i < model_hidden_size_Res2Net; i++) {
            sum += vector1[i] * vector2[i];
        }
        return sum;
    }
    private static int Compare_Similarity(float[] model_result) {
        float model_result_dot = (float) Math.sqrt(Dot(model_result, model_result));
        float max_score = -999.f;
        int max_position = -1;
        for (int i = score_pre_calculate_Speaker.length - 1; i > -1; i--) {
            if (score_data_Speaker[i][0] != -999.f) {
                float temp = Dot(score_data_Speaker[i], model_result) / (score_pre_calculate_Speaker[i] * model_result_dot);
                if (temp > max_score) {
                    max_score = temp;
                    max_position = i;
                }
            }
        }
        if (max_score > threshold_Speaker_Confirm) {
//            System.out.println("Speaker_ID: " + max_position + " / max_score: " + max_score);
            return max_position;
        } else {
//            System.out.println("Speaker_ID: Unknown"  + " / max_score: " + max_score);
            return -1;
        }
    }
    private static void Rename() {
        renameButton.setOnClickListener(v -> {
            String text = String.valueOf(input_box.getText());
            input_box.setText("");
            if (Objects.equals(text, "")) {
                addHistory(ChatMessage.TYPE_SYSTEM, rename_id);
            } else {
                text = text.replaceAll("[-`~!@#$%^&*()_+=|{}':;\"\\[\\].<>/?·！￥…（）—《》【】‘；：”“’。，、？ ]", ",");
                String[] name_array = text.split(",");
                {
                    List<String> temp_list = new ArrayList<>();
                    for (String s : name_array) {
                        if (!Objects.equals(s, "")) {
                            temp_list.add(s);
                        }
                    }
                    name_array = temp_list.toArray(new String[(int) (temp_list.size())]);
                }
                try {
                    for (int i = 0; i < speaker_name.length; i++) {
                        if (Objects.equals(speaker_name[i], name_array[0])) {
                            speaker_name[i] = name_array[1];
                            addHistory(ChatMessage.TYPE_SYSTEM, "ID " + name_array[0] + " 已重命名为" + "'" + name_array[1] + "'" +"\nThe ID " + name_array[0] + " has been renamed to " + "'" + name_array[1] + "'");
                            saveToFile(speaker_name,cache_path + file_name_speakers_names);
                            return;
                        }
                    }
                    if (Character.isDigit(name_array[0].charAt(0))) {
                        int index = Integer.parseInt(name_array[0]);
                        if ((index < score_pre_calculate_Speaker.length) && (index > -1)) {
                            int j = 1;
                            for (int i = index; i < name_array.length + index - 1; i++) {
                                if (i < score_pre_calculate_Speaker.length) {
                                    speaker_name[i] = name_array[j];
                                    addHistory(ChatMessage.TYPE_SYSTEM, "ID " + i + " 已重命名为" + "'" + name_array[j] + "'" +"\nThe ID " + i + " has been renamed to " + "'" + name_array[j] + "'");
                                    j += 1;
                                } else {
                                    break;
                                }
                            }
                            saveToFile(speaker_name,cache_path + file_name_speakers_names);
                        } else {
                            addHistory(ChatMessage.TYPE_SYSTEM, no_id);
                        }
                    } else {
                        addHistory(ChatMessage.TYPE_SYSTEM, rename_id);
                    }
                } catch (Exception e) {
                    addHistory(ChatMessage.TYPE_SYSTEM, rename_id);
                }
            }
        });
    }
    private void Read_Assets(String file_name, AssetManager mgr) {
        switch (file_name) {
            case file_name_negMean_vad -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(mgr.open(file_name_negMean_vad)));
                    String[] values = reader.readLine().split("\\s+");
                    for (int i = 0; i < values.length; i++) {
                        negMean_vad[i] = Float.parseFloat(values[i]);
                    }
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            case file_name_invStd_vad -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(mgr.open(file_name_invStd_vad)));
                    String[] values = reader.readLine().split("\\s+");
                    for (int i = 0; i < values.length; i++) {
                        invStd_vad[i] = Float.parseFloat(values[i]);
                    }
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            case file_name_speakers -> {
                try (FileInputStream inputStream = new FileInputStream(cache_path + file_name_speakers)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(Channels.newInputStream(inputStream.getChannel())));
                    String line;
                    int row = 0;
                    while ((line = reader.readLine()) != null && row < score_data_Speaker.length) {
                        String[] stringValues = line.trim().split("\\s+");
                        for (int i = 0; i < model_hidden_size_Res2Net; i++) {
                            score_data_Speaker[row][i] = Float.parseFloat(stringValues[i]);
                        }
                        row++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            case file_name_speakers_names -> {
                try (FileInputStream inputStream = new FileInputStream(cache_path + file_name_speakers_names)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(Channels.newInputStream(inputStream.getChannel())));
                    for (int i = 0; i < speaker_name.length; i++) {
                        speaker_name[i] = reader.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            case file_name_speakers_frequency -> {
                try (FileInputStream inputStream = new FileInputStream(cache_path + file_name_speakers_frequency)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(Channels.newInputStream(inputStream.getChannel())));
                    String line;
                    int row = 0;
                    while ((line = reader.readLine()) != null && row < speaker_name.length) {
                        long value = Long.parseLong(line);
                        if (value + 1 == Long.MAX_VALUE) {
                            value /= 2;
                        }
                        speaker_frequency[row] = value;
                        row++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private static void saveToFile(float[][] float2DArray, String filePath) {
        StringBuilder stringBuilder = new StringBuilder();
        for (float[] row : float2DArray) {
            for (float value : row) {
                stringBuilder.append(value);
                stringBuilder.append(" ");
            }
            stringBuilder.append("\n");
        }
        try (FileWriter writer = new FileWriter(filePath)) {
            Objects.requireNonNull(new File(filePath).getParentFile()).mkdirs();
            writer.write(stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void saveToFile(long[] longArray, String filePath) {
        StringBuilder stringBuilder = new StringBuilder();
        for (long value : longArray) {
            stringBuilder.append(value);
            stringBuilder.append("\n");
        }
        try (FileWriter writer = new FileWriter(filePath)) {
            Objects.requireNonNull(new File(filePath).getParentFile()).mkdirs();
            writer.write(stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void saveToFile(String[] stringArray, String filePath) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String value : stringArray) {
            stringBuilder.append(value);
            stringBuilder.append("\n"); // Assuming each string should be on a new line
        }
        try (FileWriter writer = new FileWriter(filePath)) {
            Objects.requireNonNull(new File(filePath).getParentFile()).mkdirs();
            writer.write(stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private native boolean Load_Models_A(AssetManager assetManager, boolean FP16, boolean USE_GPU, boolean USE_NNAPI, boolean USE_XNNPACK, boolean USE_QNN, boolean USE_DSP_NPU);
    private native boolean Load_Models_B(AssetManager assetManager, boolean FP16, boolean USE_GPU, boolean USE_NNAPI, boolean USE_XNNPACK, boolean USE_QNN, boolean USE_DSP_NPU);
    private native boolean Pre_Process(float[] neg_mean_vad, float[] inv_std_vad);
    private static native float[] Run_VAD_SR(int record_size_16k, float[] audio, int stop_asr);
}
