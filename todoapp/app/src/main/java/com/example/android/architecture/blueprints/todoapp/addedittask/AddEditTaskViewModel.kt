/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.addedittask

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.databinding.ObservableField
import android.support.annotation.StringRes
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.SingleLiveEvent
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository

/**
 * ViewModel for the Add/Edit screen.
 *
 *
 * This ViewModel only exposes [ObservableField]s, so it doesn't need to extend
 * [android.databinding.BaseObservable] and updates are notified automatically. See
 * [com.example.android.architecture.blueprints.todoapp.statistics.StatisticsViewModel] for
 * how to deal with more complex scenarios.
 */
class AddEditTaskViewModel(
        context: Application,
        private val tasksRepository: TasksRepository
) : AndroidViewModel(context), TasksDataSource.GetTaskCallback {

    val title = MutableLiveData<String>()
    val description = MutableLiveData<String>()
    val dataLoading = MutableLiveData<Boolean>().apply { value = false }
    internal val snackbarMessage = SingleLiveEvent<Int>()
    internal val taskUpdatedEvent = SingleLiveEvent<Void>()
    private var taskId: String? = null
    private val isNewTask
        get() = taskId == null
    private var isDataLoaded = false
    private var taskCompleted = false

    fun start(taskId: String?) {
        if (dataLoading.value == true) {
            // Already loading, ignore.
            return
        }
        this.taskId = taskId
        if (isNewTask || isDataLoaded) {
            // No need to populate, it's a new task or it already has data
            return
        }
        dataLoading.value = true
        taskId?.let {
            tasksRepository.getTask(it, this)
        }
    }

    override fun onTaskLoaded(task: Task) {
        title.value = task.title
        description.value = task.description
        taskCompleted = task.isCompleted
        dataLoading.value = false
        isDataLoaded = true

        // Note that there's no need to notify that the values changed because we're using
        // ObservableFields.
    }

    override fun onDataNotAvailable() {
        dataLoading.value = false
    }

    // Called when clicking on fab.
    fun saveTask() {
        title.value?.let { title ->
            description.value?.let { description ->
                Task(title, description)
            }
        }?.let { task ->
            if (task.isEmpty) {
                null // fall through to elvis case below
            } else if(isNewTask) {
                createTask(task)
            } else {
                taskId?.let { taskId ->
                    updateTask(Task(task.title, task.description, taskId).apply { isCompleted = taskCompleted })
                }
            }
        } ?: showSnackbarMessage(R.string.empty_task_message)
    }


    private fun createTask(newTask: Task) {
        tasksRepository.saveTask(newTask)
        taskUpdatedEvent.call()
    }

    private fun updateTask(task: Task) {
        if (isNewTask) {
            throw RuntimeException("updateTask() was called but task is new.")
        }
        tasksRepository.saveTask(task)
        taskUpdatedEvent.call()
    }

    private fun showSnackbarMessage(@StringRes message: Int) {
        snackbarMessage.value = message
    }
}
