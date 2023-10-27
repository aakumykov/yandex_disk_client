package com.github.aakumykov.yandex_disk_client_demo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.aakumykov.yandex_auth_helper.YandexAuthHelper;
import com.github.aakumykov.yandex_disk_client.LibrarySortingMode;
import com.github.aakumykov.yandex_disk_client.YandexDiskCloudClient;
import com.github.aakumykov.yandex_disk_client_demo.databinding.ActivityMainBinding;
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils;
import com.yandex.disk.rest.json.Resource;

import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements YandexAuthHelper.Callbacks, ItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_YA_LOGIN = 10;
    private static final String KEY_AUTH_TOKEN = "AUTH_TOKEN";
    private static final String KEY_RESOURCE_KEY = "PUBLIC_KEY";
    private static final String KEY_REMOTE_PATH = "PATH";

    private YandexAuthHelper mYandexAuthHelper;
    private YandexDiskCloudClient<DiskItem, LibrarySortingMode> mYandexDiskCloudClient;

    private ActivityMainBinding mBinding;
    private MyListAdapter mListAdapter;
    private ClipboardManager mClipboardManager;

    private LibrarySortingMode mCurrentSortingMode;
    private @Nullable String mAuthToken;

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        prepareLayout();
        prepareRecyclerView();
        prepareButtons();

        prepareYandexAuthHelper();
        prepareYandexClients();

        restoreFieldValues(); // Этот метод должен идти после создания Клиента.
        computeSortingMode();

        showYandexAuthStatus();
    }

    private void showYandexAuthStatus() {
        if (null != mAuthToken && !TextUtils.isEmpty(mAuthToken.trim()))
            mBinding.authButton.setText(mAuthToken);
        else
            mBinding.authButton.setText(R.string.BUTTON_auth);
    }

    private void prepareRecyclerView() {

        mListAdapter = new MyListAdapter(new DiffUtil.ItemCallback<DiskItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull DiskItem oldItem, @NonNull DiskItem newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull DiskItem oldItem, @NonNull DiskItem newItem) {
                return oldItem.name.equals(newItem.name);
            }
        }, this);

        mBinding.recyclerView.setAdapter(mListAdapter);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void prepareYandexAuthHelper() {
        mYandexAuthHelper = new YandexAuthHelper(this, REQUEST_CODE_YA_LOGIN, this);
    }

    private void prepareYandexClients() {
        mYandexDiskCloudClient = new MyYandexDiskClient();
    }

    private void restoreFieldValues() {
        mBinding.publicResourceKeyInput.setText(getTextFromPrefs(KEY_RESOURCE_KEY));
        mBinding.remotePathInput.setText(getTextFromPrefs(KEY_REMOTE_PATH));
        mAuthToken = getTextFromPrefs(KEY_AUTH_TOKEN);
        mBinding.authButton.setText(mAuthToken);
        mYandexDiskCloudClient.setAuthToken(mAuthToken);
    }

    private void prepareButtons() {

        mBinding.eraseFieldButton.setOnClickListener(v -> mBinding.publicResourceKeyInput.setText(""));
        mBinding.clipboardPasteButton.setOnClickListener(this::onPasteButtonClicked);
        mBinding.authButton.setOnClickListener(this::onAuthButtonClicked);
        mBinding.unAuthButton.setOnClickListener(this::onUnAuthButtonClicked);
        mBinding.getListButton.setOnClickListener(this::onGetListButtonClicked);
        mBinding.checkExistenceButton.setOnClickListener(this::onCheckExistenceButtonClicked);
        mBinding.getDownloadLinkButton.setOnClickListener(this::onGetDownloadLinkButtonClicked);
        mBinding.resetButton.setOnClickListener(this::onResetButtonClicked);

        mBinding.publicResourceKeyInput.addTextChangedListener(new AbstractTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveTextToPrefs(KEY_RESOURCE_KEY, s.toString());
            }
        });

        mBinding.remotePathInput.addTextChangedListener(new AbstractTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveTextToPrefs(KEY_REMOTE_PATH, s.toString());
            }
        });

        mBinding.sortByNameButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onSortingModeChanged();
            }
        });

        mBinding.isDirectSortToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onSortingModeChanged();
            }
        });
    }

    private void onUnAuthButtonClicked(View view) {
        mAuthToken = null;
        clearValueFromPrefs(KEY_AUTH_TOKEN);
        showYandexAuthStatus();
    }

    private void onResetButtonClicked(View view) {
        onClearButtonClicked(view);
    }

    private void onPasteButtonClicked(View view) {
        mBinding.publicResourceKeyInput.setText(clipboardText());
    }

    private void onSortingModeChanged() {
        computeSortingMode();
        redisplayList();
    }

    private void redisplayList() {
        mListAdapter.setSortingMode(mCurrentSortingMode);
    }

    private void computeSortingMode() {
        mCurrentSortingMode = sortingMode();
    }

    private void prepareLayout() {
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
    }

    private String getTextFromPrefs(String key) {
        return getPreferences().getString(key, "");
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences("prefs", MODE_PRIVATE);
    }

    private void saveTextToPrefs(String storageKey, String text) {
        getPreferences().edit()
                .putString(storageKey, text).apply();
    }

    private void clearValueFromPrefs(final String storageKey) {
        getPreferences().edit().remove(storageKey).apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_RESOURCE_KEY, mBinding.publicResourceKeyInput.getText().toString());
        outState.putString(KEY_REMOTE_PATH, mBinding.remotePathInput.getText().toString());
    }

    // FIXME: перейти на ActivityResultAPI
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mYandexAuthHelper.processAuthResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void onAuthButtonClicked(View view) {
        mYandexAuthHelper.beginAuthorization();
    }

    private void onGetListButtonClicked(View view) {

        final String dirPath = getDirPath();
        final String resourceKey = getResourceKey();

        if (!dirPath.isEmpty())
            getListByPath(dirPath);
        else if (!resourceKey.isEmpty())
            getListByKey(resourceKey);
        else
            showToast("Нужен путь к каталогу или ссылка на ресурс");
    }

    private void getListByPath(String dirPath) {
        Single.fromCallable(new Callable<List<DiskItem>>() {
            @Override
            public List<DiskItem> call() throws Exception {
                return mYandexDiskCloudClient.listDir(dirPath);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        showProgressBar();
                    }
                })
                .doOnTerminate(new Action() {
                    @Override
                    public void run() throws Exception {
                        hideProgressBar();
                    }
                })
                .subscribe(new SingleObserver<List<DiskItem>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mCompositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(List<DiskItem> diskItems) {
                        mListAdapter.clearList();
                        mListAdapter.appendList(diskItems, LibrarySortingMode.NAME_DIRECT);
                    }

                    @Override
                    public void onError(Throwable e) {
                        showError(e);
                    }
                });
    }

    private void getListByKey(String resourceKey) {

        mYandexDiskCloudClient.getListAsync(getResourceKey(),null, sortingMode(), mListAdapter.getCurrentList().size(), 2)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        showProgressBar();
                    }
                })
                .doOnTerminate(new Action() {
                    @Override
                    public void run() throws Exception {
                        hideProgressBar();
                    }
                })
                .subscribe(new SingleObserver<List<DiskItem>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mCompositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(List<DiskItem> list) {
                        if (0 == list.size())
                            showToast("Получены все элементы");
                        else
                            mListAdapter.appendList(list, mCurrentSortingMode);
                    }

                    @Override
                    public void onError(Throwable e) {
                        showError(e);
                    }
                });
    }

    private LibrarySortingMode sortingMode() {

        final boolean byName = mBinding.sortByNameButton.isChecked();
        final boolean directOrder = mBinding.isDirectSortToggle.isChecked();

        LibrarySortingMode sortingMode;

        if (byName) {
            sortingMode = (directOrder) ?
                    LibrarySortingMode.NAME_DIRECT :
                    LibrarySortingMode.NAME_REVERSE;
        } else { // by cTime
            sortingMode = (directOrder) ?
                    LibrarySortingMode.C_TIME_FROM_OLD_TO_NEW :
                    LibrarySortingMode.C_TIME_FROM_NEW_TO_OLD;
        }

        return sortingMode;
    }

    private void onCheckExistenceButtonClicked(View view) {

        final String remotePath = getRemotePath();
        if (null == remotePath) {
            showToast("Не указан путь к файлу в облаке");
            return;
        }

        mYandexDiskCloudClient.checkItemExists(getResourceKey(), remotePath)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        showProgressBar();
                    }
                })
                .doOnTerminate(new Action() {
                    @Override
                    public void run() throws Exception {
                        hideProgressBar();
                    }
                })
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        final String c = aBoolean ? "" : " не";
                        showToast("Элемент '"+remotePath+"'"+c+" существует.");
                    }

                    @Override
                    public void onError(Throwable e) {
                        showError(e);
                    }
                });
    }

    private void onGetDownloadLinkButtonClicked(View view) {

        mYandexDiskCloudClient.getItemDownloadLink(getResourceKey(), getRemotePath())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        showProgressBar();
                    }
                })
                .doOnTerminate(new Action() {
                    @Override
                    public void run() throws Exception {
                        hideProgressBar();
                    }
                })
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(String s) {
                        showToast("ССЫЛКА НА СКАЧИВАНИЕ: "+s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        showError(e);
                    }
                });
    }

    private void onClearButtonClicked(View view) {
        mListAdapter.clearList();
    }


    private String getResourceKey() {
        return mBinding.publicResourceKeyInput.getText().toString();
    }


    private String getDirPath() {
        return mBinding.dirPathInput.getText().toString();
    }

    @Nullable
    private String getRemotePath() {
        String text = mBinding.remotePathInput.getText().toString().trim();
        return TextUtils.isEmpty(text) ? null : text;
    }

    @Override
    public void onYandexAuthSuccess(@NonNull String authToken) {
        mAuthToken = authToken;
        mYandexDiskCloudClient.setAuthToken(authToken);
        saveTextToPrefs(KEY_AUTH_TOKEN, mAuthToken);
        showYandexAuthStatus();
    }

    @Override
    public void onYandexAuthFailed(@NonNull String errorMsg) {
        showToast("Ошибка аутентификации:\n"+errorMsg);
        showYandexAuthStatus();
    }

    @Override
    public void onItemClicked(DiskItem diskItem) {
        showToast("Коротко: "+diskItem.name);
    }

    @Override
    public boolean onItemLongClicked(DiskItem diskItem) {
        showLongToast("Длинно: "+diskItem.name);
        return true;
    }

    private static abstract class AbstractTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void showLongToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private void showError(Throwable t) {
        Toast.makeText(this, ExceptionUtils.getErrorMessage(t), Toast.LENGTH_LONG).show();
        Log.e(TAG, ExceptionUtils.getErrorMessage(t), t);
    }

    private void showProgressBar() {
        mBinding.progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        mBinding.progressBar.setVisibility(View.GONE);
    }

    private void showNotImplementedYet() {
        showToast("Не реализовано");
    }

    private CharSequence clipboardText() {
        @Nullable ClipData clipData = mClipboardManager.getPrimaryClip();
//        ClipDescription clipDescription = mClipboardManager.getPrimaryClipDescription();
        /*return (null != clipData) ?
                clipData.getItemAt(0).getText() :
                "";*/
        if (null != clipData) {
            final ClipData.Item item = clipData.getItemAt(0);
            return item.getText();
        }
        else
            return "";
    }

    private static class MyYandexDiskClient extends YandexDiskCloudClient<DiskItem, LibrarySortingMode> {

        @Override
        public LibrarySortingMode externalToLibrarySortingMode(LibrarySortingMode externalSortingMode) {
            // Здесь программа-пользователь библиотеки использует ту же сортировку, что и библиотека.
            return externalSortingMode;
        }

        @Override
        public DiskItem cloudItemToLocalItem(com.yandex.disk.rest.json.Resource resource) {
            final String name = resource.isDir() ? "["+resource.getName()+"]" : resource.getName();
            return new DiskItem(name, resource.getCreated().getTime());
        }

        @Override
        public String cloudFileToString(Resource resource) {
            return "[file: " + resource.getName() + "]";
        }
    }
}
