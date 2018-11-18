package tv.hooq.cast_sender_v3;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private enum CastSessionState {CONNECTED, DISCONNECTED}

    private Map<String, String> defaultUrls = new HashMap<>();

    private CastContext castContext;
    private MenuItem mediaRouteMenuItem;
    private Toolbar toolbar;
    private CastSession castSession;
    private SessionManagerListener<CastSession> sessionManagerListener;
    private CastSessionState castSessionState = CastSessionState.DISCONNECTED;
    private EditText mediaUrlEditText;
    private Spinner mediaContentSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (castSessionState == CastSessionState.CONNECTED) {
                    loadRemoteMedia();
                }
                else {
                    Snackbar.make(view, "Not connected to chrome cast", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            }
        });

        createInputFields();
        setupActionBar();

        castContext = CastContext.getSharedInstance(this);
        setupCastListener();

        defaultUrls.put("videos/mp4", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4");
        defaultUrls.put("application/x-mpegurl", "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/hls/BigBuckBunny.m3u8");
        defaultUrls.put("application/dash+xml", "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/dash/TearsOfSteel.mpd");
    }

    private void createInputFields() {
        mediaUrlEditText = findViewById(R.id.media_url_edit_text);

        mediaContentSpinner = findViewById(R.id.content_type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.content_type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mediaContentSpinner.setAdapter(adapter);
        mediaContentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String contentType = mediaContentSpinner.getSelectedItem().toString();
                mediaUrlEditText.setText(defaultUrls.get(contentType));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) { }
        });
    }

    private void setupActionBar() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        castContext.getSessionManager().removeSessionManagerListener(
                sessionManagerListener, CastSession.class);
    }

    @Override
    protected void onResume() {
        castContext.getSessionManager().addSessionManagerListener(
                sessionManagerListener, CastSession.class);
        super.onResume();
    }

    private void loadRemoteMedia() {
        if (castSession == null) {
            return;
        }
        RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }

        remoteMediaClient.load(buildMediaInfo(), true, 0);
    }

    private MediaInfo buildMediaInfo() {
        String contentType = mediaContentSpinner.getSelectedItem().toString();

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, "HOOQ Test Video");

        return new MediaInfo.Builder(mediaUrlEditText.getText().toString().trim())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(contentType)
                .setMetadata(movieMetadata)
//                .setStreamDuration(mSelectedMedia.getDuration() * 1000)
                .build();
    }

    private void setupCastListener() {
        sessionManagerListener = new SessionManagerListener<CastSession>() {

            @Override
            public void onSessionEnded(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionResumeFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionStartFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarting(CastSession session) {
            }

            @Override
            public void onSessionEnding(CastSession session) {
            }

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {
            }

            @Override
            public void onSessionSuspended(CastSession session, int reason) {
            }

            private void onApplicationConnected(CastSession castSession) {
                MainActivity.this.castSession = castSession;
                castSessionState = CastSessionState.CONNECTED;
                supportInvalidateOptionsMenu();
            }

            private void onApplicationDisconnected() {
                castSessionState = CastSessionState.DISCONNECTED;
                supportInvalidateOptionsMenu();
            }
        };
    }
}
