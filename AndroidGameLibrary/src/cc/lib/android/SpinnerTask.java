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

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class SpinnerTask<T> extends AsyncTask<T, Integer, Object> implements DialogInterface.OnCancelListener, Application.ActivityLifecycleCallbacks, DialogInterface.OnDismissListener {

    private Dialog dialog = null;
    private final WeakReference<Activity> context;
    private Object result = null;

    public SpinnerTask(Activity context) {
        this.context = new WeakReference<>(context);
        if (context instanceof Activity) {
            // we want to handle the activity pausing so we can cancel ourself
            ((Activity)context).getApplication().registerActivityLifecycleCallbacks(this);
        }
    }

    public <T extends Context> T getContext() {
        return (T)context.get();
    }

    @Override
    protected void onCancelled() {
        if (dialog != null)
            dialog.dismiss();
    }

    @Override
    protected final void onPostExecute(Object o) {
        if (dialog != null)
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
        if (!context.get().isFinishing() && !context.get().isDestroyed()) {
            dialog = ProgressDialog.show(context.get(), getProgressMessage(), null, true, isCancellable(), this);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnDismissListener(this);
        }
    }

    protected String getProgressMessage() {
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    protected abstract void doIt(T ... args) throws Exception;

    protected void onSuccess() {}

    protected void onCompleted() {}

    @Override
    protected final Object doInBackground(T... args) {
        try {
            doIt(args);
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
    public final void onDismiss(DialogInterface dialog) {
        onCompleted();
    }

    @Override
    public final void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public final void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public final void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public final void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public final void onActivityStopped(@NonNull Activity activity) {
        cancel(true);
    }

    @Override
    public final void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public final void onActivityDestroyed(@NonNull Activity activity) {

    }
}
