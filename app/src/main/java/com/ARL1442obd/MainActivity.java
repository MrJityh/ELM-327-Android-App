package com.ARL1442obd;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
//for converting to CSV (new code)
import java.io.File;
//for sending the data via wifi (new code)
//more for the server
//For date and time when saving csv
import java.util.Calendar;
import java.util.Date;
//for vin decoding
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final int MESSAGE_STATE_CHANGE = 1;

    /*0	Automatic protocol detection
   1	SAE J1850 PWM (41.6 kbaud)
   2	SAE J1850 VPW (10.4 kbaud)
   3	ISO 9141-2 (5 baud init, 10.4 kbaud)
   4	ISO 14230-4 KWP (5 baud init, 10.4 kbaud)
   5	ISO 14230-4 KWP (fast init, 10.4 kbaud)
   6	ISO 15765-4 CAN (11 bit ID, 500 kbaud)
   7	ISO 15765-4 CAN (29 bit ID, 500 kbaud)
   8	ISO 15765-4 CAN (11 bit ID, 250 kbaud) - used mainly on utility vehicles and Volvo
   9	ISO 15765-4 CAN (29 bit ID, 250 kbaud) - used mainly on utility vehicles and Volvo


    01 04 - ENGINE_LOAD
    01 05 - ENGINE_COOLANT_TEMPERATURE
    01 0C - ENGINE_RPM
    01 0D - VEHICLE_SPEED
    01 0F - INTAKE_AIR_TEMPERATURE
    01 10 - MASS_AIR_FLOW
    01 11 - THROTTLE_POSITION_PERCENTAGE
    01 1F - ENGINE_RUN_TIME
    01 2F - FUEL_LEVEL
    01 46 - AMBIENT_AIR_TEMPERATURE
    01 51 - FUEL_TYPE
    01 5E - FUEL_CONSUMPTION_1
    01 5F - FUEL_CONSUMPTION_2

   */
    //for collecting vehicle speed
    ArrayList<Integer> km_speed = new ArrayList<Integer>();
    String saveLocation = "/storage/emulated/0/Download";
    int fileCount = 0;
    String fileName = "pid_data" + fileCount + ".csv";


    String VIN = "";
    String idleTime = "";
    String engineOnTime = "";
    String mileage = "";
    ArrayList<Integer> fuelRate = new ArrayList<Integer>();
    //String random VIN

    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    protected final static char[] dtcLetters = {'P', 'C', 'B', 'U'};
    protected final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static final String[] PIDS = {
            "01", "02", "03", "04", "05", "06", "07", "08",
            "09", "0A", "0B", "0C", "0D", "0E", "0F", "10",
            "11", "12", "13", "14", "15", "16", "17", "18",
            "19", "1A", "1B", "1C", "1D", "1E", "1F", "20",
            "21", "22", "23", "24", "25", "26", "27", "28",
            "29", "2A", "2B", "2C", "2D", "2E", "2F", "30",
            "31", "32", "33", "34", "35", "36", "37", "38",
            "39", "3A", "3B", "3C", "3D", "3E", "3F", "40",
            "41", "42", "43", "44", "45", "46", "47", "48",
            "49", "4A", "4B", "4C", "4D", "4E", "4F", "50",
            "51", "52", "53", "54", "55", "56", "57", "58",
            "59", "5A", "5B", "5C", "5D", "5E", "5F", "60",};

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final float APPBAR_ELEVATION = 14f;
    private static boolean actionbar = true;
    final List<String> commandslist = new ArrayList<String>();

    final List<Double> avgconsumption = new ArrayList<Double>();
    final List<String> troubleCodesArray = new ArrayList<String>();
    MenuItem itemtemp;

    BluetoothDevice currentdevice;
    boolean commandmode = false, initialized = false, m_getPids = false, tryconnect = false, defaultStart = false;
    String devicename = null, deviceprotocol = null;

    String[] initializeCommands;
    Intent serverIntent = null;
    TroubleCodes troubleCodes;
    String VOLTAGE = "ATRV",
            PROTOCOL = "ATDP",
            RESET = "ATZ",
            PIDS_SUPPORTED20 = "0100",
            STATUS_DTC = "0101", //Status since DTC Cleared
            ENGINE_LOAD = "0104",  // A*100/255
            ENGINE_COOLANT_TEMP = "0105",  //A-40
            INTAKE_MAN_PRESSURE = "010B", //Intake manifold absolute pressure 0 - 255 kPa
            ENGINE_RPM = "010C",  //((A*256)+B)/4
            VEHICLE_SPEED = "010D",  //A
            INTAKE_AIR_TEMP = "010F",  //A-40
            MAF_AIR_FLOW = "0110", //MAF air flow rate 0 - 655.35	grams/sec ((256*A)+B) / 100  [g/s]
            THROTTLE_POSITION = "0111", //Throttle position 0 -100 % A*100/255
            OBD_STANDARDS = "011C", //OBD standards this vehicle
            ENGINE_RUN_TIME = "011F", //Run Time since engine start 256A+B (s)
            PIDS_SUPPORTED40 = "0120", //PIDs supported from 20-40
            DISTANCE_MIL = "0121", //Distance traveled with Malfunction indicator 256A+B (km)
            FUEL_RAIL_PRESSURE = "0122", // ((A*256)+B)*0.079
            FUEL_INPUT = "012F", //Fuel tank level input ??? 100/255 * A
            DISTANCE_SCC = "0131", //Distance since codes cleared 256A + B (km)
            CATALYST_TEMP_B1S1 = "013C",  //(((A*256)+B)/10)-40
            PIDS_SUPPORTED60 = "0140", //Supported Pids from 40-60
            CONT_MODULE_VOLT = "0142",  //voltage of control module ((A*256)+B)/1000
            AMBIENT_AIR_TEMP = "0146",  //A-40
            TIME_MIL = "014D", //Time run with MIL 256A + B
            TIME_SCC = "014E", //Time since codes cleared 256A + B
            ETHANOL_PERC = "0152", //Ethanol fuel % 100/255 * A
            REL_ACCEL_POS = "015A", //Relative accelator position 100/255 * A
            ENGINE_OIL_TEMP = "015C",  //A-40
            FUEL_RATE = "015E", //Engine fuel rate (256A + B)/20 L/h
            GET_VIN = "0902"; // PID for getting vin

    Toolbar toolbar;
    AppBarLayout appbar;
    String trysend = null;
    private PowerManager.WakeLock wl;
    private Menu menu;
    private EditText mOutEditText;
    private Button mSendButton, mRetrieveDB, mTroublecodes, mSendtoDB, mSavetoCSV;
    private ListView mConversationView;
    private TextView engineLoad, Fuel, voltage, coolantTemperature, Status, Loadtext, Volttext, Temptext, Centertext, Info, Airtemp_text, airTemperature, Maf_text, Maf, engineSpeedtext, engineSpeed, vehicleSpeedtext, vehicleSpeed;
    private TextView intakeAirtemptext, intakeAirtemp, mafAirFlowtext, mafAirFlow, throttlePositiontext, throttlePosition, runTimeEngStarttext, runTimeEngStart, fuelRailPressuretext, fuelRailPressure, distTraveledtext, distTraveled, ambientAirTemptext, ambientAirTemp, ethFueltext, ethFuel, engOilTemptext, engOilTemp, engFuelRatetext, enfFuelRate;
    private String mConnectedDeviceName = "Ecu";
    private int rpmval = 0, intakeairtemp = 0, ambientairtemp = 0, coolantTemp = 0, mMaf = 0,
            engineoiltemp = 0, b1s1temp = 0, Enginetype = 0, FaceColor = 0,
            whichCommand = 0, m_detectPids = 0, connectcount = 0, trycount = 0;
    private int mEnginedisplacement = 1500;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBtService = null;
    private ObdWifiManager mWifiService = null;

    StringBuilder inStream = new StringBuilder();

    // The Handler that gets information back from the BluetoothChatService
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;

    private final Handler mWifiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case ObdWifiManager.STATE_CONNECTED:
                            Status.setText(getString(R.string.title_connected_to, "ELM327 WIFI"));
                            try {
                                //changing menu text items
                                itemtemp = menu.findItem(R.id.menu_connect_wifi);
                                itemtemp.setTitle(R.string.disconnectwifi);
                            } catch (Exception e) {
                            }
                            tryconnect = false;
                            //resetValues();
                            sendEcuMessage(RESET);
                            break;
                        case ObdWifiManager.STATE_CONNECTING:
                            Status.setText(R.string.title_connecting);
                            Info.setText(R.string.tryconnectwifi);
                            break;
                        case ObdWifiManager.STATE_NONE:
                            Status.setText(R.string.title_not_connected);
                            itemtemp = menu.findItem(R.id.menu_connect_wifi);
                            itemtemp.setTitle(R.string.connectwifi);
                            if (mWifiService != null) mWifiService.disconnect();
                            mWifiService = null;
                            //resetValues();
                            break;
                    }
                    break;
                case MESSAGE_WRITE:

                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add("Command:  " + writeMessage);
                    }

                    break;

                case MESSAGE_READ:

                    String tmpmsg = clearMsg(msg);
                    //This is kinda important, should need an if statement to not report everything as vin
                    //MAybe do a "if first two chars in the string are 09, send to new function meant to parse the vin/other vehicle info if we decide in future?
                    //mConversationArrayAdapter.add("VIN ???: " + msg.obj.toString());

                    Info.setText(tmpmsg);

                    if (tmpmsg.contains(RSP_ID.NODATA.response) || tmpmsg.contains(RSP_ID.ERROR.response)) {

                        try {
                            String command = tmpmsg.substring(0, 4);

                            if (isHexadecimal(command)) {
                                removePID(command);
                            }

                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    if (commandmode || !initialized){
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + tmpmsg);
                    }
                    //after intializations stop being read, we analyze the messages in OBD
                    analyzeMsg(msg);
                    break;

                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;

                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final Handler mBtHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:

                            Status.setText(getString(R.string.title_connected_to, mConnectedDeviceName));
                            Info.setText(R.string.title_connected);
                            try {
                                itemtemp = menu.findItem(R.id.menu_connect_bt);
                                itemtemp.setTitle(R.string.disconnectbt);
                                Info.setText(R.string.title_connected);
                            } catch (Exception e) {
                            }

                            tryconnect = false;
                            //resetValues();
                            sendEcuMessage(RESET);

                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Status.setText(R.string.title_connecting);
                            Info.setText(R.string.tryconnectbt);
                            break;
                        case BluetoothService.STATE_LISTEN:

                        case BluetoothService.STATE_NONE:

                            Status.setText(R.string.title_not_connected);
                            itemtemp = menu.findItem(R.id.menu_connect_bt);
                            itemtemp.setTitle(R.string.connectbt);
                            if (tryconnect) {
                                mBtService.connect(currentdevice);
                                connectcount++;
                                if (connectcount >= 2) {
                                    tryconnect = false;
                                }
                            }
                            //resetValues();

                            break;
                    }
                    break;
                case MESSAGE_WRITE:

                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add("Command:  " + writeMessage);
                    }

                    break;
                case MESSAGE_READ:

                    String tmpmsg = clearMsg(msg);
                    Info.setText(tmpmsg);

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + tmpmsg);
                    }

                    // analyze the message
                    analyzeMsg(msg);


                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    //function to remove pids if they are not offered by the vehicle
    private void removePID(String pid) {
        int index = commandslist.indexOf(pid);

        if (index != -1) {
            commandslist.remove(index);
            Info.setText("Removed pid: " + pid);
            Log.i("SupportedPIDS", "Removed PID: " + pid);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendEcuMessage(message);
                    }
                    return true;
                }
            };

    public static boolean isHexadecimal(String text) {
        text = text.trim();

        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'};

        int hexDigitsCount = 0;

        for (char symbol : text.toCharArray()) {
            for (char hexDigit : hexDigits) {
                if (symbol == hexDigit) {
                    hexDigitsCount++;
                    break;
                }
            }
        }

        return true ? hexDigitsCount == text.length() : false;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        appbar = (AppBarLayout) findViewById(R.id.appbar);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire(10 * 60 * 1000L /*10 minutes*/);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Status = (TextView) findViewById(R.id.Status);
        engineLoad = (TextView) findViewById(R.id.Load);
        Fuel = (TextView) findViewById(R.id.Fuel);
        coolantTemperature = (TextView) findViewById(R.id.Temp);
        voltage = (TextView) findViewById(R.id.Volt);
        Loadtext = (TextView) findViewById(R.id.Load_text);
        Temptext = (TextView) findViewById(R.id.Temp_text);
        Volttext = (TextView) findViewById(R.id.Volt_text);
        Centertext = (TextView) findViewById(R.id.Center_text);
        Info = (TextView) findViewById(R.id.info);
        Airtemp_text = (TextView) findViewById(R.id.Airtemp_text);
        airTemperature = (TextView) findViewById(R.id.Airtemp);
        Maf_text = (TextView) findViewById(R.id.Maf_text);
        Maf = (TextView) findViewById(R.id.Maf);
        //new code (for dashboard)
        engineSpeedtext = (TextView) findViewById(R.id.Speed_text);
        engineSpeed = (TextView) findViewById(R.id.Speed);
        vehicleSpeedtext = (TextView) findViewById(R.id.VehicleSpeed_text);
        vehicleSpeed = (TextView) findViewById(R.id.VehicleSpeed);

        intakeAirtemptext = (TextView) findViewById(R.id.IntakeAirtemp_text);
        intakeAirtemp = (TextView) findViewById(R.id.IntakeAirtemp);
        mafAirFlowtext = (TextView) findViewById(R.id.MafAirFlow_text);
        mafAirFlow = (TextView) findViewById(R.id.MafAirFlow);
        throttlePositiontext = (TextView) findViewById(R.id.ThrottlePosition_text);
        throttlePosition = (TextView) findViewById(R.id.ThrottlePosition);
        runTimeEngStarttext = (TextView) findViewById(R.id.RunTimeEngStart_text);
        runTimeEngStart = (TextView) findViewById(R.id.RunTimeEngStart);
        fuelRailPressuretext = (TextView) findViewById(R.id.FuelRailPressure_text);
        fuelRailPressure = (TextView) findViewById(R.id.FuelRailPressure);
        distTraveledtext = (TextView) findViewById(R.id.DistanceTraveled_text);
        distTraveled = (TextView) findViewById(R.id.DistanceTraveled);


        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mRetrieveDB = (Button) findViewById(R.id.button_retrievedb);
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendtoDB = (Button) findViewById(R.id.button_sendDB);
        mSavetoCSV = (Button) findViewById(R.id.button_saveCSV);
        mTroublecodes = (Button) findViewById(R.id.button_troublecodes);
        mConversationView = (ListView) findViewById(R.id.in);


        troubleCodes = new TroubleCodes();

        visibleCMD();

        //key for commands
        //ATZ reset all
        //ATDP Describe the current Protocol
        //ATAT0-1-2 Adaptive Timing Off - adaptive Timing Auto1 - adaptive Timing Auto2
        //ATE0-1 Echo Off - Echo On
        //ATSP0 Set Protocol to Auto and save it
        //ATMA Monitor All
        //ATL1-0 Linefeeds On - Linefeeds Off
        //ATH1-0 Headers On - Headers Off
        //ATS1-0 printing of Spaces On - printing of Spaces Off
        //ATAL Allow Long (>7 byte) messages
        //ATRD Read the stored data
        //ATSTFF Set time out to maximum
        //ATSTHH Set timeout to 4ms

        initializeCommands = new String[]{"ATL0", "ATE1", "ATH1", "ATAT1", "ATSTFF", "ATI", "ATDP", "ATSP0", "0100"};

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        } else {
            if (mBtService != null) {
                if (mBtService.getState() == BluetoothService.STATE_NONE) {
                    mBtService.start();
                }
            }
        }

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);

                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(R.id.listText);

                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.parseColor("#3ADF00"));
                tv.setTextSize(10);

                // Generate ListView Item using TextView
                return view;
            }
        };

        mConversationView.setAdapter(mConversationArrayAdapter);

        mRetrieveDB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConversationArrayAdapter.add("User: Requesting current data from database...");
                DataHandler.request();

                //String sPIDs = "0100";
                //m_getPids = false;
                //sendEcuMessage(sPIDs);
            }
        });
        // Initialize the send button with a listener that for click events

        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                String message = mOutEditText.getText().toString();
                sendEcuMessage(message);
            }
        });

        mSendtoDB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                while (fileCount > 0){
                    // Assume you have a File object named csvFile representing your CSV file
                    File csvFile = new File(saveLocation, fileName);
                    mConversationArrayAdapter.add("User: Grabbing csv file at \"" + csvFile + "\"...");
                    mConversationArrayAdapter.add("User: Sending csv file to database...");

                    // Execute the AsyncTask to send data to the server
                    DataHandler.send(saveLocation + "/" + fileName);
                    //Decrement file name
                    fileCount = fileCount - 1;
                    fileName = "pid_data" + fileCount + ".csv";
                }



                //mConversationArrayAdapter.add("User: Success! Deleting local csv file");
            }
        });

        mSavetoCSV.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //IDK HOW TO DO FUNCTIONS/CLASSES IN JAVA BUT THE CSV CALL WOULD GO HERE
                mConversationArrayAdapter.clear(); //OLD LINE OF CODE FOR CLEARING CMD LIST
                mConversationArrayAdapter.add("User: Saving data to CSV file at \"" + saveLocation + "\"...");

                // Save the data to CSV file (new code to save to CSV file)
                String avg_speed = calculateAvgList(km_speed);
                //String Vin = "Vinhere";
                String avg_fuelRate = calculateAvgList(fuelRate);
                String date = "X/X/XXXX";
                String time = "1500";

                //For fuel level
                //sendEcuMessage("012F");

                Date currentTime = Calendar.getInstance().getTime();
                //Date date = DAY_OF_MONTH;

                //Generate random Trip# each time button is pressed, to be stored into CSV

                /* If wanted, we could take manual input for something
                String driverID = mOutEditText.getText().toString();
                mConversationArrayAdapter.add(driverID);
                 */

                //append all the values in one large string to be sent to saveToCSV function
                //Order of the values below ("Vin, AvgSpeed, FuelRate, IdleTime, EngineOnTime, MPG, Date, Time")
                //mileage = String.valueOf(Integer.parseInt(avg_speed)/(Integer.parseInt(engineOnTime)/3600));
                String csvData = VIN + ", " + avg_speed + ", " + avg_fuelRate + ", " + idleTime + ", " + engineOnTime + ", " + mileage + ", " + date + ", " + time;
                //VIN, avg_speed, avg_fuelrate?, idletime(NEEDS WORK), engineOnTime, mileage, date(NEEDS WORK), time(NEEDS WORK)

                //incrememnet file name
                fileCount = fileCount + 1;
                fileName = "pid_data" + fileCount + ".csv";

                //calling func to save data to csv file
                DataHandler.saveToCSV(fileName, csvData);

                km_speed.clear(); //clearing the array list 

                mConversationArrayAdapter.add("User: Success! File named \""+ fileName +"\"");
            }
        });

        mTroublecodes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String troubleCodes = "03";
                sendEcuMessage(troubleCodes);
            }
        });

        mOutEditText.setOnEditorActionListener(mWriteListener);

        //ConstraintLayout clayout = (ConstraintLayout) findViewById(R.id.mainscreen);
        ConstraintLayout rlayout = (ConstraintLayout) findViewById(R.id.mainscreen);
        rlayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               //Actionbar click
            }
        });
        
        getPreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        this.menu = menu;

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_connect_bt:

                if (mWifiService != null) {
                    if (mWifiService.isConnected()) {
                        Toast.makeText(getApplicationContext(), "First Disconnect WIFI Device.", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //new code
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                    }
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT, null);

                }

                if (mBtService == null) setupChat();

                if (item.getTitle().equals("Use Bluetooth OBDII")) {
                    // Launch the DeviceListActivity to see devices and do scan
                    serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    if (mBtService != null)
                    {
                        mBtService.stop();
                        item.setTitle(R.string.connectbt);
                    }
                }

                return true;
            case R.id.menu_connect_wifi:

                if (item.getTitle().equals("Use WiFi OBDII")) {

                    if (mWifiService == null)
                    {
                        mWifiService = new ObdWifiManager(this, mWifiHandler);
                    }

                    if (mWifiService != null) {
                        if (mWifiService.getState() == ObdWifiManager.STATE_NONE) {
                            mWifiService.connect();
                        }
                    }
                } else {
                    if (mWifiService != null)
                    {
                        mWifiService.disconnect();
                        item.setTitle(R.string.connectwifi);
                    }
                }

                return true;
                //case to enter the pids screen
            case R.id.menu_terminal:

                if (item.getTitle().equals("View Stats")) {
                    commandmode = true;
                    invisibleCMD();
                    item.setTitle(R.string.terminal);
                } else {
                    visibleCMD();
                    item.setTitle(R.string.pids);
                    commandmode = false;
                    sendEcuMessage(VOLTAGE); //may not need this
                }
                return true;

            case R.id.menu_settings:

                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, Prefs.class);
                startActivity(serverIntent);

                return true;
            case R.id.menu_exit:
                exit();

                return true;
            //case R.id.menu_reset:
            //    resetValues();
            //    return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //for bluetooth
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == MainActivity.RESULT_OK) {
                    connectDevice(data);
                }
                break;

            case REQUEST_ENABLE_BT:

                if (mBtService == null) setupChat();

                if (resultCode == MainActivity.RESULT_OK) {
                    serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    Toast.makeText(getApplicationContext(), "BT device not enabled", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setDefaultOrientation();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        getPreferences();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBtService != null) mBtService.stop();
        if (mWifiService != null)mWifiService.disconnect();

        wl.release();
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferences();
        setDefaultOrientation();
        resetValues();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

//when the back button is pressed
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (!commandmode) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setMessage("Are you sure you want exit?");
                alertDialogBuilder.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                exit();
                            }
                        });

                alertDialogBuilder.setNegativeButton("cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            } else {
                commandmode = false;
                visibleCMD();
                MenuItem item = menu.findItem(R.id.menu_terminal);
                item.setTitle(R.string.terminal);
                sendEcuMessage(VOLTAGE); //may not need this
            }

            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        if (mBtService != null) mBtService.stop();
        wl.release();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void getPreferences() {

            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(getBaseContext());

            FaceColor = Integer.parseInt(preferences.getString("FaceColor", "0"));



            mEnginedisplacement = Integer.parseInt(preferences.getString("Enginedisplacement", "1500"));

            m_detectPids = Integer.parseInt(preferences.getString("DetectPids", "0"));

            if (m_detectPids == 0) {

                commandslist.clear();

                int i = 0;

                commandslist.add(i, GET_VIN); //VIN 0902
                i++;
                commandslist.add(i, VOLTAGE); //ATRV
                i++;
                if (preferences.getBoolean("checkboxENGINE_LOAD", true)) {
                    commandslist.add(i, ENGINE_LOAD); // 0104
                    i++;
                }
                if (preferences.getBoolean("checkboxENGINE_COOLANT_TEMP", true)) {
                    commandslist.add(i, ENGINE_COOLANT_TEMP); // 0105
                    i++;
                }
                if (preferences.getBoolean("checkboxENGINE_OIL_TEMP", true)) {
                    commandslist.add(i, ENGINE_OIL_TEMP); // 015C
                    i++;
                }
                if (preferences.getBoolean("checkboxENGINE_RPM", true)) {
                    commandslist.add(i, ENGINE_RPM); // 010C
                    i++;
                }
                if (preferences.getBoolean("checkboxVEHICLE_SPEED", true)) {
                    commandslist.add(i, VEHICLE_SPEED); // 010D
                    i++;
                }
                if (preferences.getBoolean("checkboxAMBIENT_AIR_TEMP", true)) {
                    commandslist.add(i, AMBIENT_AIR_TEMP); // 0146
                    i++;
                }
                if (preferences.getBoolean("checkboxINTAKE_AIR_TEMP", true)) {
                    commandslist.add(i, INTAKE_AIR_TEMP); // 010F
                    i++;
                }
                if (preferences.getBoolean("checkboxMAF_AIR_FLOW", true)) {
                    commandslist.add(i, MAF_AIR_FLOW); // 0110
                    i++;
                }
                if (preferences.getBoolean("checkboxTHROTTLE_POSITION", true)) {
                    commandslist.add(i, THROTTLE_POSITION); // 0111
                    i++;
                }
                if (preferences.getBoolean("checkboxENGINE_RUN_TIME", true)) {
                    commandslist.add(i, ENGINE_RUN_TIME); // 011F Time since start
                    i++;
                }
                if (preferences.getBoolean("checkboxDISTANCE_MIL", true)) {
                    commandslist.add(i, DISTANCE_MIL); // 0121
                    i++;
                }
                if (preferences.getBoolean("checkboxTIME_MIL", true)) {
                    commandslist.add(i, TIME_MIL); // 014D
                    i++;
                }
                if (preferences.getBoolean("checkboxFUEL_RAIL_PRESSURE", true)) {
                    commandslist.add(i, FUEL_RAIL_PRESSURE); // 0122
                    i++;
                }
                if (preferences.getBoolean("checkboxFUEL_INPUT", true)) {
                    commandslist.add(i, FUEL_INPUT); // 012F
                    i++;
                }
                if (preferences.getBoolean("checkboxFUEL_RATE", true)) {
                    commandslist.add(i, FUEL_RATE); // 015E
                    i++;
                }
                if (preferences.getBoolean("checkboxDISTANCE_SCC", true)) {
                    commandslist.add(i, DISTANCE_SCC); // 0131
                    i++;
                }
                if (preferences.getBoolean("checkboxTIME_SCC", true)) {
                    commandslist.add(i, TIME_SCC); // 014E
                    i++;
                }
                if (preferences.getBoolean("checkboxETHANOL_PERC", true)) {
                    commandslist.add(i, ETHANOL_PERC); // 0152
                    i++;
                }
                if (preferences.getBoolean("checkboxREL_ACCEL_POS", true)) {
                    commandslist.add(i, REL_ACCEL_POS); // 015A
                }



                whichCommand = 0;
            }
    }

    private void setDefaultOrientation() {

        try {
            setTextSize();

        } catch (Exception e) {
        }
    }

    private void setTextSize() {
        int textSize = 14;
        int newTextSize = 12;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        Status.setTextSize(newTextSize);
        Fuel.setTextSize(textSize + 2);
        coolantTemperature.setTextSize(textSize);
        engineLoad.setTextSize(textSize);
        voltage.setTextSize(textSize);
        Temptext.setTextSize(textSize);
        Loadtext.setTextSize(textSize);
        Volttext.setTextSize(textSize);
        Airtemp_text.setTextSize(textSize);
        airTemperature.setTextSize(textSize);
        Maf_text.setTextSize(textSize);
        Maf.setTextSize(textSize);
        Info.setTextSize(newTextSize);
        engineSpeedtext.setTextSize(newTextSize);
        engineSpeed.setTextSize(newTextSize);
        vehicleSpeedtext.setTextSize(newTextSize);
        vehicleSpeed.setTextSize(newTextSize);

        intakeAirtemptext.setTextSize(newTextSize);
        intakeAirtemp.setTextSize(newTextSize);
        mafAirFlowtext.setTextSize(newTextSize);
        mafAirFlow.setTextSize(newTextSize);
        throttlePositiontext.setTextSize(newTextSize);
        throttlePosition.setTextSize(newTextSize);
        runTimeEngStarttext.setTextSize(newTextSize);
        runTimeEngStart.setTextSize(newTextSize);
        fuelRailPressuretext.setTextSize(newTextSize);
        fuelRailPressure.setTextSize(newTextSize);
        distTraveledtext.setTextSize(newTextSize);
        distTraveled.setTextSize(newTextSize);
    }

    public void invisibleCMD() {
        mConversationView.setVisibility(View.INVISIBLE);
        mOutEditText.setVisibility(View.INVISIBLE);
        mSendButton.setVisibility(View.INVISIBLE);
        mRetrieveDB.setVisibility(View.INVISIBLE);
        mTroublecodes.setVisibility(View.INVISIBLE);
        mSendtoDB.setVisibility(View.INVISIBLE);
        mSavetoCSV.setVisibility(View.INVISIBLE);

        engineLoad.setVisibility(View.VISIBLE);
        Fuel.setVisibility(View.VISIBLE);
        voltage.setVisibility(View.VISIBLE);
        coolantTemperature.setVisibility(View.VISIBLE);
        Loadtext.setVisibility(View.VISIBLE);
        Volttext.setVisibility(View.VISIBLE);
        Temptext.setVisibility(View.VISIBLE);
        Centertext.setVisibility(View.VISIBLE);
        Info.setVisibility(View.VISIBLE);
        //pids
        Airtemp_text.setVisibility(View.VISIBLE);
        airTemperature.setVisibility(View.VISIBLE);
        Maf_text.setVisibility(View.VISIBLE);
        Maf.setVisibility(View.VISIBLE);
        engineSpeedtext.setVisibility(View.VISIBLE);
        engineSpeed.setVisibility(View.VISIBLE);
        vehicleSpeedtext.setVisibility(View.VISIBLE);
        vehicleSpeed.setVisibility(View.VISIBLE);

        intakeAirtemptext.setVisibility(View.VISIBLE);
        intakeAirtemp.setVisibility(View.VISIBLE);
        mafAirFlowtext.setVisibility(View.VISIBLE);
        mafAirFlow.setVisibility(View.VISIBLE);
        throttlePositiontext.setVisibility(View.VISIBLE);
        throttlePosition.setVisibility(View.VISIBLE);
        runTimeEngStarttext.setVisibility(View.VISIBLE);
        runTimeEngStart.setVisibility(View.VISIBLE);
        fuelRailPressuretext.setVisibility(View.VISIBLE);
        fuelRailPressure.setVisibility(View.VISIBLE);
        distTraveledtext.setVisibility(View.VISIBLE);
        distTraveled.setVisibility(View.VISIBLE);


    }

    public void visibleCMD() {
        engineLoad.setVisibility(View.INVISIBLE);
        Fuel.setVisibility(View.INVISIBLE);
        voltage.setVisibility(View.INVISIBLE);
        coolantTemperature.setVisibility(View.INVISIBLE);
        Loadtext.setVisibility(View.INVISIBLE);
        Volttext.setVisibility(View.INVISIBLE);
        Temptext.setVisibility(View.INVISIBLE);
        Centertext.setVisibility(View.INVISIBLE);
        Info.setVisibility(View.INVISIBLE);
        //pids
        Airtemp_text.setVisibility(View.INVISIBLE);
        airTemperature.setVisibility(View.INVISIBLE);
        Maf_text.setVisibility(View.INVISIBLE);
        Maf.setVisibility(View.INVISIBLE);
        engineSpeedtext.setVisibility(View.INVISIBLE);
        engineSpeed.setVisibility(View.INVISIBLE);
        vehicleSpeedtext.setVisibility(View.INVISIBLE);
        vehicleSpeed.setVisibility(View.INVISIBLE);

        intakeAirtemptext.setVisibility(View.INVISIBLE);
        intakeAirtemp.setVisibility(View.INVISIBLE);
        mafAirFlowtext.setVisibility(View.INVISIBLE);
        mafAirFlow.setVisibility(View.INVISIBLE);
        throttlePositiontext.setVisibility(View.INVISIBLE);
        throttlePosition.setVisibility(View.INVISIBLE);
        runTimeEngStarttext.setVisibility(View.INVISIBLE);
        runTimeEngStart.setVisibility(View.INVISIBLE);
        fuelRailPressuretext.setVisibility(View.INVISIBLE);
        fuelRailPressure.setVisibility(View.INVISIBLE);
        distTraveledtext.setVisibility(View.INVISIBLE);
        distTraveled.setVisibility(View.INVISIBLE);

        mConversationView.setVisibility(View.VISIBLE);
        mOutEditText.setVisibility(View.VISIBLE);
        mSendButton.setVisibility(View.VISIBLE);
        mRetrieveDB.setVisibility(View.VISIBLE);
        mTroublecodes.setVisibility(View.VISIBLE);
        mSendtoDB.setVisibility(View.VISIBLE);
        mSavetoCSV.setVisibility(View.VISIBLE);
    }

//resetting the text values of the pids
    public void resetValues() {
        engineLoad.setText("0 %");
        voltage.setText("0 V");
        coolantTemperature.setText("0 C°");
        Info.setText("");
        airTemperature.setText("0 C°");
        Maf.setText("0 g/s");
        Fuel.setText("0 - 0 l/h");
        engineSpeed.setText("0 rpm");
        vehicleSpeed.setText("0 km/h");
        intakeAirtemp.setText("0 C°");
        mafAirFlow.setText("0 g/s");
        throttlePosition.setText("0 %");
        runTimeEngStart.setText("0 s");
        fuelRailPressure.setText("0 kPa");
        distTraveled.setText("0 km");

        //cause the elm to reinitialize, and clear the array of text
        m_getPids = false;
        whichCommand = 0;
        trycount = 0;
        initialized = false;
        defaultStart = false;
        avgconsumption.clear();
        mConversationArrayAdapter.clear();
    }
    //for connecting the bluetooth
    private void connectDevice(Intent data) {
        tryconnect = true;
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        try {
            // Attempt to connect to the device
            mBtService.connect(device);
            currentdevice = device;

        } catch (Exception e) {
        }
    }

    private void setupChat() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mBtService = new BluetoothService(this, mBtHandler);
    }

    private void sendEcuMessage(String message) {
        //check if sent data is matching read data (TEST)
        Log.d("sendEcuMessage", message);
        if( mWifiService != null)
        {
            if(mWifiService.isConnected())
            {
                try {
                    if (!message.isEmpty()) {
                        message = message + "\r";
                        byte[] send = message.getBytes();
                        mWifiService.write(send);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
        else if (mBtService != null)
        {
            // Check that we're actually connected before trying anything
            if (mBtService.getState() != BluetoothService.STATE_CONNECTED) {
                //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                if (!message.isEmpty()) {

                    message = message + "\r";
                    // Get the message bytes and tell the BluetoothChatService to write
                    byte[] send = message.getBytes();
                    mBtService.write(send);
                }
            } catch (Exception e) {
                Log.w("ECU","Error getting message: " + e.getMessage());
            }
        }
    }

    private void sendInitCommands() {
        if (initializeCommands.length != 0) {

            if (whichCommand < 0) {
                whichCommand = 0;
            }

            String send = initializeCommands[whichCommand];

            sendEcuMessage(send);

            if (whichCommand == initializeCommands.length - 1) {
                initialized = true;
                whichCommand = 0;
                sendDefaultCommands();
            } else {
                whichCommand++;
            }
        }
    }

    private void sendDefaultCommands() {

        if (!commandslist.isEmpty()) {
            //Old condition -> commandslist.size() != 0

            if (whichCommand < 0) {
                whichCommand = 0;
            }

            String send = commandslist.get(whichCommand);
            Log.d("whichCommand", whichCommand + " | " + commandslist.get(whichCommand));
            sendEcuMessage(send);

            if (whichCommand >= commandslist.size() - 1) {
                whichCommand = 0;
            } else {
                whichCommand++;
            }
        }
    }

    //removing certain substrings to clean up the message
    private String clearMsg(Message msg) {
        String tmpmsg = msg.obj.toString();

        tmpmsg = tmpmsg.replace("null", "");
        tmpmsg = tmpmsg.replaceAll("\\s", ""); //removes all [ \t\n\x0B\f\r]
        tmpmsg = tmpmsg.replaceAll(">", "");
        tmpmsg = tmpmsg.replaceAll("SEARCHING...", "");
        tmpmsg = tmpmsg.replaceAll("ATZ", "");
        tmpmsg = tmpmsg.replaceAll("ATI", "");
        tmpmsg = tmpmsg.replaceAll("atz", "");
        tmpmsg = tmpmsg.replaceAll("ati", "");
        tmpmsg = tmpmsg.replaceAll("ATDP", "");
        tmpmsg = tmpmsg.replaceAll("atdp", "");
        tmpmsg = tmpmsg.replaceAll("ATRV", "");
        tmpmsg = tmpmsg.replaceAll("atrv", "");

        return tmpmsg;
    }

    //checking if data is valid then parsing it
    private void checkPids(String tmpmsg) {
        StringBuilder pidmsg = new StringBuilder();
        //checking if payload has 49 to signify a VIN
        if (tmpmsg.contains("49")) {
            int index = tmpmsg.indexOf("49");

            pidmsg.append(tmpmsg.substring(index));
            VIN = checkVinDecode(pidmsg.toString());
        }
        //check if 41 is present in the message, then set index to start from that number in the message and read to the length to check for the message
        else if (tmpmsg.contains("41")) {
            //Old condition -> tmpmsg.indexOf("41") != -1
            int index = tmpmsg.indexOf("41");

            pidmsg.append(tmpmsg.substring(index, tmpmsg.length()));

            if (pidmsg.toString().contains("4100")) {
                //printing the supported pids to the terminal
                setPidsSupported(pidmsg.toString());
                return;
            } else {
                //print the pid msg to log cat
                Log.d("checkPids data", pidmsg.toString());
            }
        }

        //parsing the data before decoding it
        int A = 0;
        int B = 0;
        int PID = 0;

        if ((pidmsg != null) && (pidmsg.toString().matches("^[0-9A-F]+$"))) {
            //removes spaces
            String pidString = pidmsg.toString().trim();
            int index = pidmsg.indexOf("41");

            //calculating values for mode 01
            if (index != -1) {

                String submsg = pidmsg.substring(index);

                if (submsg.startsWith("41")) {
                    Log.d("reg expression:", submsg);
                    if (submsg.length() >= 8) {
                        PID = Integer.parseInt(submsg.substring(2, 4), 16);
                        A = Integer.parseInt(submsg.substring(4, 6), 16);
                        B = Integer.parseInt(submsg.substring(6, 8), 16);
                    }
                    //FIX THIS SHIT (might be fine now)
                    else if (submsg.length() < 8) {
                        PID = Integer.parseInt(submsg.substring(2, 4), 16);
                        A = Integer.parseInt(submsg.substring(4, 6), 16);
                    }


                    //print the pid msg to terminal (TEST)
                    Log.d("pid message before decoding it", String.valueOf(PID));

                    //decode pids and update UI on main thread
                    final int finalPID = PID;
                    final int finalA = A;
                    final int finalB = B;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            calculateEcuValues(finalPID, finalA, finalB);
                        }
                    });

                }
            }
        }
    }

    //new code for decoding Vin
    private String checkVinDecode(String msg){
        //check is message contains 49
        if(msg.contains("49")){
            int vinStartIndex = msg.indexOf("49");
            //check if VIN pattern exists in the hex string
            if (vinStartIndex != -1) {
                //moving start position after 49
                vinStartIndex += 2;
                // extract the VIN portion of the hex string
                String vinHex = msg.substring(vinStartIndex);

                //convert VIN hex string to bytes
                byte[] bytes = new byte[vinHex.length() / 2];
                for (int i = 0; i < vinHex.length(); i += 2) {
                    bytes[i / 2] = (byte) ((Character.digit(vinHex.charAt(i), 16) << 4)
                            + Character.digit(vinHex.charAt(i + 1), 16));
                }

                //decode bytes to ASCII characters
                StringBuilder rawVinMsg = new StringBuilder();
                for (byte b : bytes) {
                    rawVinMsg.append((char) b);
                }

                //using reg expression to get valid characters (capital letters and numbers)
                Pattern pattern = Pattern.compile("[A-Z0-9]+");
                Matcher matcher = pattern.matcher(rawVinMsg.toString());

                StringBuilder vinMsg = new StringBuilder();
                while (matcher.find()) {
                    vinMsg.append(matcher.group());
                }

                Log.d("VinDecode", "VIN = " + vinMsg.toString().trim());
                return vinMsg.toString().trim();
            }
            else {
                Log.d("VinDecode", "VIN Not found in hex string");
                return "VIN not found in hex string.";
            }
        }
        Log.d("VinDecode", "VIN Not found!");
        return "VIN not found.";
    }

    //new code for calculating the averages of array lists and converting it to a string
    public static String calculateAvgList(ArrayList<Integer> list) {
        //if theres nothing in the list
        if (list == null || list.isEmpty()) {
            //return "N/A";
            return "0";
        }

        int sum = 0;
        for (int num : list) {
            sum += num;
        }

        double answer = (double) sum / list.size();
        String s = Double.toString(answer);
        return s;
    }

    private void analyzeMsg(Message msg) {
        //cleaning the message (for mode 1)
        String tmpmsg = clearMsg(msg);
        //printing the voltage to terminal
        generateVolt(tmpmsg);
        //getting the device name and the protocol (SAE or ISO)
        getElmInfo(tmpmsg);
        //if the elm is not initialized
        if (!initialized) {
            sendInitCommands();
        } else {
            //check if 41 is present in the message, then set index to start from that number in the message and read to the length to check for the message
            //try adding calculateecumessage method into checkpids method rather than analyzepids method
            checkPids(tmpmsg);  //NEW CODE (redid function to check for valid data and parse the string accordingly)

            if (!m_getPids && m_detectPids == 1) {
                String sPIDs = "0100";
                sendEcuMessage(sPIDs);
                return;
            }

            if (commandmode) {
                getFaultInfo(tmpmsg);
                return;
            }

//            try {
//                analyzePIDS(tmpmsg);
//            } catch (Exception e) {
//                String errorMessage = "Error : " + e.getMessage();
//                Info.setText(errorMessage);
//            }

            sendDefaultCommands();
        }
    }

    private void getFaultInfo(String tmpmsg) {

            String substr = "43";
            //looking for starting position of 43 within tmpmsg string
            int index = tmpmsg.indexOf(substr);

            if (index == -1)
            {
                substr = "47";
                index = tmpmsg.indexOf(substr);
            }

            if (index != -1) {

                tmpmsg = tmpmsg.substring(index, tmpmsg.length());

                if (tmpmsg.substring(0, 2).equals(substr)) {

                    performCalculations(tmpmsg);

                    String faultCode = null;
                    String faultDesc = null;

                    if (!troubleCodesArray.isEmpty()) {
                        //Old condition -> troubleCodesArray.size() > 0

                        for (int i = 0; i < troubleCodesArray.size(); i++) {
                            faultCode = troubleCodesArray.get(i);
                            faultDesc = troubleCodes.getFaultCode(faultCode);

                            Log.e(TAG, "Fault Code: " + substr + " : " + faultCode + " desc: " + faultDesc);

                            if (faultCode != null && faultDesc != null) {
                                mConversationArrayAdapter.add(mConnectedDeviceName + ":  TroubleCode -> " + faultCode + "\n" + faultDesc);
                            } else if (faultCode != null && faultDesc == null) {
                                mConversationArrayAdapter.add(mConnectedDeviceName + ":  TroubleCode -> " + faultCode +
                                        "\n" + "Definition not found for code: " + faultCode);
                            }
                        }
                    } else {
                        faultCode = "No error found...";
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  TroubleCode -> " + faultCode);
                    }
                }
            }
    }
//for performing the fault code
    protected void performCalculations(String fault) {

        final String result = fault;
        String workingData = "";
        int startIndex = 0;
        troubleCodesArray.clear();

        try{

            if(result.contains("43")) {
                //result.indexOf("43") != -1
                workingData = result.replaceAll("^43|[\r\n]43|[\r\n]", "");
            }else if(result.contains("47")) {
                //result.indexOf("47") != -1
                workingData = result.replaceAll("^47|[\r\n]47|[\r\n]", "");
            }

            for (int begin = startIndex; begin < workingData.length(); begin += 4) {
                String dtc = "";
                byte b1 = hexStringToByteArray(workingData.charAt(begin));
                int ch1 = ((b1 & 0xC0) >> 6);
                int ch2 = ((b1 & 0x30) >> 4);
                dtc += dtcLetters[ch1];
                dtc += hexArray[ch2];
                dtc += workingData.substring(begin + 1, begin + 4);

                if (dtc.equals("P0000")) {
                    continue;
                }

                troubleCodesArray.add(dtc);
            }
        }catch(Exception e)
        {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    private byte hexStringToByteArray(char s) {
        return (byte) ((Character.digit(s, 16) << 4));
    }

    //getting the device name and the protocol
    private void getElmInfo(String tmpmsg) {

        if (tmpmsg.contains("ELM") || tmpmsg.contains("elm")) {
            devicename = tmpmsg;
        }

        if (tmpmsg.contains("SAE") || tmpmsg.contains("ISO")
                || tmpmsg.contains("sae") || tmpmsg.contains("iso") || tmpmsg.contains("AUTO")) {
            deviceprotocol = tmpmsg;
        }

        if (deviceprotocol != null && devicename != null) {
            devicename = devicename.replaceAll("STOPPED", "");
            deviceprotocol = deviceprotocol.replaceAll("STOPPED", "");
            String statusMessage = devicename + " " + deviceprotocol;
            Status.setText(statusMessage);
        }
    }

//printing the supported pids to the terminal
    private void setPidsSupported(String buffer) {

        String infoMessage = "Trying to get available pids : " + String.valueOf(trycount);
        Info.setText(infoMessage);
        trycount++;

        StringBuilder flags = new StringBuilder();
        String buf = buffer;//.toString();
        buf = buf.trim();
        buf = buf.replace("\t", "");
        buf = buf.replace(" ", "");
        buf = buf.replace(">", "");

        if (buf.indexOf("4100") == 0 || buf.indexOf("4120") == 0 || buf.indexOf("4140") == 0) {

            for (int i = 0; i < 8; i++) {
                String tmp = buf.substring(i + 4, i + 5);
                int data = Integer.valueOf(tmp, 16).intValue();
//                String retStr = Integer.toBinaryString(data);
                if ((data & 0x08) == 0x08) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x04) == 0x04) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x02) == 0x02) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x01) == 0x01) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
            }

            commandslist.clear();
            commandslist.add(0, GET_VIN);
            commandslist.add(1, VOLTAGE);
            int pid = 2;

            StringBuilder supportedPID = new StringBuilder();
            supportedPID.append("Supported PIDS:\n");
            for (int j = 0; j < flags.length(); j++) {
                if (flags.charAt(j) == '1') {
                    supportedPID.append(" " + PIDS[j] + " ");
                    if (!PIDS[j].contains("11") && !PIDS[j].contains("01") && !PIDS[j].contains("20")) {
                        commandslist.add(pid, "01" + PIDS[j]);
                        Log.i("SupportedPIDS", "Added PID" + PIDS[j] + " to index " + j);
                        pid++;
                    }
                }
            }
            m_getPids = true;
            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + supportedPID.toString());
            Log.i("SupportedPIDS", "List: " + supportedPID);
            whichCommand = 0;
            sendEcuMessage("ATRV"); //may not need this

        } else {

            return;
        }
    }

    private double calculateAverage(List<Double> listAvg) {
        Double sum = 0.0;
        for (Double val : listAvg) {
            sum += val;
        }
        return sum.doubleValue() / listAvg.size();
    }


//printing the voltage to terminal
    private void generateVolt(String msg) {

        String VoltText = null;

        if ((msg != null) && (msg.matches("\\s*[0-9]{1,2}([.][0-9]{1,2})\\s*"))) {

            VoltText = msg + "V";

            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + msg + "V");

        } else if ((msg != null) && (msg.matches("\\s*[0-9]{1,2}([.][0-9]{1,2})?V\\s*"))) {

            VoltText = msg;

            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + msg);
        }
        //updating text
        if (VoltText != null) {
            voltage.setText(VoltText);
        }
    }
    //calculating the pids
    private void calculateEcuValues(int PID, int A, int B) {

        double val = 0;
        int intval = 0;
        int tempC = 0;

        switch (PID) {

        case 4://PID(04): Engine Load
            // A*100/255
            val = A * 100 / 255;
            int calcLoad = (int) val;
            //for setting text to int value
            engineLoad.setText(Integer.toString(calcLoad) + " %");
            //adding string to terminal
            mConversationArrayAdapter.add("Engine Load: " + Integer.toString(calcLoad) + " %");

            double FuelFlowLH = (mMaf * calcLoad * mEnginedisplacement / 1000.0 / 714.0) + 0.8;

            if(calcLoad == 0)
                FuelFlowLH = 0;

            avgconsumption.add(FuelFlowLH);

            String fuelMessage = String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " l/h";
            Fuel.setText(fuelMessage);
            mConversationArrayAdapter.add("Fuel Consumption: " + String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " l/h");
            break;

        case 5://PID(05): Coolant Temperature
            //A-40
            tempC = A - 40;
            coolantTemp = tempC;
            String coolantMessage = Integer.toString(coolantTemp) + " C°";
            coolantTemperature.setText(coolantMessage);
            mConversationArrayAdapter.add("Enginetemp: " + Integer.toString(tempC) + " C°");

            break;

        case 11://PID(0B)
            //A
            mConversationArrayAdapter.add("Intake Man Pressure: " + Integer.toString(A) + " kPa");
            break;

        case 12: //PID(0C): RPM
            //((A*256)+B)/4
            val = ((A * 256) + B) / 4;
            intval = (int) val;
            rpmval = intval;
            String engineSpeedMessage = Integer.toString(intval) + " rpm";
            engineSpeed.setText(engineSpeedMessage);
            Log.d("enginespeed text:", engineSpeedMessage);
            //new code to add to array
            mConversationArrayAdapter.add("Engine Speed: " + Integer.toString(rpmval) + " rpm");
            break;


        case 13://PID(0D): KM
            // A
            //new code to add to array
            String vehicleSpeedMessage = Integer.toString(A) + " km/h";
            vehicleSpeed.setText(vehicleSpeedMessage);
            mConversationArrayAdapter.add("Vehicle Speed: " + Integer.toString(A) + " km/h");
            //new code to add to an array list for the averages to be sent to DB
            km_speed.add(A);
            break;

        case 15://PID(0F): Intake Temperature
            // A - 40
            tempC = A - 40;
            intakeairtemp = tempC;
            String airMessage = Integer.toString(intakeairtemp) + " C°";
            airTemperature.setText(airMessage);
            mConversationArrayAdapter.add("Intakeairtemp: " + Integer.toString(intakeairtemp) + " C°");
            break;

        case 16://PID(10): Maf
            // ((256*A)+B) / 100  [g/s]
            val = ((256 * A) + B) / 100;
            mMaf = (int) val;
            String mafMessage = Integer.toString(intval) + " g/s";
            mafAirFlow.setText(mafMessage);
            mConversationArrayAdapter.add("Maf Air Flow: " + Integer.toString(mMaf) + " g/s");
            break;

        case 17://PID(11): throttle position
            //A*100/255
            val = A * 100 / 255;
            intval = (int) val;
            String posMsg = Integer.toString(intval) + " %";
            throttlePosition.setText(posMsg);
            mConversationArrayAdapter.add(" Throttle position: " + Integer.toString(intval) + " %");
            break;

        case 31: //PID(1F): run time
            //256*A+B
            val = (256 * A) + B;
            intval = (int) val;
            engineOnTime = Integer.toString(intval);
            String runTimeMsg = engineOnTime + " s";
            runTimeEngStart.setText(runTimeMsg);
            mConversationArrayAdapter.add("Run time since engine start: " + Integer.toString(intval) + " seconds");
            break;

        case 35://PID(23)
            // ((A*256)+B)*0.079
            val = ((A * 256) + B) * 0.079;
            intval = (int) val;
            String fuelRailMsg = Integer.toString(intval) + " kPa";
            fuelRailPressure.setText(fuelRailMsg);
            mConversationArrayAdapter.add("Fuel Rail Pressure: " + Integer.toString(intval) + " kPa");
            break;

        case 49://PID(31)
            //(256*A)+B km
            val = (A * 256) + B;
            intval = (int) val;
            mileage = Integer.toString(intval);
            String distTravelMsg = mileage + " km";
            distTraveled.setText(distTravelMsg);
            mConversationArrayAdapter.add("Distance traveled: " + Integer.toString(intval) + " km");
            break;

        case 70://PID(46)
            // A-40 [DegC]
            tempC = A - 40;
            ambientairtemp = tempC;
            mConversationArrayAdapter.add("Ambientairtemp: " + Integer.toString(ambientairtemp) + " C°");
            break;

        case 82://PID(52)
            //100/255 * A
            val = (100/255) * A;
            intval = (int) val;
            mConversationArrayAdapter.add("Ethanol fuel %: " + Integer.toString(intval) + " %");

        case 92://PID(5C)
            //A-40
            tempC = A - 40;
            engineoiltemp = tempC;
            mConversationArrayAdapter.add("Engine oil temperature: " + Integer.toString(engineoiltemp) + " C°");
            break;

        case 94://PID(5E)
            //(256*A+B) / 20
            val = ((256 * A) + B) / 20;
            intval = (int) val;
            fuelRate.add(intval);
            mConversationArrayAdapter.add("Engine fuel rate: " + Integer.toString(intval) + " L/h");

        default:
    }
}

    enum RSP_ID {
        PROMPT(">"),
        OK("OK"),
        MODEL("ELM"),
        NODATA("NODATA"),
        SEARCH("SEARCHING"),
        ERROR("ERROR"),
        NOCONN("UNABLE"),
        NOCONN_MSG("UNABLE TO CONNECT"),
        NOCONN2("NABLETO"),
        CANERROR("CANERROR"),
        CONNECTED("ECU CONNECTED"),
        BUSBUSY("BUSBUSY"),
        BUSY("BUSY"),
        BUSERROR("BUSERROR"),
        BUSINIERR("BUSINIT:ERR"),
        BUSINIERR2("BUSINIT:BUS"),
        BUSINIERR3("BUSINIT:...ERR"),
        BUS("BUS"),
        FBERROR("FBERROR"),
        DATAERROR("DATAERROR"),
        BUFFERFULL("BUFFERFULL"),
        STOPPED("STOPPED"),
        RXERROR("<"),
        QMARK("?"),
        UNKNOWN("");
        private String response;

        RSP_ID(String response) {
            this.response = response;
        }

        @Override
        public String toString() {
            return response;
        }
    }
}