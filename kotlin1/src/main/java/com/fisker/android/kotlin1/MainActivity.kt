package com.fisker.android.kotlin1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fisker.android.kotlin1.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : AppCompatActivity() {

    lateinit var viewModel:MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        binding.viewModel = viewModel
        setContentView(binding.root)
        lifecycleScope.launch(Dispatchers.IO) {
            get()
        }
    }

    fun get() {
        val request = Request.Builder().url("http://example.com").get().build()
        val client = OkHttpClient()
        val response =  client.newCall(request).execute()

        response.body()?.let {
            viewModel.text.postValue(it.string())
        }?:run {
            viewModel.text.postValue("Error")
        }
    }
}
