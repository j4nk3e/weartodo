package io.github.juumixx.weartodo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;

import io.github.juumixx.todo.Todo;

@EActivity(R.layout.activity_wear)
public class WearActivity extends Activity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, DataApi.DataListener {

    @ViewById
    WearableRecyclerView recycler;

    private GoogleApiClient googleApiClient;
    private Adapter adapter;
    private Todo todo;

    @AfterViews
    void init() {
        recycler.setBackgroundColor(Color.DKGRAY);
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
        googleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(googleApiClient, this);
        googleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.DataApi.addListener(googleApiClient, this);
        fetchItems();
    }

    @Background
    void fetchItems() {
        PendingResult<DataItemBuffer> tasks = Wearable.DataApi.getDataItems(googleApiClient, Uri.parse("wear://*/tasks"));
        DataItemBuffer t = tasks.await();
        todo = new Todo(new String(t.get(0).getData()));
        System.out.println("todo fetched: " + todo.toString());
        t.release();
        adapter.items.clear();
        adapter.items.addAll(todo.contexts());
        notifyDataSetChanged();
    }

    @UiThread
    void notifyDataSetChanged() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            DataItem item = event.getDataItem();
            System.out.println("item update path: " + item.getUri().getPath());
            if (item.getUri().getPath().startsWith("/tasks")) {
                todo = new Todo(new String(item.getData()));
                System.out.println("todo updated: " + todo.toString());
                adapter.items.clear();
                adapter.items.addAll(todo.contexts());
                notifyDataSetChanged();
            }
        }
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        ArrayList<String> items = new ArrayList<>();

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
            textView.setTextColor(Color.WHITE);
        }

        void bind(final String text) {
            textView.setText(text);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ContextActivity_.class);
                    intent.putExtra("context", todo.contextString(text));
                    startActivity(intent);
                }
            });
        }
    }
}
