package com.example.audiorecorder.audio;


import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WavFile {

	private final byte[] header;
	private byte[] data;

	private long totalAudioLen;
	private long totalDataLen;
	private long sampleRate;
	private int bitsPerSample;
	private int channels;

	private HashMap<String, String> metadata;
	public static final String NAME_TAG = "NAME";
	public static final String SURNAME_TAG = "SURN";
	public static final String DATE_TAG = "DATE";
	public static final String TIME_TAG = "TIME";
	public static final String TITLE_TAG = "TITL";
	public static final String COMMENT_TAG = "COMM";


	private static final int HEADER_LEN = 36;
	private static final int DATA_HEADER_LEN = 8;

	public WavFile(long sampleRate, int channels) {
		header = prepareEmptyWavFileHeader();
		metadata = new HashMap<>();
		metadata.put(NAME_TAG, null);
		metadata.put(SURNAME_TAG, null);
		metadata.put(DATE_TAG, null);
		metadata.put(TIME_TAG, null);
		metadata.put(TITLE_TAG, null);
		metadata.put(COMMENT_TAG, null);
		setTotalDataLen(HEADER_LEN);
		setSampleRate(sampleRate);
		setChannels(channels);
		setBitsPerSample(16);
	}

	public static WavFile fromFile(File wavFile) {
		byte[] fileBytes;

		try {
			fileBytes = Files.readAllBytes(wavFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		if (fileBytes.length < HEADER_LEN + DATA_HEADER_LEN) return null;

		byte[] header = Arrays.copyOfRange(fileBytes, 0, HEADER_LEN + DATA_HEADER_LEN);

		int totalDataLen = readTotalDataLen(header);
		byte[] data = Arrays.copyOfRange(fileBytes, HEADER_LEN + DATA_HEADER_LEN,
				totalDataLen + DATA_HEADER_LEN);

		HashMap<String, String> metadata = readMetadata(
				Arrays.copyOfRange(fileBytes, totalDataLen + DATA_HEADER_LEN, fileBytes.length));

		WavFile resWav = new WavFile(readSampleRate(header), readChannels(header));
		resWav.metadata = metadata;
		resWav.appendAudioBytes(data);

		return resWav;
	}

	public static WavFile merged(WavFile wavFile1, WavFile wavFile2) {
		WavFile resWav = new WavFile(wavFile1.sampleRate, wavFile1.channels);
		resWav.metadata = wavFile1.metadata;
		resWav.appendAudioBytes(wavFile1.data);
		resWav.appendAudioBytes(wavFile2.data);

		return resWav;
	}

	private static int readTotalDataLen(byte[] header) {
		int b1 = header[4] & 0xff;
		int b2 = header[5] & 0xff;
		int b3 = header[6] & 0xff;
		int b4 = header[7] & 0xff;
		return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
	}

	private static int readTotalAudioLen(byte[] header) {
		int b1 = header[40] & 0xff;
		int b2 = header[41] & 0xff;
		int b3 = header[42] & 0xff;
		int b4 = header[43] & 0xff;
		return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
	}

	private static short readChannels(byte[] header) {
		return header[22];
	}

	private static int readSampleRate(byte[] header) {
		int b1 = header[24] & 0xff;
		int b2 = header[25] & 0xff;
		int b3 = header[26] & 0xff;
		int b4 = header[27] & 0xff;
		return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
	}

	private static HashMap<String, String> readMetadata(byte[] metadataBytes) {
		String metadataString = new String(metadataBytes, StandardCharsets.UTF_8);
		metadataString = metadataString.substring(metadataString.indexOf("id3 ") + "id3 ".length());
		String[] mdValues = metadataString.split(";");

		HashMap<String, String> metadata = new HashMap<>();

		for (String mdVal : mdValues) {
			String[] mdKeyVal = mdVal.split(":", 2);
			if (mdKeyVal.length > 1)
				metadata.put(mdKeyVal[0], mdKeyVal[1]);
		}

		return metadata;
	}


	private byte[] prepareEmptyWavFileHeader() {
		byte[] tmpHeader = new byte[HEADER_LEN + DATA_HEADER_LEN];

		tmpHeader[0] = 'R';  //RIFF/WAVE header
		tmpHeader[1] = 'I';
		tmpHeader[2] = 'F';
		tmpHeader[3] = 'F';

		tmpHeader[8] = 'W';
		tmpHeader[9] = 'A';
		tmpHeader[10] = 'V';
		tmpHeader[11] = 'E';
		tmpHeader[12] = 'f';  // 'fmt' chunk
		tmpHeader[13] = 'm';
		tmpHeader[14] = 't';
		tmpHeader[15] = ' ';
		tmpHeader[16] = 16; // 4 bytes: size of 'fmt' chunk
		tmpHeader[20] = 1; //format (1 -> PCM)

		tmpHeader[36] = 'd';
		tmpHeader[37] = 'a';
		tmpHeader[38] = 't';
		tmpHeader[39] = 'a';

		return tmpHeader;
	}

	public boolean setChannels(int channels) {
		if (channels < 1) return false;
		this.channels = channels;
		header[22] = (byte) channels;
		header[23] = 0;
		updateBlockAlign();
		updateBytesPerSecond();
		return true;
	}

	public boolean setBitsPerSample(int bitsPerSample) {
		if (bitsPerSample != 8 && bitsPerSample != 16) return false;
		this.bitsPerSample = bitsPerSample;
		header[34] = (byte) bitsPerSample;
		header[35] = 0;
		updateBlockAlign();
		updateBytesPerSecond();
		return true;
	}

	public boolean setSampleRate(long sampleRate) {
		if (sampleRate < 1) return false;
		this.sampleRate = sampleRate;
		header[24] = (byte) (sampleRate & 0xff);
		header[25] = (byte) ((sampleRate >> 8) & 0xff);
		header[26] = (byte) ((sampleRate >> 16) & 0xff);
		header[27] = (byte) ((sampleRate >> 24) & 0xff);
		updateBytesPerSecond();
		return true;
	}

	private void updateBytesPerSecond() {
		long bytesPS = getBytesPerSecond();
		header[28] = (byte) (bytesPS & 0xff);
		header[29] = (byte) ((bytesPS >> 8) & 0xff);
		header[30] = (byte) ((bytesPS >> 16) & 0xff);
		header[31] = (byte) ((bytesPS >> 24) & 0xff);
	}

	private void updateBlockAlign() {
		header[32] = (byte) ((bitsPerSample * channels) / 8);
		header[33] = 0;
	}


	public long getBytesPerSecond() {
		return (sampleRate * bitsPerSample * channels) / 8;
	}

	public byte[] toByteArray() {
		byte[] meta = metadataToByteArray();
		byte[] audio = ArrayUtils.addAll(header, data);
		return ArrayUtils.addAll(audio, meta);
	}

	private void setTotalDataLen(long totalDataLen) {
		this.totalDataLen = totalDataLen;
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
	}

	private void setTotalAudioLen(long totalAudioLen) {
		this.totalAudioLen = totalAudioLen;
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
	}

	public void appendAudioBytes(byte[] audioBlock) {
		setTotalAudioLen(totalAudioLen + audioBlock.length);
		setTotalDataLen(totalDataLen + audioBlock.length);

		data = ArrayUtils.addAll(data, audioBlock);
	}

	public void insertMetadataTag(String tag, String value) {
		metadata.put(tag, value);
	}

	private byte[] metadataToByteArray() {
		StringBuilder dataBuilder = new StringBuilder();

		dataBuilder.append("id3 ");

		for (Map.Entry<String, String> entry : metadata.entrySet()) {
			dataBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
		}

		return dataBuilder.toString().getBytes();
	}

	public String getMetadata(String tag) {
		return metadata.getOrDefault(tag, null);
	}
}
