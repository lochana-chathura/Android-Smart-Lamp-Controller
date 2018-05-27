package com.example.lochana.smartlampcontroller;

import android.app.ActionBar;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SchedulerActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener{

    boolean isSetStartTimePicker=true;
    TextView txt_start;
    TextView txt_end;
    Calendar calendar;
    SimpleDateFormat simpleDateFormat;
    String currentTime;
    String startTime="1800";
    String endTime="0000";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheduler);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        txt_start = findViewById(R.id.txt_set_start);
        txt_end = findViewById(R.id.txt_set_end);

        Button set_start = (Button) findViewById(R.id.btn_set_start);
        set_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment timePicker1 = new TimePickerFragment();
                timePicker1.show(getSupportFragmentManager(),"time picker1");
                isSetStartTimePicker=true;
            }
        });
        Button set_end = (Button) findViewById(R.id.btn_set_end);
        set_end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment timePicker2 = new TimePickerFragment();
                timePicker2.show(getSupportFragmentManager(),"time picker2");
                isSetStartTimePicker=false;
            }
        });
        Button enable = (Button) findViewById(R.id.btn_enable);
        enable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str=dataStringGenerater();
                Intent intent = new Intent();
                intent.putExtra("txtFromScheduler",str);
                setResult(RESULT_OK,intent);
                finish();
            }
        });
        Button disable = (Button) findViewById(R.id.btn_disable);
        disable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("txtFromScheduler","c");
                setResult(RESULT_OK,intent);
                finish();
            }
        });

    }


    private String dataStringGenerater(){
        calendar = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat("HHmm");
        currentTime=simpleDateFormat.format(calendar.getTime());
        String str="f"+startTime+"t"+endTime+"s"+currentTime;
        return str;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        if (isSetStartTimePicker){
            startTime =String.valueOf(hourOfDay)+String.valueOf(minute);
            txt_start.setText(String.format("%02d:%02d %s", hourOfDay % 12 == 0 ? 12 : hourOfDay % 12,
                    minute, hourOfDay < 12 ? "AM" : "PM"));
        }else{
            endTime =String.valueOf(hourOfDay)+String.valueOf(minute);
            txt_end.setText(String.format("%02d:%02d %s", hourOfDay % 12 == 0 ? 12 : hourOfDay % 12,
                    minute, hourOfDay < 12 ? "AM" : "PM"));
        }

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }



}
