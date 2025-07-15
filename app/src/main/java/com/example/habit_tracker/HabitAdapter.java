package com.example.habit_tracker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.PrintStream;
import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private List<HabitModel> habitList;
    private OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(HabitModel habit, int position, View anchorView);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public HabitAdapter(List<HabitModel> habitList) {
        this.habitList = habitList;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        HabitModel habit = habitList.get(position);

        holder.bind(habit); // ✅ this shows days and all text properly


        holder.title.setText(habit.getName());
        holder.description.setText(habit.getDescription());
        holder.count.setText(habit.getCurrentCount() + "/" + habit.getGoal() + " completed");
        holder.streakCount.setText("🔥 " + habit.getStreak() + " days");




        holder.doneButton.setOnClickListener(v -> {
            String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());

            long newCount = habit.getCurrentCount() + 1;
            habit.setCurrentCount(newCount);

            if (newCount >= habit.getGoal()){
                habit.setStreak(habit.getStreak() +1 );
            }




            FirebaseFirestore.getInstance()
                    .collection("habits")
                    .document(habit.getId()) // assumes habit has setId from Firestore
                    .update("currentCount", habit.getCurrentCount(),
                            "streak", habit.getStreak())
                    .addOnSuccessListener(unused -> {
                        notifyItemChanged(holder.getAdapterPosition());
                        Toast.makeText(v.getContext(), "Progress updated!", Toast.LENGTH_SHORT).show();
                    });
        });

        // Long click to trigger popup menu
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(habit, position, v);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    public void updateList(List<HabitModel> newList) {
        this.habitList = newList;
        notifyDataSetChanged();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, count, streakCount;
        ImageView streak;
        Button doneButton;
        TextView habitDays;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.habitTitle);
            description = itemView.findViewById(R.id.habitDescription);
            count = itemView.findViewById(R.id.count);
            streakCount = itemView.findViewById(R.id.streakCount);
            streak = itemView.findViewById(R.id.streak);
            doneButton = itemView.findViewById(R.id.btnDone);
            habitDays = itemView.findViewById(R.id.habitDays); // new

        }


        void bind(HabitModel habit) {
//
            String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
            // 🔁 Reset count if it's a new day
            if (!todayDate.equals(habit.getLastUpdatedDate())) {
                habit.setCurrentCount(0);
                habit.setLastUpdatedDate(todayDate);

                // Update Firestore with reset count and new date
                FirebaseFirestore.getInstance()
                        .collection("habits")
                        .document(habit.getId())
                        .update("currentCount", 0, "lastUpdatedDate", todayDate);
            }//


            title.setText(habit.getName());
            description.setText(habit.getDescription());

            long current = habit.getCurrentCount();
            long goal = habit.getGoal();
            long streakVal = habit.getStreak();

            count.setText(current + "/" + goal + " completed");
            streakCount.setText("🔥 " + streakVal + " days");

            List<String> days = habit.getDays();
            if (days != null && !days.isEmpty()) {
                habitDays.setText("Days: " + String.join(", ", days));
            } else {
                habitDays.setText("Days: Not set");
            }

            // ✅ Disable doneButton if goal already met
            if (current >= goal) {
                doneButton.setEnabled(false);
                doneButton.setText("Done ✅");
            } else {
                doneButton.setEnabled(true);
                doneButton.setText("Mark Done");
            }

            // ✅ Update logic
            doneButton.setOnClickListener(v -> {
                long newCount = habit.getCurrentCount() + 1;
                habit.setCurrentCount(newCount);

                if (newCount >= goal) {
                    habit.setStreak(habit.getStreak() + 1);
                }
                FirebaseFirestore.getInstance()
                        .collection("habits")
                        .document(habit.getId())
                        .update("currentCount", habit.getCurrentCount(), "streak", habit.getStreak())
                        .addOnSuccessListener(unused -> {
                            count.setText(newCount + "/" + goal + " completed");
                            streakCount.setText("🔥 " + habit.getStreak() + " days");
                            Toast.makeText(v.getContext(), "Progress updated!", Toast.LENGTH_SHORT).show();
                        });
            });
        }
    }
}