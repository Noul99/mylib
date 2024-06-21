package com.lymors.lycommons.data.viewmodels


import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.lymors.lycommons.data.database.MainRepository
import com.lymors.lycommons.extensions.ImageViewExtensions.uploadImageUsingWorkManager
import com.lymors.lycommons.utils.FirebaseUploadWorker
import com.lymors.lycommons.utils.MyExtensions.logT
import com.lymors.lycommons.utils.MyResult
import com.lymors.lycommons.utils.Utils.saveImageToInternalStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.reflect.KProperty


class MainViewModel @Inject constructor(private val mainRepo: MainRepository) : ViewModel() {
    private val _longClickedState = MutableStateFlow<Boolean>(false)
    val longClickedState = _longClickedState.asStateFlow()

    private val _query = MutableStateFlow<String>("")
    val query = _query.asStateFlow()


    private  val _searchingState = MutableStateFlow(false)
    val searchingState = _searchingState.asStateFlow()



    val _anyState = MutableStateFlow(Any())
    val anyState = _anyState.asStateFlow()


    fun setQuery(query:String) {
        _query.value = query
    }

    fun setAnyState(any: Any) {
        _anyState.value = any
    }
    inline fun <reified T> getAnyState(): T {
        return anyState as T
    }
    fun <T> getAnyStateFlow(): StateFlow<T> {
        return anyState as StateFlow<T>
    }
    suspend fun checkExists(path: String): MyResult<String> {
        return mainRepo.checkExists(path)
    }

    suspend fun <T : Any> uploadAnyModel(path: String, model: T): MyResult<String> {
        return mainRepo.uploadAnyModel(path, model)
    }

    suspend fun deleteAnyModel(child: String): MyResult<String> {
        return mainRepo.deleteAnyModel(child)
    }


    val map = HashMap<Class<*>, AlphaModel<*>>()
    suspend fun <T> collectAnyModels(path: String, clazz: Class<T> ,  numberOfItems: Int = 0): StateFlow<List<T>> {
        return suspendCancellableCoroutine { continuation ->
            if (map.containsKey(clazz) && map[clazz]?.path ==path && map[clazz]?.more == numberOfItems ) {
                continuation.resume(map[clazz]?.stateFlow as StateFlow<List<T>>)
            } else {
                val mutableStateFlow = MutableStateFlow<List<T>>(emptyList())
                val stateFlow = mutableStateFlow.asStateFlow()
                val a = AlphaModel(path, mutableStateFlow , numberOfItems )
                a.stateFlow = stateFlow
                map[clazz] = a
                viewModelScope.launch {
                    mainRepo.collectAnyModel(path, clazz,numberOfItems).collect {
                        mutableStateFlow.value = it
                    }
                }
                continuation.resume(map[clazz]?.stateFlow as StateFlow<List<T>>)
            }
        }
    }



    suspend fun  getMap(child: String): MyResult<Map<String, String>> {
        return mainRepo.getMap(child)
    }

    private  val _mapFlow = MutableStateFlow(emptyMap<String,Any>())
    val mapFlow = _mapFlow.asStateFlow()
    fun collectMap(child: String) {
        viewModelScope.launch {
            mainRepo.collectMap<Any>(child).collect{
                _mapFlow.value = it
            }
        }
    }

    suspend fun <T> getAnyData(path: String, clazz: Class<T>): T? {
        return mainRepo.getAnyData(path, clazz)
    }

    suspend fun <T> getModelsWithChildren(path: String, clazz: Class<T>): Flow<List<T>> {
        return mainRepo.getModelsWithChildren(path, clazz)
    }

    suspend fun <T:Any> queryModelByAProperty(path: String, clazz: Class<T>, property: String, value: String):T? {
        return mainRepo.queryModelByAProperty(path, property, value , clazz)
    }

    fun setLongClickedState(longClicked: Boolean) {
        _longClickedState.value = longClicked
    }
    fun setSearchingState(searching: Boolean) {
        _searchingState.value = searching
    }

    fun <T> collectSingleModel(path: String, clazz: Class<T>): StateFlow<T> {
        val _singleModelState = MutableStateFlow(null as T) // Initialize with null
        val aModelState: StateFlow<T> = _singleModelState.asStateFlow()

        viewModelScope.launch {
            mainRepo.collectAModel(path, clazz).collect { model ->
                _singleModelState.value = model // Update value
            }
        }
        return aModelState
    }

    suspend fun <P> uploadModelWithImage(context: Context, realTimePath: String, model: Any, imageUri: String, property: KProperty<P>): MyResult<String> {
        return viewModelScope.async {
            val modelKeyResult = uploadAnyModel(realTimePath, model)
            if (modelKeyResult is MyResult.Error) {
                return@async modelKeyResult // Return the error result immediately
            }

           modelKeyResult.whenSuccess {
                val imagePath = "$realTimePath/$it/${property.name}"
               uploadImageUsingWorkManager(context, imageUri, imagePath)
            }
            return@async MyResult.Success("Data uploaded successfully")
        }.await()
    }



}


data class AlphaModel<T>(
    var path: String,
    var _stateFlow:MutableStateFlow<List<T>>,
    var more:Int = 0
    ){
    var stateFlow:StateFlow<List<T>> = _stateFlow.asStateFlow()
}
