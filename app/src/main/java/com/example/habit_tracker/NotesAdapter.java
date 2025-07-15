package com.example.habit_tracker;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Notes> notesList;

    public NotesAdapter(List<Notes> notesList) {
        this.notesList = notesList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Notes note = notesList.get(position);
        holder.titleText.setText(note.getTitle());
        holder.contentText.setText(note.getContent());


        holder.ivEdit.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_edit_note, null);

            EditText editTitle = dialogView.findViewById(R.id.editNoteTitle);
            EditText editContent = dialogView.findViewById(R.id.editNoteContent);

            editTitle.setText(note.getTitle());
            editContent.setText(note.getContent());

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Edit Note")
                    .setView(dialogView)
                    .setPositiveButton("Update", (dialog, which) -> {
                        String newTitle = editTitle.getText().toString().trim();
                        String newContent = editContent.getText().toString().trim();

                        if (!newTitle.isEmpty() && !newContent.isEmpty()) {
                            FirebaseFirestore.getInstance()
                                    .collection("notes")
                                    .document(note.getId())
                                    .update("title", newTitle, "content", newContent)
                                    .addOnSuccessListener(unused -> {
                                        note.setTitle(newTitle);
                                        note.setContent(newContent);
                                        notifyItemChanged(position);
                                        Toast.makeText(v.getContext(), "Note updated!", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(v.getContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        holder.ivDelete.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Delete notes")
                    .setMessage("Are you sure you want to delete this notes?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseFirestore.getInstance()
                                .collection("notes")
                                .document(note.getId())
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    notesList.remove(position);
                                    notifyItemRemoved(position);
                                    Toast.makeText(v.getContext(), "Habit deleted", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("No", null)
                    .show();
        });




    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, contentText;
        ImageView ivEdit, ivDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.noteTitle);
            contentText = itemView.findViewById(R.id.noteContent);
            ivEdit = itemView.findViewById(R.id.ivEdit);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}
