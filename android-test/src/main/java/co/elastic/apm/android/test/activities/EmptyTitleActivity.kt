package co.elastic.apm.android.test.activities

import android.app.Activity
import android.os.Bundle

class EmptyTitleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""
    }
}