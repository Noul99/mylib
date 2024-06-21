package com.lymors.lycommons.managers

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.checkerframework.checker.units.qual.A


class WorkManagerDocs:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

//        // this is how to make a one time work-request
        val workManager = WorkManager.getInstance(this)
//        workManager.enqueue(OneTimeWorkRequest.Builder(MyWorkManager::class.java).build())



        val workRequest = OneTimeWorkRequestBuilder<MyWorkManager>().build()
        WorkManager.getInstance(this).enqueue(workRequest)

        val statusLiveData: LiveData<WorkInfo> = workManager.getWorkInfoByIdLiveData(workRequest.id)

//        //        You can also use the id for cancellation:
//        workManager.cancelWorkById(workRequest.id);

        statusLiveData.observe(this) { workInfo ->
            if (workInfo != null) {
                val status = workInfo.state
                val outputData = workInfo.outputData
                // Handle work status and output data based on changes
            }
        }






//        You can chain work as follows:
        val request1 = OneTimeWorkRequest.Builder(MyWorkManager::class.java).build()
        val request2 = OneTimeWorkRequest.Builder(MyWorkManager::class.java).build()
        val request3 = OneTimeWorkRequest.Builder(MyWorkManager::class.java).build()

        workManager.beginWith(listOf(request1, request2)).then(request3).enqueue()






    }

}