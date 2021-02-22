package com.example.audiorecorder.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;

public class AppUtils {
	public static void writeFileOnInternalStorage(Context context, String fileName, byte[] data) {
		File dir = new File(context.getExternalFilesDir(null), "recordings");

		if (!dir.exists()) {
			dir.mkdirs();
		}

		FileOutputStream fOut;
		File file = new File(dir, fileName);

		try {
			fOut = new FileOutputStream(file);
			fOut.write(data);
			fOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<File> listRecordingFiles(Context context) {
		File dir = new File(context.getExternalFilesDir(null), "recordings");
		File[] files = dir.listFiles();
		ArrayList<File> recordings = new ArrayList<>();

		if (files != null) {
			for (File file : files) {
				if (file.isFile() && file.getName().endsWith(".wav")) {
					recordings.add(file);
				}
			}
		}


		return recordings;
	}

	public static boolean deleteFile(Context context, URI wavFilePath) {
		File wavFile = new File(wavFilePath);
		return wavFile.delete();
	}
}
