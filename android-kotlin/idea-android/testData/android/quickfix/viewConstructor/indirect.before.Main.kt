// "Add Android View constructors using '@JvmOverloads'" "true"
// ERROR: This type has a constructor, and thus must be initialized here
// WITH_STDLIB

package com.myapp.activity

import android.view.TextView

class Foo : TextView<caret>