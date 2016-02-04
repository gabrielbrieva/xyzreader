package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

import com.example.xyzreader.remote.ArticleService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.ArrayList;

import retrofit2.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";

    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.example.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.example.xyzreader.intent.extra.REFRESHING";

    private static final String URL_BASE = "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/";

    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Time time = new Time();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(TAG, "Not online, not refreshing.");
            return;
        }

        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, true));

        // Don't even inspect the intent, we only do one thing, and that's fetch content.
        ArrayList<ContentProviderOperation> cpo = new ArrayList<ContentProviderOperation>();

        Uri dirUri = ItemsContract.Items.buildDirUri();

        // Delete all items
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        try {

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL_BASE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ArticleService service = retrofit.create(ArticleService.class);
            Call<JsonArray> call = service.listArticles();
            Response<JsonArray> response = call.execute();
            JsonArray array = null;

            if (response != null && response.isSuccess())
                array = response.body();

            if (array == null)
                throw new JsonParseException("Invalid parsed item array" );

            for (int i = 0; i < array.size(); i++) {
                ContentValues values = new ContentValues();
                JsonObject object = array.get(i).getAsJsonObject();
                values.put(ItemsContract.Items.SERVER_ID, object.get("id").getAsString());
                values.put(ItemsContract.Items.AUTHOR, object.get("author").getAsString());
                values.put(ItemsContract.Items.TITLE, object.get("title").getAsString());
                values.put(ItemsContract.Items.BODY, object.get("body").getAsString());
                values.put(ItemsContract.Items.THUMB_URL, object.get("thumb").getAsString());
                values.put(ItemsContract.Items.PHOTO_URL, object.get("photo").getAsString());
                values.put(ItemsContract.Items.ASPECT_RATIO, object.get("aspect_ratio").getAsString());
                time.parse3339(object.get("published_date").getAsString());
                values.put(ItemsContract.Items.PUBLISHED_DATE, time.toMillis(false));
                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }

            getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);

        } catch (JsonParseException | RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating content.", e);
        } catch (IOException e) {
            Log.e(TAG, "Error getting content.", e);
        }

        sendStickyBroadcast(new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, false));
    }
}
