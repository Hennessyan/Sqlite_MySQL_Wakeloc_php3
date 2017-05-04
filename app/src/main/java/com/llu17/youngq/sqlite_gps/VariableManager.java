package com.llu17.youngq.sqlite_gps;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.llu17.youngq.sqlite_gps.data.GpsContract;
import com.llu17.youngq.sqlite_gps.data.GpsDbHelper;
import com.llu17.youngq.sqlite_gps.table.GPS;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static com.llu17.youngq.sqlite_gps.UploadService.dbHelper1;

/**
 * Created by youngq on 17/5/1.
 */

public class VariableManager {

    private GpsDbHelper dbHelper;
    private SQLiteDatabase db;
    private JSONObject gps_object;
    private JSONArray GpsJsonArray;
    public static ArrayList<GPS> gpses;
    private static CountDownLatch latch = null;


    public interface Listener {
        public void onStateChange(boolean state);
    }

    private Listener mListener = null;
    public void registerListener (Listener listener) {
        mListener = listener;
    }
    public void unregisterListener (Listener listener) {
        mListener = null;
    }
    private boolean myBoolean = false;
    public void doYourWork() {
//        for(int i = 0; i < 10; i++){
//            if(i%2 == 0)
//                myBoolean = true;
//            else
//                myBoolean = false;
//
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            if (mListener != null)
//                mListener.onStateChange(myBoolean);
//
//        }
        Log.e("There is WiFi","I am here!");
        dbHelper = dbHelper1;
        final String gps_url = "http://cs.binghamton.edu/~smartpark/android/gps.php";
        final int[] result = new int[1];
        boolean label = true;

        while(label) {
            gpses = find_all_gps();

            if (gpses != null) {
                latch = new CountDownLatch(1);
                Thread t1 = new Thread() {
                    public void run() {
                        result[0] = post_data(gps_url, changeGpsDateToJson());
                        latch.countDown();
                    }
                };
                t1.start();

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.e("lalala", "-------");
                Log.e("result[0]", "!!!!!" + result[0]);
                if (result[0] == 200)
                    myBoolean = true;
                else
                    myBoolean = false;
                if (mListener != null)
                    mListener.onStateChange(myBoolean);
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            result[0] = 0;
            if(gpses == null)
                label = false;
        }
        Log.e("Upload Success","!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }


    private ArrayList<GPS> find_all_gps(){
//        dbHelper = new GpsDbHelper(this);
        db = dbHelper1.getReadableDatabase();
        Cursor c = null;
        String s = "select Id, timestamp, latitude, longitude from gps_location where Tag = 0 limit 50;";
        try {
            c = db.rawQuery(s, null);
            Log.e("cursor count gps: ", "" + c.getCount());
            if (c != null && c.getCount() > 0) {
                ArrayList<GPS> gpslist = new ArrayList<>();
                GPS gps;
                while (c.moveToNext()) {
                    gps = new GPS();
                    gps.setId(c.getString(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_ID)));
                    gps.setTimestamp(c.getLong(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_TIMESTAMP)));
                    gps.setLatitude(c.getDouble(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_LATITUDE)));
                    gps.setLongitude(c.getDouble(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_LONGITUDE)));
                    gpslist.add(gps);
                }
                return gpslist;
            } else {
                Log.e("i am here", "hello11111111");
            }
        }
        catch(Exception e){
            Log.e("exception: ", e.getMessage());
        }
        finally {
            c.close();
            db.close();
            Log.e("i am here2", "hello11111111");
        }
        Log.e("i am here3", "hello11111111");
        return null;
    }
    private JSONArray changeGpsDateToJson() {
        GpsJsonArray=null;
        GpsJsonArray = new JSONArray();
        for (int i = 0; i < gpses.size(); i++) {
            gps_object = new JSONObject();
            try {
                gps_object.put("UserID", gpses.get(i).getId());
                gps_object.put("Timestamp", gpses.get(i).getTimestamp());
                gps_object.put("Latitude", gpses.get(i).getLatitude());
                gps_object.put("Longitude", gpses.get(i).getLongitude());
                GpsJsonArray.put(gps_object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return GpsJsonArray;
    }
    private int post_data(String url, JSONArray json){
        int StatusCode = 0;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext httpContext = new BasicHttpContext();
        HttpPost httpPost = new HttpPost(url);

        try {

            StringEntity se = new StringEntity(json.toString());

            httpPost.setEntity(se);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");


            HttpResponse response = httpClient.execute(httpPost, httpContext); //execute your request and parse response
            HttpEntity entity = response.getEntity();

            String jsonString = EntityUtils.toString(entity); //if response in JSON format
            Log.e("response: ",jsonString);

            StatusCode = response.getStatusLine().getStatusCode();
            Log.e("status code: ", "" + StatusCode);
        } catch (Exception e) {
//            e.printStackTrace();
            Log.e("no wifi exception: ", e.toString());
        }
        return StatusCode;
    }
}
