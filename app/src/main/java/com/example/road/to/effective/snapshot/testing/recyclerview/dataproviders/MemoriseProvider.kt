package com.example.road.to.effective.snapshot.testing.recyclerview.dataproviders

import com.example.road.to.effective.snapshot.testing.recyclerview.data.Memorise

interface MemoriseProvider {
    fun getMemorises(): List<Memorise>
}