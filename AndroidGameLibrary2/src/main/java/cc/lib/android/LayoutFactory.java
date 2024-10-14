package cc.lib.android;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

/**
 * Created by Chris Caron on 12/15/21.
 */
public class LayoutFactory {

    final int layoutResId;
    final Class<? extends ViewModel> viewModelClass;
    final Context context;
    final LifecycleOwner owner;
    final ViewModelStoreOwner storeOwner;
    public ViewDataBinding binding;
    public ViewModel viewModel;

    public LayoutFactory(Context context, LifecycleOwner owner, ViewModelStoreOwner storeOwner, int layoutResId, Class<? extends ViewModel> viewModelClass) {
        this.layoutResId = layoutResId;
        this.viewModelClass = viewModelClass;
        this.context = context;
        this.owner = owner;
        this.storeOwner = storeOwner;
    }

    public LayoutFactory(CCActivityBase context, int layoutResId, Class<? extends ViewModel> viewModelClass) {
        this(context, context, context, layoutResId, viewModelClass);
    }

    public void build() {
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), layoutResId, null, false);
        binding.setLifecycleOwner(owner);
        viewModel = viewModelClass != null ? new ViewModelProvider(storeOwner).get(viewModelClass) : null;
    }
}
