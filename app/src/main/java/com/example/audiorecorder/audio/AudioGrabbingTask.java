package com.example.audiorecorder.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.util.concurrent.LinkedBlockingQueue;


public class AudioGrabbingTask implements Runnable {

	public static final int SAMPLE_RATE = 44100;
	public static final int BUFFER_SIZE = 4096;

	private volatile boolean running = true;

	private final LinkedBlockingQueue<short[]> audioBuffersQueue;
	private final AudioRecord audioRecord;

	public AudioGrabbingTask(LinkedBlockingQueue<short[]> audioBuffersQueue) {
		this.audioBuffersQueue = audioBuffersQueue;

		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
	}

	public void terminate() {
		running = false;
	}

	private short[] getNextBlock() {
		short[] buffer = new short[BUFFER_SIZE];

		int returnCode = audioRecord.read(buffer, 0, buffer.length);

		if (returnCode >= 0)
			return buffer;

		return null;
	}

	private void grabBlocks() throws InterruptedException {
		short[] block;
		audioRecord.startRecording();
		while (running) {
			synchronized (this) {
				block = getNextBlock();
				if (block != null)
					audioBuffersQueue.put(block);
			}
		}
		audioRecord.stop();
	}


	@Override
	public void run() {
		try {
			grabBlocks();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
