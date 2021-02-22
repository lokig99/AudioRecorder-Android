package com.example.audiorecorder.audio;


import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioProcessingTask implements Runnable {

	private final LinkedBlockingQueue<short[]> audioBuffersQueue;
	private final LinkedList<byte[]> output;

	private volatile boolean running = true;

	private IAudioProcessingTaskListener taskListener = null;

	public AudioProcessingTask(LinkedBlockingQueue<short[]> audioBuffersQueue,
	                           LinkedList<byte[]> output) {
		this.audioBuffersQueue = audioBuffersQueue;
		this.output = output;
	}

	public void terminate() {
		running = false;
	}

	private byte[] convertToByteArray(short[] block) {
		byte[] bytes = new byte[block.length * 2];
		int index = 0;

		for (short val : block) {
			bytes[index++] = (byte) (val & 0xff);
			bytes[index++] = (byte) ((val >> 8) & 0xff);
		}

		return bytes;
	}

	private void processBlocks() throws InterruptedException {
		short[] block;
		while (running) {
			synchronized (this) {
				if (audioBuffersQueue.isEmpty()) continue;

				block = audioBuffersQueue.take();

				if (!blockIsSilent(block))
					output.add(convertToByteArray(block));
				else block = new short[1];

				if (taskListener != null)
					taskListener.onBytesProcessed(block);
			}
		}
	}


	private boolean blockIsSilent(short[] block) {
		final int SILENCE_THRESHOLD = 200;
		int maxValue = Short.MIN_VALUE;

		for (short val : block)
			maxValue = Math.max(maxValue, Math.abs(val));

		return maxValue < SILENCE_THRESHOLD;
	}

	public void setTaskListener(IAudioProcessingTaskListener taskListener) {
		this.taskListener = taskListener;
	}


	@Override
	public void run() {
		try {
			processBlocks();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
