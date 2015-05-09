package com.timepath.compiler

import com.timepath.compiler.types.Type

public data class Value(val any: Any) {

    fun toBoolean(): Boolean = false

    fun plus(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs + rhs)
                is Int -> return Value(lhs + rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs + rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    fun minus(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs - rhs)
                is Int -> return Value(lhs - rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs - rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    fun times(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs * rhs)
                is Int -> return Value(lhs * rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs * rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    fun div(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs / rhs)
                is Int -> return Value(lhs / rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs / rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    fun mod(other: Value): Value {
        val lhs = any
        val rhs = other.any
        when (lhs) {
            is Float -> when (rhs) {
                is Float -> return Value(lhs % rhs)
                is Int -> return Value(lhs % rhs)
            }
            is Int -> when (rhs) {
                is Int -> return Value(lhs % rhs)
            }
        }
        throw UnsupportedOperationException("not supported")
    }

    fun cast(type: Type): Value {
        // TODO
        //        val lhs = any
        //        when (lhs) {
        //            is Number -> when (type) {
        //                is float_t -> return Value(lhs.toFloat())
        //                is int_t -> return Value(lhs.toInt())
        //            }
        //        }
        throw UnsupportedOperationException("not supported")
    }

    fun minus() = Value(0) - this
}
