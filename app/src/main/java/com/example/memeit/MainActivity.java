package com.example.memeit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.codepath.asynchttpclient.AsyncHttpClient;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import com.example.memeit.Auth.Login;
import com.example.memeit.models.DailyMeme;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Headers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    Button button;


    public static final String TRENDING_MEME_URL = "https://api.giphy.com/v1/gifs/trending?api_key=85yQsmwttEKrF5w5R3AAPMd3UpbVwKsC&limit=25&rating=g";

    Context context;
    private BottomNavigationView bottomNav;

    Button showPopup;

    TextView dailyMemeTitle;
    ImageView dailyMemeImage, appbarsinout;

    DailyMeme dailyMeme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar implementation
        Toolbar appbar = findViewById(R.id.appbar);
        setSupportActionBar(appbar);
        getSupportActionBar().setTitle("MemeIt");
        appbarsinout = appbar.findViewById(R.id.appbarsignout);
        appbarsinout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"Signed Out",Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Login.class));
                finish();
            }
        });
        // End Toolbar

        bottomNav= findViewById(R.id.botton_navigation);
        showPopup = findViewById(R.id.dailyMemeButton);



        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment;
                switch (item.getItemId()) {
                    case R.id.profile:
                        startActivity(new Intent(getApplicationContext(), Profile.class));
                        break;
                    case R.id.saved:
                        break;
                    default:
                        break;
                }
                return true;
            }
        });


        showPopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDailyMemePopup();
            }
        });

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(TRENDING_MEME_URL, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Headers headers, JSON json) {
                Log.d(TAG, "onSuccess");
                //lol
                JSONObject jsonObject = json.jsonObject;
                try {
                    JSONArray data = jsonObject.getJSONArray("data");
//                    Log.i(TAG, "Results: " + data.toString());
                    dailyMeme = DailyMeme.fromJsonArray(data);

//                    Log.i(TAG, "the dailyMeme:" + dailyMeme);
                } catch (JSONException e) {
                    Log.e(TAG, "Hit json exception", e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Headers headers, String s, Throwable throwable) {
                Log.d(TAG, "onFailure");
            }
        });
        
//        queryMemes();
    }
//testing to see if the databse connection is working properly -- Akbar Haider 4/16/2021
    //you can see the queries in logcat with the tag MainActivity. The meme names will show up that is in our database right now
    private void queryMemes() {
        ParseQuery<Memes> query = ParseQuery.getQuery(Memes.class);
        query.include(Memes.memeURL);
        query.include(Memes.UpvoteVal);
        query.include(Memes.DownvoteVal);
        query.findInBackground(new FindCallback<Memes>() {
            @Override
            public void done(List<Memes> memes, ParseException e) {
                if(e != null){
                    Log.e(TAG,"Issue with Getting Memes",e);
                    return;
                }
                for(Memes meme : memes) {
                    Log.i(TAG,"Meme Name :  " + meme.getmemeName() + ". " +  "MEME URL: " + meme.getMemeURL() + ". " + " Meme Upvote: " + meme.getUpvoteVal() + ". " + "Meme Downvote: " + meme.getDownvoteVal());


                }

            }
        });

    }

    //function to display a popup of a queried Meme from API. --Jasper
    //This will also be responsible for the call to add the new meme to the back4app db
    public void createDailyMemePopup(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View dailyMemePopup = inflater.inflate(R.layout.daily_meme_popup_page, null);

        dailyMemeTitle = dailyMemePopup.findViewById(R.id.tvDailyMemeTitle);
        dailyMemeImage = dailyMemePopup.findViewById(R.id.ivShowDailyMeme);

        //in here, call the function to add the queried meme
        addMeme(dailyMeme.getDailyMemeTitle(), dailyMeme.getDailyMemePath());

        dailyMemeTitle.setText(dailyMeme.getDailyMemeTitle());
        Glide.with(this).asGif().load(dailyMeme.getDailyMemePath()).into(dailyMemeImage);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(dailyMemePopup, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(dailyMemePopup, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        dailyMemePopup.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    //when a meme has been queried and it is not already added in the parse db,
    //add it to the parse db
    private void addMeme(String title, String URL){
        //Query the db first to check if record already exists.
        //can just look for if the title string already exists in db
        ParseQuery<ParseObject> query = ParseQuery.getQuery("memes");
        query.whereStartsWith("memeName", title);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(objects.size() == 0)
                {
                    //If meme record does not yet exist, add it to db.
                    //upvote downvote values should start as 0 in new records
                    Memes meme = new Memes();
                    meme.setMemeURL(URL);
                    meme.setmemeName(title);
                    meme.setDownvoteVal(0);
                    meme.setUpvoteVal(0);

                    meme.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if( e!= null){
                                Log.e(TAG, "Error while saving meme to db" , e);
                            }
                            else
                                Log.i(TAG, "new meme has been added! ");
                        }
                    });
                }
                Log.d(TAG, "Parse query search results " + objects);
            }
        });
//        Log.d(TAG, "Parse query search results " + results);



    }
//    //If meme record does not yet exist, add it to db.
//    //upvote downvote values should start as 0 in new records
//    Memes meme = new Memes();
//                        meme.setMemeURL(URL);
//                        meme.setmemeName(title);
//                        meme.setDownvoteVal(0);
//                        meme.setUpvoteVal(0);
//
//                        meme.saveInBackground(new SaveCallback() {
//        @Override
//        public void done(ParseException e) {
//            if( e!= null){
//                Log.e(TAG, "Error while saving meme to db" , e);
//            }
//
//            Log.i(TAG, "new meme has been added! ");
//        }
//    });
}

