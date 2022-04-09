package com.example.attendance;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.attendance.adapters.RecyclerViewAdapter;
import com.example.attendance.data.DatabaseHandler;
import com.opencsv.CSVWriter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("TAG", "onReceive: " + device.getName());
                if(device.getName() != null)
                    devicesFound.add(device.getName());
            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                progressBar.setVisibility(View.VISIBLE);
            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                progressBar.setVisibility(View.GONE);
                attendanceProcess();
            }
        }
    };

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> devicesFound = new ArrayList<>();
    private ArrayList<String> classList = new ArrayList<>();
    private EditText addclassEditText;
    private Button addclassButton;
    private RadioGroup radioGroup;
    private Button takeAttendanceBtn;
    private Button exportBtn;
    private ProgressBar progressBar;
    private RadioButton selectedRadioBtn = null;
    private DatabaseHandler databaseHandler = new DatabaseHandler(MainActivity.this);
    private RecyclerViewAdapter recyclerViewAdapter;
    private RecyclerView recyclerView;
    private Button removeBtn;

    private List<List<String>> datacache = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CSVWriter csvWriter;

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this
                    , new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(MainActivity.this, "Your Device doesn't support bluetooth !", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, intentFilter);

        addclassEditText = findViewById(R.id.newClassEdittext);
        addclassButton = findViewById(R.id.addClassButton);
        radioGroup = findViewById(R.id.ClassSelector);
        takeAttendanceBtn = findViewById(R.id.attendanceButton);
        progressBar = findViewById(R.id.progressBar);
        exportBtn = findViewById(R.id.exportButton);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new RecyclerViewAdapter(this, datacache);
        recyclerView.setAdapter(recyclerViewAdapter);
        removeBtn = findViewById(R.id.removeClassButton);
        removeBtn.setOnClickListener(this);

        initializeClassList();

        addclassButton.setOnClickListener(MainActivity.this);
        takeAttendanceBtn.setOnClickListener(MainActivity.this);
        exportBtn.setOnClickListener(MainActivity.this);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                selectedRadioBtn = findViewById(checkedId);
                datacache = databaseHandler.populate(datacache, selectedRadioBtn.getText().toString());
                recyclerViewAdapter = new RecyclerViewAdapter(this, datacache);
                recyclerView.setAdapter(recyclerViewAdapter);
            }
        );
    }

    private void initializeClassRadioGroup() {
        radioGroup.removeAllViews();
        int n = classList.size();
        for(int i=0;i<n;i++){
            if(classList.get(i) == null || classList.get(i) == "null" || classList.get(i) == "") continue;
            RadioButton radioButton = new RadioButton(MainActivity.this);
            radioButton.setId(i);
            radioButton.setText(classList.get(i));
            radioGroup.addView(radioButton);
        }
    }

    private void initializeClassList() {
        classList.clear();
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String[] temp = preferences.getString("classlist", "").split(",");
        for(String t : temp) classList.add(t);
        initializeClassRadioGroup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.cancelDiscovery();
        bluetoothAdapter.disable();
        unregisterReceiver(receiver);
    }

    DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                //Yes button clicked
                databaseHandler.delete(selectedRadioBtn.getText().toString());
                removeclassfrompref(selectedRadioBtn.getText().toString());
                break;
        }
    };

    private void removeclassfrompref(String TABLE_NAME) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        classList.remove(TABLE_NAME);
        String temp = "";
        for(String i:classList) temp = temp + i + ",";
        editor.putString("classlist", temp);
        editor.apply();
        datacache.clear();
        recyclerViewAdapter = new RecyclerViewAdapter(this, datacache);
        recyclerView.setAdapter(recyclerViewAdapter);
        initializeClassList();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.addClassButton:
                addClassUtil();
                break;
            case R.id.attendanceButton:
                if(selectedRadioBtn == null)
                    Toast.makeText(MainActivity.this, "Please select class.", Toast.LENGTH_LONG).show();
                else
                    bluetoothAdapter.startDiscovery();
                break;
            case R.id.exportButton:
                if(selectedRadioBtn == null)
                    Toast.makeText(MainActivity.this, "Please select class.", Toast.LENGTH_LONG).show();
                else
                    databaseHandler.exportDB(selectedRadioBtn.getText().toString());
                break;
            case R.id.removeClassButton:
                if(selectedRadioBtn == null)
                    Toast.makeText(MainActivity.this, "Please select class.", Toast.LENGTH_LONG).show();
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Are you sure you want to delete attendance record of class "
                        + selectedRadioBtn.getText() + " ?").setPositiveButton("Delete", dialogClickListener)
                            .setNegativeButton("Cancel", dialogClickListener).show();
                }
                break;
        }
    }

    private void attendanceProcess() {
        processDevices();
        Log.d("TAG", "attendanceProcess: " + devicesFound.size());
        String date = new SimpleDateFormat("_dd_MM_yyyy", Locale.getDefault()).format(new Date());
        databaseHandler.insert(date, devicesFound, selectedRadioBtn.getText().toString());
        datacache = databaseHandler.populate(datacache, selectedRadioBtn.getText().toString());
        recyclerViewAdapter = new RecyclerViewAdapter(this, datacache);
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    private void processDevices() {
        int n = selectedRadioBtn.getText().length();
        for(int i=0;i<devicesFound.size();i++){
            if(devicesFound.get(i).length() < n || !devicesFound.get(i).substring(0, n)
                    .contentEquals(selectedRadioBtn.getText())){
                devicesFound.remove(i);
                i--;
            } else {
                devicesFound.set(i, devicesFound.get(i).substring(n));
            }
        }
    }

    private void addClassUtil() {
        String temp = addclassEditText.getText().toString();
        addclassEditText.setText("");
        if(temp == "" || temp == null || temp == "null"){
            Toast.makeText(MainActivity.this, "Class name can't be empty !", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        String temp2 = temp.substring(0,1);
        if(temp2.equals("1") || temp2.equals("2") || temp2.equals("3") || temp2.equals("4") || temp2.equals("5")
                || temp2.equals("6") || temp2.equals("7") || temp2.equals("8") || temp2.equals("9") || temp2.equals("0")){
            Toast.makeText(this, "Class name shouldn't start with any digit", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("classlist", preferences.getString("classlist", "")+
                temp + ",");
        editor.apply();
        initializeClassList();
    }
}

