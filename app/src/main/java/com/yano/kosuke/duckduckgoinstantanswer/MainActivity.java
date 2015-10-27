package com.yano.kosuke.duckduckgoinstantanswer;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        ListView.OnItemClickListener {

    TextView abstractTextView;
    EditText searchEditText;
    Button searchButton;
    ListView relatedTopicsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // access the TextView defined in layout XML
        abstractTextView = (TextView) findViewById(R.id.abstract_texview);

        // access the EditText defined in layout XML
        searchEditText = (EditText) findViewById(R.id.search_edittext);

        // access the Button defined in layout XML
        searchButton = (Button) findViewById(R.id.search_button);
        searchButton.setOnClickListener(this);

        // access the ListView defined in layout XML
        relatedTopicsListView = (ListView) findViewById(R.id.relatedTopics_listview);
        relatedTopicsListView.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Toast toast;

        // guard condition for no search
        if (isEmptySearchEditText()) {
            showShortToast("ERROR: Please insert search term");
        }

        if (isOnline()) {
           search(searchEditText.getText().toString());
        } else {
            showShortToast("ERROR: No network connectivity");
        }

        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        String URL = (String) parent.getItemAtPosition(position);
        System.out.println("loading \"" + URL + "\"...");

        try {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(ComponentName.unflattenFromString("com.android.chrome/com.android.chrome.MAIN"));
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setData(Uri.parse(URL));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Chrome is probably not installed
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    public boolean isEmptySearchEditText() {
        String search = searchEditText.getText().toString();

        return search.length() == 0;
    }

    public boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    public void search(String search) {
        String URL = "http://api.duckduckgo.com/?q="
                + search
                + "&format=json&pretty=1&t=DuckDuckGoInstantAnswer";

        // send request
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // System.out.println(response);

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String mAbstract      = jsonObject.getString("Abstract");
                            JSONArray jsonArray   = jsonObject.getJSONArray("RelatedTopics");

                            if (mAbstract.length() == 0) {
                                mAbstract = "No abstract available.";
                            }

                            abstractTextView.setText(mAbstract);
                            setRelatedTopicsListView(jsonArray);
                        } catch (JSONException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.getMessage());
            }
        });

        queue.add(request);
    }

    public void showShortToast(String message) {
        Context context = getApplication();
        int duration    = Toast.LENGTH_SHORT;
        Toast toast     = Toast.makeText(context, message, duration);
        toast.show();
    }

    private void setRelatedTopicsListView(JSONArray jsonArray) {
        // create a list containing First URL
        List<String> firstURLs = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String firstURL       = jsonObject.getString("FirstURL");
                firstURLs.add(firstURL);
            } catch (JSONException e) {
                System.out.println(e.getMessage());
            }
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                R.layout.related_topics,
                firstURLs
        );

        relatedTopicsListView.setAdapter(arrayAdapter);
    }
}
