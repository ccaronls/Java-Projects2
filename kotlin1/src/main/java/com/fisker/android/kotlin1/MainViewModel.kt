package com.fisker.android.kotlin1

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Created by Chris Caron on 3/28/22.
 */

class MainViewModel : ViewModel() {

    val text = MutableLiveData("Loading ...")
}