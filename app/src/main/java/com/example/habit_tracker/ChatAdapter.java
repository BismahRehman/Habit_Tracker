package com.example.habit_tracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.messageText.setText(message.getContent());
        if (message.getRole().equals("user")) {
            holder.messageText.setBackgroundResource(R.drawable.user_message_background);
            holder.messageText.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.itemView.setPadding(50, 8, 8, 8);
        } else {
            holder.messageText.setBackgroundResource(R.drawable.bot_message_background);
            holder.messageText.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
            holder.itemView.setPadding(8, 8, 50, 8);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        ViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }
}