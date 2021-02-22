package com.example.audiorecorder.audio;

public interface IAudioProcessingTaskListener {

	void onBytesProcessed(short[] block);
}
