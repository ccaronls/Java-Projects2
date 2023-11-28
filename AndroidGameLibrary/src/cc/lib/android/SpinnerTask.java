package cc.lib.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class SpinnerTask<T> extends AsyncTask<T, Integer, Object>
        implements Application.ActivityLifecycleCallbacks {

    private ProgressDialog dialog = null;
    private final WeakReference<Activity> context;
    private Object result = null;

    public SpinnerTask(Activity context) {
        this.context = new WeakReference<>(context);
        if (context instanceof Activity) {
            // we want to handle the activity pausing so we can cancel ourselves
            context.getApplication().registerActivityLifecycleCallbacks(this);
        }
    }

    public void setProgressMessage(String message) {
        dialog.setMessage(message);
    }

    public <T extends Context> T getContext() {
        return (T) context.get();
    }

    @Override
    protected final void onCancelled(Object o) {
        super.onCancelled(o);
    }

    @Override
    protected final void onCancelled() {
        if (dialog != null)
            dialog.dismiss();
        onCompleted();
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
            onCompleted();
        }
    }

    /**
     * Gets called for all situations: SUCCESS, ERROR, CANCELLED
     */
    protected void onCompleted() {}

    protected void onError(@NotNull Exception e) {
        e.printStackTrace();
        Activity c = context.get();
        if (c != null) {
            c.runOnUiThread(() -> {
                new AlertDialog.Builder(c).setTitle("Error").setMessage("An error occured: " + e.getClass().getSimpleName() + "\n" + e.getMessage())
                        .setNegativeButton("Ok", null).show();
            });
        }
    }

    @Override
    protected void onPreExecute() {
        if (!context.get().isFinishing() && !context.get().isDestroyed()) {
            dialog = new ProgressDialog(context.get());
            dialog.setMessage(getProgressMessage());
            dialog.setCancelable(false); // prevents cancel from back button
            dialog.setIndeterminate(true);
            dialog.setCanceledOnTouchOutside(false); // prevents cancel from random touches
            if (isCancellable()) {
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.get().getString(R.string.popup_button_cancel), this::onCancelButtonClicked);
            }
            dialog.show();
        }
    }

    protected String getProgressMessage() {
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    protected abstract void doIt(T ... args) throws Throwable;

    protected void onSuccess() {}

    @Override
    protected final Object doInBackground(T... args) {
        try {
            doIt(args);
        } catch (Throwable e ) {
            return e;
        }
        return null;
    }

    public boolean isCancellable() {
        return true;
    }

    public boolean canInterruptOnCancel() {
        return false;
    }

    protected final void setResult(Object result) {
        this.result = result;
    }

    public final <T> T getResult() {
        return (T)result;
    }

    private void onCancelButtonClicked(DialogInterface dialog, int which) {
        cancel(canInterruptOnCancel());
        onCancelButtonClicked();
        dialog.dismiss();
    }

    protected void onCancelButtonClicked() {}

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
