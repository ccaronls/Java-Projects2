package cc.lib.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;

public abstract class SpinnerTask extends AsyncTask<String, Integer, Object> implements DialogInterface.OnCancelListener, Application.ActivityLifecycleCallbacks {

    private Dialog dialog = null;
    private final WeakReference<Context> context;
    private Object result = null;

    public SpinnerTask(Context context) {
        this.context = new WeakReference<>(context);
        if (context instanceof Activity) {
            // we want to handle the activity pausing so we can cancel ourself
            ((Activity)context).getApplication().registerActivityLifecycleCallbacks(this);
        }
    }

    @Override
    protected void onCancelled() {
        dialog.dismiss();
    }

    @Override
    protected final void onPostExecute(Object o) {
        dialog.dismiss();
        if (!isCancelled()) {
            if (o != null && o instanceof Exception) {
                Exception e = (Exception) o;
                onError(e);
            } else {
                onSuccess();
            }
        }
    }

    protected void onError(Exception e) {
        e.printStackTrace();
        new AlertDialog.Builder(context.get()).setTitle("Error").setMessage("An error occured: " + e.getClass().getSimpleName() + "\n" + e.getMessage())
                .setNegativeButton("Ok", null).show();
    }

    @Override
    protected void onPreExecute() {
        dialog = ProgressDialog.show(context.get(), getProgressMessage(), null, true, isCancellable(), this);
    }

    protected String getProgressMessage() {
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    protected abstract void doIt(String ... args) throws Exception;

    protected void onSuccess() {}

    @Override
    protected final Object doInBackground(String... strings) {
        try {
            doIt(strings);
        } catch (Exception e ) {
            return e;
        }
        return null;
    }

    public boolean isCancellable() {
        return true;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        cancel(true);
    }

    protected final void setResult(Object result) {
        this.result = result;
    }

    public final <T> T getResult() {
        return (T)result;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        cancel(true);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
}
