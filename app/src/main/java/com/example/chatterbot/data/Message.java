package com.example.chatterbot.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Message
{
    private boolean dateInfo;
    private boolean outcoming;
    private String message;
    private Date date;
    private String messageEn;

    public Message()
    {

    }

    public Message(boolean outcoming, String message, Date date)
    {
        this.dateInfo = false;
        this.outcoming = outcoming;
        this.message = message;
        this.date = date;
    }

    public Message(boolean outcoming, String message, String messageEn, Date date)
    {
        this(outcoming, message, date);
        this.messageEn = messageEn;
    }

    public Message(Date date)
    {
        this.dateInfo = true;
        this.outcoming = false;
        this.message = "";
        this.date = date;
    }

    public boolean isDateInfo()
    {
        return dateInfo;
    }

    public boolean isOutcoming()
    {
        return outcoming;
    }

    public String getMessage()
    {
        return message;
    }

    public Date getDate()
    {
        return date;
    }

    public Map<String, Object> toMap()
    {
        HashMap<String, Object> result = new HashMap<>();
        result.put("outcoming", outcoming);
        result.put("message", message);
        result.put("messageEn", messageEn);
        result.put("date", date);
        return result;
    }

    @Override
    public String toString()
    {
        return "Message{" + "dateInfo=" + dateInfo + ", outcoming=" + outcoming + ", message='" + message + '\'' + ", date=" + date + ", messageEn='" + messageEn + '\'' + '}';
    }
}
