package com.chazlakinger.typescontrol

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigNoInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class TurnOnActionHelper(config: TaskerPluginConfig<Unit>) : TaskerPluginConfigHelperNoOutputOrInput<TurnOnRunner>(config) {
    override val runnerClass: Class<TurnOnRunner> get() = TurnOnRunner::class.java
    override fun addToStringBlurb(input: TaskerInput<Unit>, blurbBuilder: StringBuilder) {
        blurbBuilder.append("Turn on underglow")
    }
}

class ActivityConfigTurnOn : Activity(), TaskerPluginConfigNoInput {
    override val context get() = applicationContext
    private val taskerHelper by lazy { TurnOnActionHelper(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskerHelper.finishForTasker()
    }
}

class TurnOnRunner : TaskerPluginRunnerActionNoOutputOrInput(), BluetoothHelper.BluetoothEventListener {
    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        val bluetoothHelper = BluetoothHelper(context)
        bluetoothHelper.connectAndTurnOn()
        return TaskerPluginResultSucess()
    }

    override fun deviceConnectionChange(device: DeviceStatus) {
        //no-op
    }

    override fun deviceStateChanged(device: DeviceStatus) {
        //no-op
    }
}