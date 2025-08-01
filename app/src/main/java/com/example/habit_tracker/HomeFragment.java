package com.example.habit_tracker;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeFragment extends Fragment {

    private RecyclerView habitRecyclerView;
    private FirebaseFirestore db;
    private HabitAdapter adapter;
    private List<HabitModel> habitList = new ArrayList<>();
    // Map abbreviated day names to full names
    private static final Map<String, String> DAY_NAME_MAP = new HashMap<>();
    static {
        DAY_NAME_MAP.put("Sun", "Sunday");
        DAY_NAME_MAP.put("Mon", "Monday");
        DAY_NAME_MAP.put("Tue", "Tuesday");
        DAY_NAME_MAP.put("Wed", "Wednesday");
        DAY_NAME_MAP.put("Thu", "Thursday");
        DAY_NAME_MAP.put("Fri", "Friday");
        DAY_NAME_MAP.put("Sat", "Saturday");
    }
    private String getToday() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        switch (day) {
            case Calendar.SUNDAY: return "Sun";
            case Calendar.MONDAY: return "Mon";
            case Calendar.TUESDAY: return "Tue";
            case Calendar.WEDNESDAY: return "Wed";
            case Calendar.THURSDAY: return "Thu";
            case Calendar.FRIDAY: return "Fri";
            case Calendar.SATURDAY: return "Sat";
            default: return "";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        habitRecyclerView = view.findViewById(R.id.habitRecyclerView);
        habitRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));



        db = FirebaseFirestore.getInstance();
        // Initialize adapter once
        adapter = new HabitAdapter(habitList);
        habitRecyclerView.setAdapter(adapter);


        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();

                // 🚨 Safety check to avoid crash
                if (pos < 0 || pos >= habitList.size()) {
                    adapter.notifyDataSetChanged(); // safely refresh to prevent stuck state
                    return;
                }


                HabitModel habit = habitList.get(pos);

                if (direction == ItemTouchHelper.LEFT) {
                    // 👇 Show delete confirmation
                    new android.app.AlertDialog.Builder(getContext())
                            .setTitle("Delete Habit")
                            .setMessage("Are you sure you want to delete this habit?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                db.collection("users")
                                        .document(userId)
                                        .collection("habits")
                                        .document(habit.getId())
                                        .delete()
                                        .addOnSuccessListener(unused -> {
                                            if (pos >= 0 && pos < habitList.size()) {
                                                habitList.remove(pos);
                                                adapter.notifyItemRemoved(pos);
                                            } else {
                                                adapter.notifyDataSetChanged(); // fallback: refresh everything
                                            }
                                            Toast.makeText(getContext(), "Habit deleted", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .setNegativeButton("No", (dialog, which) -> {
                                adapter.notifyItemChanged(pos); // restore if canceled
                            })
                            .setCancelable(false)
                            .show();

                } else if (direction == ItemTouchHelper.RIGHT) {
                    // 👇 Handle swipe right as edit
                    new android.app.AlertDialog.Builder(getContext())
                            .setTitle("Edit Habit")
                            .setMessage("Do you want to edit this habit?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                Bundle bundle = new Bundle();
                                bundle.putString("docId", habit.getId());
                                bundle.putString("name", habit.getName());
                                bundle.putString("description", habit.getDescription());
                                bundle.putLong("goal", habit.getGoal());
                                bundle.putString("reminderTime", habit.getReminderTime());
                                bundle.putBoolean("reminder", habit.isReminder());
                                bundle.putStringArrayList("days", new ArrayList<>(habit.getDays()));

                                FabFragment fabFragment = new FabFragment();
                                fabFragment.setArguments(bundle);

                                requireActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.container, fabFragment)
                                        .addToBackStack(null)
                                        .commit();
                            })
                            .setNegativeButton("No", (dialog, which) -> {
                                adapter.notifyItemChanged(pos); // ❗ Restore item if cancelled
                            })
                            .setCancelable(false)
                            .show();
                }

            }
        }).attachToRecyclerView(habitRecyclerView);


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadHabits();
    }

    private void loadHabits() {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("habits")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    habitList.clear();


                    for (DocumentSnapshot doc : value.getDocuments()) {
                        if (!doc.exists()) continue; // ✅ skip non-existing doc

                        HabitModel habit = doc.toObject(HabitModel.class);
                        if (habit != null) {
                            habit.setId(doc.getId());



                            // ✅ Save today's history (if not saved yet)
                            db.collection("habits")
                                    .document(habit.getId())
                                    .collection("history")
                                    .document(todayDate)
                                    .get()
                                    .addOnSuccessListener(historyDoc -> {
                                        if (!historyDoc.exists()) {
                                            Map<String, Object> historyData = new HashMap<>();
                                            historyData.put("count", habit.getCurrentCount());
                                            historyData.put("goal", habit.getGoal());
                                            historyData.put("timestamp", new Date());

                                            db.collection("habits")
                                                    .document(habit.getId())
                                                    .collection("history")
                                                    .document(todayDate)
                                                    .set(historyData);
                                        }
                                    });
//
                            habitList.add(habit);
                        }
                    }
                    adapter.notifyDataSetChanged(); // update only
                });
    }
}