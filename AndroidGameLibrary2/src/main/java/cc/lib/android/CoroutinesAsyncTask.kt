package cc.lib.android

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

enum class Status {
	PENDING,
	RUNNING,
	FINISHED
}

abstract class CoroutinesAsyncTask<Params, Progress, Result>(val taskName: String) {

	val TAG by lazy {
		CoroutinesAsyncTask::class.java.simpleName
	}

	companion object {
		private var threadPoolExecutor: CoroutineDispatcher? = null
	}

	var status: Status = Status.PENDING
	var preJob: Job? = null
	var bgJob: Deferred<Result?>? = null
	abstract suspend fun doInBackground(param: Params?): Result?
	open fun onProgressUpdate(value: Array<Progress>) {}
	open fun onPostExecute(result: Result?) {}
	open fun onPreExecute() {}
	open fun onCancelled(result: Result?) {}
	protected var isCancelled = false

	/**
	 * Executes background task parallel with other background tasks in the queue using
	 * default thread pool
	 */
	fun execute(params: Params? = null) {
		execute(Dispatchers.Default, params)
	}

	/**
	 * Executes background tasks sequentially with other background tasks in the queue using
	 * single thread executor @Executors.newSingleThreadExecutor().
	 */
	fun executeOnExecutor(params: Params? = null) {
		(threadPoolExecutor ?: Executors.newSingleThreadExecutor().asCoroutineDispatcher().also {
			threadPoolExecutor = it
		}).apply {
			execute(this, params)
		}
	}

	private fun execute(dispatcher: CoroutineDispatcher, params: Params? = null) {

		if (status != Status.PENDING) {
			when (status) {
				Status.RUNNING -> throw IllegalStateException("Cannot execute task:" + " the task is already running.")
				Status.FINISHED -> throw IllegalStateException(
					"Cannot execute task:"
						+ " the task has already been executed "
						+ "(a task can be executed only once)"
				)

				else -> {
				}
			}
		}

		status = Status.RUNNING

		// it can be used to setup UI - it should have access to Main Thread
		CoroutineScope(Dispatchers.IO).launch {
			preJob = launch(Dispatchers.Main) {
				printLog("$taskName onPreExecute started")
				onPreExecute()
				printLog("$taskName onPreExecute finished")
				bgJob = async(dispatcher) {
					printLog("$taskName doInBackground started")
					doInBackground(params)
				}
			}
			//preJob!!.join()
			if (!isCancelled) {
				withContext(Dispatchers.Main) {
					bgJob?.let {
						onPostExecute(it.await())
					}
					printLog("$taskName doInBackground finished")
					status = Status.FINISHED
				}
			}
		}
	}

	fun cancel(mayInterruptIfRunning: Boolean) {
		if (isCancelled || preJob == null || bgJob == null) {
			printLog("$taskName has already been cancelled/finished/not yet started.")
			return
		}
		if (mayInterruptIfRunning || (preJob?.isActive == false && bgJob?.isActive == false)) {
			isCancelled = true
			onCancelled()
			status = Status.FINISHED
			if (bgJob?.isCompleted == true) {
				CoroutineScope(Dispatchers.IO).launch(Dispatchers.Main) {
					onCancelled(bgJob!!.await())
				}
			}
			preJob?.cancel(CancellationException("PreExecute: Coroutine Task cancelled"))
			bgJob?.cancel(CancellationException("doInBackground: Coroutine Task cancelled"))
			printLog("$taskName has been cancelled.")
		}
	}

	open fun onCancelled() {}

	fun publishProgress(progress: Array<Progress>) {
		//need to update main thread
		CoroutineScope(Dispatchers.IO).launch(Dispatchers.Main) {
			if (!isCancelled) {
				onProgressUpdate(progress)
			}
		}
	}

	private fun printLog(message: String) {
		Log.d(TAG, message)
	}
}