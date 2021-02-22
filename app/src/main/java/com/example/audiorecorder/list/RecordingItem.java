package com.example.audiorecorder.list;

import java.net.URI;

public class RecordingItem {
	int imageResource;
	String nameSurname, date, time, title, comment;
	URI wavFilePath;


	public RecordingItem(int imageResource, String nameSurname, String date, String time,
	                     String title, String comment, URI wavFilePath) {
		this.imageResource = imageResource;
		this.nameSurname = nameSurname;
		this.date = date;
		this.time = time;
		this.title = title;
		this.comment = comment;
		this.wavFilePath = wavFilePath;
	}

	public int getImageResource() {
		return imageResource;
	}

	public String getNameSurname() {
		return nameSurname;
	}

	public String getDate() {
		return date;
	}

	public String getTime() {
		return time;
	}

	public String getTitle() {
		return title;
	}

	public String getComment() {
		return comment;
	}

	public URI getWavFilePath() {
		return wavFilePath;
	}

	public void setImageResource(int imageResource) {
		this.imageResource = imageResource;
	}

}
