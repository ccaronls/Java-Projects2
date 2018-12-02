package cc.lib.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

public abstract class SpinnerTask extends AsyncTask<String, Integer, Object> implements DialogInterface.OnCancelListener {

    private Dialog dialog = null;
    private final WeakReference<Context> context;
    private Object result = null;

    public SpinnerTask(Context context) {
        this.context = new WeakReference<>(context);
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
}
