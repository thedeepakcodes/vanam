package `in`.qwicklabs.vanam.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.LayoutLoaderBinding

class Loader(context: Context) {
    var title: TextView
    var message: TextView
    var dialog: Dialog = Dialog(context)

    init {
        val loader = LayoutLoaderBinding.inflate(LayoutInflater.from(context))
        title = loader.title
        message = loader.message
        dialog.setContentView(loader.root)
        dialog.setCancelable(false)

        dialog.window?.setBackgroundDrawableResource(
            R.drawable.dialog_bg
        );
    }
}