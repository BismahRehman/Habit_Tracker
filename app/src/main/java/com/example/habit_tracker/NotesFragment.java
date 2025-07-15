package com.example.habit_tracker;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotesFragment extends Fragment {

    private EditText titleInput, contentInput;
    private Button addNoteBtn;
    private RecyclerView notesRecycler;
    private FirebaseFirestore db;
    private List<Notes> notesList = new ArrayList<>();
    private NotesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        titleInput = view.findViewById(R.id.titleInput);
        contentInput = view.findViewById(R.id.contentInput);
        addNoteBtn = view.findViewById(R.id.addNoteBtn);
        notesRecycler = view.findViewById(R.id.notesRecycler);
        notesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotesAdapter(notesList);
        notesRecycler.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        addNoteBtn.setOnClickListener(v -> saveNote());

        fetchNotes();
        return view;
    }

    private void saveNote() {
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();
        if (title.isEmpty() || content.isEmpty()) return;

        String noteId = UUID.randomUUID().toString();
        Notes note = new Notes(noteId, title, content); // ← using Notes class
        db.collection("notes").document(noteId).set(note)
                .addOnSuccessListener(aVoid -> {
                    titleInput.setText("");
                    contentInput.setText("");
                });
    }

    private void fetchNotes() {
        db.collection("notes").addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;

            notesList.clear();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Notes note = doc.toObject(Notes.class); // ← using Notes class
                if (note != null) notesList.add(note);
            }
            adapter.notifyDataSetChanged();
        });
    }
}
