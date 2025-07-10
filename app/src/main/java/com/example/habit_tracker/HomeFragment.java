package com.example.habit_tracker;

import android.os.Bundle;
import android.view.*;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.*;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeFragment extends Fragment {

    private RecyclerView habitRecyclerView;
    private FirebaseFirestore db;
    private HabitAdapter adapter;
    private List<HabitModel> habitList = new ArrayList<>();

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
;

        // Set long click listener for edit
        adapter.setOnItemLongClickListener((habit, position, v) -> {
            PopupMenu menu = new PopupMenu(getContext(), v);
            menu.getMenu().add("Edit");
            menu.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Edit")) {
                    // Navigate to FabFragment
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction();
                    Bundle bundle = new Bundle();
                    bundle.putString("docId", habit.getId());
                    bundle.putString("name", habit.getName());
                    bundle.putString("description", habit.getDescription());
                    bundle.putLong("goal", habit.getGoal());
// add more if needed

                    FabFragment fabFragment = new FabFragment();
                    fabFragment.setArguments(bundle);

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, fabFragment)
                            .addToBackStack(null)
                            .commit();

                }
                return true;
            });
            menu.show();
        });

        // Swipe to delete
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
                HabitModel habit = habitList.get(pos);

                db.collection("habits").document(habit.getId())
                        .delete()
                        .addOnSuccessListener(unused ->
                                Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show());

                habitList.remove(pos);
                adapter.notifyItemRemoved(pos);
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

        db.collection("habits")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    habitList.clear();


                    for (DocumentSnapshot doc : value.getDocuments()) {
                        HabitModel habit = doc.toObject(HabitModel.class);
                        if (habit != null) {
                            habit.setId(doc.getId());
                            habitList.add(habit);
                        }
                    }

                    adapter.notifyDataSetChanged(); // update only
                });
    }
}
