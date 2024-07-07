package com.example.findit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

public class LabelResultReceiver extends BroadcastReceiver
{

    private final TextView tvLabelResult;

    public LabelResultReceiver(TextView tvLabelResult)
    {
        this.tvLabelResult = tvLabelResult;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent != null && intent.hasExtra("label"))
        {
            String label = intent.getStringExtra("label");
            tvLabelResult.setText("Object: " + label);
        }
    }
}
