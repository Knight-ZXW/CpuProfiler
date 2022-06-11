package com.knightboost.cpuprofiler.util

import java.lang.reflect.Method

object AndroidSysProperties {

    var getSystemPropertyMethod: Method? = null
    var getLongMethod: Method? = null
    var getIntMethod: Method? = null
    var setMethod: Method? = null

    init {
        try {
            val methods = Class.forName("android.os.SystemProperties").methods
            val methodLength = methods.size
            for (method in methods) {
                val name = method.name
                if ("get" == name) {
                    getSystemPropertyMethod = method
                } else if ("set" == name) {
                    setMethod = method
                } else if ("getLong" == name){
                    getLongMethod = method
                } else if("getInt" == name){
                    getIntMethod = method
                }
            }

        } catch (e: Exception) {
        }
    }

    fun getSystemProperty(property:String,defaultValue:String?):String?{
        val method = getSystemPropertyMethod
        if (method ==null){
            return defaultValue
        }
        return try {
            method.invoke(null, arrayOf(property,defaultValue)) as String?
        }catch (e:Exception){
            defaultValue
        }

        return defaultValue

    }

}