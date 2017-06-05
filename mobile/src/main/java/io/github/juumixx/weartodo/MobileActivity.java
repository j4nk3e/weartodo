package io.github.juumixx.weartodo;

import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.dropbox.core.v2.users.FullAccount;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import io.github.juumixx.todo.Task;
import io.github.juumixx.todo.Todo;

@EActivity(R.layout.activity_main)
public class MobileActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

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
            navigationView.getMenu().getItem(4).getSubMenu().getItem(1).setTitle(R.string.logout);
        } else {
            headerName.setText(R.string.app_name);
            headerEmail.setText("");
            navigationView.getMenu().getItem(4).getSubMenu().getItem(1).setTitle(R.string.login);
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

    public void updateFile(String txt) {
        Todo todo = new Todo(txt);
        for (Task task : todo.getTasks()) {
            System.out.println(task.getContent());
        }
    }
}
