package com.example.audiorecorder.list;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audiorecorder.R;

import java.util.ArrayList;

public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder> {

	ArrayList<RecordingItem> recordingItems;
	boolean isCheckboxVisible = false;
	boolean isPlayButtonEnabled = true;
	OnCheckedChangeListener checkedListener;
	OnPlayClickedListener clickedListener;

	public interface OnCheckedChangeListener {
		void onCheckedChange(int position, boolean isChecked);
	}

	public interface OnPlayClickedListener {
		void onClick(int position);
	}


	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		this.checkedListener = listener;
	}

	public void setOnPlayClickedListener(OnPlayClickedListener listener) {
		this.clickedListener = listener;
	}

	public static class RecordingViewHolder extends RecyclerView.ViewHolder {
		public ImageView imageView;
		public TextView tvNameSurname, tvTitle, tvComment, tvDate, tvTime;
		public CheckBox checkBox;

		public RecordingViewHolder(@NonNull View itemView, OnCheckedChangeListener listener,
		                           OnPlayClickedListener clickedListener) {
			super(itemView);
			imageView = itemView.findViewById(R.id.imageView);
			tvNameSurname = itemView.findViewById(R.id.textViewName);
			tvTitle = itemView.findViewById(R.id.textViewTitle);
			tvComment = itemView.findViewById(R.id.textViewComment);
			tvDate = itemView.findViewById(R.id.textViewDate);
			tvTime = itemView.findViewById(R.id.textViewTime);
			checkBox = itemView.findViewById(R.id.checkBox);

			checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
				if (listener != null) {
					int position = getAdapterPosition();
					if (position != RecyclerView.NO_POSITION) {
						listener.onCheckedChange(getAdapterPosition(), isChecked);
					}
				}
			});

			imageView.setOnClickListener(v -> {

				if (listener != null) {
					int position = getAdapterPosition();
					if (position != RecyclerView.NO_POSITION) {
						clickedListener.onClick(position);
					}
				}

			});
		}
	}

	public RecordingAdapter(ArrayList<RecordingItem> recordingItems) {
		this.recordingItems = recordingItems;
	}


	@NonNull
	@Override
	public RecordingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View inflater = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.list_row, parent, false);

		return new RecordingViewHolder(inflater, checkedListener, clickedListener);
	}

	@Override
	public void onBindViewHolder(@NonNull RecordingViewHolder holder, int position) {
		RecordingItem recordingItem = recordingItems.get(position);

		holder.imageView.setImageResource(recordingItem.getImageResource());
		holder.tvNameSurname.setText(recordingItem.getNameSurname());
		holder.tvTitle.setText(recordingItem.getTitle());
		holder.tvComment.setText(recordingItem.getComment());
		holder.tvDate.setText(recordingItem.getDate());
		holder.tvTime.setText(recordingItem.getTime());

		holder.checkBox.setChecked(false);
		if (isCheckboxVisible)
			holder.checkBox.setVisibility(View.VISIBLE);
		else
			holder.checkBox.setVisibility(View.INVISIBLE);

		if (!isPlayButtonEnabled)
			holder.imageView.setColorFilter(Color.LTGRAY);
		else holder.imageView.setColorFilter(Color.DKGRAY);
	}

	@Override
	public int getItemCount() {
		return recordingItems.size();
	}

	public void updateCheckboxVisibility(boolean isVisible) {
		isCheckboxVisible = isVisible;
		notifyDataSetChanged();
	}

	public void setPlayButtonEnabled(boolean isEnabled) {
		isPlayButtonEnabled = isEnabled;
		notifyDataSetChanged();
	}

	public boolean isPlayButtonEnabled() {
		return isPlayButtonEnabled;
	}
}
