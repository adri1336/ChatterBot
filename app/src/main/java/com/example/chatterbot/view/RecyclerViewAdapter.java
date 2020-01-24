package com.example.chatterbot.view;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatterbot.R;
import com.example.chatterbot.data.Message;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>
{
    private List<Message> messages = new ArrayList<>();
    private OnMessageClickListener onMessageClickListener;

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final RecyclerViewAdapter.ViewHolder holder, int position)
    {
        final Message message = messages.get(position);
        if(message.isDateInfo())
        {
            holder.messageLinearLayout.setLongClickable(false);
            holder.messageLinearLayout.setClickable(false);
            holder.contentLinearLayout.setGravity(Gravity.CENTER);
            holder.messageLinearLayout.setBackgroundResource(R.drawable.bubble_date);
            holder.tvMessage.setText(DateFormat.getDateInstance(DateFormat.LONG).format(message.getDate()));
            holder.tvTime.setVisibility(View.GONE);
        }
        else
        {
            holder.messageLinearLayout.setLongClickable(true);
            holder.messageLinearLayout.setClickable(true);
            if(message.isOutcoming())
            {
                holder.contentLinearLayout.setGravity(Gravity.END);
                holder.messageLinearLayout.setBackgroundResource(R.drawable.bubble_outcoming);
            }
            else
            {
                holder.contentLinearLayout.setGravity(Gravity.START);
                holder.messageLinearLayout.setBackgroundResource(R.drawable.bubble_incoming);
            }
            holder.tvMessage.setText(message.getMessage());
            holder.tvTime.setVisibility(View.VISIBLE);
            holder.tvTime.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(message.getDate()));

            holder.messageLinearLayout.setOnTouchListener(new View.OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    int action = event.getAction();
                    switch(action)
                    {
                        case MotionEvent.ACTION_DOWN:
                        {
                            holder.messageLinearLayout.setBackgroundResource(R.drawable.bubble_date);
                            break;
                        }
                        default:
                        {
                            holder.messageLinearLayout.setBackgroundResource(message.isOutcoming() ? R.drawable.bubble_outcoming : R.drawable.bubble_incoming);
                            break;
                        }
                    }
                    return false;
                }
            });

            if(onMessageClickListener != null)
            {
                holder.messageLinearLayout.setOnLongClickListener(new View.OnLongClickListener()
                {
                    @Override
                    public boolean onLongClick(View v)
                    {
                        onMessageClickListener.onClick(message, holder.messageLinearLayout);
                        return true;
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount()
    {
        return messages == null ? 0 : messages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        private LinearLayout contentLinearLayout, messageLinearLayout;
        private TextView tvMessage, tvTime;
        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            contentLinearLayout = itemView.findViewById(R.id.contentLinearLayout);
            messageLinearLayout = itemView.findViewById(R.id.messageLinearLayout);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }

    public void addMessage(Message message)
    {
        messages.add(message);
        notifyItemInserted(messages.size());
    }

    public void setOnMessageClickListener(OnMessageClickListener onMessageClickListener)
    {
        this.onMessageClickListener = onMessageClickListener;
    }

    public interface OnMessageClickListener
    {
        void onClick(Message message, LinearLayout linearLayout);
    }
}
