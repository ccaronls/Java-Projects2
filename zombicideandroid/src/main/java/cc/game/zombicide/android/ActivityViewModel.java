package cc.game.zombicide.android;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Created by Chris Caron on 12/13/21.
 */
public class ActivityViewModel extends ViewModel {

    public MutableLiveData<Boolean> consoleVisible = new MutableLiveData(true);

    public MutableLiveData<Boolean> loading = new MutableLiveData(false);

    public MutableLiveData<Boolean> playing = new MutableLiveData(false);

}
