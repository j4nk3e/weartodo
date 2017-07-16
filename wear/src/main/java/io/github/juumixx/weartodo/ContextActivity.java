package io.github.juumixx.weartodo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.CurvedChildLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;

import io.github.juumixx.todo.Task;
import io.github.juumixx.todo.Todo;

@EActivity(R.layout.activity_wear)
public class ContextActivity extends Activity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    @ViewById
    WearableRecyclerView recycler;

    private GoogleApiClient googleApiClient;
    private Adapter adapter;

    @AfterViews
    void init() {
        recycler.setBackgroundColor(Color.WHITE);
        adapter = new Adapter();
        recycler.setCenterEdgeItems(true);
        CurvedChildLayoutManager layout = new CurvedChildLayoutManager(getApplicationContext());
        recycler.setLayoutManager(layout);
        recycler.setAdapter(adapter);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String items = getIntent().getExtras().getString("context");
        if (items != null) {
            adapter.items.clear();
            Todo todo = new Todo(items);
            adapter.items.addAll(todo.getTasks());
        }
        googleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        googleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        ArrayList<Task> items = new ArrayList<>();

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
            View contactView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new Holder(contactView);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class Holder extends RecyclerView.ViewHolder {
        private final TextView textView;

        Holder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
            textView.setTextColor(Color.BLACK);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }

        void bind(Task task) {
            textView.setText(task.getContent());
        }
    }
}
