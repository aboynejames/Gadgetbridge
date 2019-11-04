package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.ImportExportSharedPreferences;




public class HealthspanAnalysis extends AbstractGBActivity {

    private static final Logger LOG = LoggerFactory.getLogger(HealthspanAnalysis.class);
    private static SharedPreferences sharedPrefs;
    private ImportExportSharedPreferences shared_file = new ImportExportSharedPreferences();

    private TextView dbPath;
    private WebView webContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_healthspan);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        RESTapiCall tokenObj = new RESTapiCall();
        WebView webView = (WebView) findViewById(R.id.webview);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        String hsURL = "file:///android_asset/index.html";
        webView.loadUrl(hsURL);
        webView.addJavascriptInterface(tokenObj, "wvt");
    }

    class RESTapiCall
    {
        @JavascriptInterface
        public String setAPItoken() throws JSONException {
            String tokLive;
            String keyLive;
            String deviceLive;
            String deviceFirst;
            // make db call for pubkey and token
            String liveToken = queryToken("1");
            String publickeyIN = queryToken("2");
            // get list of active devices
            List deviceList = queryDevices();
            if (deviceList.size() == 0)
            {
                // nothing set none
                deviceFirst = "none";
            }
            else {
                // deviceFirst = (String) deviceList.get(0);
                JSONObject listd = (JSONObject) deviceList.get(0);
                deviceFirst = listd.getString("IDENTIFER");
            }
            keyLive = publickeyIN; //"22FQ8dJEApww33p31935";
            tokLive = liveToken; //"9d93d9d8cv7js9sj4765s120sllkudp389cm";
            deviceLive = deviceFirst; //"F1:D1:D5:6A:32:D6";

            JSONObject Rsettings = new JSONObject();
            Rsettings.put("publickey", keyLive);
            Rsettings.put("token", tokLive);
            Rsettings.put("deviceL", deviceLive);

            return String.valueOf(Rsettings);
        }
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
            tStatus = "none";
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
            // Toast.makeText(getApplicationContext(),"RETURN devicelist-- " + deviceArray.toString(), Toast.LENGTH_SHORT).show();
            return deviceArray;
        } catch (Exception e) {
            e.printStackTrace();
            // GB.toast(this, "error query token" + e.toString(), Toast.LENGTH_LONG, GB.ERROR, e);
            List deviceArray = new ArrayList<>();
            deviceArray.add("none");
            return deviceArray;
        }
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

}
