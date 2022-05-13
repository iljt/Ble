package com.example.ble.activity

import androidx.lifecycle.lifecycleScope
import com.example.ble.R
import com.example.ble.base.BaseActivity
import com.example.ble.databinding.ActivityConfigCancmdBinding
import com.me.blelib.ext.logE
import com.me.blelib.ext.toHexString
import com.me.blelib.manager.BleDataListener
import com.me.blelib.manager.BleManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.experimental.and
import kotlin.experimental.xor

class ConfigCanCmdActivity : BaseActivity<ActivityConfigCancmdBinding>() {


    override fun initView() {
        mBinding.commonTitle.setTitle("配置CAN命令")
        mBinding.commonTitle.setLeftIcon(R.mipmap.left_black_icon)
      //  mBinding.hardwareSettingView.setTitle("硬件版本")
    }

    override fun initData() {
        BleManager.instance.setListener(bleDataListener)
    }


    private val bleDataListener = object : BleDataListener() {

        override fun onResponseData(resultBytes: ByteArray) {
            super.onResponseData(resultBytes)
            "设备返回数据: ${resultBytes.toHexString()}".logE()
            if(resultBytes.size==20) {
                if(resultBytes[10] == 0x03.toByte() && resultBytes[11] ==0x00.toByte()){ //seed返回
                    //倒叙
                    // kotlin中，位运算只能是Int 和 Long类型,所以大部分我们都需要通过toInt()或者toLong()方法转为Int或Long类型
                    val seedTemp132=  (resultBytes[15].toInt() shl 24) + (resultBytes[14].toInt() shl 16) + (resultBytes[13].toInt() shl 8) + (resultBytes[12].toInt() shl 0)
                    "seedTemp132= $seedTemp132".logE()

                    val seedTemp32 =  (((resultBytes[15] xor resultBytes[12]) and 0x00ff.toByte()).toInt() shl 24) + (((resultBytes[14] xor resultBytes[12]) and 0x00ff.toByte()).toInt() shl 16) + (((resultBytes[14] xor resultBytes[12]) and 0x00ff.toByte()).toInt() shl 8) + (resultBytes[12].toInt() shl 0)
                    "seedTemp32= $seedTemp32".logE()

                    //真正想要的值 2424879451
                    val randomKey32 = (seedTemp32.toByte() xor 0x20211010.toByte()).toLong()
                    "randomKey32= $randomKey32".logE()

                    //校验key指令funByte=0x04
                    BleManager.instance.sendValidKeyCommand(funByte =0x04, randomKey = randomKey32)
                }
            }else if(resultBytes[10] == 0x04.toByte() && resultBytes[11] ==0x00.toByte()){//校验key指令返回
                //编程日期指令funByte=0x05
                BleManager.instance.sendDateCommand(funByte =0x05)
            }else if(resultBytes[10] == 0x05.toByte() && resultBytes[11] ==0x00.toByte()){//编程日期指令返回
                //数据长度与地址指令
            }
        }

    }
    override fun initListener() {
        mBinding.queryCancmdSettingView.setOnSettingItemListener {
            BleManager.instance.sendBleQueryCommand()
        }

        mBinding.configCancmdSettingView.setOnSettingItemListener {
            BleManager.instance.sendConfigCanCommand()
        }

        //发送boot指令funByte=0x01
        mBinding.burnrecordSettingView.setOnSettingItemListener {
            lifecycleScope .launch {
                for (i in 1..10){
                    BleManager.instance.sendCommand(i.toByte(),0x01)
                    delay(50)
                }
                delay(200)
                //获取软件信息指令funByte=0x02
                BleManager.instance.sendCommand(funByte = 0x02)
                delay(200)
                //请求seed指令funByte=0x03
                BleManager.instance.sendCommand(funByte =0x03)
                //NewGroup/BinaryBleManage
            }

        }
    }


}