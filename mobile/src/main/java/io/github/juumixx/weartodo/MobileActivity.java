package io.github.juumixx.weartodo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.TextView;

import com.dropbox.core.v2.users.FullAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import io.github.juumixx.todo.Todo;

@EActivity(R.layout.activity_main)
public class MobileActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = MobileActivity.class.getSimpleName();

    @ViewById
    Toolbar toolbar;

    @ViewById
    NavigationView navigationView;

    @ViewById
    DrawerLayout drawerLayout;

    TextView headerName;
    TextView headerEmail;

    @Bean
    DropboxController dropboxController;

    private GoogleApiClient googleApiClient;

    @AfterViews
    void init() {
        dropboxController.init();
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        headerName = (TextView) navigationView.getHeaderView(0).findViewById(R.id.headerName);
        headerEmail = (TextView) navigationView.getHeaderView(0).findViewById(R.id.headerEmail);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        dropboxController.updateActivity(this);
    }

    @Click
    void fabClicked(FloatingActionButton fab) {
        Snackbar.make(fab, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @UiThread
    public void updateAccount(FullAccount account) {
        if (account != null) {
            headerName.setText(account.getName().getDisplayName());
            headerEmail.setText(account.getEmail());
            navigationView.getMenu().getItem(1).getSubMenu().getItem(1).setTitle(R.string.logout);
        } else {
            headerName.setText(R.string.app_name);
            headerEmail.setText("");
            navigationView.getMenu().getItem(1).getSubMenu().getItem(1).setTitle(R.string.login);
        }
        dropboxController.getFiles();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_login) {
            if (dropboxController.isLoggedIn()) {
                dropboxController.logout();
            } else {
                dropboxController.login(this);
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @UiThread
    public void updateFile(String txt) {
        Todo todo = new Todo(txt);
        SubMenu subMenu = navigationView.getMenu().getItem(0).getSubMenu();
        subMenu.clear();
        for (String context : todo.contexts()) {
            subMenu.add(context);
        }
        PutDataRequest req = PutDataRequest.create("/tasks").setUrgent().setData(todo.toString().getBytes());
        Wearable.DataApi.putDataItem(googleApiClient, req);
    }
}
