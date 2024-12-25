package com.example.streamease.fragmentscenes

import androidx.fragment.app.Fragment

open class Scenes:Fragment() {
    open fun onMovedto(){}
    open fun onMovedFrom(){}
    open fun navid(): Int {
        return 0
    }
}