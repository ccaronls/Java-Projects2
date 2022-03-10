package cc.lib.android;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * Created by Chris Caron on 12/15/21.
 */
public abstract class LayoutFactory<T extends ViewDataBinding, VM extends ViewModel> {

    public LayoutFactory(CCActivityBase activity, int layoutResId, Class<VM> viewModelClass) {
        T binding = (T)DataBindingUtil.setContentView(activity, layoutResId);
        binding.setLifecycleOwner(activity);
        VM vm = null;
        if (viewModelClass != null) {
            //vm = new ViewModelProvider(activity).get(viewModelClass);
            new ViewModelProvider(activity).get(viewModelClass);
        }
        onLayoutInflated(binding, vm);
    }


    protected abstract void onLayoutInflated(T layoutBinding, VM viewModel);
}
