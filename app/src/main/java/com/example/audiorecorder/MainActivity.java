package com.example.audiorecorder;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.audiorecorder.audio.AudioGrabbingTask;
import com.example.audiorecorder.audio.AudioProcessingTask;
import com.example.audiorecorder.audio.IAudioProcessingTaskListener;
import com.example.audiorecorder.audio.WavFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import static com.example.audiorecorder.utils.AppUtils.writeFileOnInternalStorage;

public class MainActivity extends AppCompatActivity implements IAudioProcessingTaskListener {

	ProgressBar progressBar;
	Button recordButton, stopButton, deleteButton, saveButton, listButton;
	EditText etName, etSurname, etTitle, etComment;

	AudioGrabbingTask provider;
	AudioProcessingTask processor;
	Thread providerThread, processorThread;

	LinkedList<byte[]> recordingAudioDraft;
	RecorderState currentState;

	public enum RecorderState {
		IDLE,
		RECORDING,
		PAUSED
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		recordingAudioDraft = new LinkedList<>();

		progressBar = findViewById(R.id.progressBar);

		stopButton = findViewById(R.id.buttonStop);
		recordButton = findViewById(R.id.buttonRecord);
		deleteButton = findViewById(R.id.buttonDelete);
		saveButton = findViewById(R.id.buttonSave);
		listButton = findViewById(R.id.buttonList);

		etName = findViewById(R.id.editTextPersonName);
		etSurname = findViewById(R.id.editTextPersonSurname);
		etTitle = findViewById(R.id.editTextTitle);
		etComment = findViewById(R.id.editTextComment);

		checkRecordPermission();
		checkExternalWriteReadPermission();

		updateRecorderState(RecorderState.IDLE);
	}

	private void checkRecordPermission() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
				!= PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
					123);
		}
	}

	private void checkExternalWriteReadPermission() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					1);
		}

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
					1);
		}
	}


	@Override
	public void onBytesProcessed(short[] block) {
		final int H_MAX = 32767;
		int maxValue = Integer.MIN_VALUE;

		for (int val : block) {
			maxValue = Math.max(maxValue, Math.abs(val));
		}

		double soundLevel = 20 * Math.log((double) maxValue / H_MAX);
		progressBar.setProgress((int) (1.25 * soundLevel + 100), true);
	}

	private void updateRecorderState(RecorderState state) {
		switch (state) {
			case IDLE:
				toggleMetadataFields(false);
				recordButton.setEnabled(true);
				stopButton.setEnabled(false);
				listButton.setEnabled(true);
				deleteButton.setEnabled(false);
				saveButton.setEnabled(false);
				break;
			case PAUSED:
				toggleMetadataFields(true);
				recordButton.setEnabled(true);
				stopButton.setEnabled(false);
				listButton.setEnabled(false);
				deleteButton.setEnabled(true);
				saveButton.setEnabled(true);
				break;
			case RECORDING:
				toggleMetadataFields(false);
				recordButton.setEnabled(false);
				stopButton.setEnabled(true);
				listButton.setEnabled(false);
				deleteButton.setEnabled(false);
				saveButton.setEnabled(false);
				break;
		}

		currentState = state;
	}


	public void onRecordButton(View view) {
		updateRecorderState(RecorderState.RECORDING);

		LinkedBlockingQueue<short[]> audioBufferQueue = new LinkedBlockingQueue<>();

		provider = new AudioGrabbingTask(audioBufferQueue);
		processor = new AudioProcessingTask(audioBufferQueue, recordingAudioDraft);
		processor.setTaskListener(this);

		providerThread = new Thread(provider);
		processorThread = new Thread(processor);

		providerThread.start();
		processorThread.start();
	}

	public void onStopButton(View view) {
		try {
			provider.terminate();
			processor.terminate();
			processorThread.join();
			providerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (recordingAudioDraft.isEmpty()) {
			updateRecorderState(RecorderState.IDLE);
			Toast.makeText(this, "Recording is empty!", Toast.LENGTH_SHORT).show();
		} else
			updateRecorderState(RecorderState.PAUSED);

		progressBar.setProgress(0, true);
	}

	public void onSaveButton(View view) {
		WavFile wav = new WavFile(AudioGrabbingTask.SAMPLE_RATE, 1);

		for (byte[] block : recordingAudioDraft) {
			wav.appendAudioBytes(block);
		}

		wav.insertMetadataTag(WavFile.NAME_TAG, etName.getText().toString());
		wav.insertMetadataTag(WavFile.SURNAME_TAG, etSurname.getText().toString());
		wav.insertMetadataTag(WavFile.TITLE_TAG, etTitle.getText().toString());
		wav.insertMetadataTag(WavFile.COMMENT_TAG, etComment.getText().toString());

		LocalDateTime dateTime = LocalDateTime.now();
		DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm");
		DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		wav.insertMetadataTag(WavFile.DATE_TAG, dateTime.format(formatterDate));
		wav.insertMetadataTag(WavFile.TIME_TAG, dateTime.format(formatterTime));

		clearMetadata();

		String filename = "recording-" + System.currentTimeMillis() + ".wav";

		writeFileOnInternalStorage(this, filename, wav.toByteArray());

		recordingAudioDraft.clear();
		updateRecorderState(RecorderState.IDLE);

		Toast.makeText(this, "Recording saved!", Toast.LENGTH_SHORT).show();
	}

	public void onDeleteButton(View view) {
		recordingAudioDraft.clear();
		clearMetadata();
		Toast.makeText(this, "Recording draft deleted!", Toast.LENGTH_SHORT).show();
		updateRecorderState(RecorderState.IDLE);
	}

	public void onListButton(View view) {
		Intent intent = new Intent(this, ListActivity.class);
		startActivity(intent);
	}

	private void toggleMetadataFields(boolean isEnabled) {
		etName.setEnabled(isEnabled);
		etSurname.setEnabled(isEnabled);
		etTitle.setEnabled(isEnabled);
		etComment.setEnabled(isEnabled);
	}

	private void clearMetadata() {
		etName.setText(null);
		etSurname.setText(null);
		etComment.setText(null);
		etTitle.setText(null);
	}
}