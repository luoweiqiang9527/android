// "Annotate containing class with ''@Parcelize''" "true"
// ERROR: Class 'Foo' should be annotated with '@Parcelize'
// WITH_STDLIB

package com.myapp.activity

import android.os.*
import kotlinx.parcelize.*

object StringParceler : Parceler<String> {
    override fun create(parcel: Parcel) = TODO()
    override fun String.write(parcel: Parcel, flags: Int) = TODO()
}

class Foo(@<caret>TypeParceler<String, StringParceler> val a: String)