package io.github.juumixx.weartodo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dropbox.core.v2.users.FullAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import io.github.juumixx.todo.Task;
import io.github.juumixx.todo.Todo;

@EActivity(R.layout.activity_main)
public class MobileActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, Runnable {
    private static final String TAG = MobileActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_STORAGE = 1;
    
    @ViewById
    Toolbar toolbar;
    
    @ViewById
    NavigationView navigationView;
    
    @ViewById
    DrawerLayout drawerLayout;
    
    @ViewById
    RecyclerView recycler;
    
    TextView headerName;
    TextView headerEmail;
    
    @Bean
    DropboxController dropboxController;
    
    @Bean
    TodoAdapter adapter;
    
    private GoogleApiClient googleApiClient;
    
    @AfterViews
    void init() {
        dropboxController.init();
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                                                                 R.string.navigation_drawer_open,
                                                                 R.string.navigation_drawer_close
        );
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
                    public void onConnectionFailed(@NonNull ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                }).build();
        
        adapter.setItemsChanged(this);
        recycler.setAdapter(adapter);
        TaskTouchHelperCallback touchHelperCallback = new TaskTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(touchHelperCallback);
        touchHelper.attachToRecyclerView(recycler);
        
        int permissionCheck =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            initFiles();
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                               PERMISSIONS_REQUEST_STORAGE
            );
        }
    }
    
    @AfterViews
    @Background
    void initFiles() {
        File todoFile = new File(Environment.getExternalStorageDirectory() + "/Sync/todo/todo.txt");
        try (FileReader fr = new FileReader(todoFile)) {
            BufferedReader br = new BufferedReader(fr);
            String line;
            Todo todo = new Todo();
            while ((line = br.readLine()) != null) {
                todo.readLine(line);
            }
            updateTodo(todo);
        } catch (IOException e) {
            Log.e(MobileActivity.class.getSimpleName(), "Error reading file", e);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_STORAGE) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    initFiles();
                }
            }
        }
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
    
    public void updateFile(String txt) {
        Todo todo = new Todo(txt);
        updateTodo(todo);
    }
    
    @UiThread
    void updateTodo(Todo todo) {
        adapter.items.clear();
        adapter.items.addAll(todo.getTasks());
        adapter.notifyDataSetChanged();
        
        updateMenu(todo);
        updateWear(todo);
    }
    
    @UiThread
    void updateMenu(Todo todo) {
        SubMenu subMenu = navigationView.getMenu().getItem(0).getSubMenu();
        subMenu.clear();
        for (String context : todo.contexts()) {
            subMenu.add(context);
        }
    }
    
    @UiThread
    void updateWear(Todo todo) {
        PutDataRequest req =
                PutDataRequest.create("/tasks").setUrgent().setData(todo.toString().getBytes());
        Wearable.DataApi.putDataItem(googleApiClient, req);
    }
    
    @Override
    public void run() {
        Todo todo = new Todo();
        todo.getTasks().addAll(adapter.items);
        saveFile(todo);
    }
    
    @Background
    void saveFile(Todo todo) {
        File todoFile = new File(Environment.getExternalStorageDirectory() + "/Sync/todo/todo.txt");
        try (FileWriter fw = new FileWriter(todoFile)) {
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(todo.toString());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            Log.e(MobileActivity.class.getSimpleName(), "Error writing file", e);
        }
        
        updateMenu(todo);
        updateWear(todo);
    }
    
    @EBean
    static class TodoAdapter extends RecyclerView.Adapter<TodoViewHolder> {
        @SystemService
        LayoutInflater layoutInflater;
        
        private ArrayList<Task> items;
        private Runnable itemsChanged;
        
        TodoAdapter() {
            super();
            items = new ArrayList<>();
        }
        
        void setItemsChanged(Runnable itemsChanged) {
            this.itemsChanged = itemsChanged;
        }
        
        @Override
        public TodoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = layoutInflater.inflate(R.layout.item_todo, parent, false);
            return new TodoViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(TodoViewHolder holder, int position) {
            holder.bind(items.get(position));
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }
        
        void onItemMove(int positionStart, int positionEnd) {
            Task item = items.remove(positionStart);
            items.add(positionEnd, item);
            notifyItemMoved(positionStart, positionEnd);
            itemsChanged.run();
        }
    }
    
    static class TodoViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        
        TodoViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.text);
        }
        
        void bind(Task task) {
            if (task.getCompleted()) {
                textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textView.setPaintFlags(textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }
            textView.setText(task.getTxt());
        }
    }
    
    private static class TaskTouchHelperCallback extends ItemTouchHelper.Callback {
        private final TodoAdapter adapter;
        
        TaskTouchHelperCallback(TodoAdapter adapter) {
            super();
            this.adapter = adapter;
        }
        
        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }
        
        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }
        
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }
        
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                RecyclerView.ViewHolder target) {
            adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }
        
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }
    }
}
