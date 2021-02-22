package com.example.audiorecorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.audiorecorder.audio.WavFile;
import com.example.audiorecorder.list.RecordingAdapter;
import com.example.audiorecorder.list.RecordingItem;
import com.example.audiorecorder.utils.AppUtils;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;

import java.util.Iterator;

public class ListActivity extends AppCompatActivity {
	RecyclerView recyclerView;
	RecordingAdapter adapter;
	RecyclerView.LayoutManager layoutManager;

	ArrayList<RecordingItem> recordingItems;
	ArrayList<Integer> checkedPositions;

	ActionMode actionMode;
	ActionMode.Callback callbackMergeMode, callbackDeleteMode;

	MediaPlayer mediaPlayer;
	int recentlyPlayedPosition = RecyclerView.NO_POSITION;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);

		checkedPositions = new ArrayList<>();
		recordingItems = new ArrayList<>();
		loadRecordings();
		buildRecyclerView();
		buildActionModeCallbacks();
		buildMediaPlayer();
	}


	private void loadRecordings() {
		recordingItems.clear();

		for (File recording : AppUtils.listRecordingFiles(this)) {
			WavFile wavFile = WavFile.fromFile(recording);
			if (wavFile != null) {
				String nameSurname = wavFile.getMetadata(WavFile.NAME_TAG) + " " +
						wavFile.getMetadata(WavFile.SURNAME_TAG);
				String title = wavFile.getMetadata(WavFile.TITLE_TAG);
				String comment = wavFile.getMetadata(WavFile.COMMENT_TAG);
				String date = wavFile.getMetadata(WavFile.DATE_TAG);
				String time = wavFile.getMetadata(WavFile.TIME_TAG);
				int image = R.drawable.ic_play;

				RecordingItem recordingItem = new RecordingItem(image, nameSurname, date, time,
						title, comment, recording.toURI());

				recordingItems.add(recordingItem);
			}
		}
	}

	private void changeItemIcon(int position, int imageResource) {
		recordingItems.get(position).setImageResource(imageResource);
		adapter.notifyItemChanged(position);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		adapter.setPlayButtonEnabled(false);
		switch (item.getItemId()) {
			case R.id.deleteMode:
				onDeleteMode(item);
				return true;
			case R.id.mergeMode:
				onMergeMode(item);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void onMergeMode(MenuItem item) {
		if (actionMode == null)
			actionMode = startActionMode(callbackMergeMode);
		adapter.updateCheckboxVisibility(true);
	}

	public void onDeleteMode(MenuItem item) {
		if (actionMode == null)
			actionMode = startActionMode(callbackDeleteMode);
		adapter.updateCheckboxVisibility(true);
	}

	private void buildRecyclerView() {
		recyclerView = findViewById(R.id.recyclerView);
		recyclerView.setHasFixedSize(false);
		layoutManager = new LinearLayoutManager(this);
		adapter = new RecordingAdapter(recordingItems);
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.setAdapter(adapter);

		adapter.setOnCheckedChangeListener((position, isChecked) -> {
			if (isChecked) {
				checkedPositions.add(position);
			} else {
				checkedPositions.remove(Integer.valueOf(position));
			}
		});

		adapter.setOnPlayClickedListener(position -> {
			if (adapter.isPlayButtonEnabled()) {
				RecordingItem item = recordingItems.get(position);
				switch (item.getImageResource()) {
					case R.drawable.ic_play:
						changeItemIcon(position, R.drawable.ic_pause);
						playRecording(position);
						break;
					case R.drawable.ic_pause:
						changeItemIcon(position, R.drawable.ic_play);
						stopPlayback();
						break;
				}
			}
		});
	}

	private void deletePositions(ArrayList<Integer> positions) {
		ArrayList<RecordingItem> itemsToDelete = new ArrayList<>();

		for (int position : positions) {
			itemsToDelete.add(recordingItems.get(position));
		}

		for (Iterator<RecordingItem> iter = recordingItems.iterator(); iter.hasNext(); ) {
			RecordingItem item = iter.next();
			if (itemsToDelete.contains(item)) {
				AppUtils.deleteFile(this, item.getWavFilePath());
				iter.remove();
			}
		}

		adapter.notifyDataSetChanged();
		checkedPositions.clear();
	}

	private void mergePositions(ArrayList<Integer> positions) {
		WavFile tmpWav;
		ArrayList<RecordingItem> itemsToMerge = new ArrayList<>();

		for (int i = 1; i < positions.size(); i++) {
			itemsToMerge.add(recordingItems.get(positions.get(i)));
		}

		RecordingItem baseItem = recordingItems.get(positions.get(0));
		tmpWav = WavFile.fromFile(new File(baseItem.getWavFilePath()));

		for (RecordingItem item : itemsToMerge) {
			WavFile wavToMerge = WavFile.fromFile(new File(item.getWavFilePath()));
			tmpWav = WavFile.merged(tmpWav, wavToMerge);
		}

		AppUtils.writeFileOnInternalStorage(this, new File(baseItem.getWavFilePath()).getName(),
				tmpWav.toByteArray());

		positions.remove(0);
		deletePositions(positions);
	}

	private void playRecording(int position) {
		if (recentlyPlayedPosition != RecyclerView.NO_POSITION &&
				recentlyPlayedPosition != position)
			changeItemIcon(recentlyPlayedPosition, R.drawable.ic_play);
		stopPlayback();

		recentlyPlayedPosition = position;
		RecordingItem item = recordingItems.get(position);

		try {
			mediaPlayer.setDataSource(item.getWavFilePath().getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		mediaPlayer.prepareAsync();
	}

	private void stopPlayback() {
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
			mediaPlayer.reset();
		}
	}

	private void resetPlayback() {
		stopPlayback();
		recentlyPlayedPosition = RecyclerView.NO_POSITION;
		for (RecordingItem item : recordingItems) {
			item.setImageResource(R.drawable.ic_play);
		}
		adapter.notifyDataSetChanged();
	}

	private void buildMediaPlayer() {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioAttributes(
				new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
						.setUsage(AudioAttributes.USAGE_MEDIA).build());
		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			mediaPlayer.reset();
			return false;
		});

		mediaPlayer.setOnPreparedListener(mp -> mediaPlayer.start());
		mediaPlayer.setOnCompletionListener(mp -> {
			mediaPlayer.reset();
			changeItemIcon(recentlyPlayedPosition, R.drawable.ic_play);
		});
	}

	private void buildActionModeCallbacks() {
		callbackMergeMode = new ActionMode.Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				getMenuInflater().inflate(R.menu.list_merge_menu, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				resetPlayback();
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
					case R.id.mergeCancel:
						mode.finish();
						return true;
					case R.id.mergeConfirm:
						if (checkedPositions.size() < 2)
							Toast.makeText(getBaseContext(), "Select at least 2 items!",
									Toast.LENGTH_SHORT).show();
						else
							mergePositions(checkedPositions);
						mode.finish();
						return true;
					default:
						mode.finish();
						return false;
				}
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				actionMode = null;
				adapter.updateCheckboxVisibility(false);
				adapter.setPlayButtonEnabled(true);
			}
		};

		callbackDeleteMode = new ActionMode.Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				getMenuInflater().inflate(R.menu.list_delete_menu, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				resetPlayback();
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
					case R.id.deleteCancel:
						mode.finish();
						return true;
					case R.id.deleteConfirm:
						if (checkedPositions.size() == 0)
							Toast.makeText(getBaseContext(), "No items selected!",
									Toast.LENGTH_SHORT).show();
						else
							deletePositions(checkedPositions);
						mode.finish();
						return true;
					default:
						mode.finish();
						return false;
				}
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				actionMode = null;
				adapter.updateCheckboxVisibility(false);
				adapter.setPlayButtonEnabled(true);
			}
		};

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mediaPlayer != null) mediaPlayer.release();
	}
}