package io.github.juumixx.weartodo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.res.StringRes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@EBean(scope = EBean.Scope.Singleton)
class DropboxController {
    private static final String TAG = DropboxController.class.getSimpleName();

    @RootContext
    Context context;

    @StringRes(R.string.app_key)
    String appKey;
    @StringRes(R.string.dropbox_token)
    String dropboxToken;

    private DbxClientV2 dropboxClient;
    private FullAccount account;
    private MobileActivity activity;
    private SharedPreferences preferences;

    void init() {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String accessToken = preferences.getString(dropboxToken, "");
        if (accessToken.isEmpty()) {
            accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                preferences.edit().putString(dropboxToken, accessToken).apply();
                initAndLoadData(accessToken);
            }
        } else {
            initAndLoadData(accessToken);
        }
    }

    private void initAndLoadData(String accessToken) {
        DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(DropboxController.class.getName())
                .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                .build();

        dropboxClient = new DbxClientV2(requestConfig, accessToken);
        loadData();
    }

    @Background
    void loadData() {
        try {
            account = dropboxClient.users().getCurrentAccount();
            if (activity != null) {
                activity.updateAccount(account);
            }
        } catch (DbxException e) {
            Log.e(TAG, "Error loading data", e);
        }
    }

    boolean isLoggedIn() {
        return dropboxClient != null && account != null;
    }

    void updateActivity(MobileActivity activity) {
        this.activity = activity;
        init();
    }

    void login(MobileActivity activity) {
        updateActivity(activity);
        if (!isLoggedIn()) {
            Auth.startOAuth2Authentication(context, appKey);
        }
    }

    void logout() {
        dropboxClient = null;
        account = null;
        preferences.edit().remove(dropboxToken).apply();
        activity.updateAccount(null);
    }

    @Background
    void getFiles() {
        try {
            ListFolderResult result = dropboxClient.files().listFolder("/todo");
            for (Metadata metadata : result.getEntries()) {
                if (metadata.getName().equals("todo.txt")) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DbxDownloader<FileMetadata> downloader = dropboxClient.files().download(metadata.getPathLower());
                    downloader.download(out);
                    activity.updateFile(out.toString("UTF-8"));
                }
                Log.d(TAG, metadata.getName() + " " + metadata.getPathLower());
            }
        } catch (DbxException e) {
            Log.e(TAG, "Error loading files", e);
        } catch (IOException e) {
            Log.e(TAG, "Error downloading files", e);
        }
    }
}