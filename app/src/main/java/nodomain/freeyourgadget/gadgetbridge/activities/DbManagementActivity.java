/*  Copyright (C) 2016-2018 Alberto, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.ImportExportSharedPreferences;


public class DbManagementActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(DbManagementActivity.class);
    private static SharedPreferences sharedPrefs;
    private ImportExportSharedPreferences shared_file = new ImportExportSharedPreferences();

    private Button exportDBButton;
    private Button importDBButton;
    private Button deleteOldActivityDBButton;
    private Button deleteDBButton;
    private Button firstDBButton;
    private Button syncDBButton;
    private Button tokenButton;
    private Button pubkeyButton;
    private TextView dbPath;

    private RequestQueue mRequestQueue;
    private StringRequest mStringRequest;
    private String url = "http://165.227.244.213:8882";//"http://188.166.138.93:8882/";
    private String urlsave = "http://165.227.244.213:8882/datamsave/";//"http:// 188.166.138.93:8882/datamsave/";
    private String urldevice = "http://165.227.244.213:8882/devicesave/";
    private String urlsync = "http://165.227.244.213:8882/sync/";
    private String urltoken;
    private String urlDevicesave;
    private String comrefIN = "cnrl-2356388731";
    //private String urlsyncdevice;
    private String editTokenStr;
    // holds sync data for POST call
    ArrayList batchdata;
    JSONArray arraysync = new JSONArray();
    private String nowDate;
    private String lastsyncDate;
    private Object listd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_management);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // has the first time setup been run, check mobiledb
        dbPath = (TextView) findViewById(R.id.activity_db_management_path);
        dbPath.setText(getExternalPath());

        startStatus();

        exportDBButton = (Button) findViewById(R.id.exportDBButton);
        exportDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDB();
            }
        });
        importDBButton = (Button) findViewById(R.id.importDBButton);
        importDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importDB();
            }
        });

        // save manual input token
        tokenButton = findViewById(R.id.tokenButton);
        tokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText editToken = (EditText)findViewById(R.id.tokenText);
                String tokentoSave = editToken.getText().toString();
                // save to sqlite
                SaveUpdateToken(tokentoSave, "1");
            }
        });

        // save publickey identity
        pubkeyButton = findViewById(R.id.pubkeyButton);
        pubkeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editToken = (EditText)findViewById(R.id.publickey);
                String keytoSave = editToken.getText().toString();
                // save to sqlite
                SaveUpdateToken(keytoSave, "2");
            }
        });

        tokenButton.setVisibility(View.INVISIBLE);
        pubkeyButton.setVisibility(View.INVISIBLE);

        int oldDBVisibility = hasOldActivityDatabase() ? View.VISIBLE : View.GONE;

        deleteOldActivityDBButton = (Button) findViewById(R.id.deleteOldActivityDB);
        deleteOldActivityDBButton.setVisibility(oldDBVisibility);
        deleteOldActivityDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteOldActivityDbFile();
            }
        });

        deleteDBButton = (Button) findViewById(R.id.emptyDBButton);
        deleteDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteActivityDatabase();
            }
        });
    }

    private boolean hasOldActivityDatabase() {
        return new DBHelper(this).existsDB("ActivityDatabase");
    }

    private String getExternalPath() {
        try {
            return FileUtils.getExternalFilesDir().getAbsolutePath();
        } catch (Exception ex) {
            LOG.warn("Unable to get external files dir", ex);
        }
        return getString(R.string.dbmanagementactivvity_cannot_access_export_path);
    }

    private void exportShared() {
        // BEGIN EXAMPLE
        File myPath = null;
        try {
            myPath = FileUtils.getExternalFilesDir();
            File myFile = new File(myPath, "Export_preference");
            shared_file.exportToFile(sharedPrefs,myFile,null);
        } catch (IOException ex) {
            GB.toast(this, getString(R.string.dbmanagementactivity_error_exporting_shared, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    private void importShared() {
        // BEGIN EXAMPLE
        File myPath = null;
        try {
            myPath = FileUtils.getExternalFilesDir();
            File myFile = new File(myPath, "Export_preference");
            shared_file.importFromFile(sharedPrefs,myFile );
        } catch (Exception ex) {
            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_error_importing_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    private void exportDB() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(this);
            File dir = FileUtils.getExternalFilesDir();
            File destFile = helper.exportDB(dbHandler, dir);
            GB.toast(this, getString(R.string.dbmanagementactivity_exported_to, destFile.getAbsolutePath()), Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception ex) {
            GB.toast(this, getString(R.string.dbmanagementactivity_error_exporting_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    // check for first time use
    private void startStatus() {
        // has the starting addtional sqlite tables been added?
        String status = queryToken("3");
        // Toast.makeText(getApplicationContext(),"status start: " + status, Toast.LENGTH_LONG).show();
        if(Objects.equals(status, "true")) {
            // display sync button
            firstDBButton = findViewById(R.id.firstDBButton);
            firstDBButton.setVisibility(View.INVISIBLE);
            syncDBButton = findViewById(R.id.syncDBButton);
            syncDBButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                // first check there is interenet connection
                    boolean InternetOn = isNetworkConnected();
                    if(InternetOn == true) {
                        syncDB();
                    }
                    else {
                    Toast.makeText(getApplicationContext(),"NO INTERNET CONNECTION", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        else {
            // need to query deviceData saved? If yes only show First Setup button
            syncDBButton = findViewById(R.id.syncDBButton);
            syncDBButton.setVisibility(View.INVISIBLE);
            TextView tv = (TextView) findViewById(R.id.syncDate);
            tv.setText("Please enter publickey and storage token below then click FIRST SETUP");
            firstDBButton = findViewById(R.id.firstDBButton);
            firstDBButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                // first check there is interenet connection
                boolean InternetOn = isNetworkConnected();
                if(InternetOn == true) {
                    Toast.makeText(getApplicationContext(),"FIRST SETUP CLICKED", Toast.LENGTH_LONG).show();
                        firstSettingsDB();
                }
                else {
                    Toast.makeText(getApplicationContext(),"NO INTERNET CONNECTION", Toast.LENGTH_LONG).show();
                }
                }
            });
        }
    }

    // first time connection to network
    private void firstSettingsDB() {
        // check token and publickey are entered, if not prompt to do so
        EditText editToken = (EditText)findViewById(R.id.tokenText);
        String tokentoSave = editToken.getText().toString();
        SaveUpdateToken(tokentoSave, "1");
        // form token URL  check if saved token
        EditText editTokenk = (EditText)findViewById(R.id.publickey);
        String keytoSave = editTokenk.getText().toString();
        SaveUpdateToken(keytoSave, "2");
        // if both entered proceed to save device data
        urlDevicesave = urldevice + keytoSave + "/" + tokentoSave;
        JSONObject deviceList = queryDeviceFull(keytoSave);
        JSONObject deviceAtt = queryDeviceAttrib();
        // now  prepare JSON and save to data vault
        prepareDeviceData(deviceList, deviceAtt);
    }

    // batching utility method
    public static <T> List<List<T>> getBatches(List collection, int batchSize){
        int i = 0;
        List<List<T>> batches = new ArrayList<List<T>>();
        while(i<collection.size()){
            int nextInc = Math.min(collection.size()-i,batchSize);
            List<T> batch = collection.subList(i,i+nextInc);
            batches.add(batch);
            i = i + nextInc;
        }
        return batches;
    }

    // setup additonal tables
    private void startAddtables() {

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            // need to create a new table
            db.execSQL("CREATE TABLE IF NOT EXISTS TOKEN(TID INTEGER, hashtoken STRING)");

            try {
                db.execSQL("INSERT INTO TOKEN(TID, hashtoken) VALUES (1, 'none' )");
                // Toast.makeText(this, "insert first TOKEN", Toast.LENGTH_LONG).show();
                db.execSQL("INSERT INTO TOKEN(TID, hashtoken) VALUES (2, '0000000' )");
                // Toast.makeText(this, "insert blank publickey", Toast.LENGTH_LONG).show();
                db.execSQL("INSERT INTO TOKEN(TID, hashtoken) VALUES (3, 'false' )");
                // Toast.makeText(this, "setup tables", Toast.LENGTH_LONG).show();
                // GB.toast(this, "First time table setup", Toast.LENGTH_LONG, GB.INFO);
                } catch (Exception e) {
                /* no table set it up */

                }
        } catch (Exception ex) {
            //GB.toast(this, "error with PRE sync", Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    private void prepareDeviceData ( JSONObject deviceIN, JSONObject deviceAttin) {
        // form device JSONobject
        JSONObject merged = new JSONObject();
        JSONObject[] objs = new JSONObject[] { deviceIN, deviceAttin };
        for (JSONObject obj : objs) {
            Iterator it = obj.keys();
            while (it.hasNext()) {
                String key = (String)it.next();
                try {
                    merged.put(key, obj.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        PostDevicedata("device", merged);
    }

    // prepare data to sync to peer to peer network
    private void syncDB() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            GB.toast(this, "getting ready for sync", Toast.LENGTH_LONG, GB.INFO);
            // PUBLICKEY AND DATA COMPUTATIONAL REFERENCE - BOTH HASHES
            String publickeyIN = queryToken("2");
            String comref = comrefIN;
            // get token from app
            // form token URL  check if saved token
            String liveToken = queryToken("1");
            if(liveToken != "none") {
                urltoken = urlsave + publickeyIN + "/" + liveToken;
                // need to check how many devices OR any new devices added?
                List deviceList = queryDevices();
                for (int i = 0; i < deviceList.size(); i++) {

                    JSONObject listd = (JSONObject) deviceList.get(i);
                    String deviceID = listd.getString("_id");
                    //Toast.makeText(this, "device ID == " + deviceID, Toast.LENGTH_LONG).show();
                    JSONObject listdM = (JSONObject) deviceList.get(i);
                    String deviceMac = listdM.getString("IDENTIFER");
                    //Toast.makeText(this, "device MAC == " + deviceMac, Toast.LENGTH_LONG).show();
                    String urlsyncdevice = urlsync + publickeyIN + "/" + liveToken + "/" + deviceMac;
                    // Make network API call to get last sync date from storage
                    syncData(urlsyncdevice, deviceID, deviceMac, publickeyIN, comref);
                    //volleyGet(urlsyncdevice, publickeyIN, liveToken);
                }
            }
            else {
                Toast.makeText(this, "Please add a TOKEN", Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            //GB.toast(this, "error with PRE sync", Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }
/*
    private void volleyGet(final String didIN, String publickeyIN, String liveTokenIN) {
        //Toast.makeText(getApplicationContext(),"volleyGET:" + didIN, Toast.LENGTH_LONG).show();
        String liveGet = urlsync + publickeyIN + "/" + liveTokenIN + "/E3:30:80:7A:77:B5";//F1:D1:D5:6A:32:D6";

        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.GET, liveGet, null,
                new Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // Toast.makeText(getApplicationContext(),"GET Response : "+ response.toString(), Toast.LENGTH_LONG).show();

                    }
                }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //serverResp.setText("Error getting response");
                Toast.makeText(getApplicationContext(),"save volley GET data error "+ error.toString(), Toast.LENGTH_LONG).show();//display the response on screen
                Log.i(null,"Error :" + error.toString());
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }
*/
    public void syncData(final String urlsyncdevice, final String deviceID, final String deviceMac, final String publickeyIN, final String comrefIN) {
         getSyncResponse(new DataCallback() {
            String lastsyncitem;
            @Override
            public void onSuccess(String result) {
                lastsyncitem = result;
                // process next batch of syning
                // form query to get last sync date
                Date liveDate;
                Long longmillseconds;
                Long millseconds;
                liveDate = DateTimeUtils.todayUTC();
                longmillseconds = liveDate.getTime();
                millseconds = longmillseconds / 1000;

                long lastsyncData3 = Integer.parseInt(lastsyncitem);
                long datemillprev = Long.valueOf(lastsyncData3 * 1000);
                Integer addtick =  Integer.parseInt(lastsyncitem);
                Integer startQuerytime = addtick + 2;
                String newstartQuery = startQuerytime.toString();

                long msTime = System.currentTimeMillis();
                SimpleDateFormat formatter = new SimpleDateFormat("MM'/'dd'/'y hh:mm aa");
                String curDate = formatter.format(datemillprev);
                TextView tv = (TextView) findViewById(R.id.syncText);
                tv.setText("Last sync date: " + curDate);
                Toast.makeText(getApplicationContext(),"SYNCING" + deviceID, Toast.LENGTH_LONG).show();

                nowDate = millseconds.toString();
                TextView ntv = (TextView) findViewById(R.id.nowDate);
                ntv.setText("NOW date: " + liveDate);
                // make backup on mobile app of sync date
                syncTimestampDB(newstartQuery, deviceID);
                queryDeviceData(deviceID, deviceMac, publickeyIN, comrefIN, nowDate, newstartQuery);
            }
        }, urlsyncdevice);

    }

    public interface DataCallback {
        void onSuccess(String result);
    }

    private String getSyncResponse(final DataCallback callback, String urlsyncdevice) {
        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.GET, urlsyncdevice, null,
                new Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // Toast.makeText(getApplicationContext(),"String Response : "+ response.toString(), Toast.LENGTH_LONG).show();
                        // extract first JSONobject from array
                        JSONObject returnEntry = null;
                        String firstSynctime;
                        try {
                            returnEntry = response.getJSONObject(response.length()-1);
                            firstSynctime = returnEntry.optString("timestamp");
                            // if null, first time use
                            if (firstSynctime.length() == 0) {
                                firstSynctime = "1";
                            }
                            callback.onSuccess(firstSynctime);

                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(),"SYNC TIME server nothing returned", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }

                    }
                }, new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //serverResp.setText("Error getting response");
                Toast.makeText(getApplicationContext(),"save volley sync error "+ error.toString(), Toast.LENGTH_LONG).show();//display the response on screen
                Log.i(null,"Error :" + error.toString());
            }
        });

        // Access the RequestQueue through your singleton class.
        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
        return null;
    }

    private void queryDeviceData(String deviceID, String deviceMac, String publickeyIN, String comrefIN, String nowTimeIN, String syncTimeIn) {
        // query for token or ask to input
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            // form the query for data
            // Define a projection that specifies which columns from the database
            // you will actually use after this query.
            String[] projection = {
                    "TIMESTAMP",
                    "DEVICE_ID",
                    "USER_ID",
                    "RAW_INTENSITY",
                    "STEPS",
                    "RAW_KIND",
                    "HEART_RATE"
            };
            TextView tv = (TextView) findViewById(R.id.syncText);
            tv.setText("Last CLOUD date: " + syncTimeIn);
            TextView ntv = (TextView) findViewById(R.id.nowDate);
            ntv.setText("NOW date: " + nowTimeIN);
            // Filter results
            String selection = "DEVICE_ID" + " = ? AND TIMESTAMP BETWEEN ? AND ?";
            String[] selectionArgs = {deviceID, syncTimeIn, nowTimeIN};
            // How you want the results sorted in the resulting Cursor
            String sortOrderd =
                    "TIMESTAMP" + " ASC";

            Cursor cursor = db.query(
                    "MI_BAND_ACTIVITY_SAMPLE",   // The table to query
                    projection,             // The array of columns to return (pass null to get all)
                    selection,              // The columns for the WHERE clause
                    selectionArgs,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sortOrderd               // The sort order
            );
            String message = null;
            // iterate of data in cursor form/ pass data on to volley
            // find size of data
            Integer syncLength = cursor.getCount();
            //Toast.makeText(getApplicationContext(),"cursor length" + syncLength, Toast.LENGTH_LONG).show();
            // first prepare normal list array from Cursor to allow sublit operation to batch
            List all = new ArrayList<>();
            while (cursor.moveToNext()) {
                JSONObject item = new JSONObject();

                long timeId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("TIMESTAMP"));
                long devicegId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("DEVICE_ID"));
                long userId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("USER_ID"));
                long rawiId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("RAW_INTENSITY"));
                long stepsId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("STEPS"));
                long rawkId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("RAW_KIND"));
                long heartId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("HEART_RATE"));

                // form an object and add to array list
                item.put("timestamp", timeId);
                item.put("device_mac", deviceMac);
                item.put("device_id", devicegId);
                item.put("user_id",userId);
                item.put("raw_intensity", rawiId);
                item.put("steps", stepsId);
                item.put("raw_kind", rawkId);
                item.put("heart_rate", heartId);
                item.put("publickey", publickeyIN);
                item.put("compref", comrefIN);
                all.add(item);

            }
            cursor.close();
            // find size of new LIST
            Integer allLength = all.size();
            // prepare batches
            List batched = getBatches(all, 100);
            Integer batchLength = batched.size();
            Toast.makeText(getApplicationContext(), "Batched length" + batchLength, Toast.LENGTH_LONG).show();
            if (batchLength <= 1 && syncLength != 0) {
                // Toast.makeText(getApplicationContext(), "less than 1 - go ahead sync", Toast.LENGTH_LONG).show();
                // make a put ie save to HS network
                prepareSyncJSON((List) batched.get(0), deviceID);

            } else {
                if (syncLength == 0) {
                    Toast.makeText(getApplicationContext(), "Nothing to Sync", Toast.LENGTH_LONG).show();

                } else {
                    // Toast.makeText(getApplicationContext(), "Preparing Batches Sync", Toast.LENGTH_LONG).show();
                    // itterate batch listed
                    int j = 1;
                    int sizeB = batched.size();
                    // Toast.makeText(getApplicationContext(), "Batch size " + sizeB, Toast.LENGTH_SHORT).show();
                    for (int i = 0; i < sizeB; i++) {
                        // Toast.makeText(getApplicationContext(), "loop-- " + i, Toast.LENGTH_SHORT).show();
                        Object singleB = batched.get(i);
                        // Toast.makeText(getApplicationContext(), "json " + singleB.toString(), Toast.LENGTH_SHORT).show();
                        prepareSyncJSON((List) batched.get(i), deviceID);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareSyncJSON (Object batchIN, String didIN) {
        // form JSONarray
        Object json = null;
        JSONArray jsonArray = null;
        try {
            json = new JSONTokener(batchIN.toString()).nextValue();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (json instanceof JSONArray) {
            jsonArray = (JSONArray) json;
        }
        PostandRequestResponse(didIN, jsonArray);
    }

    private void PostandRequestResponse(final String didIN, final JSONArray syncDataIn) {
        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.POST, urltoken, syncDataIn,
        new Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                //.setText("String Response : "+ response.toString());
                // Toast.makeText(getApplicationContext(),"String Response : "+ response.toString(), Toast.LENGTH_LONG).show();
                JSONObject respPost = null;
                try {
                    respPost = response.getJSONObject(response.length()-1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Toast.makeText(getApplicationContext(),"FIRST : "+ respPost.toString(), Toast.LENGTH_LONG).show();
                String syncMessage = respPost.optString("save");

                TextView stv=(TextView)findViewById(R.id.syncDate);
                String SyncReponse = syncMessage;
                stv.setText("Device confirm " + SyncReponse);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //serverResp.setText("Error getting response");
                Toast.makeText(getApplicationContext(),"save volley POST data error "+ error.toString(), Toast.LENGTH_LONG).show();//display the response on screen
                Log.i(null,"Error :" + error.toString());
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    // save last sync timestamp to sqlite as a backup, Primary Chick is to network
    private void syncTimestampDB(String syncdataIN, String dID) {
        Integer addone =  Integer.parseInt(syncdataIN);
        Integer addtwo = addone + 2;
        String localdata = addtwo.toString();
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();

        } catch (Exception e) {
            e.printStackTrace();
            //tv.setText("error SYNSTAMP: " + e.toString());
            GB.toast(this, "error syncstamp" + e.toString(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void PostDevicedata(final String didIN, final JSONObject deviceDataIn) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, urlDevicesave, deviceDataIn,
                new Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //.setText("String Response : "+ response.toString());
                        // Toast.makeText(getApplicationContext(), "String Response : " + response.toString(), Toast.LENGTH_LONG).show();
                        // extract message
                         String messageD =  response.optString("save");
                        // Toast.makeText(getApplicationContext(), "String  : " + messageD, Toast.LENGTH_LONG).show();
                        if(Objects.equals(messageD, "passedD")) {
                            // update buttons
                            // Toast.makeText(getApplicationContext(), "pass logic", Toast.LENGTH_LONG).show();
                           firstDBButton.setVisibility(View.INVISIBLE);
                           syncDBButton.setVisibility(View.VISIBLE);
                           // update table set device data saved
                            SaveUpdateToken("true", "3");
                        }
                    }
                }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //serverResp.setText("Error getting response");
                Toast.makeText(getApplicationContext(),"save volley POST data error "+ error.toString(), Toast.LENGTH_LONG).show();//display the response on screen
                Log.i(null,"Error :" + error.toString());
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }


    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean SaveUpdateToken(String tokenIN, String tidIN){
        String localtoken = tokenIN.toString();
        boolean tableExists = false;
        try (DBHandler dbHandler = GBApplication.acquireDB())
        {
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            // need to create a new table to save sync data to ptop work
            /* get cursor on it */
            try {
                db.query("TOKEN", null,
                        null, null, null, null, null);
                tableExists = true;

                if(tableExists == true) {

                    if(tidIN == "1") {
                        // then update the table with the new token/key
                        db.execSQL("UPDATE TOKEN SET hashtoken = '" + tokenIN + "' WHERE TID = 1");
                        // Toast.makeText(this, "second UPDATED TOKEN", Toast.LENGTH_SHORT).show();
                    }
                    else if(tidIN == "2") {
                        db.execSQL("UPDATE TOKEN SET hashtoken = '" + tokenIN + "' WHERE TID = 2");
                        // Toast.makeText(this, "second UPDATED PUBKEY", Toast.LENGTH_SHORT).show();
                    }
                    else if(tidIN == "3") {
                        db.execSQL("UPDATE TOKEN SET hashtoken = '" + tokenIN + "' WHERE TID = 3");
                        // Toast.makeText(this, "device data saved to network", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                /* no table set it up */
                db.execSQL("CREATE TABLE IF NOT EXISTS TOKEN(TID INTEGER, hashtoken STRING)");
                Toast.makeText(this, "Database Created TOKEN", Toast.LENGTH_LONG).show();
                tableExists = true;

                if(tableExists == true) {
                    // insert first sync date
                    db.execSQL("INSERT INTO TOKEN(TID, hashtoken) VALUES (1, '" + localtoken + "' )");
                    // Toast.makeText(this, "insert first TOKEN", Toast.LENGTH_LONG).show();
                    db.execSQL("INSERT INTO TOKEN(TID, hashtoken) VALUES (2, '0000000' )");
                    // Toast.makeText(this, "insert blank publickey", Toast.LENGTH_LONG).show();
                    db.execSQL("INSERT INTO TOKEN(TID, hashtoken) VALUES (3, 'false' )");
                    // Toast.makeText(this, "setup tables", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            GB.toast(this, "setup" + e.toString(), Toast.LENGTH_LONG, GB.ERROR, e);
            //TextView stv=(TextView)findViewById(R.id.syncDate);
            //stv.setText("insert token" + e.toString());
            return false;
        }

        return true;
    }

    private String queryToken(String typeid) {
        String tStatus;
        // query for token or ask to input
        try (DBHandler dbHandler = GBApplication.acquireDB())
        {
            // Toast.makeText(getApplicationContext(),"start token query ", Toast.LENGTH_SHORT).show();
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();

            String tokenString;
            // form query
            // query last sync table to get last sync date
            String[] projectiond = {
                    "hashtoken"
            };
            // Filter results
            String selectiond = "TID = ?";
            String[] selectionArgsd = {typeid};
            // How you want the results sorted in the resulting Cursor
            String sortOrder = "";

            Cursor liveToken = db.query(
                    "TOKEN",   // The table to query
                    projectiond,             // The array of columns to return (pass null to get all)
                    selectiond,              // The columns for the WHERE clause
                    selectionArgsd,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sortOrder               // The sort order
            );
            liveToken.moveToFirst();
            tokenString = liveToken.getString(liveToken.getColumnIndex("hashtoken"));
            tStatus = tokenString;
            // return tokenString;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"ERROR token query ", Toast.LENGTH_SHORT).show();
             // no tables to setup additional tables
            startAddtables();
            tStatus = "none";
            // return "none";
        }
        // Toast.makeText(getApplicationContext(),"RETURN token query-- " + tStatus, Toast.LENGTH_SHORT).show();
        return tStatus;
    }

    private List queryDevices() {
        try (DBHandler dbHandler = GBApplication.acquireDB())
        {
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            // form query
            String[] projectiond = {
                    "_id","IDENTIFIER"
            };
            // Filter results
            String selectiond = "";
            String[] selectionArgsd = {};
            // How you want the results sorted in the resulting Cursor
            String sortOrder = "";
            Cursor liveDevices = db.query(
                    "DEVICE",   // The table to query
                    projectiond,             // The array of columns to return (pass null to get all)
                    selectiond,              // The columns for the WHERE clause
                    selectionArgsd,          // The values for the WHERE clause
                    null,            // don't group the rows
                    null,             // don't filter by row groups
                    sortOrder               // The sort order
            );
            List deviceArray = new ArrayList<>();
            while (liveDevices.moveToNext()) {
                //ArrayList<String> dlist = new ArrayList<String>();
                JSONObject dlist = new JSONObject();
                Integer deviceId = liveDevices.getInt(liveDevices.getColumnIndexOrThrow("_id"));
                String deviceMac = liveDevices.getString(liveDevices.getColumnIndex("IDENTIFIER"));
                dlist.put("_id",deviceId.toString());
                dlist.put("IDENTIFER", deviceMac);
                deviceArray.add(dlist);
            }
            String listdb = deviceArray.toString();
            liveDevices.close();
            return deviceArray;
        } catch (Exception e) {
            e.printStackTrace();
            //GB.toast(this, "error query token" + e.toString(), Toast.LENGTH_LONG, GB.ERROR, e);
            TextView stv=(TextView)findViewById(R.id.syncDate);
            stv.setText("number devices" + e.toString());
            List deviceArray = new ArrayList<>();
            deviceArray.add("none");
            return deviceArray;
        }
    }

    private JSONObject queryDeviceFull(String PublickeyIN) {
        try (DBHandler dbHandler = GBApplication.acquireDB())
        {
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            // form query
            String[] projectiond = {
                    "_id","NAME","MANUFACTURER","IDENTIFIER","TYPE","MODEL"
            };

            // Filter results
            String selectiond = "";
            String[] selectionArgsd = {};
            // How you want the results sorted in the resulting Cursor
            String sortOrder = "";
            Cursor liveDevices = db.query(
                    "DEVICE",   // The table to query
                    projectiond,             // The array of columns to return (pass null to get all)
                    selectiond,              // The columns for the WHERE clause
                    selectionArgsd,          // The values for the WHERE clause
                    null,            // don't group the rows
                    null,             // don't filter by row groups
                    sortOrder               // The sort order
            );
            List deviceArray = new ArrayList<>();
            JSONObject dlist = new JSONObject();
            while (liveDevices.moveToNext()) {
                // JSONObject dlist = new JSONObject();
                // Integer deviceId = liveDevices.getInt(liveDevices.getColumnIndexOrThrow("_id"));
                String devicenName = liveDevices.getString(liveDevices.getColumnIndex("NAME"));
                String deviceManufacturer = liveDevices.getString(liveDevices.getColumnIndex("MANUFACTURER"));
                String deviceMac = liveDevices.getString(liveDevices.getColumnIndex("IDENTIFIER"));
                String deviceType = liveDevices.getString(liveDevices.getColumnIndex("TYPE"));
                String deviceModel = liveDevices.getString(liveDevices.getColumnIndex("MODEL"));

                dlist.put("publickey", PublickeyIN);
                dlist.put("device_sensor1", "lightLED");
                dlist.put("device_sensor2", "accelerometer");
                dlist.put("device_name", devicenName);
                dlist.put("device_manufacturer", deviceManufacturer);
                dlist.put("device_mac", deviceMac);
                dlist.put("device_type", deviceType);
                dlist.put("device_model", deviceModel);
                dlist.put("cnrl", "cnrl-33221101");

                deviceArray.add(dlist);
            }
            // String listdb = deviceArray.toString();
            liveDevices.close();
            // return deviceArray;
            return dlist;

        } catch (Exception e) {
            e.printStackTrace();
            //GB.toast(this, "error device attributes " + e.toString(), Toast.LENGTH_LONG, GB.ERROR, e);
            TextView stv=(TextView)findViewById(R.id.syncDate);
            stv.setText("device attributes " + e.toString());
            List deviceArray = new ArrayList<>();
            deviceArray.add("none");
            JSONObject dlist = new JSONObject();
            return dlist;
        }
    }

    private JSONObject queryDeviceAttrib() {
        try (DBHandler dbHandler = GBApplication.acquireDB())
        {
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            // form query
            String[] projectiond = {
                    "_id","FIRMWARE_VERSION1","FIRMWARE_VERSION2","VALID_FROM_UTC","VALID_TO_UTC","DEVICE_ID","VOLATILE_IDENTIFIER"
            };
            // Filter results
            String selectiond = "";
            String[] selectionArgsd = {};
            // How you want the results sorted in the resulting Cursor
            String sortOrder = "";
            Cursor liveDAtt = db.query(
                    "DEVICE_ATTRIBUTES",   // The table to query
                    projectiond,             // The array of columns to return (pass null to get all)
                    selectiond,              // The columns for the WHERE clause
                    selectionArgsd,          // The values for the WHERE clause
                    null,            // don't group the rows
                    null,             // don't filter by row groups
                    sortOrder               // The sort order
            );
            List deviceAttArray = new ArrayList<>();
            JSONObject dlist = new JSONObject();
            while (liveDAtt.moveToNext()) {

                String deviceFirmware = liveDAtt.getString(liveDAtt.getColumnIndex("FIRMWARE_VERSION1"));
                String deviceFirmware2 = liveDAtt.getString(liveDAtt.getColumnIndex("FIRMWARE_VERSION2"));
                String deviceValidF = liveDAtt.getString(liveDAtt.getColumnIndex("VALID_FROM_UTC"));
                String deviceValidT = liveDAtt.getString(liveDAtt.getColumnIndex("VALID_TO_UTC"));
                String deviceMac = liveDAtt.getString(liveDAtt.getColumnIndex("DEVICE_ID"));
                String deviceVol = liveDAtt.getString(liveDAtt.getColumnIndex("VOLATILE_IDENTIFIER"));

                // dlist.put("_id",deviceId.toString());
                dlist.put("device_firmware", deviceFirmware);
                dlist.put("device_firmware2", deviceFirmware2);
                dlist.put("device_validfrom", deviceValidF);
                dlist.put("device_validto", deviceValidT);
                dlist.put("device_sensor1", "lightLED");
                dlist.put("device_sensor2", "accelerometer");
                dlist.put("device_mobile", "993399393939");
                dlist.put("active", true);

                deviceAttArray.add(dlist);
            }

            liveDAtt.close();
            // return deviceAttArray;
            return dlist;

        } catch (Exception e) {
            e.printStackTrace();
            //GB.toast(this, "error device attributes" + e.toString(), Toast.LENGTH_LONG, GB.ERROR, e);
            TextView stv=(TextView)findViewById(R.id.syncDate);
            stv.setText("devices attributes" + e.toString());
            List deviceArray = new ArrayList<>();
            deviceArray.add("none");
            // return deviceArray;
            JSONObject dlist = new JSONObject();
            return dlist;
        }
    }

    private boolean getPublickey() {

        return true;
    }

    private void importDB() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.dbmanagementactivity_import_data_title)
                .setMessage(R.string.dbmanagementactivity_overwrite_database_confirmation)
                .setPositiveButton(R.string.dbmanagementactivity_overwrite, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try (DBHandler dbHandler = GBApplication.acquireDB()) {
                            importShared();
                            DBHelper helper = new DBHelper(DbManagementActivity.this);
                            File dir = FileUtils.getExternalFilesDir();
                            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
                            File sourceFile = new File(dir, sqLiteOpenHelper.getDatabaseName());
                            helper.importDB(dbHandler, sourceFile);
                            helper.validateDB(sqLiteOpenHelper);
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_import_successful), Toast.LENGTH_LONG, GB.INFO);
                        } catch (Exception ex) {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_error_importing_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void deleteActivityDatabase() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.dbmanagementactivity_delete_activity_data_title)
                .setMessage(R.string.dbmanagementactivity_really_delete_entire_db)
                .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (GBApplication.deleteActivityDatabase(DbManagementActivity.this)) {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_database_successfully_deleted), Toast.LENGTH_SHORT, GB.INFO);
                        } else {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_db_deletion_failed), Toast.LENGTH_SHORT, GB.INFO);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void deleteOldActivityDbFile() {
        new AlertDialog.Builder(this).setCancelable(true);
        new AlertDialog.Builder(this).setTitle(R.string.dbmanagementactivity_delete_old_activity_db);
        new AlertDialog.Builder(this).setMessage(R.string.dbmanagementactivity_delete_old_activitydb_confirmation);
        new AlertDialog.Builder(this).setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (GBApplication.deleteOldActivityDatabase(DbManagementActivity.this)) {
                    GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_old_activity_db_successfully_deleted), Toast.LENGTH_SHORT, GB.INFO);
                } else {
                    GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_old_activity_db_deletion_failed), Toast.LENGTH_SHORT, GB.INFO);
                }
            }
        });
        new AlertDialog.Builder(this).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        new AlertDialog.Builder(this).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
